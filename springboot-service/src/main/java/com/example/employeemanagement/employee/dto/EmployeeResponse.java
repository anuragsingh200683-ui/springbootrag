package com.example.employeemanagement.employee.dto;

import com.example.employeemanagement.employee.entity.EmployeeStatus;
import com.example.employeemanagement.employee.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {

    private Long id;
    /** Human-friendly code derived from id, e.g. "EMP-000042". Not a stored column. */
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private LocalDate dateOfJoining;
    private Gender gender;
    private String address;
    private BigDecimal salary;
    private EmployeeStatus status;
    private DepartmentSummary department;
    private DesignationSummary designation;
    private Instant createdAt;
    private Instant updatedAt;
}
