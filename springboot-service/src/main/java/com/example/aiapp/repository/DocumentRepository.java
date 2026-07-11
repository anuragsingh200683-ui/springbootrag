package com.example.aiapp.repository;

import com.example.aiapp.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByDocumentId(UUID documentId);
}
