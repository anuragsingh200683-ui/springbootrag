package com.example.employeemanagement.designation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * What the API returns for a Designation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignationResponse {

    private Long id;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
