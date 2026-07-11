"""
FastAPI AI service entrypoint.

Run locally with:
    uvicorn main:app --reload --port 8000
"""
import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.logging_config import configure_logging
from app.database import Base, engine
from app.exceptions import register_exception_handlers
from app.routers import documents, qa

configure_logging()
logger = logging.getLogger(__name__)

app = FastAPI(
    title="AI Document Q&A - FastAPI Service",
    description="Handles PDF text extraction, chunking, embeddings, pgvector search and Claude calls.",
    version="1.0.0",
)

# Allow the Spring Boot backend (and browser dev tools) to call this service.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

register_exception_handlers(app)
app.include_router(documents.router)
app.include_router(qa.router)


@app.on_event("startup")
def on_startup():
    logger.info("Starting FastAPI AI service...")
    # Creates ORM-managed tables if they don't already exist (documents table
    # itself is owned/created by Spring Boot / sql-scripts/init.sql).
    Base.metadata.create_all(bind=engine)
    logger.info("Startup complete.")


@app.get("/health", tags=["health"])
def health_check():
    return {"status": "UP", "service": "fastapi-ai-service"}
