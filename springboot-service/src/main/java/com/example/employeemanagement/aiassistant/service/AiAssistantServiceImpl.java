package com.example.employeemanagement.aiassistant.service;

import com.example.employeemanagement.aiassistant.dto.*;
import com.example.employeemanagement.common.exception.ResourceNotFoundException;
import com.example.employeemanagement.employee.entity.Employee;
import com.example.employeemanagement.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves employee data from the EMS database and generates the actual AI response
 * in-process via EmsAiPromptService (previously proxied to fastapi-service).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiAssistantServiceImpl implements AiAssistantService {

    private final EmployeeRepository employeeRepository;
    private final EmsAiPromptService emsAiPromptService;

    @Override
    public EmployeeSummaryResponse getEmployeeSummary(Long employeeId) {
        Employee employee = resolveEmployee(employeeId);
        String summary = emsAiPromptService.summarize(employee);

        return EmployeeSummaryResponse.builder()
                .employeeId(employeeId)
                .summary(summary)
                .model(emsAiPromptService.modelName())
                .build();
    }

    @Override
    public ProfileGenerateResponse generateEmployeeProfile(Long employeeId, ProfileGenerateRequest request) {
        Employee employee = resolveEmployee(employeeId);
        String profile = emsAiPromptService.generateProfile(employee, request.getTone());

        return ProfileGenerateResponse.builder()
                .employeeId(employeeId)
                .profile(profile)
                .model(emsAiPromptService.modelName())
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        Employee employeeContext = request.getEmployeeId() != null ? resolveEmployee(request.getEmployeeId()) : null;
        String reply = emsAiPromptService.chat(request.getMessage(), employeeContext, request.getHistory());

        return ChatResponse.builder()
                .reply(reply)
                .model(emsAiPromptService.modelName())
                .build();
    }

    @Override
    public NlSearchResponse naturalLanguageSearch(NlSearchRequest request) {
        NlSearchResult result = emsAiPromptService.naturalLanguageSearch(request.getQuery());

        return NlSearchResponse.builder()
                .filters(NlSearchFiltersDto.builder()
                        .keyword(result.keyword())
                        .departmentName(result.departmentName())
                        .designationName(result.designationName())
                        .status(result.status())
                        .build())
                .explanation(result.explanation())
                .model(emsAiPromptService.modelName())
                .build();
    }

    private Employee resolveEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> ResourceNotFoundException.forId("Employee", employeeId));
    }
}
