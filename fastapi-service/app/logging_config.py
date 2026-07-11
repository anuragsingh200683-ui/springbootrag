"""
Logging configuration for the FastAPI AI service.
Logs to console (stdout) and a rotating file under ./logs/fastapi-service.log
"""
import logging
import logging.handlers
import os
from app.config import get_settings

LOG_DIR = "logs"
LOG_FILE = os.path.join(LOG_DIR, "fastapi-service.log")


def configure_logging() -> None:
    settings = get_settings()
    os.makedirs(LOG_DIR, exist_ok=True)

    log_format = "%(asctime)s | %(levelname)-8s | %(name)s | %(message)s"
    formatter = logging.Formatter(log_format)

    root_logger = logging.getLogger()
    root_logger.setLevel(settings.log_level.upper())

    # Avoid duplicate handlers on reload
    if root_logger.handlers:
        return

    console_handler = logging.StreamHandler()
    console_handler.setFormatter(formatter)
    root_logger.addHandler(console_handler)

    file_handler = logging.handlers.RotatingFileHandler(
        LOG_FILE, maxBytes=5 * 1024 * 1024, backupCount=5, encoding="utf-8"
    )
    file_handler.setFormatter(formatter)
    root_logger.addHandler(file_handler)

    # Quiet down noisy third-party loggers
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
    logging.getLogger("sentence_transformers").setLevel(logging.WARNING)
