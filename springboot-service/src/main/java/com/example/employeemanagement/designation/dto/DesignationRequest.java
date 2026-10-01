package com.example.employeemanagement.designation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for creating/updating a Designation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DesignationRequest {

    @Schema(example = "Senior Software Engineer")
    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;

    @Schema(example = "Individual contributor, technical leadership track")
    @Size(max = 500, message = "description must be at most 500 characters")
    private String description;
}
