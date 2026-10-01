package com.example.employeemanagement.attendance.controller;

import com.example.employeemanagement.attendance.dto.AttendanceResponse;
import com.example.employeemanagement.attendance.dto.CheckInRequest;
import com.example.employeemanagement.attendance.dto.CheckOutRequest;
import com.example.employeemanagement.attendance.service.AttendanceService;
import com.example.employeemanagement.common.dto.PagedResponse;
import com.example.employeemanagement.common.exception.InvalidRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

/**
 * REST API for Attendance Management: check-in, check-out, history.
 * Base path: /api/ems/attendance (permitAll - see SecurityConfig).
 */
@RestController
@RequestMapping("/api/ems/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Attendance Management")
public class AttendanceController {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "attendanceDate", "checkInTime", "checkOutTime", "workingMinutes");

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    @Operation(summary = "Check in for today", description = "Fails if the employee already checked in today.")
    public ResponseEntity<AttendanceResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.checkIn(request));
    }

    @PostMapping("/check-out")
    @Operation(summary = "Check out for today", description = "Fails if the employee hasn't checked in, or already checked out, today.")
    public ResponseEntity<AttendanceResponse> checkOut(@Valid @RequestBody CheckOutRequest request) {
        return ResponseEntity.ok(attendanceService.checkOut(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single attendance record by id")
    public ResponseEntity<AttendanceResponse> getAttendance(@PathVariable Long id) {
        return ResponseEntity.ok(attendanceService.getAttendanceById(id));
    }

    @GetMapping
    @Operation(summary = "Attendance history", description = "employeeId/from/to are optional filters; from/to are inclusive (yyyy-MM-dd).")
    public ResponseEntity<PagedResponse<AttendanceResponse>> getAttendanceHistory(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "attendanceDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new InvalidRequestException("sortBy must be one of " + SORTABLE_FIELDS + " but was '" + sortBy + "'");
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(direction, sortBy));

        return ResponseEntity.ok(attendanceService.getAttendanceHistory(employeeId, from, to, pageable));
    }
}
