package com.example.employeemanagement.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NlSearchResponse {
    private NlSearchFiltersDto filters;
    private String explanation;
    private String model;
}
