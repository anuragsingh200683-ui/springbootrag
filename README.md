# AI Document Q&A + Employee Management System

Two local apps share one Spring Boot backend:

- **AI Document Q&A** (`react-ui`): upload a PDF. It gets chunked and embedded into pgvector, and you can ask questions that OpenAI answers using the retrieved context.
- **Employee Management System** (`react-ui/ems-app`): departments, designations, employees, attendance, leave, a dashboard, and an AI assistant.

All API access is secured with **JWTs issued by Keycloak**. The backend never stores usernames or passwords.

## Architecture

```
react-ui (3000) ─┐                                      ┌─> PostgreSQL + pgvector (5432)
                 ├─ Bearer JWT ─> springboot-service ───┼─> Redis (6379, Q&A answer cache)
ems-app  (5173) ─┘                (8090)                └─> OpenAI API (chat + embeddings)
      │                              │
      └── login (Auth Code + PKCE) ──┴── validates tokens with ──> Keycloak (8180)
```

- **springboot-service** (Java 26 / Spring Boot 4.1) handles everything:
  - Stores uploaded PDFs and document metadata.
  - Runs the whole AI pipeline **in-process**: PDF text extraction (PDFBox) → chunking → embeddings (OpenAI `text-embedding-3-small`, 1536-dim) → pgvector storage.
  - Answers questions through a classifier graph that routes each question to one of rag / database / rest_api / tool / general, then generates the answer (`com.example.aiapp.qa`).
  - Caches answers in Redis.
  - Hosts the EMS REST API and its AI assistant.
  - Acts as an **OAuth2 Resource Server**, validating Keycloak-issued JWTs.
- **Keycloak** (Docker) is the authorization server. It owns the users, passwords and roles, and issues signed access tokens.
- **react-ui** (Create React App) is the document upload and Q&A UI.
- **react-ui/ems-app** (Vite + MUI) is the EMS UI.
- **fastapi-service** (Python) is the previous AI worker. It's kept in the repo but **springboot-service no longer calls it**. See [fastapi-service](#fastapi-service-legacy).

## Prerequisites

- **JDK 26.** Spring Boot 4.1 with `java.version` 26.
- **Maven 3.9+.** There's no Maven wrapper in the repo.
- **Node.js 20+ / npm.**
- **Docker Desktop.** Runs Postgres + pgvector, Redis and Keycloak.
- **An OpenAI API key.** Document Q&A and the EMS AI assistant need it.

## 1. Start PostgreSQL + Redis

```bash
docker compose up -d
```

This starts `pgvector/pgvector:pg16` on port 5432 and Redis on port 6379. The first time it creates the database, it also runs `sql-scripts/init.sql`.

**Using an existing local PostgreSQL instead:**

```bash
createdb ai_docs_db
psql -d ai_docs_db -f sql-scripts/init.sql
```

The `vector` extension must be installed on the server. `CREATE EXTENSION vector;` needs the pgvector binary to be present.

> **Upgrading an older database:** embeddings moved from 384-dim (local sentence-transformers) to 1536-dim (OpenAI).
> - Re-running `init.sql` migrates the `document_chunks.embedding` column and **clears existing chunks**.
> - Re-upload your documents afterwards.

## 2. Start Keycloak (auth server)

```bash
docker run -d --name ai-app-keycloak -p 8180:8080 ^
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin ^
  -v "%cd%\springboot-service\keycloak:/opt/keycloak/data/import" ^
  quay.io/keycloak/keycloak:26.3 start-dev --import-realm
```

Run it from the repo root (`cmd.exe` syntax shown). On first start, Keycloak imports the `aiapp` realm from `springboot-service/keycloak/realm-aiapp.json`. Later, start the same container with `docker start ai-app-keycloak`.

- **Admin console:** http://localhost:8180. Log in as `admin` / `admin`.
- **Demo users** (local dev only; the passwords are in plain text in the realm file):

  | Username | Password | Roles |
  |---|---|---|
  | `user1` | `user1pass` | USER |
  | `admin1` | `admin1pass` | USER, ADMIN |

