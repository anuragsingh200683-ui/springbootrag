package com.example.employeemanagement.department.service;

import com.example.employeemanagement.common.exception.DuplicateResourceException;
import com.example.employeemanagement.common.exception.ResourceNotFoundException;
import com.example.employeemanagement.department.dto.DepartmentRequest;
import com.example.employeemanagement.department.dto.DepartmentResponse;
import com.example.employeemanagement.department.entity.Department;
import com.example.employeemanagement.department.mapper.DepartmentMapper;
import com.example.employeemanagement.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentServiceImpl.class);

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException(
                    "A department named '" + request.getName() + "' already exists");
        }
        Department saved = departmentRepository.save(departmentMapper.toEntity(request));
        log.info("Created department id={} name={}", saved.getId(), saved.getName());
        return departmentMapper.toResponse(saved);
    }

    @Override
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Department", id));

        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException(
                    "A department named '" + request.getName() + "' already exists");
        }

        departmentMapper.updateEntity(department, request);
        Department saved = departmentRepository.save(department);
        log.info("Updated department id={}", saved.getId());
        return departmentMapper.toResponse(saved);
    }

    @Override
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw ResourceNotFoundException.forId("Department", id);
        }
        departmentRepository.deleteById(id);
        log.info("Deleted department id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Department", id));
        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toResponse)
                .toList();
    }
}
