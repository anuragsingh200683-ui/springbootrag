package com.example.employeemanagement.department.repository;

import com.example.employeemanagement.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Department> findByNameIgnoreCase(String name);

    /** Used by update to detect "renamed to a name some other department already has". */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
