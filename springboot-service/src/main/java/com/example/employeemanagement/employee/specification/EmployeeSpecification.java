package com.example.employeemanagement.employee.specification;

import com.example.employeemanagement.employee.entity.Employee;
import com.example.employeemanagement.employee.entity.EmployeeStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a single dynamic query for GET /api/ems/employees combining a free-text
 * "keyword" search (name/email/employee code) with optional exact-match filters.
 * Keeps the repository free of a combinatorial explosion of finder methods.
 */
public final class EmployeeSpecification {

    private EmployeeSpecification() {
    }

    public static Specification<Employee> search(String keyword, Long departmentId, Long designationId, EmployeeStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                Predicate byFirstName = cb.like(cb.lower(root.get("firstName")), like);
                Predicate byLastName = cb.like(cb.lower(root.get("lastName")), like);
                Predicate byEmail = cb.like(cb.lower(root.get("email")), like);
                predicates.add(cb.or(byFirstName, byLastName, byEmail));
            }
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            if (designationId != null) {
                predicates.add(cb.equal(root.get("designation").get("id"), designationId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
