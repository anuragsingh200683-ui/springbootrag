package com.example.employeemanagement.leave.controller;

import com.example.employeemanagement.common.dto.PagedResponse;
import com.example.employeemanagement.common.exception.InvalidRequestException;
import com.example.employeemanagement.leave.dto.LeaveApplyRequest;
import com.example.employeemanagement.leave.dto.LeaveDecisionRequest;
import com.example.employeemanagement.leave.dto.LeaveResponse;
import com.example.employeemanagement.leave.entity.LeaveStatus;
import com.example.employeemanagement.leave.service.LeaveService;
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
 * REST API for Leave Management: apply, approve, reject, history.
 * Base path: /api/ems/leaves (permitAll - see SecurityConfig).
 */
@RestController
@RequestMapping("/api/ems/leaves")
@RequiredArgsConstructor
@Tag(name = "Leave", description = "Leave Management")
public class LeaveController {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "startDate", "endDate", "status", "appliedAt");

    private final LeaveService leaveService;

    @PostMapping
    @Operation(summary = "Apply for leave")
    public ResponseEntity<LeaveResponse> applyLeave(@Valid @RequestBody LeaveApplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.applyLeave(request));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve a pending leave application")
    public ResponseEntity<LeaveResponse> approveLeave(@PathVariable Long id,
                                                        @Valid @RequestBody LeaveDecisionRequest request) {
        return ResponseEntity.ok(leaveService.approveLeave(id, request));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject a pending leave application")
    public ResponseEntity<LeaveResponse> rejectLeave(@PathVariable Long id,
                                                       @Valid @RequestBody LeaveDecisionRequest request) {
        return ResponseEntity.ok(leaveService.rejectLeave(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single leave application by id")
    public ResponseEntity<LeaveResponse> getLeave(@PathVariable Long id) {
        return ResponseEntity.ok(leaveService.getLeaveById(id));
    }

    @GetMapping
    @Operation(summary = "Leave history", description = "employeeId/status/from/to are optional filters; from/to are inclusive (yyyy-MM-dd) and match any leave overlapping the window.")
    public ResponseEntity<PagedResponse<LeaveResponse>> getLeaveHistory(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "appliedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new InvalidRequestException("sortBy must be one of " + SORTABLE_FIELDS + " but was '" + sortBy + "'");
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(direction, sortBy));

        return ResponseEntity.ok(leaveService.getLeaveHistory(employeeId, status, from, to, pageable));
    }
}
