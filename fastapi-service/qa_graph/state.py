"""
Shared graph state for the QA LangGraph workflow.

This is new and internal to the graph - it does not replace or change
app/schemas.py. QueryRequest/QueryResponse (the contract Spring Boot depends
on) are untouched; GraphState only flows between graph.py and nodes.py here
in qa_graph/.
"""
from typing import List, Literal, Optional
from typing_extensions import TypedDict
from uuid import UUID

from app.schemas import SourceChunk

# The five routes the classifier can pick between.
RouteCategory = Literal["rag", "database", "rest_api", "tool", "general"]


class GraphState(TypedDict, total=False):
    # ---- inputs, mirror QueryRequest ----
    question: str
    document_id: Optional[UUID]
    top_k: Optional[int]

    # ---- classifier output ----
    category: RouteCategory

    # ---- branch outputs (only the branch that ran populates its field) ----
    context_chunks: List[str]
    sources: List[SourceChunk]
    no_context: bool
    db_result: str
    api_result: str
    tool_result: str

    # ---- final output, mirrors QueryResponse ----
    answer: str
    model: str
