===========================
WHAT IS LANGGRAPH?
===========================

** LangGraph is a framework used to build stateful AI workflows.

** In LangGraph, we represent the application using:

		State + Node + Edges + Decisions


		LangGraph = State + Nodes + Edges + Decisions

** In TestAI, this workflow lives in `fastapi-service/qa_graph/` and is
   invoked by `app/routers/qa.py` every time `/api/qa/query` is called.


============================
WHY DO WE NEED LANGGRAPH?
============================

=> A simple LangChain pipeline is usually linear:

		chain = embed | search | prompt | llm | parser

   This is exactly what `app/services/llm_service.py` was before this
   integration - a straight LCEL chain with one path, no branching.

=> Real-world AI applications often require branching and loops:


User Question
      |
      v
Classifier Node
      |
      +--> Document question -------> RAG Node
      |
      +--> Metadata question -------> Database Node
      |
      +--> Live-data question ------> REST API Node
      |
      +--> Calculation / date-time -> Tool Node
      |
      +--> Anything else -----------> General Node


=> LangGraph is useful when an application requires:

		-- Conditions & branches
		-- Loops and retries
		-- Tool Calling
		-- Multiple Agents
		-- Conversation Memory
		-- Human Approval
		-- Persistent State
		-- Streaming Execution

   TestAI's `/api/qa/query` needed exactly this once it had to handle more
   than "search a document and answer" - it now also answers metadata
   questions, calculations, and general chat, each through a different path.


=============================
LANGCHAIN VERSUS LANGGRAPH
=============================

-- LangChain

	** Build prompts, chains, retrievers and tools
	** Best for linear pipelines
	** Usually follows a fixed flow
	** Good for simple RAG
	** Limited workflow/state control

	In TestAI: `app/services/llm_service.py` - still there, unchanged,
	still LangChain (ChatPromptTemplate | ChatModel | StrOutputParser).
	Used only when `USE_LANGGRAPH=false` in .env.

-- LangGraph

	** Orchestrate complete AI workflows
	** Best for branching and looping
	** Supports dynamic execution paths
	** Good for agentic-based applications
	** State is the core concept

	In TestAI: `qa_graph/` - the classifier decides, at runtime, which of
	five nodes handles a given question.


=========
STATE
=========

-> State stores the data shared between all nodes.

Generic example:

class ChatState(TypedDict, total=False):
    question: str
    category: str
    context: str
    answer: str

TestAI's real state - `qa_graph/state.py`:

    from typing import List, Literal, Optional
    from typing_extensions import TypedDict
    from uuid import UUID

    from app.schemas import SourceChunk

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

Note it mirrors `QueryRequest`/`QueryResponse` from `app/schemas.py` - the
FastAPI contract that Spring Boot depends on never changes, no matter which
node produced the answer.


============
NODE
============

** A node is a Python function that performs one task.

Generic example:

def fee_node(state):
    return {
        "context": "The course fee is INR 18,000."
    }

TestAI's real database node - `qa_graph/nodes.py` (trimmed):

    def database_node(state: GraphState, config: RunnableConfig) -> dict:
        db = config["configurable"]["db"]
        question_lower = state["question"].lower()

        sql, template = _DEFAULT_DB_QUERY
        for keywords, candidate_sql, candidate_template in _DB_QUERIES:
            if any(kw in question_lower for kw in keywords):
                sql, template = candidate_sql, candidate_template
                break

        count = db.execute(text(sql)).scalar_one()
        return {"db_result": template.format(count)}

