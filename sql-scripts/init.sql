-- ============================================================
-- AI Document Q&A Application - Database Initialization Script
-- PostgreSQL 15+ with pgvector extension
-- ============================================================

-- Enable pgvector extension (required for embedding storage/search)
CREATE EXTENSION IF NOT EXISTS vector;

-- ------------------------------------------------------------
-- Table: documents
-- Stores metadata about each uploaded PDF (managed by Spring Boot)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS documents (
    id              BIGSERIAL PRIMARY KEY,
    document_id     UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    file_name       VARCHAR(255) NOT NULL,
    file_path       VARCHAR(1024) NOT NULL,
    content_type    VARCHAR(100),
    file_size_bytes BIGINT,
    status          VARCHAR(30) NOT NULL DEFAULT 'UPLOADED', -- UPLOADED, PROCESSING, PROCESSED, FAILED
    chunk_count     INTEGER DEFAULT 0,
    error_message   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_documents_document_id ON documents (document_id);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents (status);

-- ------------------------------------------------------------
-- Table: document_chunks
-- Stores text chunks + embeddings for semantic search (managed by Spring Boot's
-- com.example.aiapp.ingest package - previously managed by fastapi-service).
-- Embedding dimension = 1536 (OpenAI text-embedding-3-small).
-- Change vector(1536) below if you switch embedding models.
--
-- NOTE: this migrated from vector(384) (sentence-transformers all-MiniLM-L6-v2).
-- Old 384-dim embeddings are not compatible with the new model's vector space, so
-- existing rows must be wiped and documents reprocessed after this change - see
-- the migration steps below.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS document_chunks (
    id              BIGSERIAL PRIMARY KEY,
    document_id     UUID NOT NULL,
    chunk_index     INTEGER NOT NULL,
    chunk_text      TEXT NOT NULL,
    embedding       vector(1536) NOT NULL,
    token_count     INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_document
        FOREIGN KEY (document_id)
        REFERENCES documents (document_id)
        ON DELETE CASCADE
);

-- Migration for a database created before the 1536-dim switch. CREATE TABLE IF NOT
-- EXISTS above is a no-op on such a database (the table already exists), so without
-- this block document_chunks.embedding would keep its old dimension forever and every
-- insert of a new 1536-dim embedding would fail with a pgvector dimension mismatch.
-- Idempotent: does nothing once the column is already vector(1536).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_attribute a
        JOIN pg_class c ON c.oid = a.attrelid
        WHERE c.relname = 'document_chunks'
          AND a.attname = 'embedding'
          AND NOT a.attisdropped
          AND format_type(a.atttypid, a.atttypmod) <> 'vector(1536)'
    ) THEN
        RAISE NOTICE 'Migrating document_chunks.embedding to vector(1536) - existing chunks will be cleared and documents must be reprocessed.';
        DROP INDEX IF EXISTS idx_chunks_embedding_cosine;
        TRUNCATE TABLE document_chunks;
        ALTER TABLE document_chunks ALTER COLUMN embedding TYPE vector(1536);
    END IF;
END
$$;
-- (the CREATE INDEX below recreates idx_chunks_embedding_cosine after the migration)

CREATE INDEX IF NOT EXISTS idx_chunks_document_id ON document_chunks (document_id);

-- Approximate nearest-neighbour index for fast cosine similarity search.
-- IVFFlat requires ANALYZE after data is loaded; lists=100 is a reasonable
-- default for small/medium datasets on a local machine.
CREATE INDEX IF NOT EXISTS idx_chunks_embedding_cosine
    ON document_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- ------------------------------------------------------------
-- Table: qa_history (optional audit trail of questions/answers)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS qa_history (
    id              BIGSERIAL PRIMARY KEY,
    document_id     UUID,
    question        TEXT NOT NULL,
    answer          TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_qa_history_document_id ON qa_history (document_id);
