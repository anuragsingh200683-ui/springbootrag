package com.example.employeemanagement.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Filters extracted from a natural-language query - shaped to line up 1:1 with
 * the query params GET /api/ems/employees already accepts (keyword/status),
 * plus department/designation *names* (the frontend resolves those to ids,
 * since this DTO doesn't have DB access to look them up).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NlSearchFiltersDto {
    private String keyword;
    private String departmentName;
    private String designationName;
    private String status;
}
