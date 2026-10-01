# Command Reference — AI Document Q&A + EMS

All commands assume you're on Windows (`cmd.exe`), starting from `C:\Users\Admin\Claude\Projects\testai`.

Start services in this order: **Docker (Postgres + Redis) → Keycloak → Spring Boot → React apps**.

---

## 1. Docker Compose (Postgres + Redis)

```bash
cd C:\Users\Admin\Claude\Projects\testai

REM Start Postgres (pgvector) + Redis in the background
docker compose up -d

REM Check both containers are healthy
docker compose ps

REM View logs (all services, or a specific one)
docker compose logs -f
docker compose logs -f postgres
docker compose logs -f redis

REM Stop containers (keeps data)
docker compose down

REM Stop containers AND wipe all data (fresh start; init.sql runs again)
docker compose down -v
```

**If you're not using Docker**, run against a local Postgres install instead:

```bash
createdb ai_docs_db
psql -d ai_docs_db -f sql-scripts\init.sql
```

---

## 2. Keycloak (auth server, port 8180)

```bash
cd C:\Users\Admin\Claude\Projects\testai

REM First time only: create the container and import the 'aiapp' realm
docker run -d --name ai-app-keycloak -p 8180:8080 ^
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin ^
  -v "%cd%\springboot-service\keycloak:/opt/keycloak/data/import" ^
  quay.io/keycloak/keycloak:26.3 start-dev --import-realm

REM Every other time
docker start ai-app-keycloak

REM Stop it
docker stop ai-app-keycloak
```

- **Ready check:** http://localhost:8180/realms/aiapp/.well-known/openid-configuration should return JSON.
- **Admin console:** http://localhost:8180, logging in as `admin` / `admin`.
- **App logins:** `user1` / `user1pass` (USER) or `admin1` / `admin1pass` (ADMIN).

The realm is imported only when the container is created. After editing `realm-aiapp.json`, recreate the container:

```bash
docker rm -f ai-app-keycloak
```

Then run the first-time `docker run` command again.

---

## 3. Spring Boot service (backend, port 8090)

Via terminal:

```bash
cd C:\Users\Admin\Claude\Projects\testai\springboot-service
set OPENAI_API_KEY=sk-...
mvn clean install
mvn spring-boot:run
```

Via IntelliJ:
1. Open the `springboot-service` folder as a Maven project.
2. Set the Project SDK to JDK 26.
3. Add `OPENAI_API_KEY` to the run configuration's environment variables.
4. Run `AiAppApplication.java`.

Checks:
- Health: http://localhost:8090/actuator/health
- Swagger UI: http://localhost:8090/swagger-ui.html. Click **Authorize** and paste a token from step 5.

Run the tests (no Postgres, Redis or Keycloak needed):

```bash
mvn test
```

---

## 4. React frontends

**Document Q&A UI (port 3000):**

```bash
cd C:\Users\Admin\Claude\Projects\testai\react-ui
npm install
npm start
```

**EMS UI (port 5173):**

```bash
cd C:\Users\Admin\Claude\Projects\testai\react-ui\ems-app
npm install
npm run dev
```

Both redirect to the Keycloak login page first.

---

## 5. Get a token and call the API from the command line

```bash
REM Get an access token (valid 5 minutes); copy "access_token" from the JSON
curl -d "grant_type=password&client_id=aiapp-web&username=admin1&password=admin1pass" ^
  http://localhost:8180/realms/aiapp/protocol/openid-connect/token

REM Call the API with it
curl -H "Authorization: Bearer <access_token>" http://localhost:8090/api/documents
curl -H "Authorization: Bearer <access_token>" http://localhost:8090/api/ems/departments
```

| Response | Meaning |
|---|---|
| `401` | Missing, expired or invalid token |
| `403` | Valid token, but the user lacks the role (e.g. `user1` creating a department) |

---

## Full startup sequence (one terminal each)

**Terminal 1 — Docker (Postgres, Redis, Keycloak):**
```bash
cd C:\Users\Admin\Claude\Projects\testai
docker compose up -d
docker start ai-app-keycloak
```

**Terminal 2 — Spring Boot:**
```bash
cd C:\Users\Admin\Claude\Projects\testai\springboot-service
set OPENAI_API_KEY=sk-...
mvn spring-boot:run
```

**Terminal 3 — Document Q&A UI:**
```bash
cd C:\Users\Admin\Claude\Projects\testai\react-ui
npm start
```

**Terminal 4 — EMS UI:**
```bash
cd C:\Users\Admin\Claude\Projects\testai\react-ui\ems-app
npm run dev
```

---

## Shutting everything down

```bash
REM In each terminal running a live server: Ctrl+C

REM Then stop the containers:
cd C:\Users\Admin\Claude\Projects\testai
docker compose down
docker stop ai-app-keycloak
```

---

## fastapi-service (legacy, port 8000, optional)

springboot-service no longer calls fastapi-service. You only need this to run it on its own.

> ⚠️ It still uses 384-dim local embeddings, but the shared database now has `vector(1536)`. Its document processing and RAG Q&A endpoints will fail against the current schema. Its EMS AI endpoints are unaffected. See README.md.

```bash
cd C:\Users\Admin\Claude\Projects\testai\fastapi-service

REM One-time setup
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt --break-system-packages
copy .env.example .env
REM then edit .env: set LLM_PROVIDER, ANTHROPIC_API_KEY or OPENAI_API_KEY

REM Every time you want to run it
venv\Scripts\activate
uvicorn main:app --reload --port 8000
```

- Health check: http://localhost:8000/health
- API docs: http://localhost:8000/docs

---

## Useful one-offs

```bash
REM Force Maven to re-check dependency versions (after editing pom.xml)
mvn clean install -U

REM Check what version of a Maven dependency actually resolved
mvn dependency:tree | findstr spring-security-oauth2

REM Inspect Postgres directly
docker exec -it ai-app-postgres psql -U ai_app_user -d ai_docs_db
```

Inside psql, list the tables and check the rows:

```sql
\dt
SELECT document_id, file_name, status FROM documents;
SELECT document_id, chunk_index FROM document_chunks;
```

```bash
REM Keycloak logs (e.g. failed logins show as LOGIN_ERROR)
docker logs -f ai-app-keycloak
```
