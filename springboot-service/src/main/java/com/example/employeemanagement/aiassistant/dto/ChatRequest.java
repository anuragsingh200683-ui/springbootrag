package com.example.employeemanagement.aiassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "message is required")
    @Size(max = 2000)
    private String message;

    /** Optional - grounds the chatbot's answer in a specific employee's data. */
    private Long employeeId;

    /** Optional prior turns, oldest first. */
    private List<ChatMessageDto> history;
}
