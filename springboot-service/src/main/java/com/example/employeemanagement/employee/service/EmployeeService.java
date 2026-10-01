package com.example.employeemanagement.employee.service;

import com.example.employeemanagement.common.dto.PagedResponse;
import com.example.employeemanagement.employee.dto.EmployeeRequest;
import com.example.employeemanagement.employee.dto.EmployeeResponse;
import com.example.employeemanagement.employee.entity.EmployeeStatus;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    EmployeeResponse createEmployee(EmployeeRequest request);

    EmployeeResponse updateEmployee(Long id, EmployeeRequest request);

    void deleteEmployee(Long id);

    EmployeeResponse getEmployeeById(Long id);

    /**
     * Single entry point backing GET /api/ems/employees - handles plain listing,
     * pagination, sorting (via Pageable) and search/filtering all at once.
     * keyword/departmentId/designationId/status are all optional (null = no filter).
     */
    PagedResponse<EmployeeResponse> searchEmployees(String keyword, Long departmentId, Long designationId,
                                                     EmployeeStatus status, Pageable pageable);
}
