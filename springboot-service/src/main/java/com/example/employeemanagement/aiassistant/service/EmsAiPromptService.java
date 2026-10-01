package com.example.employeemanagement.aiassistant.service;

import com.example.aiapp.ai.ChatCompletionClient;
import com.example.employeemanagement.aiassistant.dto.ChatMessageDto;
import com.example.employeemanagement.common.exception.AiServiceException;
import com.example.employeemanagement.employee.entity.Employee;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In-process replacement for fastapi-service's /api/ems/ai/* endpoints, reusing the
 * same OpenAI chat client the AI Document Q&A pipeline uses (com.example.aiapp.ai).
 * Prompts are ported verbatim from fastapi-service/app/employeemanagement/routers/*.py
 * and services/prompt_utils.py.
 */
@Service
public class EmsAiPromptService {

    private static final Logger log = LoggerFactory.getLogger(EmsAiPromptService.class);

    private static final String SUMMARY_SYSTEM_PROMPT =
            "You are an HR assistant. Write a concise, factual 2-3 sentence summary of the "
                    + "employee described below, suitable for a manager skimming a team roster. "
                    + "Only state facts given to you - never invent details such as skills, "
                    + "performance, or personality traits that were not provided.";

    private static final String CHAT_SYSTEM_PROMPT =
            "You are an HR helpdesk chatbot for an Employee Management System. Answer the "
                    + "user's question helpfully and concisely. If employee context is provided, you "
                    + "may reference it. Do not make up company policies, salary figures, or personal "
                    + "data you were not given - say you don't have that information instead. Keep "
                    + "answers under 150 words unless the user explicitly asks for more detail.";

    private static final String NL_SEARCH_SYSTEM_PROMPT =
            "You translate a natural-language employee search request into structured search "
                    + "filters for an Employee Management System. Respond with ONLY a single JSON object - "
                    + "no prose, no markdown code fences - matching exactly this shape:\n"
                    + "{\"keyword\": string|null, \"departmentName\": string|null, \"designationName\": string|null, "
                    + "\"status\": \"ACTIVE\"|\"INACTIVE\"|null, \"explanation\": string}\n"
                    + "keyword should capture a person's name if one is mentioned, else null. departmentName/"
                    + "designationName should be the department or job title mentioned, in natural casing "
                    + "(e.g. \"Engineering\", \"Senior Software Engineer\"), or null if none is mentioned. status "
                    + "should be ACTIVE or INACTIVE only if the user clearly asks for active/inactive/former "
                    + "employees, else null. explanation is one short sentence describing the filters you applied.";

    private static final int MAX_HISTORY_TURNS = 10;

    private final ChatCompletionClient chatCompletionClient;
    private final ObjectMapper objectMapper;

    public EmsAiPromptService(ChatCompletionClient chatCompletionClient, ObjectMapper objectMapper) {
        this.chatCompletionClient = chatCompletionClient;
        this.objectMapper = objectMapper;
    }

    public String modelName() {
        return chatCompletionClient.modelName();
    }

    public String summarize(Employee employee) {
        return safeComplete(SUMMARY_SYSTEM_PROMPT, describeEmployee(employee));
    }

    public String generateProfile(Employee employee, String tone) {
        String effectiveTone = (tone == null || tone.isBlank()) ? "professional" : tone;
        String systemPrompt = "You are an HR assistant writing a short employee directory profile in a "
                + effectiveTone + " tone. Use only the facts given below - never invent skills, achievements, "
                + "or personal details that were not provided. Write 3-5 sentences, no headings or bullet points.";
        return safeComplete(systemPrompt, describeEmployee(employee));
    }

    public String chat(String message, Employee employeeContext, List<ChatMessageDto> history) {
        List<String> parts = new ArrayList<>();
        if (employeeContext != null) {
            parts.add("Employee context:\n" + describeEmployee(employeeContext));
        }
        if (history != null && !history.isEmpty()) {
            int from = Math.max(0, history.size() - MAX_HISTORY_TURNS);
            String historyText = history.subList(from, history.size()).stream()
                    .map(m -> m.getRole() + ": " + m.getContent())
                    .collect(Collectors.joining("\n"));
            parts.add("Conversation so far:\n" + historyText);
        }
        parts.add("User: " + message);
        return safeComplete(CHAT_SYSTEM_PROMPT, String.join("\n\n", parts));
    }

    public NlSearchResult naturalLanguageSearch(String query) {
        String raw = safeCompleteJson(NL_SEARCH_SYSTEM_PROMPT, query);
        try {
            JsonNode node = objectMapper.readTree(stripCodeFences(raw));
            return new NlSearchResult(
                    textOrNull(node, "keyword"),
                    textOrNull(node, "departmentName"),
                    textOrNull(node, "designationName"),
                    textOrNull(node, "status"),
                    node.path("explanation").asText("Applied the filters shown above."));
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("NL search: could not parse model output as JSON: {}", raw);
            throw new AiServiceException("Could not parse AI response as JSON: " + e.getMessage(), e);
        }
    }

    /** Every OpenAI call in this service is wrapped the same way fastapi-service's
     *  llm_client.generate() wrapped every provider call: any failure becomes a single
     *  AiServiceException, mapped to HTTP 502 by EmsGlobalExceptionHandler. */
    private String safeComplete(String systemPrompt, String userPrompt) {
        try {
            return chatCompletionClient.complete(systemPrompt, userPrompt);
        } catch (Exception e) {
            log.error("OpenAI call failed (EMS AI assistant)", e);
            throw new AiServiceException(e.getMessage(), e);
        }
    }

    private String safeCompleteJson(String systemPrompt, String userPrompt) {
        try {
            return chatCompletionClient.completeJson(systemPrompt, userPrompt);
        } catch (Exception e) {
            log.error("OpenAI call failed (EMS AI assistant)", e);
            throw new AiServiceException(e.getMessage(), e);
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return (value.isMissingNode() || value.isNull()) ? null : value.asText();
    }

    /** Defensive: response_format=json_object should already guarantee raw JSON, but strip
     *  markdown fences too in case the model adds them anyway. */
    private String stripCodeFences(String raw) {
        String text = raw.strip();
        if (text.startsWith("```")) {
            text = text.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").strip();
        }
        return text;
    }

    private String describeEmployee(Employee employee) {
        List<String> lines = new ArrayList<>();
        lines.add("Name: " + employee.getFirstName() + " " + employee.getLastName());
        if (employee.getDesignation() != null && employee.getDesignation().getName() != null) {
            lines.add("Designation: " + employee.getDesignation().getName());
        }
        if (employee.getDepartment() != null && employee.getDepartment().getName() != null) {
            lines.add("Department: " + employee.getDepartment().getName());
        }
        if (employee.getDateOfJoining() != null) {
            lines.add("Date of joining: " + employee.getDateOfJoining());
        }
        if (employee.getStatus() != null) {
            lines.add("Status: " + employee.getStatus());
        }
        if (employee.getEmail() != null) {
            lines.add("Email: " + employee.getEmail());
        }
        if (employee.getPhone() != null) {
            lines.add("Phone: " + employee.getPhone());
        }
        if (employee.getAddress() != null) {
            lines.add("Address: " + employee.getAddress());
        }
        if (employee.getSalary() != null) {
            lines.add("Salary: " + employee.getSalary());
        }
        return String.join("\n", lines);
    }
}
