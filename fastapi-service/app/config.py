"""
Centralized application configuration, loaded from environment variables / .env file.
"""
from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # Database
    database_url: str = "postgresql+psycopg2://ai_app_user:ai_app_password@localhost:5432/ai_docs_db"

    # LLM provider selection: "anthropic" (Claude) or "openai"
    llm_provider: str = "anthropic"

    # Claude / Anthropic
    anthropic_api_key: str = ""
    claude_model: str = "claude-sonnet-4-5-20250929"

    # OpenAI (used only when llm_provider = "openai")
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"

    # Embeddings (local model, no external API required)
    embedding_model: str = "all-MiniLM-L6-v2"
    embedding_dimension: int = 384

    # Chunking
    chunk_size: int = 1000
    chunk_overlap: int = 150

    # Semantic search
    top_k_results: int = 4

    # Shared upload directory (written by Spring Boot, read by FastAPI)
    upload_dir: str = "./uploaded_files"

    # Logging
    log_level: str = "INFO"


@lru_cache
def get_settings() -> Settings:
    return Settings()
