package com.example.employeemanagement.department.controller;

import com.example.employeemanagement.department.dto.DepartmentRequest;
import com.example.employeemanagement.department.dto.DepartmentResponse;
import com.example.employeemanagement.department.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Department Management.
 * Base path: /api/ems/departments (permitAll - see SecurityConfig).
 */
@RestController
@RequestMapping("/api/ems/departments")
@RequiredArgsConstructor
@Tag(name = "Department", description = "Department Management")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @Operation(summary = "Create a new department")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse response = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing department")
    public ResponseEntity<DepartmentResponse> updateDepartment(@PathVariable Long id,
                                                                 @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a department")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a department by id")
    public ResponseEntity<DepartmentResponse> getDepartment(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @GetMapping
    @Operation(summary = "List all departments")
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }
}
