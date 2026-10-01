package com.example.employeemanagement.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Minimal Department projection embedded inside EmployeeResponse - avoids
 * pulling in the full department.dto.DepartmentResponse (description/timestamps
 * aren't relevant in this context).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentSummary {
    private Long id;
    private String name;
}
