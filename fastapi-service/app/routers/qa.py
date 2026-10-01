"""
Question-answering endpoint. Called by Spring Boot when a user asks a
question in the React UI.

As of the LangGraph integration, this routes through the classifier -> RAG /
database / rest_api / tool / general -> answer generator workflow in
qa_graph/ (see run_qa_graph). The previous direct
embed -> search -> generate_answer chain is preserved unchanged below and
stays reachable by setting USE_LANGGRAPH=false in .env, so a problem with the
graph never blocks document Q&A - it's an instant rollback, not a rewrite.
"""
import logging
from typing import List, Tuple

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.config import get_settings
from app.database import get_db
from app.exceptions import NoRelevantContextError
from app.models import QaHistory
from app.schemas import QueryRequest, QueryResponse, SourceChunk
from app.services.embedding_service import generate_embedding
from app.services.llm_service import active_model_name, generate_answer
from app.services.vector_store_service import semantic_search
from qa_graph.graph import run_qa_graph

logger = logging.getLogger(__name__)
settings = get_settings()
router = APIRouter(prefix="/api/qa", tags=["qa"])


@router.post("/query", response_model=QueryResponse)
def query_document(request: QueryRequest, db: Session = Depends(get_db)):
    logger.info("Question received (document_id=%s): %s", request.document_id, request.question)

    if settings.use_langgraph:
        answer, sources, model = _answer_via_graph(db, request)
    else:
        answer, sources, model = _answer_via_legacy_chain(db, request)

    db.add(QaHistory(document_id=request.document_id, question=request.question, answer=answer))
    db.commit()

    return QueryResponse(question=request.question, answer=answer, sources=sources, model=model)


def _answer_via_graph(db: Session, request: QueryRequest) -> Tuple[str, List[SourceChunk], str]:
    final_state = run_qa_graph(
        db=db,
        question=request.question,
        document_id=request.document_id,
        top_k=request.top_k,
    )

    # Preserves the exact old 404 behavior for the primary document-Q&A use
    # case: if the classifier routed to RAG and nothing was retrieved, this
    # still raises NoRelevantContextError like it did before LangGraph
    # existed. Other categories (database/rest_api/tool/general) don't
    # depend on document context, so this check only applies to "rag".
    if final_state.get("category") == "rag" and final_state.get("no_context"):
        raise NoRelevantContextError(str(request.document_id))

    return final_state["answer"], final_state.get("sources", []), final_state["model"]


def _answer_via_legacy_chain(db: Session, request: QueryRequest) -> Tuple[str, List[SourceChunk], str]:
    """
    Preserved exactly as it was before the LangGraph integration. Setting
    USE_LANGGRAPH=false in .env falls back to this with zero other code
    changes - it still respects LLM_PROVIDER (Anthropic or OpenAI) via
    app/services/llm_service.py, unlike the LangGraph nodes which are
    OpenAI-only.
    """
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
    return answer, sources, active_model_name()
