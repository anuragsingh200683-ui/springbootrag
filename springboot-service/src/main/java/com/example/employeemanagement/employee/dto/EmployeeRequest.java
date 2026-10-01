package com.example.employeemanagement.employee.dto;

import com.example.employeemanagement.employee.entity.EmployeeStatus;
import com.example.employeemanagement.employee.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for creating/updating an Employee. status defaults to ACTIVE when
 * null (see EmployeeServiceImpl) so it's optional on create.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequest {

    @NotBlank(message = "firstName is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "lastName is required")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    @Size(max = 150)
    private String email;

    @Pattern(regexp = "^[0-9+()\\-\\s]{6,20}$", message = "phone must be a valid phone number")
    private String phone;

    @Past(message = "dateOfBirth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "dateOfJoining is required")
    @PastOrPresent(message = "dateOfJoining cannot be in the future")
    private LocalDate dateOfJoining;

    private Gender gender;

    @Size(max = 300)
    private String address;

    @NotNull(message = "salary is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "salary must not be negative")
    private BigDecimal salary;

    /** Optional - defaults to ACTIVE if omitted. */
    private EmployeeStatus status;

    @Schema(example = "1")
    @NotNull(message = "departmentId is required")
    private Long departmentId;

    @Schema(example = "1")
    @NotNull(message = "designationId is required")
    private Long designationId;
}
