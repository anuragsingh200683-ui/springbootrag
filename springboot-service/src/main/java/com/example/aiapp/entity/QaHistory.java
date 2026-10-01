package com.example.aiapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Audit trail of questions/answers - same shape as fastapi-service's SQLAlchemy QaHistory model. */
@Entity
@Table(name = "qa_history")
public class QaHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public QaHistory() {
    }

    public QaHistory(UUID documentId, String question, String answer) {
        this.documentId = documentId;
        this.question = question;
        this.answer = answer;
    }

    public Long getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
