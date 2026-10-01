package com.example.employeemanagement.aiassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NlSearchRequest {

    @NotBlank(message = "query is required")
    @Size(max = 500)
    private String query;
}
