# Keycloak auth server (local dev)

springboot-service does not store users or passwords. Keycloak (realm `aiapp`) owns them and issues signed JWT access tokens. The API validates each `Authorization: Bearer <token>` against Keycloak's public keys.

## Start

From the `springboot-service` directory:

```bash
docker run -d --name ai-app-keycloak -p 8180:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  -v "$(pwd)/keycloak:/opt/keycloak/data/import" \
  quay.io/keycloak/keycloak:26.3 start-dev --import-realm
```

- Admin console: http://localhost:8180 (admin / admin)
- Issuer: `http://localhost:8180/realms/aiapp`. Override it on the API side with the `AUTH_ISSUER_URI` env var.
- `--import-realm` only imports `realm-aiapp.json` if the realm doesn't exist yet. To re-import after editing the file, delete the realm in the console, or remove and recreate the container.

## What the realm contains

| Item | Purpose |
|---|---|
| Roles `USER`, `ADMIN` | Mapped to `ROLE_USER` / `ROLE_ADMIN` by `KeycloakJwtAuthenticationConverter` |
| Client `aiapp-api` | Token audience. The API rejects tokens whose `aud` doesn't contain it |
| Client `aiapp-web` | Public client: Auth Code + PKCE for the React apps, and the password grant for curl/Postman. Its audience mapper adds `aiapp-api` to `aud` |
| `user1` / `user1pass` | Role USER (**demo only**) |
| `admin1` / `admin1pass` | Roles USER and ADMIN (**demo only**) |

## Get a token and call the API

```bash
TOKEN=$(curl -s -d "grant_type=password&client_id=aiapp-web&username=admin1&password=admin1pass" \
  http://localhost:8180/realms/aiapp/protocol/openid-connect/token | python -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

curl -H "Authorization: Bearer $TOKEN" http://localhost:8090/api/documents
```

## Authorization rules (`SecurityConfig`)

- **Public:** `/actuator/health`, `/actuator/info`, Swagger UI, and `/v3/api-docs/**`.
- **ADMIN:**
  - POST, PUT and DELETE on `/api/ems/departments|designations|employees/**`
  - `PUT /api/ems/leaves/*/approve|reject`
  - `DELETE /api/documents/**`
- **USER or ADMIN:** everything else under `/api/**`.
- **Error codes:** a missing or invalid token gets 401, and a token without the required role gets 403. Both return the standard `ErrorResponse` JSON.
