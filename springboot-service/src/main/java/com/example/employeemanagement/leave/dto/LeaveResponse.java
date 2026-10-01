package com.example.employeemanagement.leave.dto;

import com.example.employeemanagement.leave.entity.LeaveStatus;
import com.example.employeemanagement.leave.entity.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    /** Inclusive day count between startDate and endDate. */
    private long numberOfDays;
    private String reason;
    private LeaveStatus status;
    private String decidedBy;
    private String decisionRemarks;
    private Instant appliedAt;
    private Instant decidedAt;
}
