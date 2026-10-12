# spring-boot-todolist

A to-do application built as a **REST API + React SPA**: a Spring Boot modular monolith serving
JSON, and a separate Vite single-page app.

日本語版は [README.ja.md](README.ja.md) にあります。

| | |
|--|--|
| Backend | Java 21, Spring Boot 4.0.1, Spring Security 7, Spring Data JPA, Flyway, PostgreSQL |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS, axios, lucide-react, sonner |
| Auth | Stateless JWT (HS384) |
| Docs | [ER](docs/ER.md) · [FLOW](docs/FLOW.md) · [DESIGN](docs/DESIGN.md) · [MANUAL](docs/MANUAL.md) · [TESTING](docs/TESTING.md) · [SEED](docs/SEED.md) · [CI/CD](docs/CICD.md) |
| Build requirements | [task.md](task.md) |
| Live demo | [Open in Codespaces](#live-demo) — or run it locally in two minutes |
| Manual | [MANUAL](docs/MANUAL.md) — screen-by-screen guide, with a 15s walkthrough |

---

## User experience

![15 second walkthrough](docs/demo.gif)

The interface is one screen. Everything — capture, triage, editing, search — happens on the
dashboard, because a to-do list that makes you navigate to add a task will not be used.

**Views are derived, not filed.** Inbox, Today, Upcoming and Overdue are predicates over your
tasks rather than folders you drop things into, so nothing has to be organised to show up in the
right place, and the five views can never disagree with the data.

**Two switches, four quadrants.** A task carries *Urgent* and *Important* independently; the
Eisenhower quadrant is computed from the pair. There is no quadrant field to keep in sync.

**Nothing is destroyed quietly.** Deleting asks first and then soft-deletes. Deleting a project
returns its tasks to the Inbox instead of taking them down with it.

### Visual language

| | |
|--|--|
| Palette | Tailwind `slate` for surfaces and text; a violet→fuchsia gradient for the single primary action on each screen |
| Priority | `emerald` Low · `amber` Medium · `rose` High — as a badge and as the stripe down the card edge |
| Quadrants | Dashed borders: `rose` Do · `violet` Schedule · `amber` Delegate · `slate` Eliminate |
| Dark mode | A manual toggle adding `.dark` to `<html>`, remembered per browser. Not tied to the OS setting, so the choice stays where the user put it |
| Type | System font stack, `tabular-nums` for counts so numbers do not jitter as they update |
| Icons | lucide-react, 16px, always paired with a text label |

Colour never carries meaning alone: every priority badge and quadrant also states its name in
words, so the list stays readable without colour vision.

### How it responds

| Moment | What the UI does |
|--|--|
| Loading | Skeleton cards shaped like the real rows, not a spinner over an empty page |
| Empty | A purposeful empty state explaining what to do next, not a blank panel |
| Success | A toast, bottom-right — away from the task list it just changed |
| Failure | The server's message verbatim, so validation errors say what is actually wrong |
| Destructive | A confirmation dialog, every time |
| Progress | Today's completion updates the moment a checkbox is ticked |

Full rationale and the screen inventory are in [DESIGN.md](docs/DESIGN.md) §5; the step-by-step
guide is [MANUAL.md](docs/MANUAL.md).

---

## Live demo

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/NweNweWinThan-X/spring-boot-todolist)

> **No server is hosting this application.** Nothing is deployed, so there is no public URL that
> runs on someone else's machine. The button above is the closest thing: it builds the whole
> stack in a browser-based environment, and the steps below run it locally. Both start with the
> demo data already loaded.

### Option A — in the browser, nothing installed

Click the badge. GitHub Codespaces builds the environment from
[`.devcontainer/devcontainer.json`](.devcontainer/devcontainer.json) — JDK 21, Node 22 and the
npm packages — then run two commands in its terminal:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2   # terminal 1
npm --prefix frontend run dev                           # terminal 2
```

Port 5173 is forwarded and opens by itself. The SPA reaches the API through the Vite dev proxy
at `/api`, so the generated `*.app.github.dev` hostname never has to appear in the API's CORS
allowlist.

Requires a GitHub account, and it draws on that account's Codespaces free tier. It is a real
environment rather than a hosted page, so the first build takes a couple of minutes.

### Option B — on your machine, two minutes

```bash
git clone https://github.com/NweNweWinThan-X/spring-boot-todolist.git
cd spring-boot-todolist
```

```bash
# Terminal 1 — API on :8080, in-memory H2, no environment variables needed
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

```bash
# Terminal 2 — SPA on :5173
npm --prefix frontend install && npm --prefix frontend run dev
```

Open <http://localhost:5173> and sign in:

| | |
|--|--|
| Username | `demo` |
| Password | `Demo1234!` |

The account arrives with 19 tasks across 3 projects and 4 labels, arranged so every view is
populated on first sight — overdue, due today, upcoming, inbox, all three statuses and all four
Eisenhower quadrants. A second account, `alex` with the same password, exists to show that
accounts cannot see each other's data. Full dataset in [SEED](docs/SEED.md).

Nothing is written outside the process: the `h2` profile keeps everything in memory and discards
it when you stop the server.

> **Ports are fixed on purpose.** The SPA calls `http://localhost:8080/api/v1` and the API's CORS
> allowlist contains only `:3000` and `:5173`, so moving either port breaks the pair.

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
| `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` / `APP_ADMIN_EMAIL` | Creates an administrator on first start of the `local`, `dev` and `h2` profiles |

### 1. Backend

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Flyway migrates on startup. The API listens on `http://localhost:8080`.

Two profiles need no setup at all and come with demo data already loaded — sign in as `demo`
with `Demo1234!`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # PostgreSQL on localhost, no env vars
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2    # in-memory, no database to install
```

Note that `.env` is read by `docker compose` only — Spring Boot does not load it. See
[SEED](docs/SEED.md).

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
├─ docs/            ER, FLOW, DESIGN, TESTING, SEED, CI/CD
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
