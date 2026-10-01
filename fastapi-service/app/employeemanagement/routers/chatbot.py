"""
POST /api/ems/ai/chat

General-purpose HR helpdesk chatbot. Optionally grounded with one employee's
context and/or prior conversation turns, both supplied by the caller (this
service holds no conversation state itself).
"""
import logging

from fastapi import APIRouter

from app.employeemanagement.schemas import ChatRequest, ChatResponse
from app.employeemanagement.services.llm_client import active_model_name, generate
from app.employeemanagement.services.prompt_utils import describe_employee

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/ems/ai", tags=["AI Assistant"])

SYSTEM_PROMPT = (
    "You are an HR helpdesk chatbot for an Employee Management System. Answer the "
    "user's question helpfully and concisely. If employee context is provided, you "
    "may reference it. Do not make up company policies, salary figures, or personal "
    "data you were not given - say you don't have that information instead. Keep "
    "answers under 150 words unless the user explicitly asks for more detail."
)

# Only the most recent turns are sent - keeps prompts small and avoids unbounded growth.
_MAX_HISTORY_TURNS = 10


@router.post("/chat", response_model=ChatResponse)
def chat(request: ChatRequest):
    logger.info("HR chatbot message received (length=%d chars)", len(request.message))

    parts = []
    if request.employeeContext:
        parts.append("Employee context:\n" + describe_employee(request.employeeContext))
    if request.history:
        history_text = "\n".join(f"{m.role}: {m.content}" for m in request.history[-_MAX_HISTORY_TURNS:])
        parts.append("Conversation so far:\n" + history_text)
    parts.append(f"User: {request.message}")

    reply = generate(SYSTEM_PROMPT, "\n\n".join(parts))
    return ChatResponse(reply=reply, model=active_model_name())
