package com.example.employeemanagement.employee.service;

import com.example.employeemanagement.common.dto.PagedResponse;
import com.example.employeemanagement.common.exception.DuplicateResourceException;
import com.example.employeemanagement.common.exception.ResourceNotFoundException;
import com.example.employeemanagement.department.entity.Department;
import com.example.employeemanagement.department.repository.DepartmentRepository;
import com.example.employeemanagement.designation.entity.Designation;
import com.example.employeemanagement.designation.repository.DesignationRepository;
import com.example.employeemanagement.employee.dto.EmployeeRequest;
import com.example.employeemanagement.employee.dto.EmployeeResponse;
import com.example.employeemanagement.employee.entity.Employee;
import com.example.employeemanagement.employee.entity.EmployeeStatus;
import com.example.employeemanagement.employee.mapper.EmployeeMapper;
import com.example.employeemanagement.employee.repository.EmployeeRepository;
import com.example.employeemanagement.employee.specification.EmployeeSpecification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("An employee with email '" + request.getEmail() + "' already exists");
        }
        Department department = resolveDepartment(request.getDepartmentId());
        Designation designation = resolveDesignation(request.getDesignationId());

        Employee saved = employeeRepository.save(employeeMapper.toEntity(request, department, designation));
        log.info("Created employee id={} email={}", saved.getId(), saved.getEmail());
        return employeeMapper.toResponse(saved);
    }

    @Override
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Employee", id));

        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(request.getEmail(), id)) {
            throw new DuplicateResourceException("An employee with email '" + request.getEmail() + "' already exists");
        }

        Department department = resolveDepartment(request.getDepartmentId());
        Designation designation = resolveDesignation(request.getDesignationId());

        employeeMapper.updateEntity(employee, request, department, designation);
        Employee saved = employeeRepository.save(employee);
        log.info("Updated employee id={}", saved.getId());
        return employeeMapper.toResponse(saved);
    }

    @Override
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw ResourceNotFoundException.forId("Employee", id);
        }
        employeeRepository.deleteById(id);
        log.info("Deleted employee id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Employee", id));
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> searchEmployees(String keyword, Long departmentId, Long designationId,
                                                            EmployeeStatus status, Pageable pageable) {
        Page<Employee> page = employeeRepository.findAll(
                EmployeeSpecification.search(keyword, departmentId, designationId, status), pageable);
        return PagedResponse.from(page, employeeMapper::toResponse);
    }

    private Department resolveDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> ResourceNotFoundException.forId("Department", departmentId));
    }

    private Designation resolveDesignation(Long designationId) {
        return designationRepository.findById(designationId)
                .orElseThrow(() -> ResourceNotFoundException.forId("Designation", designationId));
    }
}
