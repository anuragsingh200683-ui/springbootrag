package com.example.aiapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/** Sent to FastAPI's POST /api/qa/query, which expects snake_case field names. */
public class FastApiQueryRequest {

    @JsonProperty("document_id")
    private UUID documentId;

    private String question;

    @JsonProperty("top_k")
    private Integer topK;

    public FastApiQueryRequest() {
    }

    public FastApiQueryRequest(UUID documentId, String question, Integer topK) {
        this.documentId = documentId;
        this.question = question;
        this.topK = topK;
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

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }
}
