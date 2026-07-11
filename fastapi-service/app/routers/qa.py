"""
Question-answering endpoint: semantic search over pgvector + Claude generation.
Called by Spring Boot when a user asks a question in the React UI.
"""
import logging

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.config import get_settings
from app.database import get_db
from app.exceptions import NoRelevantContextError
from app.models import QaHistory
from app.schemas import QueryRequest, QueryResponse
from app.services.embedding_service import generate_embedding
from app.services.llm_service import active_model_name, generate_answer
from app.services.vector_store_service import semantic_search

logger = logging.getLogger(__name__)
settings = get_settings()
router = APIRouter(prefix="/api/qa", tags=["qa"])


@router.post("/query", response_model=QueryResponse)
def query_document(request: QueryRequest, db: Session = Depends(get_db)):
    logger.info("Question received (document_id=%s): %s", request.document_id, request.question)

    query_embedding = generate_embedding(request.question)
    sources = semantic_search(
        db,
        query_embedding=query_embedding,
        document_id=request.document_id,
        top_k=request.top_k,
    )

    if not sources:
        raise NoRelevantContextError(str(request.document_id))

    context_texts = [s.chunk_text for s in sources]
    answer = generate_answer(request.question, context_texts)

    db.add(QaHistory(document_id=request.document_id, question=request.question, answer=answer))
    db.commit()

    return QueryResponse(
        question=request.question,
        answer=answer,
        sources=sources,
        model=active_model_name(),
    )
