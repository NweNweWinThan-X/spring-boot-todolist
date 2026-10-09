# spring-boot-todolist

A to-do application built as a **REST API + React SPA**: a Spring Boot modular monolith serving
JSON, and a separate Vite single-page app.

日本語版は [README.ja.md](README.ja.md) にあります。

| | |
|--|--|
| Backend | Java 21, Spring Boot 4.0.1, Spring Security 7, Spring Data JPA, Flyway, PostgreSQL |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS, axios, lucide-react, sonner |
| Auth | Stateless JWT (HS384) |
| Docs | [ER](docs/ER.md) · [FLOW](docs/FLOW.md) · [DESIGN](docs/DESIGN.md) · [TESTING](docs/TESTING.md) · [CI/CD](docs/CICD.md) |
| Build requirements | [task.md](task.md) |

---

## Running locally

### Prerequisites

| Tool | Version |
|--|--|
| JDK | 21 |
| Node.js | 20+ |
| PostgreSQL | 16+ |

```bash
createdb todolist
cp .env.example .env     # fill in DB_PASSWORD
```

| Variable | Purpose |
|--|--|
| `DB_URL` | Defaults to `jdbc:postgresql://localhost:5432/todolist` |
| `DB_USERNAME` / `DB_PASSWORD` | Connection credentials (required) |
| `JWT_SECRET` | Base64 signing key, 256 bits or more. Unset means a random key per process, so tokens stop verifying after a restart |
| `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` / `APP_ADMIN_EMAIL` | Creates an administrator on first start of the `local` and `h2` profiles |

### 1. Backend

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Flyway migrates on startup. The API listens on `http://localhost:8080`.

### 2. Frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

The SPA listens on `http://localhost:5173`. Both `:5173` and `:3000` are allowed CORS origins,
so `npm run dev -- --port 3000` works too. Point it elsewhere with `VITE_API_BASE_URL`.

### Without PostgreSQL

The `h2` profile uses an in-memory database, disables Flyway, builds the schema with
`ddl-auto: update`, and exposes the H2 console at `/h2-console`. Never used in a deployed
environment.

```bash
SPRING_PROFILES_ACTIVE=h2 ./mvnw spring-boot:run
```

---

## API

Base path `/api/v1`. Every endpoint except registration and sign-in needs
`Authorization: Bearer <token>`.

| Method | Path | Purpose |
|--|--|--|
| `POST` | `/auth/register` | Create an account, return a token and user summary |
| `POST` | `/auth/login` | Exchange credentials for a token |
| `GET` | `/auth/me` | Restore a session from a stored token |
| `GET` | `/tasks` | List tasks, filtered and paged |
| `GET` | `/tasks/progress` | Completion counts for a day |
| `GET` | `/tasks/{id}` | One task |
| `POST` | `/tasks` | Create (`201` + `Location`) |
| `PUT` | `/tasks/{id}` | Replace every field |
| `PATCH` | `/tasks/{id}/status` | Change status only |
| `DELETE` | `/tasks/{id}` | Soft delete (`204`) |
| `GET` `POST` `PUT` `DELETE` | `/projects`, `/projects/{id}` | Project CRUD |
| `PATCH` | `/projects/{id}/archived?archived=` | Archive or restore |
| `GET` `POST` `PUT` `DELETE` | `/labels`, `/labels/{id}` | Label CRUD |

### Task query parameters

| Parameter | Values |
|--|--|
| `view` | `ALL` `INBOX` `TODAY` `OVERDUE` `UPCOMING` |
| `status` | `PENDING` `IN_PROGRESS` `COMPLETED` |
| `priority` | `LOW` `MEDIUM` `HIGH` |
| `projectId`, `labelId` | Numeric id |
| `urgent`, `important` | Boolean — both together select one Eisenhower quadrant |
| `from`, `to` | `YYYY-MM-DD` due-date range |
| `search` | Matched against the title, case-insensitively |
| `sortBy` | `id` `title` `status` `priority` `dueDate` `startTime` `createdAt` `updatedAt` |
| `direction` | `ASC` `DESC` |
| `page`, `size` | Zero-based index, size capped at 100 |

