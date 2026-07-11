"""
Custom application exceptions and a FastAPI exception-handler registration function.
"""
import logging
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

logger = logging.getLogger(__name__)


class AppException(Exception):
    """Base class for all handled application errors."""

    def __init__(self, message: str, status_code: int = 500):
        self.message = message
        self.status_code = status_code
        super().__init__(message)


class DocumentNotFoundError(AppException):
    def __init__(self, document_id: str):
        super().__init__(f"Document not found: {document_id}", status_code=404)


class PdfExtractionError(AppException):
    def __init__(self, detail: str):
        super().__init__(f"Failed to extract text from PDF: {detail}", status_code=422)


class EmbeddingGenerationError(AppException):
    def __init__(self, detail: str):
        super().__init__(f"Failed to generate embeddings: {detail}", status_code=500)


class LlmServiceError(AppException):
    def __init__(self, detail: str):
        super().__init__(f"Claude API call failed: {detail}", status_code=502)


class NoRelevantContextError(AppException):
    def __init__(self, document_id: str):
        super().__init__(
            f"No indexed content found for document {document_id}. Upload/process it first.",
            status_code=404,
        )


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(AppException)
    async def app_exception_handler(request: Request, exc: AppException):
        logger.error("AppException on %s %s: %s", request.method, request.url.path, exc.message)
        return JSONResponse(
            status_code=exc.status_code,
            content={"error": exc.__class__.__name__, "message": exc.message, "path": str(request.url.path)},
        )

    @app.exception_handler(Exception)
    async def unhandled_exception_handler(request: Request, exc: Exception):
        logger.exception("Unhandled exception on %s %s", request.method, request.url.path)
        return JSONResponse(
            status_code=500,
            content={"error": "InternalServerError", "message": "An unexpected error occurred.", "path": str(request.url.path)},
        )
