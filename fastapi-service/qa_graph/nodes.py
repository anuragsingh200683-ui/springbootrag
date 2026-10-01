"""
LangGraph node implementations for the QA workflow.

Design notes:
- Every node has the standard LangGraph node signature
  (state: GraphState, config: RunnableConfig) -> dict, and returns only the
  keys it updates - LangGraph merges partial updates into the running state.
- The SQLAlchemy `Session` for the current request is deliberately NOT part
  of GraphState (state can be checkpointed, and a live DB session has no
  business being serialized). It is injected per-request via
  RunnableConfig["configurable"]["db"] instead - see graph.py.
- All existing services (embedding_service, vector_store_service) are
  imported and used exactly as they were in the pre-LangGraph qa.py - only
  the orchestration around them changed.
- Per your requirement, every node's LLM is OpenAI-only (ChatOpenAI), which
  is independent of LLM_PROVIDER (that setting only affects the legacy
  app/services/llm_service.py chain used when USE_LANGGRAPH=false).
"""
import logging

from langchain_core.messages import HumanMessage, SystemMessage
from langchain_core.runnables import RunnableConfig
from langchain_openai import ChatOpenAI
from pydantic import BaseModel, Field
from sqlalchemy import text

from app.config import get_settings
from app.exceptions import LlmServiceError
from app.services.embedding_service import generate_embedding
from app.services.vector_store_service import semantic_search
from qa_graph.state import GraphState, RouteCategory
from qa_graph.tools import TOOLS, TOOLS_BY_NAME

logger = logging.getLogger(__name__)
settings = get_settings()

# Cache of ChatOpenAI clients so each (temperature, bind_tools) combination is
# built once per process, mirroring the lazy-singleton pattern already used
# in app/services/llm_service.py.
_llm_cache = {}


def _get_llm(temperature: float = 0.0, bind_tools: bool = False):
    if not settings.openai_api_key:
        raise LlmServiceError(
            "OPENAI_API_KEY is not configured. Set it in fastapi-service/.env - "
            "required for the LangGraph nodes regardless of LLM_PROVIDER."
        )
    key = (temperature, bind_tools)
    if key not in _llm_cache:
        llm = ChatOpenAI(model=settings.openai_model, api_key=settings.openai_api_key, temperature=temperature)
        _llm_cache[key] = llm.bind_tools(TOOLS) if bind_tools else llm
    return _llm_cache[key]


# ---------------------------------------------------------------------------
# Classifier node
# ---------------------------------------------------------------------------

class RouteDecision(BaseModel):
    category: RouteCategory = Field(
        ...,
        description=(
            "rag: question about the content of an uploaded document. "
            "database: question about app metadata, e.g. how many documents "
            "have been uploaded/processed, or how many questions have been asked. "
            "rest_api: question needing live external data (e.g. weather). "
            "tool: an arithmetic calculation, or a request for the current date/time. "
            "general: anything else - general knowledge or chit-chat."
        ),
    )


CLASSIFIER_SYSTEM_PROMPT = (
    "You are a routing classifier for a document Q&A application. "
    "Classify the user's question into exactly one category: "
    "rag, database, rest_api, tool, or general."
)


def classifier_node(state: GraphState, config: RunnableConfig) -> dict:
    question = state["question"]
    has_document = state.get("document_id") is not None
    context_hint = "A document IS selected for this question." if has_document else "No document is selected."

    try:
        structured_llm = _get_llm().with_structured_output(RouteDecision)
        decision: RouteDecision = structured_llm.invoke(
            [
                SystemMessage(content=CLASSIFIER_SYSTEM_PROMPT),
                HumanMessage(content=f"{context_hint}\n\nQuestion: {question}"),
            ]
        )
        category = decision.category
    except Exception:
        # Fail safe rather than failing the request: default to the behavior
        # the app had before LangGraph existed (always RAG when a document is
        # selected), so a classifier hiccup never breaks the primary use case.
        logger.exception("Classifier call failed, falling back to a safe default category")
        category = "rag" if has_document else "general"

    logger.info("Classifier routed question to category=%s", category)
    return {"category": category}


def route_by_category(state: GraphState) -> RouteCategory:
    return state["category"]


# ---------------------------------------------------------------------------
# RAG node - unchanged embedding + vector search, reused as-is
# ---------------------------------------------------------------------------

def rag_node(state: GraphState, config: RunnableConfig) -> dict:
    db = config["configurable"]["db"]

    query_embedding = generate_embedding(state["question"])
    sources = semantic_search(
        db,
        query_embedding=query_embedding,
        document_id=state.get("document_id"),
        top_k=state.get("top_k"),
    )

    if not sources:
        return {"sources": [], "context_chunks": [], "no_context": True}

    return {
        "sources": sources,
        "context_chunks": [s.chunk_text for s in sources],
        "no_context": False,
    }


# ---------------------------------------------------------------------------
# Database node - a fixed set of safe, read-only queries.
# Deliberately NOT LLM-generated SQL: keyword matching keeps this
# injection-proof and predictable given the app's current small query surface.
# ---------------------------------------------------------------------------

