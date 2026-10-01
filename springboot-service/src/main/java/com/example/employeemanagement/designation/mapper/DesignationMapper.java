package com.example.employeemanagement.designation.mapper;

import com.example.employeemanagement.designation.dto.DesignationRequest;
import com.example.employeemanagement.designation.dto.DesignationResponse;
import com.example.employeemanagement.designation.entity.Designation;
import org.springframework.stereotype.Component;

@Component
public class DesignationMapper {

    public Designation toEntity(DesignationRequest request) {
        return Designation.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    public void updateEntity(Designation designation, DesignationRequest request) {
        designation.setName(request.getName());
        designation.setDescription(request.getDescription());
    }

    public DesignationResponse toResponse(Designation designation) {
        return DesignationResponse.builder()
                .id(designation.getId())
                .name(designation.getName())
                .description(designation.getDescription())
                .createdAt(designation.getCreatedAt())
                .updatedAt(designation.getUpdatedAt())
                .build();
    }
}
