package com.example.employeemanagement.employee.controller;

import com.example.employeemanagement.common.dto.PagedResponse;
import com.example.employeemanagement.common.exception.InvalidRequestException;
import com.example.employeemanagement.employee.dto.EmployeeRequest;
import com.example.employeemanagement.employee.dto.EmployeeResponse;
import com.example.employeemanagement.employee.entity.EmployeeStatus;
import com.example.employeemanagement.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * REST API for Employee Management: CRUD + search + pagination + sorting.
 * Base path: /api/ems/employees (permitAll - see SecurityConfig).
 */
@RestController
@RequestMapping("/api/ems/employees")
@RequiredArgsConstructor
@Tag(name = "Employee", description = "Employee Management")
public class EmployeeController {

    /** Allowlist - prevents arbitrary/unsafe property names reaching the JPA sort clause. */
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "firstName", "lastName", "email", "dateOfJoining", "salary", "status");

    private final EmployeeService employeeService;

    @PostMapping
    @Operation(summary = "Create a new employee")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing employee")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id,
                                                             @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an employee")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an employee by id")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @GetMapping
    @Operation(summary = "List/search employees with pagination and sorting",
            description = "keyword matches first name, last name or email (case-insensitive, partial). "
                    + "departmentId/designationId/status are exact-match filters. All are optional.")
    public ResponseEntity<PagedResponse<EmployeeResponse>> getAllEmployees(
            @Parameter(description = "Free-text search across first name, last name, email")
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "One of: id, firstName, lastName, email, dateOfJoining, salary, status")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "asc or desc")
            @RequestParam(defaultValue = "asc") String sortDir) {

        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new InvalidRequestException(
                    "sortBy must be one of " + SORTABLE_FIELDS + " but was '" + sortBy + "'");
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(direction, sortBy));

        return ResponseEntity.ok(
                employeeService.searchEmployees(keyword, departmentId, designationId, status, pageable));
    }
}
