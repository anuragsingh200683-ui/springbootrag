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
-- Stores text chunks + embeddings for semantic search (managed by FastAPI)
-- Embedding dimension = 384 (sentence-transformers all-MiniLM-L6-v2)
-- Change vector(384) below if you switch embedding models.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS document_chunks (
    id              BIGSERIAL PRIMARY KEY,
    document_id     UUID NOT NULL,
    chunk_index     INTEGER NOT NULL,
    chunk_text      TEXT NOT NULL,
    embedding       vector(384) NOT NULL,
    token_count     INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_document
        FOREIGN KEY (document_id)
        REFERENCES documents (document_id)
        ON DELETE CASCADE
);

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
