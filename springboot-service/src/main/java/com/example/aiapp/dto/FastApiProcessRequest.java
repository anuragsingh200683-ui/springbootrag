package com.example.aiapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/** Sent to FastAPI's POST /api/documents/process, which expects snake_case field names. */
public class FastApiProcessRequest {

    @JsonProperty("document_id")
    private UUID documentId;

    @JsonProperty("file_path")
    private String filePath;

    public FastApiProcessRequest() {
    }

    public FastApiProcessRequest(UUID documentId, String filePath) {
        this.documentId = documentId;
        this.filePath = filePath;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
