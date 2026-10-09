# To-Do Application — Build Instructions (Option A: REST API + React SPA)

Prompt instructions for building the application, as issued. Each phase was given as a
role-scoped prompt. The implementation notes at the end record where the delivered code
intentionally differs from the prompt.

---

## Phase 1: Backend Setup (Spring Boot REST API)

**Role:** Senior Java Backend Developer

**Task:** Create a Spring Boot 3.x REST API for a To-Do List application using Java 17,
Spring Data JPA, H2 database, and Lombok.

### Requirements

1. **Domain Entities**
   - `User`: id (Long), username (String), email (String), password (String),
     createdAt (LocalDateTime)
   - `Task`: id (Long), title (String), description (String),
     status (Enum: PENDING, IN_PROGRESS, COMPLETED),
     priority (Enum: LOW, MEDIUM, HIGH), dueDate (LocalDate),
     user (ManyToOne to User), createdAt (LocalDateTime), updatedAt (LocalDateTime)

2. **Layers & Architecture**
   - DTOs: `TaskRequestDto` and `TaskResponseDto` using Jakarta Validation annotations
     (`@NotBlank`, `@NotNull`, etc.)
   - Repository: `TaskRepository extends JpaRepository` with custom methods:
     - `findByUserId(Long userId)`
     - `findByUserIdAndStatus(Long userId, TaskStatus status)`
   - Service Layer: `TaskService` containing CRUD logic, pagination/sorting, and data
     transformation using builders/mappers.
   - Controller Layer: `TaskController` (`@RestController`) under `/api/v1/tasks`:
     - `GET /api/v1/tasks` (optional query params: status, priority, sortBy, page, size)
     - `GET /api/v1/tasks/{id}`
     - `POST /api/v1/tasks`
     - `PUT /api/v1/tasks/{id}`
     - `PATCH /api/v1/tasks/{id}/status` (quick status update)
     - `DELETE /api/v1/tasks/{id}`

3. **Configuration & Exception Handling**
   - `@RestControllerAdvice` for uniform API error responses (timestamp, status code,
     error message, field validation map).
   - `application.yml` with H2 database configuration (enable H2 console) and Spring Data
     JPA settings (`ddl-auto: update`).
   - CORS mapping allowing `http://localhost:3000` and `http://localhost:5173`.

Output clean, production-ready Java code organized logically by package.

---

## Phase 2: Security & Authentication (Spring Security + JWT)

**Role:** Security Specialist & Spring Developer

**Task:** Implement stateless JWT authentication for the Spring Boot REST API built in Phase 1.

### Requirements

1. **Security Configuration**
   - Spring Security 6.x `SecurityFilterChain` bean.
   - `BCryptPasswordEncoder` for hashing passwords.
   - Session management policy `STATELESS`.
   - Public endpoints: `/api/v1/auth/**` and `/h2-console/**`.
   - Secure endpoints: `/api/v1/tasks/**` requiring a valid JWT bearer token.

2. **JWT Infrastructure**
   - `JwtTokenProvider` for generating, parsing, and validating JWT access tokens
     (expiration and secret key configured externally).
   - `JwtAuthenticationFilter` to extract the token from the `Authorization` header and
     inject user details into `SecurityContextHolder`.

3. **Authentication Endpoints** (`/api/v1/auth`)
   - `POST /api/v1/auth/register` — validate email, hash password, save user,
     return JWT + user summary.
   - `POST /api/v1/auth/login` — verify credentials, return JWT + user summary.

4. **Data Isolation**
   - `TaskService` retrieves the authenticated user from the `SecurityContext` and ensures
     users can only CRUD their own tasks.

### Reference implementation supplied with the prompt

- **Maven dependencies:** `spring-boot-starter-security`, `jjwt-api`, `jjwt-impl` (runtime),
  `jjwt-jackson` (runtime).
- **Configuration:** `app.jwt.secret` (>= 256 bits), `app.jwt.expiration-ms: 86400000`,
  `spring.h2.console.enabled: true` at `/h2-console`, `ddl-auto: update`.
- **`JwtTokenProvider`** — `Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))`,
  `generateToken(Authentication)`, `getUsernameFromToken`, `validateToken`.
- **`JwtAuthenticationFilter`** — `OncePerRequestFilter`, reads `Bearer ` prefix,
  loads `UserDetails`, sets `UsernamePasswordAuthenticationToken`.
- **`CustomUserDetailsService`** — `findByUsernameOrEmail`, returns a Spring `User`.
- **`SecurityConfig`** — CORS source, CSRF disabled, `STATELESS`, `permitAll` on
  `/api/v1/auth/**` and `/h2-console/**`, `frameOptions sameOrigin`,
  `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`.
- **`AuthDto`** — nested `RegisterRequest`, `LoginRequest`, `AuthResponse`.
- **`AuthController`** — `/register` (duplicate checks, auto-login), `/login`.
- **`SecurityUtils`** — `getCurrentAuthenticatedUser()` from `SecurityContextHolder`.

