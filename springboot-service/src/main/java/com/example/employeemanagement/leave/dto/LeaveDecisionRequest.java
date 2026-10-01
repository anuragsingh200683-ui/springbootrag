package com.example.employeemanagement.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Used for both approve and reject - there's no Spring Security principal to
 * pull the approver from, so it's passed explicitly.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveDecisionRequest {

    @NotBlank(message = "decidedBy is required")
    @Size(max = 150)
    private String decidedBy;

    @Size(max = 500)
    private String remarks;
}
