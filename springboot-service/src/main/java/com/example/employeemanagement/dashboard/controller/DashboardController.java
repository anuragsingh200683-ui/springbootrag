package com.example.employeemanagement.dashboard.controller;

import com.example.employeemanagement.dashboard.dto.DashboardSummaryResponse;
import com.example.employeemanagement.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only aggregation API backing the EMS dashboard.
 * Base path: /api/ems/dashboard (permitAll - see SecurityConfig).
 */
@RestController
@RequestMapping("/api/ems/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregated headcount, attendance and leave stats")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get the dashboard summary",
            description = "Headcount by status/department, today's attendance snapshot, and leave counts by status.")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
