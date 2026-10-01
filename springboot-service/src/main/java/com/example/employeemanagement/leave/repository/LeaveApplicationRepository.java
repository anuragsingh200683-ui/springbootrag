package com.example.employeemanagement.leave.repository;

import com.example.employeemanagement.leave.entity.LeaveApplication;
import com.example.employeemanagement.leave.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long>, JpaSpecificationExecutor<LeaveApplication> {

    long countByStatus(LeaveStatus status);
}
