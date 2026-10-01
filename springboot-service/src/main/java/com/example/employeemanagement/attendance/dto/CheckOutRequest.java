package com.example.employeemanagement.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckOutRequest {

    @NotNull(message = "employeeId is required")
    private Long employeeId;
}
