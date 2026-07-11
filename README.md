# AI Document Q&A Application

A local, production-shaped RAG (retrieval-augmented generation) application: upload a PDF, it gets chunked and embedded into pgvector, and you can ask questions that Claude answers using the retrieved context.

## Architecture

```
react-ui (3000)  --HTTP-->  springboot-service (8080)  --REST-->  fastapi-service (8000)
                                     |                                    |
                                     +---------------> PostgreSQL (5432) <+
                                                        (documents table +
                                                         document_chunks/pgvector)
                             Redis (6379) <--- Spring Boot (Q&A answer cache)
```

- **springboot-service** (Java 21 / Spring Boot 3): receives PDF uploads, stores metadata in PostgreSQL, calls FastAPI to process documents and answer questions, caches Q&A answers in Redis, exposes REST APIs to React, secured with HTTP Basic auth.
- **fastapi-service** (Python 3.12 / FastAPI): extracts PDF text (pypdf), splits it into chunks (LangChain), generates embeddings locally (sentence-transformers), stores/searches vectors in pgvector, and sends retrieved context + question to Claude (Anthropic API).
- **react-ui**: simple UI to upload PDFs and chat with the document.

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Python 3.12
- Node.js 18+ / npm
- PostgreSQL 15+ with the `pgvector` extension available (or use the provided Docker Compose)
- Redis (or use Docker Compose)
- An Anthropic API key ([console.anthropic.com](https://console.anthropic.com/))

## 1. Start PostgreSQL + Redis

**Option A — Docker Desktop (recommended):**

```bash
docker compose up -d
```

This starts `pgvector/pgvector:pg16` on port 5432 (auto-runs `sql-scripts/init.sql`) and Redis on port 6379.

**Option B — Existing local PostgreSQL install:**

```bash
createdb ai_docs_db
psql -d ai_docs_db -f sql-scripts/init.sql
```

Make sure the `vector` extension is installed on your PostgreSQL server before running the script (`CREATE EXTENSION vector;` requires the pgvector binary to be present).

## 2. Run the FastAPI AI service

```bash
cd fastapi-service
python -m venv venv
venv\Scripts\activate          # Windows
pip install -r requirements.txt
copy .env.example .env         # then edit .env and set ANTHROPIC_API_KEY
uvicorn main:app --reload --port 8000
```

Verify: http://localhost:8000/health should return `{"status": "UP", ...}`. Interactive docs at http://localhost:8000/docs.

The first request that needs embeddings will download the `all-MiniLM-L6-v2` model (~90MB) automatically.

## 3. Run the Spring Boot backend

```bash
cd springboot-service
mvn spring-boot:run
```

Defaults (edit `src/main/resources/application.yml` to change):
- DB: `jdbc:postgresql://localhost:5432/ai_docs_db`, user `ai_app_user` / password `ai_app_password`
- Redis: `localhost:6379`
- FastAPI base URL: `http://localhost:8000`
- Basic auth: username `admin`, password `changeme`
- Uploaded PDFs are saved to `./uploaded_files`

Verify: http://localhost:8080/actuator/health.

## 4. Run the React frontend

```bash
cd react-ui
npm install
copy .env.example .env.local    # optional: override API URL/credentials
npm start
```

Opens http://localhost:3000. Upload a PDF, wait for status `PROCESSED`, then ask a question.

## REST API Reference

### Spring Boot (`http://localhost:8080`, HTTP Basic auth required)

| Method | Path | Description |
|---|---|---|
| POST | `/api/documents/upload` | multipart `file` field (PDF). Stores metadata, saves file, triggers FastAPI processing. |
| GET | `/api/documents` | List all uploaded documents with status. |
| GET | `/api/documents/{documentId}` | Get one document's status. |
| POST | `/api/qa/ask` | Body: `{ "documentId": "<uuid or null>", "question": "..." }`. Returns answer + source chunks (Redis-cached). |

### FastAPI (`http://localhost:8000`, called internally by Spring Boot)

| Method | Path | Description |
|---|---|---|
| POST | `/api/documents/process` | Body: `{ "documentId": "...", "filePath": "..." }`. Extracts, chunks, embeds, stores in pgvector. |
| POST | `/api/qa/query` | Body: `{ "documentId": "...", "question": "..." }`. Semantic search + Claude answer. |
| GET | `/health` | Health check. |

## Configuration Reference

| Setting | Location | Notes |
|---|---|---|
| `ANTHROPIC_API_KEY` | `fastapi-service/.env` | Required for Claude answers. |
| `CLAUDE_MODEL` | `fastapi-service/.env` | Defaults to `claude-sonnet-4-5-20250929`. |
| `EMBEDDING_MODEL` | `fastapi-service/.env` | Local sentence-transformers model; no API key needed. |
| `CHUNK_SIZE` / `CHUNK_OVERLAP` | `fastapi-service/.env` | Text splitting parameters. |
| `app.security.username/password` | `springboot-service/application.yml` | Basic auth credentials — change before any non-local use. |
| `app.storage.upload-dir` | `springboot-service/application.yml` | Where PDFs are saved; must be readable by fastapi-service. |

## Exception Handling & Logging

- **Spring Boot**: `@RestControllerAdvice` (`GlobalExceptionHandler`) maps custom exceptions (`DocumentNotFoundException`, `FileStorageException`, `FastApiServiceException`, `InvalidFileTypeException`) and validation errors to structured JSON error responses. Logs to console + `logs/springboot-service.log` (rolling, via `logback-spring.xml`).
- **FastAPI**: custom `AppException` subclasses (`PdfExtractionError`, `EmbeddingGenerationError`, `LlmServiceError`, `NoRelevantContextError`, `DocumentNotFoundError`) are caught by global exception handlers in `main.py` and returned as structured JSON. Logs to console + `logs/fastapi-service.log` (rotating file handler).

## Notes on Design Choices

- **Embeddings run locally** via `sentence-transformers` (`all-MiniLM-L6-v2`, 384-dim) rather than calling an external embeddings API — this keeps the app fully local/free aside from the Claude calls themselves, and avoids requiring a second API key. Swap in OpenAI or Claude-compatible embeddings by editing `fastapi-service/app/services/embedding_service.py` (and updating `vector(384)` in `sql-scripts/init.sql` / `EMBEDDING_DIMENSION` to match the new model's output size).
- **Redis** caches Q&A answers by a hash of `(documentId, question)` to avoid repeat embedding + Claude calls for repeated questions.
- **File sharing**: Spring Boot writes uploaded PDFs to a local folder and passes the file path to FastAPI — both services must run on the same machine (as specified) so this shared path resolves for both.
- Security is intentionally simple (single HTTP Basic user) since this is a local single-user app; harden it (OAuth2/JWT, per-user accounts, HTTPS) before exposing it beyond localhost.
