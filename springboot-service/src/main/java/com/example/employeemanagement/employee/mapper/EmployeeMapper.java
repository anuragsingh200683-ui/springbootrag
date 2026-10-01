package com.example.employeemanagement.employee.mapper;

import com.example.employeemanagement.department.entity.Department;
import com.example.employeemanagement.designation.entity.Designation;
import com.example.employeemanagement.employee.dto.DepartmentSummary;
import com.example.employeemanagement.employee.dto.DesignationSummary;
import com.example.employeemanagement.employee.dto.EmployeeRequest;
import com.example.employeemanagement.employee.dto.EmployeeResponse;
import com.example.employeemanagement.employee.entity.Employee;
import com.example.employeemanagement.employee.entity.EmployeeStatus;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    /** department/designation are resolved by the service before calling this. */
    public Employee toEntity(EmployeeRequest request, Department department, Designation designation) {
        return Employee.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .dateOfJoining(request.getDateOfJoining())
                .gender(request.getGender())
                .address(request.getAddress())
                .salary(request.getSalary())
                .status(request.getStatus() != null ? request.getStatus() : EmployeeStatus.ACTIVE)
                .department(department)
                .designation(designation)
                .build();
    }

    public void updateEntity(Employee employee, EmployeeRequest request, Department department, Designation designation) {
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setDateOfJoining(request.getDateOfJoining());
        employee.setGender(request.getGender());
        employee.setAddress(request.getAddress());
        employee.setSalary(request.getSalary());
        employee.setStatus(request.getStatus() != null ? request.getStatus() : employee.getStatus());
        employee.setDepartment(department);
        employee.setDesignation(designation);
    }

    public EmployeeResponse toResponse(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeCode(formatEmployeeCode(employee.getId()))
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .dateOfBirth(employee.getDateOfBirth())
                .dateOfJoining(employee.getDateOfJoining())
                .gender(employee.getGender())
                .address(employee.getAddress())
                .salary(employee.getSalary())
                .status(employee.getStatus())
                .department(DepartmentSummary.builder()
                        .id(employee.getDepartment().getId())
                        .name(employee.getDepartment().getName())
                        .build())
                .designation(DesignationSummary.builder()
                        .id(employee.getDesignation().getId())
                        .name(employee.getDesignation().getName())
                        .build())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }

    private String formatEmployeeCode(Long id) {
        return "EMP-" + String.format("%06d", id);
    }
}
