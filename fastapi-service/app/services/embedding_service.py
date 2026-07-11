"""
Generates vector embeddings for text chunks using a local sentence-transformers
model (no external API key required, keeps the app fully local/free to run).
"""
import logging
import threading
from typing import List

from sentence_transformers import SentenceTransformer

from app.config import get_settings
from app.exceptions import EmbeddingGenerationError

logger = logging.getLogger(__name__)
settings = get_settings()

_model = None
_model_lock = threading.Lock()


def _get_model() -> SentenceTransformer:
    global _model
    if _model is None:
        with _model_lock:
            if _model is None:
                logger.info("Loading embedding model '%s' (first call may take a while)...", settings.embedding_model)
                _model = SentenceTransformer(settings.embedding_model)
    return _model


def generate_embeddings(texts: List[str]) -> List[List[float]]:
    if not texts:
        return []
    try:
        model = _get_model()
        vectors = model.encode(texts, show_progress_bar=False, convert_to_numpy=True)
        return [vec.tolist() for vec in vectors]
    except Exception as exc:
        logger.exception("Embedding generation failed")
        raise EmbeddingGenerationError(str(exc)) from exc


def generate_embedding(text: str) -> List[float]:
    return generate_embeddings([text])[0]
