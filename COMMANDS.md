# Command Reference — AI Document Q&A App

All commands assume you're on Windows and starting from `C:\Users\Admin\Claude\Projects\testai`. Run services in this order: **Docker (Postgres + Redis) → FastAPI → Spring Boot → React**.

---

## 1. Docker Compose (Postgres + Redis)

```bash
cd C:\Users\Admin\Claude\Projects\testai

# Start Postgres (pgvector) + Redis in the background
docker compose up -d

# Check both containers are healthy
docker compose ps

# View logs (all services, or a specific one)
docker compose logs -f
docker compose logs -f postgres
docker compose logs -f redis

# Stop containers (keeps data)
docker compose down

# Stop containers AND wipe all data (fresh start)
docker compose down -v
```

**If you're not using Docker** — run against a local Postgres install instead:

```bash
createdb ai_docs_db
psql -d ai_docs_db -f sql-scripts\init.sql
```

---

## 2. FastAPI service (AI worker, port 8000)

```bash
cd C:\Users\Admin\Claude\Projects\testai\fastapi-service

# One-time setup
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt --break-system-packages
copy .env.example .env
REM then edit .env: set LLM_PROVIDER, ANTHROPIC_API_KEY or OPENAI_API_KEY

# Every time you want to run it
venv\Scripts\activate
uvicorn main:app --reload --port 8000
```

Health check: open http://localhost:8000/health — expect `{"status": "UP", ...}`.
Interactive API docs: http://localhost:8000/docs

---

## 3. Spring Boot service (backend, port 8080)

Via terminal:

```bash
cd C:\Users\Admin\Claude\Projects\testai\springboot-service
mvn clean install
mvn spring-boot:run
```

Via IntelliJ: open the `springboot-service` folder as a Maven project, set Project SDK to your JDK, then run `AiAppApplication.java` directly (green ▶ button).

Health check: http://localhost:8080/actuator/health

---

## 4. React frontend (UI, port 3000)

```bash
cd C:\Users\Admin\Claude\Projects\testai\react-ui
npm install
npm start
```

Opens http://localhost:3000 automatically.

---

## Full startup sequence (copy-paste, one terminal each)

**Terminal 1 — Docker:**
```bash
cd C:\Users\Admin\Claude\Projects\testai
docker compose up -d
```

**Terminal 2 — FastAPI:**
```bash
cd C:\Users\Admin\Claude\Projects\testai\fastapi-service
venv\Scripts\activate
uvicorn main:app --reload --port 8000
```

**Terminal 3 — Spring Boot:**
```bash
cd C:\Users\Admin\Claude\Projects\testai\springboot-service
mvn spring-boot:run
```

**Terminal 4 — React:**
```bash
cd C:\Users\Admin\Claude\Projects\testai\react-ui
npm start
```

---

## Shutting everything down

```bash
REM In each terminal running a live server: Ctrl+C

REM Then stop the containers:
cd C:\Users\Admin\Claude\Projects\testai
docker compose down
```

---

## Useful one-offs

```bash
REM Force-refresh FastAPI's Python dependencies after editing requirements.txt
pip install -r requirements.txt --break-system-packages

REM Force Maven to re-check dependency versions (after editing pom.xml)
mvn clean install -U

REM Check what version of a Maven dependency actually resolved
mvn dependency:tree | findstr spring-data-redis

REM Inspect Postgres directly (requires psql on PATH, or use Docker exec)
docker exec -it ai-app-postgres psql -U ai_app_user -d ai_docs_db

REM Inside psql: list tables, check row counts
\dt
SELECT * FROM documents;
SELECT document_id, chunk_index FROM document_chunks;
```
