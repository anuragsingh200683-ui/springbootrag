package com.example.employeemanagement.leave.mapper;

import com.example.employeemanagement.leave.dto.LeaveResponse;
import com.example.employeemanagement.leave.entity.LeaveApplication;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;

@Component
public class LeaveMapper {

    public LeaveResponse toResponse(LeaveApplication leave) {
        return LeaveResponse.builder()
                .id(leave.getId())
                .employeeId(leave.getEmployee().getId())
                .employeeName(leave.getEmployee().getFirstName() + " " + leave.getEmployee().getLastName())
                .leaveType(leave.getLeaveType())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .numberOfDays(ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1)
                .reason(leave.getReason())
                .status(leave.getStatus())
                .decidedBy(leave.getDecidedBy())
                .decisionRemarks(leave.getDecisionRemarks())
                .appliedAt(leave.getAppliedAt())
                .decidedAt(leave.getDecidedAt())
                .build();
    }
}
