package com.example.employeemanagement.designation.repository;

import com.example.employeemanagement.designation.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DesignationRepository extends JpaRepository<Designation, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Designation> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
