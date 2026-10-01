package com.example.employeemanagement.aiassistant.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileGenerateRequest {

    /** e.g. professional, friendly, formal. Defaults to "professional" if omitted. */
    @Size(max = 30)
    private String tone;
}