---

## Phase 3: Frontend Architecture Setup (React + TypeScript)

**Role:** Senior Frontend Engineer

**Task:** Create a modern React (TypeScript) SPA dashboard using Vite, Tailwind CSS,
Lucide React icons, and Axios.

### Requirements

1. **Axios Configuration**
   - Centralized Axios instance with baseURL `http://localhost:8080/api/v1`.
   - Request interceptor attaching the JWT from localStorage/session as
     `Authorization: Bearer <token>`.
   - Response interceptor catching 401 Unauthorized and redirecting to `/login`.

2. **Type Definitions**
   - TypeScript interfaces for `Task`, `TaskStatus` (`'PENDING' | 'IN_PROGRESS' | 'COMPLETED'`),
     `TaskPriority` (`'LOW' | 'MEDIUM' | 'HIGH'`), `User`, `AuthResponse`, and `ApiError`.

3. **Authentication Context & Routes**
   - `AuthContext` providing user state, `login()`, `logout()`, and `isAuthenticated`.
   - React Router setup:
     - Public routes: `/login`, `/register`
     - Protected route guard: redirect unauthenticated users to `/login`
     - Main dashboard: `/` (protected)

---

## Phase 4: Frontend UI Components & Dashboard

**Role:** Frontend UI/UX Developer

**Task:** Implement the core To-Do List Dashboard UI and components in React with Tailwind CSS.

### Requirements

1. **Layout Structure**
   - Responsive navbar: logo, user email display, dark/light mode button, logout button.
   - Sidebar/header controls: search input, status filter tabs
     (All, Pending, In Progress, Completed), priority filter dropdown.

2. **Core Components**
   - `QuickAddTaskBar` — inline text field + quick priority selector + "Add Task" button
     at the top of the list.
   - `TaskList` / `TaskGrid` — task list with smooth state transitions, plus an empty state
     component when no tasks exist.
   - `TaskCard`:
     - Title & description
     - Priority badge with colour coding (Low: green, Medium: yellow, High: red)
     - Checkbox toggling status between PENDING and COMPLETED via API
     - Due date indicator, overdue tasks highlighted in red
     - Action icons: Edit (opens modal) and Delete (confirm prompt)
   - `TaskModal` — modal drawer for creating/editing full task details
     (title, description, priority, due date, status).

3. **UX Features**
   - Toast notifications (react-hot-toast or sonner) for success/error feedback.
   - Loading skeletons while fetching tasks.

Deliver modular, clean React code using functional components and hooks.

---

## Phase 5: Uniform error handling (@RestControllerAdvice)

**Task:** Intercept standard Spring exceptions (validation failures) and custom business
exceptions, converting them into a standardised JSON response.

### Requirements

1. **Error response DTO** — `timestamp`, `status`, `error`, `message`, `path`, and a
   `fieldErrors` map of field name to validation message, omitted when absent.
2. **Exception types** — a not-found exception mapping to 404, and an access-denied exception
   mapping to 403.
3. **Advice** — handlers for not-found (404), `MethodArgumentNotValidException` (400 with the
   field map), `BadCredentialsException` (401), access denied (403), and a catch-all (500).

### Example responses supplied with the prompt

```json
{ "timestamp": "...", "status": 400, "error": "Validation Failed",
  "message": "Input validation failed for one or more fields.",
  "path": "/api/v1/tasks", "fieldErrors": { "title": "must not be blank" } }
```

```json
{ "timestamp": "...", "status": 404, "error": "Not Found",
  "message": "Task not found with id: 99", "path": "/api/v1/tasks/99" }
```

---

## Phase 6: Domain model from the design references

**Task:** Reference the linked designs and build the application structure they describe.

**References**

