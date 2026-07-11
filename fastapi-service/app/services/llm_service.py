"""
Sends retrieved context + the user's question to an LLM and returns the answer.

Built as a LangChain LCEL chain (ChatPromptTemplate | chat model | StrOutputParser)
so the prompt, model, and output parsing are composable/swappable pieces rather than
provider-specific request/response handling. Supports two interchangeable providers,
selected via LLM_PROVIDER in .env:
  - "anthropic" (Claude, default)
  - "openai"
"""
import logging
from typing import List

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate

from app.config import get_settings
from app.exceptions import LlmServiceError

logger = logging.getLogger(__name__)
settings = get_settings()

# Cache of built chains, keyed by provider name, so the underlying chat model client
# is constructed once (mirrors the previous lazy-singleton client pattern).
_chains = {}

SYSTEM_PROMPT = (
    "You are a helpful assistant answering questions about a user-uploaded document. "
    "Only use the provided context to answer. If the context does not contain the answer, "
    "say you don't have enough information in the document rather than guessing."
)

_PROMPT = ChatPromptTemplate.from_messages(
    [
        ("system", SYSTEM_PROMPT),
        (
            "human",
            "Context from the document:\n\n{context}\n\n"
            "Question: {question}\n\n"
            "Answer using only the context above.",
        ),
    ]
)


def _build_context_block(context_chunks: List[str]) -> str:
    return "\n\n---\n\n".join(context_chunks) if context_chunks else "(no context retrieved)"


def _build_chain(provider: str):
    if provider == "anthropic":
        if not settings.anthropic_api_key:
            raise LlmServiceError("ANTHROPIC_API_KEY is not configured. Set it in fastapi-service/.env")
        from langchain_anthropic import ChatAnthropic

        llm = ChatAnthropic(model=settings.claude_model, api_key=settings.anthropic_api_key, max_tokens=1024)
    elif provider == "openai":
        if not settings.openai_api_key:
            raise LlmServiceError("OPENAI_API_KEY is not configured. Set it in fastapi-service/.env")
        from langchain_openai import ChatOpenAI

        llm = ChatOpenAI(model=settings.openai_model, api_key=settings.openai_api_key, max_tokens=1024)
    else:
        raise LlmServiceError(
            f"Unknown LLM_PROVIDER '{settings.llm_provider}'. Set it to 'anthropic' or 'openai' in .env"
        )

    return _PROMPT | llm | StrOutputParser()


def _get_chain(provider: str):
    if provider not in _chains:
        _chains[provider] = _build_chain(provider)
    return _chains[provider]


def active_model_name() -> str:
    """Returns whichever model name is actually in use, for the API response."""
    return settings.openai_model if settings.llm_provider.lower() == "openai" else settings.claude_model


def generate_answer(question: str, context_chunks: List[str]) -> str:
    provider = settings.llm_provider.lower().strip()
    context_block = _build_context_block(context_chunks)

    try:
        chain = _get_chain(provider)
        answer = chain.invoke({"question": question, "context": context_block}).strip()
        return answer or f"{provider.title()} did not return a text response."
    except LlmServiceError:
        raise
    except Exception as exc:
        logger.exception("%s API call failed", provider)
        raise LlmServiceError(str(exc)) from exc
