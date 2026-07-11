"""
Stores and queries chunk embeddings in pgvector (via the document_chunks table).
"""
import logging
from typing import List, Optional
from uuid import UUID

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.config import get_settings
from app.models import DocumentChunk
from app.schemas import SourceChunk

logger = logging.getLogger(__name__)
settings = get_settings()


def delete_existing_chunks(db: Session, document_id: UUID) -> int:
    result = db.execute(delete(DocumentChunk).where(DocumentChunk.document_id == document_id))
    db.commit()
    return result.rowcount or 0


def store_chunks(
    db: Session,
    document_id: UUID,
    chunk_texts: List[str],
    embeddings: List[List[float]],
) -> int:
    # Re-processing a document should not create duplicate rows.
    delete_existing_chunks(db, document_id)

    rows = [
        DocumentChunk(
            document_id=document_id,
            chunk_index=idx,
            chunk_text=text,
            embedding=embedding,
            token_count=len(text.split()),
        )
        for idx, (text, embedding) in enumerate(zip(chunk_texts, embeddings))
    ]
    db.bulk_save_objects(rows)
    db.commit()
    logger.info("Stored %d chunks for document %s", len(rows), document_id)
    return len(rows)


def semantic_search(
    db: Session,
    query_embedding: List[float],
    document_id: Optional[UUID] = None,
    top_k: Optional[int] = None,
) -> List[SourceChunk]:
    k = top_k or settings.top_k_results

    # cosine_distance: 0 = identical, 2 = opposite. similarity = 1 - distance
    distance_expr = DocumentChunk.embedding.cosine_distance(query_embedding)
    stmt = select(DocumentChunk, distance_expr.label("distance")).order_by(distance_expr)

    if document_id is not None:
        stmt = stmt.where(DocumentChunk.document_id == document_id)

    stmt = stmt.limit(k)
    results = db.execute(stmt).all()

    return [
        SourceChunk(
            chunk_index=row.DocumentChunk.chunk_index,
            chunk_text=row.DocumentChunk.chunk_text,
            similarity_score=round(1 - float(row.distance), 4),
        )
        for row in results
    ]
