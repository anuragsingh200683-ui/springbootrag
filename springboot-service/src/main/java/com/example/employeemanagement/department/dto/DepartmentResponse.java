package com.example.employeemanagement.department.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * What the API returns for a Department. Never expose the entity directly.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResponse {

    private Long id;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
