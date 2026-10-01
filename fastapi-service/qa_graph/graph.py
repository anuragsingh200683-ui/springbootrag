"""
Builds and compiles the LangGraph StateGraph for the QA workflow, and exposes
run_qa_graph() as the single entry point used by app/routers/qa.py.

The graph is compiled once at import time (cheap - it's just structure).
Per-request data (the SQLAlchemy session, the checkpoint thread id) is
injected via RunnableConfig on each .invoke() call, not baked into the graph.
"""
import logging
from typing import Optional
from uuid import UUID, uuid4

from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph
from sqlalchemy.orm import Session

from qa_graph.nodes import (
    answer_generator_node,
    classifier_node,
    database_node,
    general_node,
    rag_node,
    rest_api_node,
    route_by_category,
    tool_node,
)
from qa_graph.state import GraphState

logger = logging.getLogger(__name__)


def _build_graph():
    builder = StateGraph(GraphState)

    builder.add_node("classifier", classifier_node)
    builder.add_node("rag", rag_node)
    builder.add_node("database", database_node)
    builder.add_node("rest_api", rest_api_node)
    builder.add_node("tool", tool_node)
    builder.add_node("general", general_node)
    builder.add_node("answer_generator", answer_generator_node)

    builder.add_edge(START, "classifier")
    builder.add_conditional_edges(
        "classifier",
        route_by_category,
        {
            "rag": "rag",
            "database": "database",
            "rest_api": "rest_api",
            "tool": "tool",
            "general": "general",
        },
    )

    for node in ("rag", "database", "rest_api", "tool", "general"):
        builder.add_edge(node, "answer_generator")

    builder.add_edge("answer_generator", END)

    # In-process checkpointer: gives per-thread continuity within a running
    # instance. Swap for langgraph's PostgresSaver later (same graph shape,
    # Postgres is already in this stack) if checkpoints need to survive a
    # restart.
    return builder.compile(checkpointer=InMemorySaver())


# Compiled once per process - reused across requests, same as the module-level
# `engine` in app/database.py.
_compiled_graph = _build_graph()


def run_qa_graph(
    db: Session,
    question: str,
    document_id: Optional[UUID] = None,
    top_k: Optional[int] = None,
) -> dict:
    """
    Runs the QA graph for a single question and returns the final state dict.
    Raises whatever the underlying nodes raise (e.g. LlmServiceError) -
    app/routers/qa.py decides how to translate that into an HTTP response,
    exactly as it did for the pre-LangGraph code path.
    """
    # Each call answers exactly one question in isolation - initial_state above is
    # rebuilt from scratch every time, and nothing here reads back a prior turn's
    # state. thread_id used to be str(document_id), or the single literal
    # "no-document" when no document was selected: LangGraph's checkpointer merges
    # each node's partial update onto whatever state is already checkpointed for a
    # thread_id, so reusing document_id (or that one shared constant) as the key let
    # an earlier, unrelated question's leftover fields (sources, context_chunks,
    # no_context, tool_result, ...) leak into a later question's response - and, for
    # every request with no document selected, across different users entirely,
    # since they all shared "no-document". A fresh id per call gives each question
    # its own checkpoint, so there is nothing to inherit.
    thread_id = str(uuid4())

    initial_state: GraphState = {
        "question": question,
        "document_id": document_id,
        "top_k": top_k,
    }

    return _compiled_graph.invoke(
        initial_state,
        config={"configurable": {"db": db, "thread_id": thread_id}},
    )
