package com.example.employeemanagement.attendance.mapper;

import com.example.employeemanagement.attendance.dto.AttendanceResponse;
import com.example.employeemanagement.attendance.dto.AttendanceStatus;
import com.example.employeemanagement.attendance.entity.Attendance;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceResponse toResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .employeeId(attendance.getEmployee().getId())
                .employeeName(attendance.getEmployee().getFirstName() + " " + attendance.getEmployee().getLastName())
                .attendanceDate(attendance.getAttendanceDate())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .workingMinutes(attendance.getWorkingMinutes())
                .status(attendance.getCheckOutTime() == null ? AttendanceStatus.CHECKED_IN : AttendanceStatus.CHECKED_OUT)
                .build();
    }
}
