package com.example.aiapp.dto;

public class SourceChunkDto {
    private int chunkIndex;
    private String chunkText;
    private double similarityScore;

    public SourceChunkDto() {
    }

    public SourceChunkDto(int chunkIndex, String chunkText, double similarityScore) {
        this.chunkIndex = chunkIndex;
        this.chunkText = chunkText;
        this.similarityScore = similarityScore;
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
