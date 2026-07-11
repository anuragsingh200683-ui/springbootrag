"""
Splits extracted document text into overlapping chunks suitable for embedding.
"""
import logging
from typing import List

from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()


def split_text_into_chunks(text: str) -> List[str]:
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=settings.chunk_size,
        chunk_overlap=settings.chunk_overlap,
        separators=["\n\n", "\n", ". ", " ", ""],
    )
    chunks = splitter.split_text(text)
    chunks = [c.strip() for c in chunks if c.strip()]
    logger.info("Split text (%d chars) into %d chunks", len(text), len(chunks))
    return chunks
