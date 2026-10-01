package com.example.employeemanagement.aiassistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    /** "user" or "assistant" */
    @NotBlank
    private String role;

    @NotBlank
    private String content;
}
