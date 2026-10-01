package com.example.employeemanagement.leave.specification;

import com.example.employeemanagement.leave.entity.LeaveApplication;
import com.example.employeemanagement.leave.entity.LeaveStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Builds the dynamic query for GET /api/ems/leaves (leave history/filter). */
public final class LeaveApplicationSpecification {

    private LeaveApplicationSpecification() {
    }

    public static Specification<LeaveApplication> filter(Long employeeId, LeaveStatus status, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            // Overlap semantics: any leave that touches the [from, to] window.
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
