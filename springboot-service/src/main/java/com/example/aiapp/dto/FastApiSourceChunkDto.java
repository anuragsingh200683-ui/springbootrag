package com.example.aiapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mirrors FastAPI's SourceChunk schema (snake_case field names).
 * Kept separate from SourceChunkDto (camelCase), which is the contract with the React frontend.
 */
public class FastApiSourceChunkDto {

    @JsonProperty("chunk_index")
    private int chunkIndex;

    @JsonProperty("chunk_text")
    private String chunkText;

    @JsonProperty("similarity_score")
    private double similarityScore;

    public FastApiSourceChunkDto() {
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getChunkText() {
        return chunkText;
    }

    public void setChunkText(String chunkText) {
        this.chunkText = chunkText;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }
}
