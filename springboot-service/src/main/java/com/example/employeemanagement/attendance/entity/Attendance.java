package com.example.employeemanagement.attendance.entity;

import com.example.employeemanagement.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * JPA entity mapped to the "attendance" table. One row per employee per
 * calendar day - enforced both here (unique constraint) and in the service
 * layer (friendlier error message than a raw DB constraint violation).
 */
@Entity
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(
        name = "uk_attendance_employee_date", columnNames = {"employee_id", "attendance_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in_time", nullable = false)
    private Instant checkInTime;

    @Column(name = "check_out_time")
    private Instant checkOutTime;

    /** Filled in on check-out: Duration.between(checkInTime, checkOutTime).toMinutes(). */
    @Column(name = "working_minutes")
    private Long workingMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