- [The conceptual design of Todoist](https://tuanmon.com/the-conceptual-design-of-todoist/)
- [To-do List Design (Figma community)](https://www.figma.com/community/file/1352825756540721031/to-do-list-design)

### Delivered

| From | Built |
|--|--|
| Todoist article | `Project` (1:N with tasks), `Label` (M:N), Inbox and Today as derived views |
| Figma design | Dark violet UI, greeting header, daily progress card, start/end time, per-task alert flag, priority pills |
| TaskTactic | Eisenhower matrix as `urgent` × `important` |

### Not built

Pomodoro timer, saved Filters, long-term Goals, reminder delivery. `alertEnabled` is stored but
no notification is sent.

---

## Phase 7: Documentation and testing

- `docs/ER.md`, `docs/FLOW.md`, `docs/DESIGN.md` — entity model, request and process flows,
  architecture and UI
- `docs/TESTING.md` — manual test guide per feature with screenshots, numbered scenarios, and
  the automated suites
- Playwright end-to-end suite and backend unit tests
- `README.md` in English, `README.ja.md` in Japanese

---

## Implementation notes — where the delivered code differs, and why

The project was scaffolded in a previous step from the house template in `Tasks.xlsx`
(Spring Boot 4.0.1 / Java 21 / PostgreSQL + Flyway, conventions in `.claude/skills/`).
These phases were implemented on top of that scaffold rather than against a fresh
Spring Boot 3 / Java 17 / H2 project.

### Stack

| Prompt | Delivered | Reason |
|--|--|--|
| Spring Boot 3.x, Java 17 | Spring Boot 4.0.1, Java 21 | Keeps the already-verified house scaffold; downgrading would be a rewrite |
| H2 as the database | PostgreSQL + Flyway, with an `h2` dev profile | Deployed profiles keep versioned migrations; `-Dspring.profiles.active=h2` gives H2 + console + `ddl-auto: update` as the prompt asked |
| `application.yml` | `application.properties` | Existing project format; same keys |
| `ddl-auto: update` | `validate` (Postgres) / `update` (h2 profile) | Flyway owns the Postgres schema |

### Corrections to the supplied reference code

| Issue | Resolution |
|--|--|
| `CustomUserDetailsService.loadUserByUsername` declared `UserDetailsService` as its return type | Returns `UserDetails`; the snippet as written does not compile |
| JWT secret hard-coded in config | `app.jwt.secret=${JWT_SECRET:}`; a random key is generated when unset, with a warning. Required by security-audit No.19 |
| `new BCryptPasswordEncoder()` (strength 10) | Strength 12, as security-audit requires (`BCryptの強度 ≥ 12`) |
| `frameOptions sameOrigin` applied globally | Scoped to a dedicated `/h2-console/**` chain; the API and web chains keep `frameOptions deny` |
| `/h2-console/**` permitted unconditionally | Chain is `@ConditionalOnProperty(spring.h2.console.enabled=true)`, which only the `h2` profile sets |
| CSRF disabled for the whole application | Disabled only on the stateless `/api/**` chain; the Thymeleaf chain keeps CSRF enabled |
| `register` returning raw strings (`"Error: Username is already taken!"`) | `AppException` + `AppErrorCode`, rendered through the uniform `ApiErrorResponse` |
| `.orElseThrow()` with no explicit exception | Explicit `AppException`; required by `java-clean-code` |
| `new RuntimeException(...)` in `SecurityUtils` | `AppException(UNAUTHENTICATED)`; `RuntimeException` is banned by `java-clean-code` |
| `@Data` DTOs with setters | Immutable `record` DTOs |
| New `User` entity duplicating the existing `AppUser` | Extended `AppUser` with `email`; one user table |
| `ResponseEntity<?>` | Typed `ResponseEntity<AuthResponseDto>` |
| Login distinguishing "user not found" from "bad password" | Single `INVALID_CREDENTIALS` response, to avoid account enumeration |
| Phase 5 advice returning `"An unexpected error occurred: " + ex.getMessage()` | Fixed message; exception text in a response body leaks table and column names, which `server.error.include-message=never` exists to prevent |
| Phase 5 `fieldErrors` built with `HashMap` | `LinkedHashMap`, so key order is stable between responses |
| Phase 5 per-type exceptions extending `RuntimeException` | Kept the single `AppException` + `AppErrorCode` enum, which the `java-clean-code` skill mandates. A custom `AccessDeniedException` would also shadow Spring Security's own, which the advice already catches |

### Additions beyond the prompt

- `GET /api/v1/auth/me` so the SPA can restore a session from a stored token.
- `search` query parameter on the task list (Phase 4 requires a search box).
- `sortBy` validated against an allowlist; an arbitrary value returns 400 rather than
  sorting by an internal field.
- Allowlist `@Pattern` on task title/description that accepts Japanese text but rejects
  `<`/`>` and control characters (security-audit No.20).
- `DELETE` is a soft delete (`AuditableEntity.deleted`), matching the house audit convention.
- Task list responses use a `PagedResponse` envelope rather than serialising `PageImpl`.
- Optimistic UI with rollback for status toggle and delete.

### Known trade-off

The JWT is stored in `localStorage`, as Phase 3 specifies. That is readable by any XSS on the
origin. The mitigations in place are the output-escaping and input-allowlist rules from
`security-audit`. Moving to an httpOnly refresh-token cookie would remove the exposure and is
the recommended hardening step if this goes to production.

### Naming

`com.gs.gs8.todolist` was renamed to `com.todolist`, and `Gs8Exception` / `Gs8ErrorCode` to
`AppException` / `AppErrorCode`, on request. The `com/gs/gs8` directories are gone.

### The Thymeleaf view layer was removed

The house template integrated Vite with Thymeleaf, which produced a second `node_modules` and a
server-rendered UI that Option A never uses. The root frontend tooling, the Thymeleaf templates
and `HomeController` were removed; the backend now serves JSON only and the SPA is the sole UI.

### Frontend location

The SPA lives in `frontend/` as a standalone Vite project (its own `package.json`), because
Phase 3 specifies a cross-origin SPA on port 5173 calling `http://localhost:8080`. The
Thymeleaf + Vite integration from the house template is untouched and still builds; it is now
redundant for this architecture and can be retired on request.
