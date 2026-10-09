# FLOW — Request and Process Flows

How a request travels through the system, and the sequences behind each feature.

---

## 1. Deployment shape

```mermaid
flowchart LR
    B["Browser<br/>React SPA :3000"]
    V["Vite dev server<br/>(dev only)"]
    A["Spring Boot API<br/>:8080"]
    D[("PostgreSQL<br/>:5432")]

    B -. "served by" .-> V
    B -- "fetch /api/v1/** <br/>Authorization: Bearer" --> A
    A -- "JDBC" --> D
```

The SPA and the API are separate origins, so every call is cross-origin and CORS is explicit.
There is no server-rendered UI: the backend answers JSON only.

---

## 2. Request pipeline

```mermaid
flowchart TD
    REQ([HTTP request]) --> CORS[CorsFilter]
    CORS --> CHAIN{securityMatcher}
    CHAIN -->|"/api/**"| JWT[JwtAuthenticationFilter]
    CHAIN -->|"/h2-console/**"| H2["permitAll<br/>frameOptions SAMEORIGIN<br/>(dev profiles only)"]
    CHAIN -->|anything else| DENY["denyAll → 401 JSON"]

    JWT --> AUTHZ{authenticated?}
    AUTHZ -->|"no, and not /auth/**"| E401["401 ApiErrorResponse"]
    AUTHZ -->|yes| CTRL[Controller]

    CTRL -->|"@Valid"| VAL{valid?}
    VAL -->|no| E400["400 + fieldErrors"]
    VAL -->|yes| SVC["Service<br/>@Transactional"]

    SVC --> OWN["CurrentUserService<br/>.requireCurrentUser()"]
    OWN --> REPO["Repository<br/>scoped by user_id"]
    REPO --> DTO[DTO projection]
    DTO --> RES([200 / 201 / 204])
```

Three security chains exist so that a relaxation one surface needs cannot weaken another:

| Order | Matcher | Session | CSRF | Framing |
|--|--|--|--|--|
| 1 | `/api/**` | `STATELESS` | disabled — bearer only, no ambient cookie to ride on | `DENY` |
| 2 | `/h2-console/**` | default | disabled | `SAMEORIGIN` — console renders in frames |
| 3 | everything else | `STATELESS` | disabled | `DENY` |

Chain 2 is registered only when `spring.h2.console.enabled=true`, which no deployed profile sets.

---

## 3. Registration and sign-in

```mermaid
sequenceDiagram
    actor U as User
    participant S as SPA
    participant C as AuthController
    participant A as AuthService
    participant R as AppUserRepository
    participant J as JwtTokenProvider

    U->>S: submit registration
    S->>C: POST /api/v1/auth/register
    C->>A: register(dto)
    A->>R: existsActiveByUsername / ByEmail
    alt already taken
        R-->>A: true
        A-->>C: AppException(DUPLICATE_USERNAME | DUPLICATE_EMAIL)
        C-->>S: 409 ApiErrorResponse
    else available
        A->>A: passwordEncoder.encode (bcrypt, strength 12)
        A->>R: save(AppUser)
        A->>J: generateToken(username)
        J-->>A: signed JWT
        A-->>C: AuthResponseDto
        C-->>S: 201 { accessToken, expiresIn, user }
        S->>S: localStorage.setItem(token)
    end
```

Sign-in is the same shape with `authenticationManager.authenticate`. A wrong name and a wrong
password both return the single `INVALID_CREDENTIALS` code, so the endpoint does not reveal
which accounts exist. `LoginAttemptService` locks an identifier after 5 failures for 15 minutes.

---

## 4. Authenticated call

```mermaid
sequenceDiagram
    participant S as SPA (axios)
    participant F as JwtAuthenticationFilter
    participant D as AppUserDetailsService
    participant C as TaskController
    participant V as TaskService
    participant R as TaskRepository

    S->>F: GET /api/v1/tasks?view=TODAY<br/>Authorization: Bearer …
    F->>F: validateToken
    alt invalid or expired
        F-->>S: (stays anonymous) → 401 JSON
        S->>S: interceptor clears token, redirects to /login
    else valid
        F->>D: loadUserByUsername(subject)
        D-->>F: UserDetails
        F->>C: SecurityContext populated
        C->>V: findTasks(TaskQuery)
        V->>V: requireCurrentUser()
        V->>R: findAll(Specification, Pageable)<br/>@EntityGraph(project, labels)
        R-->>V: Page<Task>
        V-->>C: Page<TaskResponseDto>
        C-->>S: 200 PagedResponse
    end
```

