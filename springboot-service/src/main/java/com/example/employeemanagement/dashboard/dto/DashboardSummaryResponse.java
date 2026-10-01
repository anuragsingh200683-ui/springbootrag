package com.example.employeemanagement.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Single aggregate payload backing the EMS dashboard landing page. Everything
 * here is read-only and computed on the fly from the other modules' repositories
 * - there is no dashboard table.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private LocalDate asOf;

    // Headcount
    private long totalEmployees;
    private long activeEmployees;
    private long inactiveEmployees;
    private long totalDepartments;
    private long totalDesignations;
    private List<DepartmentHeadcount> employeesByDepartment;

    // Today's attendance snapshot
    private long checkedInToday;
    private long checkedOutToday;
    private long notCheckedInToday;

    // Leave
    private long pendingLeaves;
    private long approvedLeaves;
    private long rejectedLeaves;
}
