"""
LangGraph orchestration layer for the FastAPI AI service.

Lives as a top-level package (sibling to app/), not app.langgraph, and is
deliberately NOT named "langgraph" - that name is already taken by the
installed langgraph pip package. A folder literally named langgraph sitting
next to main.py would shadow the real package the moment anything does
`import langgraph`, silently breaking the whole graph. qa_graph avoids that
collision entirely.

This package is purely additive: it does not modify any existing service,
model, or schema in app/. It reuses app.services.*, app.config, and
app.exceptions as-is, and is wired into the existing /api/qa/query endpoint
from app/routers/qa.py.
"""
