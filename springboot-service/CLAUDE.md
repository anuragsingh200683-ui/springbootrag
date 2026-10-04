# springboot-service

Spring Boot service that hosts two modules in one app:

- `com.example.aiapp`: AI document Q&A, ingestion, and the order-saga transaction demo.
- `com.example.employeemanagement` (EMS): employee, department, designation, attendance, leave, dashboard, AI assistant.

## Stack

- Java 26, Spring Boot 4.1, Maven. Use modern Java (records, switch expressions, pattern matching, text blocks) where it reads better.
- Spring MVC, Spring Data JPA, PostgreSQL (H2 in tests), Redis, Spring Security as an OAuth2 resource server (Keycloak JWT), Actuator, springdoc.
- There is no Kafka, Kubernetes, or AWS code here. Don't add any without being asked.
- Running tests: use the `spring-test-runner` agent. The JDK and Maven are not on PATH; that agent's file has the paths.
- Port 8090 is the user's own IntelliJ run. Never stop it or bind to it.

## Package layout

Code is organized by feature. Follow the existing pattern for the module you are in:

```text
com.example.employeemanagement.<feature>
├── controller
├── dto
├── entity
├── mapper
├── repository
├── service          (interface + *Impl)
└── specification    (JPA Specifications for filtered search, when needed)
```

Shared EMS code lives in `employeemanagement.common` (config, dto, exception). Don't create new top-level layered packages or a generic `util` package. Search for an existing helper before adding one.

## Code style

- **Lombok:** match the module. EMS code uses `@RequiredArgsConstructor` and `@Slf4j`. `aiapp` code mostly uses explicit constructors and `LoggerFactory`. Don't convert existing files.
- Constructor injection only. No field `@Autowired`.
- Readable over clever. Avoid stream chains or one-liners that are harder to follow than a loop.
- No magic numbers or strings in business logic. No static mutable state. No `new Thread(...)`; use Spring-managed executors.
- `Optional` only as a return type, never as a field or parameter.
- Externalize config in `application.yml` with `${ENV_VAR}` placeholders. Use `@ConfigurationProperties` for structured config. Never hard-code secrets, URLs, or credentials.

## Controllers and DTOs

- Controllers stay thin: validate (`@Valid`), call one service method, return a `ResponseEntity`. No business logic, repository calls, or external API calls.
- Never return JPA entities from REST endpoints. Map with the feature's `mapper` class.
- Status codes: POST create returns 201, DELETE returns 204, GET/PUT/PATCH return 200.
- Paginated endpoints take `Pageable` and must cap the page size.

## Error handling

Each module has its own `@RestControllerAdvice`. Don't add a third one or a new error body.

| Module | Handler | Error body |
|---|---|---|
| EMS | `EmsGlobalExceptionHandler` (scoped by `basePackages`) | `EmsApiError` |
| aiapp | `GlobalExceptionHandler` | `ErrorResponse` |

- EMS exceptions: `ResourceNotFoundException` → 404, `DuplicateResourceException` → 409, `InvalidRequestException` → 400, `AiServiceException` → 502. Reuse these before creating new ones.
- Bean-validation failures → 400. Upstream AI or LLM failures → 502.
- Never swallow exceptions, `catch (Exception e) { return null; }`, or leak stack traces, SQL, or class names to clients.
- Log an exception once, where it is handled. Don't log and rethrow at every layer.

## Logging and security

- SLF4J with placeholders (`log.info("... id={}", id)`), not string concatenation.
- Never log passwords, JWTs, API keys, or personal data.
- Every new endpoint needs an explicit authorization decision in the security config. Don't open endpoints to `permitAll` without saying why.
- Don't expose sensitive Actuator endpoints publicly.

## Database and transactions

- `@Transactional` goes on service methods, never controllers. Use `readOnly = true` for reads.
- Don't hold a DB transaction open across slow external calls (LLM, embeddings, HTTP). Do the external call outside the transaction.
- Watch for N+1 queries: no `repository.findById` inside loops, and use fetch joins or entity graphs where needed.
- No unbounded `findAll()` on tables that can grow. Use pagination.
- Avoid `CascadeType.ALL` and `FetchType.EAGER` unless justified.
- Redis: every cached value needs a TTL and an eviction path. Key format is `<domain>:v1:<id>`.

## External calls

- Every outbound HTTP or LLM call needs a connect and read timeout.
- Retry only idempotent operations, and never on 4xx or validation errors.

## Tests

- JUnit 5 and Mockito for services: happy path, not found, validation failure, and dependency failure.
- `@WebMvcTest` with `spring-security-test` for controllers. `@SpringBootTest` on H2 for integration.
- Test behavior, not coverage. Run the suite before calling work done.

## Legacy code

The app is mid-migration from FastAPI. Don't delete old implementations until the user explicitly says so.

## Code review

For an in-depth Java/Spring review, use the `java-code-reviewer` agent.