The owning account always comes from the security context, never from a request parameter. A
task belonging to someone else returns **404, not 403** — a 403 would confirm the row exists.

---

## 5. Task lifecycle

```mermaid
stateDiagram-v2
    [*] --> PENDING: POST /tasks
    PENDING --> IN_PROGRESS: PATCH /status
    IN_PROGRESS --> COMPLETED: PATCH /status
    PENDING --> COMPLETED: checkbox toggle
    COMPLETED --> PENDING: checkbox toggle
    PENDING --> [*]: DELETE (soft)
    IN_PROGRESS --> [*]: DELETE (soft)
    COMPLETED --> [*]: DELETE (soft)
```

`DELETE` sets `deleted = true`. The row stays for audit; every finder filters it out.

---

## 6. Project deletion

```mermaid
sequenceDiagram
    participant C as ProjectController
    participant P as ProjectService
    participant E as ApplicationEventPublisher
    participant L as ProjectDeletedListener
    participant R as TaskRepository

    C->>P: delete(id)
    P->>P: requireOwnProject(id) → markDeleted()
    P->>E: publish ProjectDeletedEvent
    Note over E,L: BEFORE_COMMIT, same transaction
    E->>L: onProjectDeleted
    L->>R: detachFromProject → project_id = NULL
    Note over R: tasks fall back to the Inbox
    P-->>C: commit → 204
```

The listener lives in the **task** module and runs `BEFORE_COMMIT`, so the detach and the
soft-delete succeed or fail together. The project module never imports the task module.

---

## 7. Error flow

```mermaid
flowchart TD
    X([exception]) --> WHERE{raised where?}
    WHERE -->|inside a @RestController method| API["ApiExceptionHandler<br/>@RestControllerAdvice(annotations = RestController)<br/>HIGHEST_PRECEDENCE"]
    WHERE -->|"before a handler resolved<br/>(unknown path, bad method)"| GLOBAL["GlobalExceptionHandler<br/>@RestControllerAdvice<br/>LOWEST_PRECEDENCE"]
    WHERE -->|inside a security filter| FILTER["SecurityConfig entry point<br/>writes the body directly"]

    API --> BODY[ApiErrors.response]
    GLOBAL --> BODY
    FILTER --> BODY
    BODY --> OUT([uniform ApiErrorResponse])
```

An annotation-scoped advice can only match when a handler method was resolved, so failures
raised earlier need the unscoped advice. All three paths build the body through `ApiErrors`, so
the shape never diverges.

| Situation | Status | Code |
|--|--|--|
| Bean validation failed | 400 | `E0400` + `fieldErrors` |
| Unparseable body, bad enum or bad number in a parameter | 400 | `E0400` |
| No token, expired token, forged token | 401 | `E0401` |
| Wrong credentials | 401 | `E0402` |
| Authenticated but not permitted | 403 | `E0403` |
| Absent, or owned by someone else | 404 | `E0404` |
| Unknown `/api` path | 404 | `E0404` |
| Wrong HTTP method | 405 | `E0405` |
| Duplicate username / email / project / label | 409 | `E0409`–`E0411` |
| Unsupported `Content-Type` | 415 | `E0415` |
| Anything else | 500 | `E0500`, detail logged only |

---

## 8. Local startup

```mermaid
flowchart LR
    subgraph T1["Terminal 1"]
      M["./mvnw spring-boot:run<br/>-Dspring-boot.run.profiles=local"]
    end
    subgraph T2["Terminal 2"]
      N["cd frontend && npm run dev"]
    end
    M --> FW["Flyway migrates"] --> API["API :8080"]
    N --> SPA["SPA :3000"]
    SPA --> API
```

`JWT_SECRET` is read from the environment. When unset, a random key is generated per process and
a warning is logged — fine for development, but tokens stop verifying after a restart.
