package com.example.employeemanagement.designation.controller;

import com.example.employeemanagement.designation.dto.DesignationRequest;
import com.example.employeemanagement.designation.dto.DesignationResponse;
import com.example.employeemanagement.designation.service.DesignationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Designation Management.
 * Base path: /api/ems/designations (permitAll - see SecurityConfig).
 */
@RestController
@RequestMapping("/api/ems/designations")
@RequiredArgsConstructor
@Tag(name = "Designation", description = "Designation Management")
public class DesignationController {

    private final DesignationService designationService;

    @PostMapping
    @Operation(summary = "Create a new designation")
    public ResponseEntity<DesignationResponse> createDesignation(@Valid @RequestBody DesignationRequest request) {
        DesignationResponse response = designationService.createDesignation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing designation")
    public ResponseEntity<DesignationResponse> updateDesignation(@PathVariable Long id,
                                                                   @Valid @RequestBody DesignationRequest request) {
        return ResponseEntity.ok(designationService.updateDesignation(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a designation")
    public ResponseEntity<Void> deleteDesignation(@PathVariable Long id) {
        designationService.deleteDesignation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a designation by id")
    public ResponseEntity<DesignationResponse> getDesignation(@PathVariable Long id) {
        return ResponseEntity.ok(designationService.getDesignationById(id));
    }

    @GetMapping
    @Operation(summary = "List all designations")
    public ResponseEntity<List<DesignationResponse>> getAllDesignations() {
        return ResponseEntity.ok(designationService.getAllDesignations());
    }
}
