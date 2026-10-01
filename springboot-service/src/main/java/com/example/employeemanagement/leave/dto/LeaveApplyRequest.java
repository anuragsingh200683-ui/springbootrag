package com.example.employeemanagement.leave.dto;

import com.example.employeemanagement.leave.entity.LeaveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApplyRequest {

    @NotNull(message = "employeeId is required")
    private Long employeeId;

    @NotNull(message = "leaveType is required")
    private LeaveType leaveType;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;

    @NotBlank(message = "reason is required")
    @Size(max = 500)
    private String reason;
}
