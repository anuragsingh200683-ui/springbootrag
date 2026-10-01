package com.example.employeemanagement.attendance.specification;

import com.example.employeemanagement.attendance.entity.Attendance;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Builds the dynamic query for GET /api/ems/attendance (history/filter). */
public final class AttendanceSpecification {

    private AttendanceSpecification() {
    }

    public static Specification<Attendance> filter(Long employeeId, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("attendanceDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("attendanceDate"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