- **Adding a user:**
  1. In the **aiapp** realm, go to Users → Add user.
  2. On the Credentials tab, set a password with "Temporary" turned off.
  3. On Role mapping, assign `USER`, and also `ADMIN` if needed.

See `springboot-service/keycloak/README.md` for details.

## 3. Run the Spring Boot backend

```bash
cd springboot-service
set OPENAI_API_KEY=sk-...
mvn spring-boot:run
```

You can also run `AiAppApplication` from IntelliJ, with `OPENAI_API_KEY` set in the run configuration.

Defaults live in `src/main/resources/application.yml`:
- **Port:** `8090`.
- **Database:** `jdbc:postgresql://localhost:5432/ai_docs_db`, user `ai_app_user` / password `ai_app_password`.
- **Redis:** `localhost:6379`.
- **JWT issuer:** `http://localhost:8180/realms/aiapp`. Override it with the `AUTH_ISSUER_URI` env var.
- **OpenAI models:** `gpt-4o-mini` for chat and `text-embedding-3-small` for embeddings.
- **Uploaded PDFs:** saved to `./uploaded_files`.

**Verify:**
- Health check: http://localhost:8090/actuator/health (public).
- Swagger UI: http://localhost:8090/swagger-ui.html. Click **Authorize** and paste a token.

## 4. Run the frontends

**Document Q&A:**

```bash
cd react-ui
npm install
npm start
```

**EMS:**

```bash
cd react-ui\ems-app
npm install
npm run dev
```

- react-ui opens http://localhost:3000 and ems-app runs at http://localhost:5173.
- Both redirect to the Keycloak login page first. Sign in as `user1` or `admin1`.
- Admin-only buttons are hidden for non-admin users.
- To override the API or Keycloak URLs, copy `.env.example` to `.env.local` in either app.

## Authentication & authorization

1. The app sends the browser to Keycloak (Authorization Code + PKCE). The password is typed on Keycloak's page, never in the app.
2. Keycloak returns a signed **JWT access token**. It's RS256-signed and lasts 5 minutes; `keycloak-js` refreshes it automatically.
3. Every API call sends `Authorization: Bearer <token>`.
4. springboot-service validates the token **locally** using Keycloak's cached public keys (JWKS). It checks the signature, expiry, issuer, and audience `aiapp-api`.
5. `realm_access.roles` in the token map to `ROLE_USER` / `ROLE_ADMIN`.

| Access | Endpoints |
|---|---|
| Public | `/actuator/health`, `/actuator/info`, Swagger UI, `/v3/api-docs/**` |
| **ADMIN** | POST/PUT/DELETE on `/api/ems/departments/**`, `/api/ems/designations/**`, `/api/ems/employees/**`; `PUT /api/ems/leaves/{id}/approve` and `/reject`; `DELETE /api/documents/**` |
| USER or ADMIN | everything else under `/api/**` |

- A missing or invalid token gets **401**. A valid token without the required role gets **403**.
- Both errors return JSON (`timestamp`, `status`, `error`, `message`, `path`).

**Getting a token for curl or Postman:**

```bash
curl -d "grant_type=password&client_id=aiapp-web&username=admin1&password=admin1pass" ^
  http://localhost:8180/realms/aiapp/protocol/openid-connect/token
```

Then call the API:

```bash
curl -H "Authorization: Bearer <access_token>" http://localhost:8090/api/documents
```

## REST API reference (`http://localhost:8090`, Bearer token required)

### Documents & Q&A

| Method | Path | Description |
|---|---|---|
| POST | `/api/documents/upload` | Multipart `file` field (PDF). Stores the file, then extracts, chunks, embeds and stores it in pgvector. |
| GET | `/api/documents` | List uploaded documents with their status. |
| GET | `/api/documents/{documentId}` | One document's status. |
| DELETE | `/api/documents/{documentId}` | Delete a document and its chunks. **ADMIN** only. |
| POST | `/api/qa/ask` | Body: `{ "documentId": "<uuid or null>", "question": "..." }`. Returns the answer plus source chunks. Redis-cached. |

### Employee Management System (`/api/ems`)

