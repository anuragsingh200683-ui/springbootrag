package com.example.aiapp.repository;

import com.example.aiapp.entity.QaHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QaHistoryRepository extends JpaRepository<QaHistory, Long> {
}