### Error responses

Every failure has the same shape:

```json
{
  "timestamp": "2026-10-10T01:23:45.678Z",
  "status": 400,
  "error": "E0400",
  "message": "リクエストの内容が正しくありません。",
  "path": "/api/v1/tasks",
  "fieldErrors": { "title": "must not be blank" }
}
```

Tasks are always scoped to the authenticated account. A task belonging to someone else returns
**404**, not 403, so the response does not confirm that the row exists.

---

## Project layout

```text
spring-boot-todolist/
├─ src/main/java/com/todolist/
│  ├─ modules/
│  │  ├─ auth/      accounts, JWT issuance, current-user resolution
│  │  ├─ project/   areas of life that group tasks
│  │  ├─ label/     reusable metadata
│  │  └─ task/      the to-do items
│  └─ shared/       config, domain, exception, log, security, web
├─ src/main/resources/db/migration/   Flyway
├─ frontend/        React + TypeScript SPA
├─ docs/            ER, FLOW, DESIGN
├─ .claude/         skills used by Claude Code on this repo
├─ .husky/          git hooks
├─ Dockerfile  docker-compose.yml  Makefile
└─ task.md          build requirements, phase by phase
```

Each module is `domain / dto / repository / service / controller`. Modules depend on `shared`,
not on each other — the one exception is `task`, which references `project` and `label`. The
reverse direction goes through a domain event instead.

---

## Conventions

Coding standards and review criteria live in `.claude/skills/`:

| Skill | Covers |
|--|--|
| `java-clean-code` | Exceptions, controller/service/DTO rules, package layout, plus `java-code-style.md` and `jpa-best-practices.md` |
| `java-code-review` | Reviewing a diff against those rules |
| `security-audit` | 21 OWASP-oriented checks |
| `commit` | Japanese commit messages |

The essentials:

- Throw `AppException` with an `AppErrorCode`; never `IllegalArgumentException`,
  `RuntimeException`, or a bare `.orElseThrow()`
- No business logic, repositories or `try-catch` in a controller — `ApiExceptionHandler` handles
  failures
- Services default to `@Transactional(readOnly = true)`; write methods override it
- Reads use DTO projection or an entity graph; entities never reach the API
- Two-space indent, 100-column lines, Javadoc on public API

---

## Build and test

```bash
./mvnw clean verify            # compile, 32 backend tests, package
cd frontend && npm run build   # typecheck and bundle
cd frontend && npm run test:e2e  # 22 Playwright tests (needs both servers running)
```

See [TESTING](docs/TESTING.md) for the manual test guide, scenarios and screenshots.

---

## Deployment

```bash
make build    # build the Docker image
make deploy   # build and restart
make up       # start
make down     # stop
make logs     # follow application logs
make exec     # shell into the app container
make clean    # prune unused images
```

| Service | Port |
|--|--|
| `web` — nginx serving the SPA | 9001 |
| `app` — the REST API | 9002 |
| `db` — PostgreSQL 17 | 9432 |

`DB_PASSWORD` and `JWT_SECRET` are both required; `docker compose` refuses to start without
them. The SPA's API URL is baked into its bundle at build time — set `API_BASE_URL` in `.env`
and rebuild the `web` image when it changes. See [CI/CD](docs/CICD.md).

### Branch protection

`.husky/pre-push` rejects direct pushes to `develop` and `release`. `.husky/pre-commit` compiles
the backend first. Hooks install with `npm install` at the repository root.

## CI/CD

GitHub Actions runs the backend tests, the frontend build, the Playwright suite and a Docker
build on every push and pull request. A `v*` tag publishes the API and SPA images to GHCR.
Details in [CI/CD](docs/CICD.md).
