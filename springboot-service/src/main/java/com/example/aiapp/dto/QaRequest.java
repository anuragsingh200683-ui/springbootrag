package com.example.aiapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class QaRequest {

    /** Optional: restrict the search to a single document. Null = search across all documents. */
    private UUID documentId;

    @NotBlank(message = "Question must not be blank")
    @Size(max = 2000, message = "Question must be at most 2000 characters")
    private String question;

    public QaRequest() {
    }

    public QaRequest(UUID documentId, String question) {
        this.documentId = documentId;
        this.question = question;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
