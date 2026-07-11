"""
Pydantic request/response schemas for the FastAPI AI service.
"""
from typing import List, Optional
from uuid import UUID

from pydantic import BaseModel, Field


class ProcessDocumentRequest(BaseModel):
    document_id: UUID = Field(..., description="UUID assigned by Spring Boot when the document row was created")
    file_path: str = Field(..., description="Absolute or relative path to the PDF on local disk (shared with Spring Boot)")


class ProcessDocumentResponse(BaseModel):
    document_id: UUID
    chunk_count: int
    status: str = "PROCESSED"
    message: str = "Document processed and indexed successfully."


class DeleteDocumentResponse(BaseModel):
    document_id: UUID
    deleted_chunks: int
    status: str = "DELETED"
    message: str = "Document chunks deleted successfully."


class QueryRequest(BaseModel):
    document_id: Optional[UUID] = Field(None, description="Restrict search to a single document; omit to search all documents")
    question: str = Field(..., min_length=1, max_length=2000)
    top_k: Optional[int] = Field(None, ge=1, le=20)


class SourceChunk(BaseModel):
    chunk_index: int
    chunk_text: str
    similarity_score: float


class QueryResponse(BaseModel):
    question: str
    answer: str
    sources: List[SourceChunk]
    model: str
