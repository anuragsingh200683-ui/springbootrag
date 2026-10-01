package com.example.employeemanagement.aiassistant.service;

import com.example.employeemanagement.aiassistant.dto.*;

public interface AiAssistantService {

    EmployeeSummaryResponse getEmployeeSummary(Long employeeId);

    ProfileGenerateResponse generateEmployeeProfile(Long employeeId, ProfileGenerateRequest request);

    ChatResponse chat(ChatRequest request);

    NlSearchResponse naturalLanguageSearch(NlSearchRequest request);
}
