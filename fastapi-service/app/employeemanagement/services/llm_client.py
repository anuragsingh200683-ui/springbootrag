"""
Generic LLM client for the EMS AI Assistant module.

Separate from app/services/llm_service.py (which is hard-wired to the
document Q&A system prompt/context format) but reuses the same Settings
- api keys, model names, provider choice - so there's still only one place
(.env) to configure LLM credentials for the whole fastapi-service.
"""
import logging

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate

from app.config import get_settings
from app.employeemanagement.exceptions import AiServiceError

logger = logging.getLogger(__name__)
settings = get_settings()

# Cache of built chains, keyed by provider name (mirrors llm_service.py's pattern).
_chains = {}

# Single placeholder-only template - actual prompt text always flows through
# the invoke() variables below, never through the template string itself, so
# literal "{"/"}" characters in a caller's prompt (e.g. JSON instructions in
# nl_search.py) can never be mistaken for template placeholders.
_PROMPT = ChatPromptTemplate.from_messages(
    [
        ("system", "{system_prompt}"),
        ("human", "{user_prompt}"),
    ]
)


def _build_chain(provider: str):
    if provider == "anthropic":
        if not settings.anthropic_api_key:
            raise AiServiceError("ANTHROPIC_API_KEY is not configured. Set it in fastapi-service/.env")
        from langchain_anthropic import ChatAnthropic

        llm = ChatAnthropic(model=settings.claude_model, api_key=settings.anthropic_api_key, max_tokens=1024)
    elif provider == "openai":
        if not settings.openai_api_key:
            raise AiServiceError("OPENAI_API_KEY is not configured. Set it in fastapi-service/.env")
        from langchain_openai import ChatOpenAI

        llm = ChatOpenAI(model=settings.openai_model, api_key=settings.openai_api_key, max_tokens=1024)
    else:
        raise AiServiceError(
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


def generate(system_prompt: str, user_prompt: str) -> str:
    """Runs one system+human prompt through the configured LLM provider and returns raw text."""
    provider = settings.llm_provider.lower().strip()
    try:
        chain = _get_chain(provider)
        result = chain.invoke({"system_prompt": system_prompt, "user_prompt": user_prompt}).strip()
        return result or f"{provider.title()} did not return a text response."
    except AiServiceError:
        raise
    except Exception as exc:
        logger.exception("%s API call failed (EMS AI assistant)", provider)
        raise AiServiceError(str(exc)) from exc
