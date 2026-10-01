"""
EMS AI Assistant specific exceptions. Subclasses of the existing
app.exceptions.AppException so they get the same JSON error shape and are
handled by the app-wide handler already registered in main.py - no changes
needed there.
"""
from app.exceptions import AppException


class AiServiceError(AppException):
    """Raised when the underlying LLM call fails or its response can't be used."""

    def __init__(self, detail: str):
        super().__init__(f"AI service call failed: {detail}", status_code=502)