_DB_QUERIES = [
    (
        ("processed", "indexed"),
        "SELECT COUNT(*) FROM documents WHERE status = 'PROCESSED'",
        "{0} document(s) have finished processing.",
    ),
    (
        ("how many question", "number of question", "qa history", "questions asked", "questions have"),
        "SELECT COUNT(*) FROM qa_history",
        "{0} question(s) have been asked so far.",
    ),
    (
        ("how many document", "number of document", "count of document", "total document", "documents have"),
        "SELECT COUNT(*) FROM documents",
        "There are {0} document(s) uploaded in total.",
    ),
]
_DEFAULT_DB_QUERY = ("SELECT COUNT(*) FROM documents", "There are {0} document(s) uploaded in total.")


def database_node(state: GraphState, config: RunnableConfig) -> dict:
    db = config["configurable"]["db"]
    question_lower = state["question"].lower()

    sql, template = _DEFAULT_DB_QUERY
    for keywords, candidate_sql, candidate_template in _DB_QUERIES:
        if any(kw in question_lower for kw in keywords):
            sql, template = candidate_sql, candidate_template
            break

    try:
        count = db.execute(text(sql)).scalar_one()
        result = template.format(count)
    except Exception:
        logger.exception("Database node query failed")
        result = "I couldn't read that information from the database right now."

    return {"db_result": result}


# ---------------------------------------------------------------------------
# REST API node - template/stub for a real external integration
# ---------------------------------------------------------------------------

def rest_api_node(state: GraphState, config: RunnableConfig) -> dict:
    # Intentionally a stub: wire in a real external call here (e.g. via
    # httpx) when you have a concrete API to integrate. The graph shape
    # (classifier -> rest_api -> answer_generator) will not need to change.
    result = (
        "This is a placeholder REST API node - no external call is wired in yet. "
        "Replace the body of rest_api_node() in qa_graph/nodes.py with a real "
        "HTTP call when you have an API to integrate."
    )
    return {"api_result": result}


# ---------------------------------------------------------------------------
# Tool node - LLM decides which tool(s) to call, we execute, single pass
# ---------------------------------------------------------------------------

def tool_node(state: GraphState, config: RunnableConfig) -> dict:
    llm_with_tools = _get_llm(bind_tools=True)
    ai_message = llm_with_tools.invoke(
        [
            SystemMessage(content="Use the available tools to answer the question precisely."),
            HumanMessage(content=state["question"]),
        ]
    )

    if not ai_message.tool_calls:
        return {"tool_result": ai_message.content or "No tool was needed for this question."}

    outputs = []
    for call in ai_message.tool_calls:
        tool_fn = TOOLS_BY_NAME.get(call["name"])
        if tool_fn is None:
            outputs.append(f"Unknown tool requested: {call['name']}")
            continue
        try:
            outputs.append(str(tool_fn.invoke(call["args"])))
        except Exception as exc:
            logger.exception("Tool '%s' execution failed", call["name"])
            outputs.append(f"Tool '{call['name']}' failed: {exc}")

    return {"tool_result": "\n".join(outputs)}


# ---------------------------------------------------------------------------
# General node - no retrieval; answer_generator_node does the actual LLM call
# ---------------------------------------------------------------------------

def general_node(state: GraphState, config: RunnableConfig) -> dict:
    return {}


# ---------------------------------------------------------------------------
# Answer generator - the single place that produces the final answer for
# every category, mirroring the prompt style already used in llm_service.py.
# ---------------------------------------------------------------------------

_ANSWER_SYSTEM_PROMPTS = {
    "rag": (
        "You are a helpful assistant answering questions about a user-uploaded document. "
        "Only use the provided context to answer. If the context does not contain the "
        "answer, say you don't have enough information in the document rather than guessing."
    ),
    "database": "You are a helpful assistant. Use the provided database result to answer the question concisely.",
    "rest_api": "You are a helpful assistant. Use the provided data to answer the question concisely.",
    "tool": "You are a helpful assistant. Use the provided tool result to answer the question concisely.",
    "general": "You are a helpful, general-purpose assistant. Answer the question directly and concisely.",
}


def _context_for(state: GraphState) -> str:
    category = state["category"]
    if category == "rag":
        chunks = state.get("context_chunks") or []
        return "\n\n---\n\n".join(chunks) if chunks else "(no context retrieved)"
    if category == "database":
        return state.get("db_result", "")
    if category == "rest_api":
        return state.get("api_result", "")
    if category == "tool":
        return state.get("tool_result", "")
    return ""


def answer_generator_node(state: GraphState, config: RunnableConfig) -> dict:
    category = state["category"]
    system_prompt = _ANSWER_SYSTEM_PROMPTS[category]

    if category == "general":
        human_content = state["question"]
    else:
        context = _context_for(state)
        human_content = f"Context:\n\n{context}\n\nQuestion: {state['question']}\n\nAnswer using only the context above."

    try:
        llm = _get_llm()
        response = llm.invoke([SystemMessage(content=system_prompt), HumanMessage(content=human_content)])
        answer = (response.content or "").strip() or "The model did not return a text response."
    except LlmServiceError:
        raise
    except Exception as exc:
        logger.exception("Answer generator LLM call failed")
        raise LlmServiceError(str(exc)) from exc

    return {"answer": answer, "model": settings.openai_model}
