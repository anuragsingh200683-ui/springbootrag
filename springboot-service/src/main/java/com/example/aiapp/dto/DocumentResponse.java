package com.example.aiapp.dto;

import com.example.aiapp.entity.Document;

import java.time.Instant;
import java.util.UUID;

public class DocumentResponse {
    private UUID documentId;
    private String fileName;
    private String status;
    private Integer chunkCount;
    private String errorMessage;
    private Instant createdAt;

    public DocumentResponse() {
    }

    private DocumentResponse(Builder builder) {
        this.documentId = builder.documentId;
        this.fileName = builder.fileName;
        this.status = builder.status;
        this.chunkCount = builder.chunkCount;
        this.errorMessage = builder.errorMessage;
        this.createdAt = builder.createdAt;
    }

    public static DocumentResponse fromEntity(Document document) {
        return DocumentResponse.builder()
                .documentId(document.getDocumentId())
                .fileName(document.getFileName())
                .status(document.getStatus().name())
                .chunkCount(document.getChunkCount())
                .errorMessage(document.getErrorMessage())
                .createdAt(document.getCreatedAt())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static class Builder {
        private UUID documentId;
        private String fileName;
        private String status;
        private Integer chunkCount;
        private String errorMessage;
        private Instant createdAt;

        public Builder documentId(UUID documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder chunkCount(Integer chunkCount) {
            this.chunkCount = chunkCount;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public DocumentResponse build() {
            return new DocumentResponse(this);
        }
    }
}
