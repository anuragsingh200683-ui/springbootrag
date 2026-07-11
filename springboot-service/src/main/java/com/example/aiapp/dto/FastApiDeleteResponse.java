package com.example.aiapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/** Deserialized from FastAPI's DELETE /api/documents/{document_id} response (snake_case field names). */
public class FastApiDeleteResponse {

    @JsonProperty("document_id")
    private UUID documentId;

    @JsonProperty("deleted_chunks")
    private int deletedChunks;

    private String status;
    private String message;

    public FastApiDeleteResponse() {
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public int getDeletedChunks() {
        return deletedChunks;
    }

    public void setDeletedChunks(int deletedChunks) {
        this.deletedChunks = deletedChunks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
