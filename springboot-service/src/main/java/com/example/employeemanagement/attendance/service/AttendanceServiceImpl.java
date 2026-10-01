package com.example.employeemanagement.attendance.service;

import com.example.employeemanagement.attendance.dto.AttendanceResponse;
import com.example.employeemanagement.attendance.dto.CheckInRequest;
import com.example.employeemanagement.attendance.dto.CheckOutRequest;
import com.example.employeemanagement.attendance.entity.Attendance;
import com.example.employeemanagement.attendance.mapper.AttendanceMapper;
import com.example.employeemanagement.attendance.repository.AttendanceRepository;
import com.example.employeemanagement.attendance.specification.AttendanceSpecification;
import com.example.employeemanagement.common.dto.PagedResponse;
import com.example.employeemanagement.common.exception.InvalidRequestException;
import com.example.employeemanagement.common.exception.ResourceNotFoundException;
import com.example.employeemanagement.employee.entity.Employee;
import com.example.employeemanagement.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceServiceImpl.class);

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceMapper attendanceMapper;

    @Override
    public AttendanceResponse checkIn(CheckInRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> ResourceNotFoundException.forId("Employee", request.getEmployeeId()));

        LocalDate today = LocalDate.now();
        attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .ifPresent(existing -> {
                    throw new InvalidRequestException(
                            "Employee " + employee.getId() + " has already checked in today (" + today + ")");
                });

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .attendanceDate(today)
                .checkInTime(Instant.now())
                .build();

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Employee id={} checked in at {}", employee.getId(), saved.getCheckInTime());
        return attendanceMapper.toResponse(saved);
    }

    @Override
    public AttendanceResponse checkOut(CheckOutRequest request) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(request.getEmployeeId(), today)
                .orElseThrow(() -> new InvalidRequestException(
                        "Employee " + request.getEmployeeId() + " has not checked in today (" + today + ")"));

        if (attendance.getCheckOutTime() != null) {
            throw new InvalidRequestException(
                    "Employee " + request.getEmployeeId() + " has already checked out today (" + today + ")");
        }

        Instant checkOutTime = Instant.now();
        attendance.setCheckOutTime(checkOutTime);
        attendance.setWorkingMinutes(Duration.between(attendance.getCheckInTime(), checkOutTime).toMinutes());

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Employee id={} checked out at {} ({} min)", attendance.getEmployee().getId(), checkOutTime, saved.getWorkingMinutes());
        return attendanceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse getAttendanceById(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Attendance", id));
        return attendanceMapper.toResponse(attendance);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AttendanceResponse> getAttendanceHistory(Long employeeId, LocalDate from, LocalDate to, Pageable pageable) {
        Page<Attendance> page = attendanceRepository.findAll(
                AttendanceSpecification.filter(employeeId, from, to), pageable);
        return PagedResponse.from(page, attendanceMapper::toResponse);
    }
}
