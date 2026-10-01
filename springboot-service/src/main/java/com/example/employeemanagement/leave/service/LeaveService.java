package com.example.employeemanagement.leave.service;

import com.example.employeemanagement.common.dto.PagedResponse;
import com.example.employeemanagement.leave.dto.LeaveApplyRequest;
import com.example.employeemanagement.leave.dto.LeaveDecisionRequest;
import com.example.employeemanagement.leave.dto.LeaveResponse;
import com.example.employeemanagement.leave.entity.LeaveStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface LeaveService {

    LeaveResponse applyLeave(LeaveApplyRequest request);

    LeaveResponse approveLeave(Long id, LeaveDecisionRequest request);

    LeaveResponse rejectLeave(Long id, LeaveDecisionRequest request);

    LeaveResponse getLeaveById(Long id);

    /** employeeId/status/from/to are all optional filters (null = no filter). */
    PagedResponse<LeaveResponse> getLeaveHistory(Long employeeId, LeaveStatus status, LocalDate from, LocalDate to, Pageable pageable);
}
