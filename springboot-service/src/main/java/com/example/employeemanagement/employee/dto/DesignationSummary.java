package com.example.employeemanagement.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Minimal Designation projection embedded inside EmployeeResponse.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignationSummary {
    private Long id;
    private String name;
}
