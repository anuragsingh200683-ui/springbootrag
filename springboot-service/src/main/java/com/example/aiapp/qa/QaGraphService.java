package com.example.aiapp.qa;

import com.example.aiapp.ai.ChatCompletionClient;
import com.example.aiapp.ai.OpenAiEmbeddingClient;
import com.example.aiapp.entity.DocumentStatus;
import com.example.aiapp.exception.LlmServiceException;
import com.example.aiapp.exception.NoRelevantContextException;
import com.example.aiapp.ingest.SemanticSearchResult;
import com.example.aiapp.ingest.VectorStoreRepository;
import com.example.aiapp.repository.DocumentRepository;
import com.example.aiapp.repository.QaHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-process replacement for fastapi-service's LangGraph-routed POST /api/qa/query:
 * classify the question into one of five routes, run that route's branch, then generate
 * the final answer. Ported from fastapi-service/qa_graph/{graph,nodes,tools}.py - see
 * those files for the original design notes this mirrors.
 */
@Service
public class QaGraphService {

    private static final Logger log = LoggerFactory.getLogger(QaGraphService.class);

    private static final String CLASSIFIER_SYSTEM_PROMPT =
            "You are a routing classifier for a document Q&A application. "
                    + "Classify the user's question into exactly one category: "
                    + "rag, database, rest_api, tool, or general. "
                    + "rag: question about the content of an uploaded document. "
                    + "database: question about app metadata, e.g. how many documents have been "
                    + "uploaded/processed, or how many questions have been asked. "
                    + "rest_api: question needing live external data (e.g. weather). "
                    + "tool: an arithmetic calculation, or a request for the current date/time. "
                    + "general: anything else - general knowledge or chit-chat. "
                    + "Respond with only a JSON object of the form {\"category\": \"<one of the five values above>\"}.";

    private static final String TOOL_SYSTEM_PROMPT = "Use the available tools to answer the question precisely.";

    private static final Map<RouteCategory, String> ANSWER_SYSTEM_PROMPTS = Map.of(
            RouteCategory.RAG, "You are a helpful assistant answering questions about a user-uploaded document. "
                    + "Only use the provided context to answer. If the context does not contain the answer, "
                    + "say you don't have enough information in the document rather than guessing.",
            RouteCategory.DATABASE, "You are a helpful assistant. Use the provided database result to answer the question concisely.",
            RouteCategory.REST_API, "You are a helpful assistant. Use the provided data to answer the question concisely.",
            RouteCategory.TOOL, "You are a helpful assistant. Use the provided tool result to answer the question concisely.",
            RouteCategory.GENERAL, "You are a helpful, general-purpose assistant. Answer the question directly and concisely.");

    private static final String REST_API_PLACEHOLDER =
            "This is a placeholder REST API route - no external call is wired in yet. "
                    + "Replace QaGraphService.restApiBranch() with a real HTTP call when you have an API to integrate.";

    private final ChatCompletionClient chatCompletionClient;
    private final OpenAiEmbeddingClient embeddingClient;
    private final VectorStoreRepository vectorStoreRepository;
    private final DocumentRepository documentRepository;
    private final QaHistoryRepository qaHistoryRepository;
    private final QaTools qaTools;
    private final ObjectMapper objectMapper;
    private final int defaultTopK;

    public QaGraphService(ChatCompletionClient chatCompletionClient,
                           OpenAiEmbeddingClient embeddingClient,
                           VectorStoreRepository vectorStoreRepository,
                           DocumentRepository documentRepository,
                           QaHistoryRepository qaHistoryRepository,
                           QaTools qaTools,
                           ObjectMapper objectMapper,
                           @Value("${app.qa.top-k}") int defaultTopK) {
        this.chatCompletionClient = chatCompletionClient;
        this.embeddingClient = embeddingClient;
        this.vectorStoreRepository = vectorStoreRepository;
        this.documentRepository = documentRepository;
        this.qaHistoryRepository = qaHistoryRepository;
        this.qaTools = qaTools;
        this.objectMapper = objectMapper;
        this.defaultTopK = defaultTopK;
    }

