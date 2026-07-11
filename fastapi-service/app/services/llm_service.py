"""
Sends retrieved context + the user's question to an LLM and returns the answer.
Supports two interchangeable providers, selected via LLM_PROVIDER in .env:
  - "anthropic" (Claude, default)
  - "openai"
"""
import logging
from typing import List

from app.config import get_settings
from app.exceptions import LlmServiceError

logger = logging.getLogger(__name__)
settings = get_settings()

_anthropic_client = None
_openai_client = None

SYSTEM_PROMPT = (
    "You are a helpful assistant answering questions about a user-uploaded document. "
    "Only use the provided context to answer. If the context does not contain the answer, "
    "say you don't have enough information in the document rather than guessing."
)


def _build_user_message(question: str, context_chunks: List[str]) -> str:
    context_block = "\n\n---\n\n".join(context_chunks) if context_chunks else "(no context retrieved)"
    return (
        f"Context from the document:\n\n{context_block}\n\n"
        f"Question: {question}\n\n"
        "Answer using only the context above."
    )


def _get_anthropic_client():
    global _anthropic_client
    if _anthropic_client is None:
        import anthropic

        if not settings.anthropic_api_key:
            raise LlmServiceError("ANTHROPIC_API_KEY is not configured. Set it in fastapi-service/.env")
        _anthropic_client = anthropic.Anthropic(api_key=settings.anthropic_api_key)
    return _anthropic_client


def _get_openai_client():
    global _openai_client
    if _openai_client is None:
        import openai

        if not settings.openai_api_key:
            raise LlmServiceError("OPENAI_API_KEY is not configured. Set it in fastapi-service/.env")
        _openai_client = openai.OpenAI(api_key=settings.openai_api_key)
    return _openai_client


def _ask_anthropic(question: str, context_chunks: List[str]) -> str:
    client = _get_anthropic_client()
    response = client.messages.create(
        model=settings.claude_model,
        max_tokens=1024,
        system=SYSTEM_PROMPT,
        messages=[{"role": "user", "content": _build_user_message(question, context_chunks)}],
    )
    answer = "".join(block.text for block in response.content if block.type == "text").strip()
    return answer or "Claude did not return a text response."


def _ask_openai(question: str, context_chunks: List[str]) -> str:
    client = _get_openai_client()
    response = client.chat.completions.create(
        model=settings.openai_model,
        max_tokens=1024,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": _build_user_message(question, context_chunks)},
        ],
    )
    answer = (response.choices[0].message.content or "").strip()
    return answer or "OpenAI did not return a text response."


def active_model_name() -> str:
    """Returns whichever model name is actually in use, for the API response."""
    return settings.openai_model if settings.llm_provider.lower() == "openai" else settings.claude_model


def generate_answer(question: str, context_chunks: List[str]) -> str:
    provider = settings.llm_provider.lower().strip()

    try:
        if provider == "openai":
            return _ask_openai(question, context_chunks)
        elif provider == "anthropic":
            return _ask_anthropic(question, context_chunks)
        else:
            raise LlmServiceError(
                f"Unknown LLM_PROVIDER '{settings.llm_provider}'. Set it to 'anthropic' or 'openai' in .env"
            )
    except LlmServiceError:
        raise
    except Exception as exc:
        logger.exception("%s API call failed", provider)
        raise LlmServiceError(str(exc)) from exc
