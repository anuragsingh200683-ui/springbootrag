package com.example.aiapp.ingest;

/** One chunk of extracted document text paired with its embedding, ready to persist. */
public record ChunkRecord(int chunkIndex, String chunkText, float[] embedding) {
}