| Resource | Endpoints |
|---|---|
| Departments | `GET/POST /departments`, `GET/PUT/DELETE /departments/{id}` |
| Designations | `GET/POST /designations`, `GET/PUT/DELETE /designations/{id}` |
| Employees | `GET/POST /employees`, `GET/PUT/DELETE /employees/{id}` |
| Attendance | `POST /attendance/check-in`, `POST /attendance/check-out`, `GET /attendance`, `GET /attendance/{id}` |
| Leave | `POST /leaves`, `GET /leaves`, `GET /leaves/{id}`, `PUT /leaves/{id}/approve`, `PUT /leaves/{id}/reject` |
| Dashboard | `GET /dashboard/summary` |
| AI assistant | `GET /ai/employees/{id}/summary`, `POST /ai/employees/{id}/profile`, `POST /ai/chat`, `POST /ai/search` |

Swagger UI has the full request and response schemas.

## Configuration reference

| Setting | Where | Notes |
|---|---|---|
| `OPENAI_API_KEY` | env var for springboot-service | Required for Q&A and the EMS AI assistant. Never commit it. |
| `app.openai.chat-model` / `embedding-model` | `application.yml` | `gpt-4o-mini` / `text-embedding-3-small`. If you change the embedding model's dimension, update `vector(1536)` in `init.sql`. |
| `app.qa.chunk-size` / `chunk-overlap` / `top-k` | `application.yml` | Chunking and retrieval parameters. |
| `app.cache.qa-ttl-seconds` | `application.yml` | Redis TTL for cached answers. |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `application.yml` | Keycloak realm URL. Overridable with `AUTH_ISSUER_URI`. |
| `...jwt.audiences` | `application.yml` | `aiapp-api`. Tokens without this audience are rejected. |
| `app.security.jwt.roles-claim` | `application.yml` | `realm_access.roles` (Keycloak's realm roles). |
| `app.storage.upload-dir` | `application.yml` | Where PDFs are saved. |
| `REACT_APP_API_BASE_URL`, `REACT_APP_KEYCLOAK_*` | `react-ui/.env.local` | API URL and Keycloak URL / realm / client. |
| `VITE_API_PROXY_TARGET`, `VITE_KEYCLOAK_*` | `react-ui/ems-app/.env.local` | The Vite dev proxy forwards `/api` to this URL. Keycloak settings as above. |

## Exception handling & logging

- `GlobalExceptionHandler` (documents/Q&A) and the EMS module's own handler map exceptions to structured JSON errors.
- Failed OpenAI calls are wrapped (`LlmServiceException`, `EmbeddingGenerationException`, `AiServiceException`) and returned as **502**.
- Logs go to the console and `springboot-service/logs/springboot-service.log` (`logback-spring.xml`).

## fastapi-service (legacy)

`fastapi-service` is the original Python AI worker. It provides document processing, LangGraph Q&A and the EMS AI endpoints on port 8000. It's kept in the repo, but **springboot-service no longer calls it**: all of that logic now runs in-process in Java. You can still run it on its own; see `COMMANDS.md`.

> ⚠️ **Embedding mismatch:** fastapi-service still embeds with the local 384-dim `all-MiniLM-L6-v2` model and maps `document_chunks.embedding` as `vector(384)`. The shared database now uses `vector(1536)`.
> - Its document processing (`/api/documents/process`) and RAG Q&A (`/api/qa/query`) **will fail against the current schema** until its embedding model and dimension are switched to match.
> - Its EMS AI endpoints (`/api/ems/ai/*`) don't use embeddings and are unaffected.

## Notes on design choices

- **OpenAI embeddings instead of local ones.** Embeddings use the OpenAI API so the Java service needs no ML runtime. The trade-off is an extra API dependency and cost per upload.
- **Redis** caches Q&A answers by a hash of `(documentId, question)`, so repeated questions skip the embedding and LLM calls.
- **Stateless JWT validation.** The API checks tokens offline, so it needs no session store or per-request call to Keycloak. Logout doesn't revoke an already-issued access token; the 5-minute lifetime bounds that window.
- **Before using this beyond localhost:** replace the demo users and the Keycloak admin password, run Keycloak in production mode with a real database and HTTPS, and keep `OPENAI_API_KEY` in a secrets store.
