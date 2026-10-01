package com.example.employeemanagement.dashboard.service;

import com.example.employeemanagement.attendance.repository.AttendanceRepository;
import com.example.employeemanagement.dashboard.dto.DashboardSummaryResponse;
import com.example.employeemanagement.dashboard.dto.DepartmentHeadcount;
import com.example.employeemanagement.department.repository.DepartmentRepository;
import com.example.employeemanagement.designation.repository.DesignationRepository;
import com.example.employeemanagement.employee.entity.EmployeeStatus;
import com.example.employeemanagement.employee.repository.EmployeeRepository;
import com.example.employeemanagement.leave.entity.LeaveStatus;
import com.example.employeemanagement.leave.repository.LeaveApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Pulls straight from every other EMS module's repository - intentionally has
 * no entity/table of its own. All reads, so a single read-only transaction
 * keeps the multiple count queries consistent with each other.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;

    @Override
    public DashboardSummaryResponse getSummary() {
        LocalDate today = LocalDate.now();

        long activeEmployees = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long checkedInToday = attendanceRepository.countByAttendanceDate(today);
        long checkedOutToday = attendanceRepository.countByAttendanceDateAndCheckOutTimeIsNotNull(today);

        return DashboardSummaryResponse.builder()
                .asOf(today)
                .totalEmployees(employeeRepository.count())
                .activeEmployees(activeEmployees)
                .inactiveEmployees(employeeRepository.countByStatus(EmployeeStatus.INACTIVE))
                .totalDepartments(departmentRepository.count())
                .totalDesignations(designationRepository.count())
                .employeesByDepartment(employeeRepository.countEmployeesGroupByDepartment().stream()
                        .map(row -> DepartmentHeadcount.builder()
                                .departmentName((String) row[0])
                                .employeeCount((Long) row[1])
                                .build())
                        .toList())
                .checkedInToday(checkedInToday)
                .checkedOutToday(checkedOutToday)
                .notCheckedInToday(Math.max(activeEmployees - checkedInToday, 0))
                .pendingLeaves(leaveApplicationRepository.countByStatus(LeaveStatus.PENDING))
                .approvedLeaves(leaveApplicationRepository.countByStatus(LeaveStatus.APPROVED))
                .rejectedLeaves(leaveApplicationRepository.countByStatus(LeaveStatus.REJECTED))
                .build();
    }
}
