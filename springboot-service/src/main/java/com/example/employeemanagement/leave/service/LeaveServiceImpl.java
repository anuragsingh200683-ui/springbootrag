package com.example.employeemanagement.leave.service;

import com.example.employeemanagement.common.dto.PagedResponse;
import com.example.employeemanagement.common.exception.InvalidRequestException;
import com.example.employeemanagement.common.exception.ResourceNotFoundException;
import com.example.employeemanagement.employee.entity.Employee;
import com.example.employeemanagement.employee.repository.EmployeeRepository;
import com.example.employeemanagement.leave.dto.LeaveApplyRequest;
import com.example.employeemanagement.leave.dto.LeaveDecisionRequest;
import com.example.employeemanagement.leave.dto.LeaveResponse;
import com.example.employeemanagement.leave.entity.LeaveApplication;
import com.example.employeemanagement.leave.entity.LeaveStatus;
import com.example.employeemanagement.leave.mapper.LeaveMapper;
import com.example.employeemanagement.leave.repository.LeaveApplicationRepository;
import com.example.employeemanagement.leave.specification.LeaveApplicationSpecification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class LeaveServiceImpl implements LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveServiceImpl.class);

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveMapper leaveMapper;

    @Override
    public LeaveResponse applyLeave(LeaveApplyRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidRequestException("endDate cannot be before startDate");
        }
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> ResourceNotFoundException.forId("Employee", request.getEmployeeId()));

        LeaveApplication leave = LeaveApplication.builder()
                .employee(employee)
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .status(LeaveStatus.PENDING)
                .build();

        LeaveApplication saved = leaveApplicationRepository.save(leave);
        log.info("Employee id={} applied for leave id={} ({} to {})", employee.getId(), saved.getId(),
                saved.getStartDate(), saved.getEndDate());
        return leaveMapper.toResponse(saved);
    }

    @Override
    public LeaveResponse approveLeave(Long id, LeaveDecisionRequest request) {
        return decide(id, request, LeaveStatus.APPROVED);
    }

    @Override
    public LeaveResponse rejectLeave(Long id, LeaveDecisionRequest request) {
        return decide(id, request, LeaveStatus.REJECTED);
    }

    private LeaveResponse decide(Long id, LeaveDecisionRequest request, LeaveStatus newStatus) {
        LeaveApplication leave = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Leave application", id));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidRequestException(
                    "Leave application " + id + " has already been " + leave.getStatus() + " and cannot be changed");
        }

        leave.setStatus(newStatus);
        leave.setDecidedBy(request.getDecidedBy());
        leave.setDecisionRemarks(request.getRemarks());
        leave.setDecidedAt(Instant.now());

        LeaveApplication saved = leaveApplicationRepository.save(leave);
        log.info("Leave application id={} {} by {}", id, newStatus, request.getDecidedBy());
        return leaveMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveResponse getLeaveById(Long id) {
        LeaveApplication leave = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Leave application", id));
        return leaveMapper.toResponse(leave);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<LeaveResponse> getLeaveHistory(Long employeeId, LeaveStatus status, LocalDate from, LocalDate to, Pageable pageable) {
        Page<LeaveApplication> page = leaveApplicationRepository.findAll(
                LeaveApplicationSpecification.filter(employeeId, status, from, to), pageable);
        return PagedResponse.from(page, leaveMapper::toResponse);
    }
}
