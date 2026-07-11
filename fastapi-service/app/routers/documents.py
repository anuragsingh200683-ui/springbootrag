"""
Document processing endpoint: extract -> chunk -> embed -> store.
Called by Spring Boot after it saves a PDF and inserts the metadata row.
"""
import logging
from uuid import UUID

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas import DeleteDocumentResponse, ProcessDocumentRequest, ProcessDocumentResponse
from app.services.pdf_service import extract_text_from_pdf
from app.services.chunking_service import split_text_into_chunks
from app.services.embedding_service import generate_embeddings
from app.services.vector_store_service import delete_existing_chunks, store_chunks

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/documents", tags=["documents"])


@router.post("/process", response_model=ProcessDocumentResponse)
def process_document(request: ProcessDocumentRequest, db: Session = Depends(get_db)):
    logger.info("Processing document %s from %s", request.document_id, request.file_path)

    text = extract_text_from_pdf(request.file_path)
    chunks = split_text_into_chunks(text)
    embeddings = generate_embeddings(chunks)
    chunk_count = store_chunks(db, request.document_id, chunks, embeddings)

    return ProcessDocumentResponse(
        document_id=request.document_id,
        chunk_count=chunk_count,
        status="PROCESSED",
        message=f"Document processed and indexed successfully with {chunk_count} chunks.",
    )


@router.delete("/{document_id}", response_model=DeleteDocumentResponse)
def delete_document(document_id: UUID, db: Session = Depends(get_db)):
    """
    Removes indexed chunks/embeddings for a document. Called by Spring Boot as part
    of deleting a document. Idempotent: deleting a document with no indexed chunks
    (e.g. it never finished processing) returns deleted_chunks=0 rather than erroring.

    Note: Postgres also cascades document_chunks deletes when the owning row in the
    `documents` table (Spring Boot side) is removed (see sql-scripts/init.sql), so this
    endpoint is a belt-and-suspenders cleanup rather than the only place chunks get removed.
    """
    logger.info("Deleting indexed chunks for document %s", document_id)
    deleted_count = delete_existing_chunks(db, document_id)

    return DeleteDocumentResponse(
        document_id=document_id,
        deleted_chunks=deleted_count,
        status="DELETED",
        message=f"Removed {deleted_count} indexed chunk(s) for document {document_id}.",
    )
