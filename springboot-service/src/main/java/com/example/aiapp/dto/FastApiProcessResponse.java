package com.example.aiapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/** Deserialized from FastAPI's POST /api/documents/process response (snake_case field names). */
public class FastApiProcessResponse {

    @JsonProperty("document_id")
    private UUID documentId;

    @JsonProperty("chunk_count")
    private int chunkCount;

    private String status;
    private String message;

    public FastApiProcessResponse() {
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
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
