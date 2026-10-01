package com.example.employeemanagement.department.mapper;

import com.example.employeemanagement.department.dto.DepartmentRequest;
import com.example.employeemanagement.department.dto.DepartmentResponse;
import com.example.employeemanagement.department.entity.Department;
import org.springframework.stereotype.Component;

/**
 * Manual entity <-> DTO conversion (no MapStruct dependency in the project yet;
 * plain methods keep this module dependency-free and easy to follow).
 */
@Component
public class DepartmentMapper {

    public Department toEntity(DepartmentRequest request) {
        return Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    /** Applies request fields onto an already-persisted entity (used by update). */
    public void updateEntity(Department department, DepartmentRequest request) {
        department.setName(request.getName());
        department.setDescription(request.getDescription());
    }

    public DepartmentResponse toResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}
