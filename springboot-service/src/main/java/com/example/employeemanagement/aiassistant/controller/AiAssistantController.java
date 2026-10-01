package com.example.employeemanagement.aiassistant.controller;

import com.example.employeemanagement.aiassistant.dto.*;
import com.example.employeemanagement.aiassistant.service.AiAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for the AI Assistant module. Proxies to fastapi-service, enriching
 * requests with employee data pulled from this service's own database first.
 * Base path: /api/ems/ai (permitAll - see SecurityConfig).
 */
@RestController
@RequestMapping("/api/ems/ai")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "Employee summary, profile generation, HR chatbot, natural-language search")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @GetMapping("/employees/{id}/summary")
    @Operation(summary = "Generate a short AI summary of an employee")
    public ResponseEntity<EmployeeSummaryResponse> getEmployeeSummary(@PathVariable Long id) {
        return ResponseEntity.ok(aiAssistantService.getEmployeeSummary(id));
    }

    @PostMapping("/employees/{id}/profile")
    @Operation(summary = "Generate a narrative AI profile of an employee")
    public ResponseEntity<ProfileGenerateResponse> generateEmployeeProfile(
            @PathVariable Long id, @Valid @RequestBody(required = false) ProfileGenerateRequest request) {
        return ResponseEntity.ok(aiAssistantService.generateEmployeeProfile(
                id, request != null ? request : new ProfileGenerateRequest()));
    }

    @PostMapping("/chat")
    @Operation(summary = "Send a message to the HR chatbot")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(aiAssistantService.chat(request));
    }

    @PostMapping("/search")
    @Operation(summary = "Translate a natural-language query into employee search filters",
            description = "Returns filters compatible with GET /api/ems/employees (keyword/status). "
                    + "departmentName/designationName must be resolved to ids by the caller.")
    public ResponseEntity<NlSearchResponse> naturalLanguageSearch(@Valid @RequestBody NlSearchRequest request) {
        return ResponseEntity.ok(aiAssistantService.naturalLanguageSearch(request));
    }
}
