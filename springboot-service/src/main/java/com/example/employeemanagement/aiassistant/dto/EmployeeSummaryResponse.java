package com.example.employeemanagement.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSummaryResponse {
    private Long employeeId;
    private String summary;
    private String model;
}
