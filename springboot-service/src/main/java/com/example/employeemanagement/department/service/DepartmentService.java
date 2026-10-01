package com.example.employeemanagement.department.service;

import com.example.employeemanagement.department.dto.DepartmentRequest;
import com.example.employeemanagement.department.dto.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse createDepartment(DepartmentRequest request);

    DepartmentResponse updateDepartment(Long id, DepartmentRequest request);

    void deleteDepartment(Long id);

    DepartmentResponse getDepartmentById(Long id);

    List<DepartmentResponse> getAllDepartments();
}
