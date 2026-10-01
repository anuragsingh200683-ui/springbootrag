package com.example.aiapp.ingest;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Plain-JDBC access to the pgvector-backed {@code document_chunks} table, replacing
 * fastapi-service's SQLAlchemy vector_store_service.py. Embeddings are sent to/read from
 * Postgres via the pgvector text literal format ("[v1,v2,...]" cast with ::vector), which
 * needs no custom JDBC type registration and keeps the driver-level plumbing simple.
 */
@Repository
public class VectorStoreRepository {

    private final JdbcTemplate jdbcTemplate;

    public VectorStoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertChunks(UUID documentId, List<ChunkRecord> chunks) {
        String sql = "INSERT INTO document_chunks (document_id, chunk_index, chunk_text, embedding, token_count) "
                + "VALUES (?, ?, ?, ?::vector, ?)";
        List<Object[]> batchArgs = chunks.stream()
                .map(c -> new Object[]{
                        documentId,
                        c.chunkIndex(),
                        c.chunkText(),
                        toVectorLiteral(c.embedding()),
                        approximateTokenCount(c.chunkText())
                })
                .toList();
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    public List<SemanticSearchResult> semanticSearch(UUID documentId, float[] queryEmbedding, int topK) {
        String vectorLiteral = toVectorLiteral(queryEmbedding);
        String sql = documentId != null
                ? "SELECT chunk_index, chunk_text, (embedding <=> ?::vector) AS distance "
                        + "FROM document_chunks WHERE document_id = ? ORDER BY embedding <=> ?::vector LIMIT ?"
                : "SELECT chunk_index, chunk_text, (embedding <=> ?::vector) AS distance "
                        + "FROM document_chunks ORDER BY embedding <=> ?::vector LIMIT ?";
        Object[] args = documentId != null
                ? new Object[]{vectorLiteral, documentId, vectorLiteral, topK}
                : new Object[]{vectorLiteral, vectorLiteral, topK};

        return jdbcTemplate.query(sql, (rs, rowNum) -> new SemanticSearchResult(
                rs.getInt("chunk_index"),
                rs.getString("chunk_text"),
                1.0 - rs.getDouble("distance")
        ), args);
    }

    public void deleteByDocumentId(UUID documentId) {
        jdbcTemplate.update("DELETE FROM document_chunks WHERE document_id = ?", documentId);
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        return sb.append(']').toString();
    }

    private int approximateTokenCount(String text) {
        return text.isBlank() ? 0 : text.trim().split("\\s+").length;
    }
}
