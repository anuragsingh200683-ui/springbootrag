"""
SQLAlchemy ORM models. These map onto tables created by sql-scripts/init.sql.

Note: `documents` is owned by the Spring Boot / JPA side (created via
sql-scripts/init.sql), not by this service's SQLAlchemy models. The real
foreign-key constraint (with ON DELETE CASCADE) already exists at the
Postgres level via init.sql, so document_id here is intentionally a plain
column with no SQLAlchemy-level ForeignKey(): declaring one would require
SQLAlchemy to resolve a "documents" table that isn't part of this app's
ORM metadata, which fails at insert time.
"""
import uuid
from datetime import datetime

from pgvector.sqlalchemy import Vector
from sqlalchemy import Column, Integer, String, Text, DateTime
from sqlalchemy.dialects.postgresql import UUID

from app.config import get_settings
from app.database import Base

settings = get_settings()


class DocumentChunk(Base):
    __tablename__ = "document_chunks"

    id = Column(Integer, primary_key=True, autoincrement=True)
    document_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    chunk_index = Column(Integer, nullable=False)
    chunk_text = Column(Text, nullable=False)
    embedding = Column(Vector(settings.embedding_dimension), nullable=False)
    token_count = Column(Integer, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)


class QaHistory(Base):
    __tablename__ = "qa_history"

    id = Column(Integer, primary_key=True, autoincrement=True)
    document_id = Column(UUID(as_uuid=True), nullable=True, index=True)
    question = Column(Text, nullable=False)
    answer = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
