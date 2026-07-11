"""
Extracts raw text from a PDF file on disk.
"""
import logging
import os

from pypdf import PdfReader

from app.exceptions import PdfExtractionError

logger = logging.getLogger(__name__)


def extract_text_from_pdf(file_path: str) -> str:
    if not os.path.isfile(file_path):
        raise PdfExtractionError(f"File not found at path: {file_path}")

    try:
        reader = PdfReader(file_path)
        pages_text = []
        for page_number, page in enumerate(reader.pages):
            text = page.extract_text() or ""
            pages_text.append(text)
        full_text = "\n\n".join(pages_text).strip()
    except Exception as exc:  # pypdf can raise several different error types
        logger.exception("Failed reading PDF at %s", file_path)
        raise PdfExtractionError(str(exc)) from exc

    if not full_text:
        raise PdfExtractionError("No extractable text found (PDF may be scanned/image-only).")

    logger.info("Extracted %d characters from %s (%d pages)", len(full_text), file_path, len(reader.pages))
    return full_text
