package com.example.aiapp.ingest;

/** A single retrieved chunk, with cosine similarity in [-1, 1] (higher = more relevant). */
public record SemanticSearchResult(int chunkIndex, String chunkText, double similarityScore) {
}