Two differences from the generic example worth noticing:
  1. The SQLAlchemy `Session` isn't stored in state - it's injected through
     `config["configurable"]["db"]` per request (state can be checkpointed;
     a live DB connection shouldn't be).
  2. The SQL is a fixed, keyword-matched query, never LLM-generated -
     keeps this node injection-proof.


==========
EDGE
==========

** An edge connects one node to another node.

Generic example:  course_node -----------> fee_node

    builder.add_edge("fee", "answer")

TestAI's real edges - `qa_graph/graph.py`:

    for node in ("rag", "database", "rest_api", "tool", "general"):
        builder.add_edge(node, "answer_generator")

    builder.add_edge("answer_generator", END)

Every branch converges on one `answer_generator` node - there's a single
place that ever calls the LLM to produce the user-facing answer, regardless
of which route fired.


==================
CONDITIONAL EDGE
==================

** A conditional edge decides which node should execute next.

Generic example:

    builder.add_conditional_edges(
        "classify",
        route_question,
        {
            "course": "course",
            "fee": "fee"
        }
    )

TestAI's real conditional edge - `qa_graph/graph.py`:

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

`route_by_category` is a one-liner in `qa_graph/nodes.py`:

    def route_by_category(state: GraphState) -> RouteCategory:
        return state["category"]


=======================================
TestAI QA GRAPH ARCHITECTURE
=======================================

** Handles document Q&A, app-metadata questions, external data (future),
   calculations/date-time, and general chit-chat.


                         +--> RAG Node -----------+
                         |                        |
START -> Classifier Node-+--> Database Node ------+-> Answer Generator -> END
                         |                        |
                         +--> REST API Node ------+
                         |                        |
                         +--> Tool Node -----------+
                         |                        |
                         +--> General Node --------+


"What does the document say about refunds?" -> RAG node
"How many documents have been uploaded?"     -> Database node
(future external integration, e.g. weather)  -> REST API node
"What is 23 * 47?"                            -> Tool node
"Hello"                                       -> General node


===============================
PROJECT SETUP - HOW IT'S WIRED IN TESTAI
===============================

Unlike a fresh tutorial project, this was integrated into your existing
`fastapi-service`, so the setup steps look a little different:

1) Project already exists - no new project was created.
   `fastapi-service/qa_graph/` is a new top-level package, sibling to `app/`.
   (Not named `langgraph/` - that name is already taken by the installed
   pip package, and a same-named local folder would shadow it.)

2) `requirements.txt` already has the libraries added - OpenAI, not Ollama,
   since this app is OpenAI-only per your requirement:

    langchain-core==0.3.86
    langchain-openai==0.3.35
    langgraph==1.0.1

3) Virtual environment already exists at `fastapi-service/venv` - activate
   it and run `pip install -r requirements.txt` to pick up `langgraph`.

4) No local model server step (no `ollama serve`) - `ChatOpenAI` calls the
   OpenAI API directly using `OPENAI_API_KEY` from `.env`.

5) The graph logic is split across four files instead of one `app.py`,
   matching the rest of this project's structure (`app/services/`,
   `app/routers/` are also split by responsibility):

   qa_graph/state.py   - GraphState (shown above)
   qa_graph/tools.py   - calculator + current_datetime tools
   qa_graph/nodes.py   - classifier_node, rag_node, database_node,
                         rest_api_node, tool_node, general_node,
                         answer_generator_node
   qa_graph/graph.py   - builds the StateGraph, compiles it, exposes
                         run_qa_graph()

   Full build in `qa_graph/graph.py`:

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

        return builder.compile(checkpointer=InMemorySaver())

    _compiled_graph = _build_graph()


===============================
RUN THE GRAPH
===============================

There's no CLI while-loop here - the graph is compiled once at import time
and invoked once per HTTP request, from `app/routers/qa.py`:

    def _answer_via_graph(db: Session, request: QueryRequest):
        final_state = run_qa_graph(
            db=db,
            question=request.question,
            document_id=request.document_id,
            top_k=request.top_k,
        )

        if final_state.get("category") == "rag" and final_state.get("no_context"):
            raise NoRelevantContextError(str(request.document_id))

        return final_state["answer"], final_state.get("sources", []), final_state["model"]

To try it yourself, start the service and POST a question:

    cd fastapi-service
    venv\Scripts\activate
    uvicorn main:app --reload --port 8000

    curl -X POST http://localhost:8000/api/qa/query ^
      -H "Content-Type: application/json" ^
      -d "{\"question\": \"What is 23 * 47?\"}"

Watch the terminal or `logs/fastapi-service.log` - every request logs
`Classifier routed question to category=...`, the same way the reference
example prints `Classifier selected: ...`.