    public QaAnswer answer(String question, UUID documentId, Integer topK) {
        RouteCategory category = classify(question, documentId != null);
        log.info("Classifier routed question to category={}", category);

        List<SemanticSearchResult> sources = List.of();
        boolean noContext = false;
        String context;

        switch (category) {
            case RAG -> {
                sources = ragBranch(question, documentId, topK != null ? topK : defaultTopK);
                noContext = sources.isEmpty();
                context = noContext
                        ? "(no context retrieved)"
                        : sources.stream()
                                .map(SemanticSearchResult::chunkText)
                                .reduce((a, b) -> a + "\n\n---\n\n" + b)
                                .orElse("");
            }
            case DATABASE -> context = databaseBranch(question);
            case REST_API -> context = REST_API_PLACEHOLDER;
            case TOOL -> context = toolBranch(question);
            default -> context = "";
        }

        if (category == RouteCategory.RAG && noContext) {
            throw new NoRelevantContextException("No relevant context was found to answer this question.");
        }

        String answer = generateAnswer(category, question, context);
        return new QaAnswer(question, answer, sources, chatCompletionClient.modelName());
    }

    private RouteCategory classify(String question, boolean hasDocument) {
        String contextHint = hasDocument ? "A document IS selected for this question." : "No document is selected.";
        try {
            String json = chatCompletionClient.completeJson(CLASSIFIER_SYSTEM_PROMPT, contextHint + "\n\nQuestion: " + question);
            JsonNode node = objectMapper.readTree(json);
            return RouteCategory.fromWireValue(node.path("category").asText(null));
        } catch (Exception e) {
            // Fail safe rather than failing the request: default to the behavior the app
            // had before this graph existed (always RAG when a document is selected), so
            // a classifier hiccup never breaks the primary use case.
            log.warn("Classifier call failed, falling back to a safe default category", e);
            return hasDocument ? RouteCategory.RAG : RouteCategory.GENERAL;
        }
    }

    private List<SemanticSearchResult> ragBranch(String question, UUID documentId, int topK) {
        float[] queryEmbedding = embeddingClient.embed(question);
        return vectorStoreRepository.semanticSearch(documentId, queryEmbedding, topK);
    }

    private String databaseBranch(String question) {
        String questionLower = question.toLowerCase();
        try {
            long count;
            String template;
            if (containsAny(questionLower, "processed", "indexed")) {
                count = documentRepository.countByStatus(DocumentStatus.PROCESSED);
                template = "%d document(s) have finished processing.";
            } else if (containsAny(questionLower, "how many question", "number of question", "qa history",
                    "questions asked", "questions have")) {
                count = qaHistoryRepository.count();
                template = "%d question(s) have been asked so far.";
            } else {
                count = documentRepository.count();
                template = "There are %d document(s) uploaded in total.";
            }
            return template.formatted(count);
        } catch (Exception e) {
            log.warn("Database route query failed", e);
            return "I couldn't read that information from the database right now.";
        }
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String toolBranch(String question) {
        ChatCompletionClient.ToolTurnResult result =
                chatCompletionClient.completeWithTools(TOOL_SYSTEM_PROMPT, question, QaTools.definitions());

        if (result.toolCalls().isEmpty()) {
            String direct = result.directAnswer();
            return (direct == null || direct.isBlank()) ? "No tool was needed for this question." : direct;
        }

        StringBuilder outputs = new StringBuilder();
        for (ChatCompletionClient.ToolCall call : result.toolCalls()) {
            if (outputs.length() > 0) {
                outputs.append('\n');
            }
            outputs.append(executeTool(call));
        }
        return outputs.toString();
    }

    private String executeTool(ChatCompletionClient.ToolCall call) {
        try {
            JsonNode args = objectMapper.readTree(call.argumentsJson());
            return switch (call.name()) {
                case QaTools.CALCULATOR -> qaTools.calculate(args.path("expression").asText(""));
                case QaTools.CURRENT_DATETIME -> qaTools.currentDateTime();
                default -> "Unknown tool requested: " + call.name();
            };
        } catch (Exception e) {
            log.warn("Tool '{}' execution failed", call.name(), e);
            return "Tool '" + call.name() + "' failed: " + e.getMessage();
        }
    }

    private String generateAnswer(RouteCategory category, String question, String context) {
        String systemPrompt = ANSWER_SYSTEM_PROMPTS.get(category);
        String humanContent = category == RouteCategory.GENERAL
                ? question
                : "Context:\n\n" + context + "\n\nQuestion: " + question + "\n\nAnswer using only the context above.";
        try {
            String answer = chatCompletionClient.complete(systemPrompt, humanContent).strip();
            return answer.isBlank() ? "The model did not return a text response." : answer;
        } catch (Exception e) {
            log.error("Answer generation LLM call failed", e);
            throw new LlmServiceException(e.getMessage(), e);
        }
    }
}
