package com.example.employeemanagement.designation.service;

import com.example.employeemanagement.designation.dto.DesignationRequest;
import com.example.employeemanagement.designation.dto.DesignationResponse;

import java.util.List;

public interface DesignationService {

    DesignationResponse createDesignation(DesignationRequest request);

    DesignationResponse updateDesignation(Long id, DesignationRequest request);

    void deleteDesignation(Long id);

    DesignationResponse getDesignationById(Long id);

    List<DesignationResponse> getAllDesignations();
}
