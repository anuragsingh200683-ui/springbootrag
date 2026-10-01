package com.example.employeemanagement.attendance.service;

import com.example.employeemanagement.attendance.dto.AttendanceResponse;
import com.example.employeemanagement.attendance.dto.CheckInRequest;
import com.example.employeemanagement.attendance.dto.CheckOutRequest;
import com.example.employeemanagement.common.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AttendanceService {

    AttendanceResponse checkIn(CheckInRequest request);

    AttendanceResponse checkOut(CheckOutRequest request);

    AttendanceResponse getAttendanceById(Long id);

    /** employeeId/from/to are all optional filters (null = no filter). */
    PagedResponse<AttendanceResponse> getAttendanceHistory(Long employeeId, LocalDate from, LocalDate to, Pageable pageable);
}
