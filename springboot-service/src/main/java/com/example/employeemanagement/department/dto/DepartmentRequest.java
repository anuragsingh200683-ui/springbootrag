package com.example.employeemanagement.department.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for creating/updating a Department. Kept separate from the entity
 * so the API contract doesn't leak persistence details (id, timestamps).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequest {

    @Schema(example = "Engineering")
    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;

    @Schema(example = "Builds and maintains the product")
    @Size(max = 500, message = "description must be at most 500 characters")
    private String description;
}
