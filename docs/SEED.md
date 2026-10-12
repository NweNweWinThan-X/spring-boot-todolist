# SEED — Demo data for development and testing

A fixed dataset that makes every screen, view and quadrant non-empty the moment the
application starts, so you can look at the UI and run manual tests without clicking a
single task into existence first.

| | |
|--|--|
| PostgreSQL | [`db/seed/R__demo_seed.sql`](../src/main/resources/db/seed/R__demo_seed.sql) — a Flyway **repeatable** migration |
| H2 | [`db/seed/h2-demo-seed.sql`](../src/main/resources/db/seed/h2-demo-seed.sql) — loaded by `spring.sql.init` |
| Accounts | `demo` / `alex`, both with password `Demo1234!` |
| Rows | 2 users, 5 projects, 5 labels, 21 tasks, 10 task–label links |

---

## 1. Loading it

### The `dev` profile — PostgreSQL, zero setup

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Needs no environment variables. `application-dev.properties` defaults to
`jdbc:postgresql://localhost:5432/todolist` as `postgres`, adds `classpath:db/seed` to
`spring.flyway.locations`, and Flyway applies the seed after the schema migrations.

### The `h2` profile — in-memory, no database to install

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

`h2` disables Flyway and builds the schema with `ddl-auto=update`, so the seed cannot be a
Flyway migration there. It is loaded by `spring.sql.init.data-locations` instead, which is why
that profile also sets `spring.jpa.defer-datasource-initialization=true` — without it the
script would run before Hibernate created the tables and fail on every insert.

### The `local` profile — no demo data

`local` reads `classpath:db/migration` only. Use it when you want the production-shaped
configuration: credentials from the environment, schema from Flyway, and an empty database.

---

## 2. Why there are two files

The two cannot be one file. The migrations use PostgreSQL features H2 does not have —
partial indexes (`create index ... where deleted = false`) and `timestamptz` — which is why
the `h2` profile cannot run `db/migration` at all. The seeds differ in two more places:

| | PostgreSQL | H2 |
|--|--|--|
| Relative dates | `current_date - 5` | `dateadd('DAY', -5, current_date)` |
| Sequence bump | `setval(..., greatest(...))` | `alter sequence ... restart with` |

Both hold the same 21 tasks with the same ids, so a test written against one profile reads
identically on the other.

---

## 3. What the data is for

Every row exists to make something specific visible. The dataset is built around
`current_date`, so the views stay correct no matter when you seed.

| Task | Due | What it demonstrates |
|--|--|--|
| Submit quarterly tax return | −5d | `OVERDUE`, quadrant `DO` |
| Fix broken deploy script | −1d | `OVERDUE` while `IN_PROGRESS` |
| Team stand-up notes | today | `TODAY`, a 09:00–09:15 time range with the alert on |
| Review pull request #42 | today | `TODAY` + `DO`, two labels on one task |
| Dentist appointment | today | `TODAY` in a second project, alert on |
| Write design doc for billing | +2d | `UPCOMING`, quadrant `SCHEDULE` |
| Plan Q3 roadmap | +5d | `UPCOMING` |
| Renew passport | +6d | `UPCOMING` at the far edge of the window |
| Finish Spring Boot 4 chapter | +3d | `IN_PROGRESS` with an evening time range |
| Cancel unused subscriptions | +4d | quadrant `ELIMINATE` with a due date |
| **Book flights for holiday** | **+10d** | **outside** `UPCOMING` — the boundary case |
| Read Hibernate 7 release notes | — | no due date at all |
| Buy milk and coffee | today | `INBOX` (no project) and `TODAY` at once |
| Reply to recruiter email | — | `INBOX` + `DELEGATE` |
| Scroll social media | — | quadrant `ELIMINATE` |
| Ship v1.0 release notes | −2d | `COMPLETED` and past due — proves `OVERDUE` excludes completed |
| Set up CI pipeline | −3d | `COMPLETED` |
| Water the plants | today | `COMPLETED` today, so daily progress is non-zero |
| Hand over old client site | +3d | a task inside an **archived** project |
| Alex quarterly review | today | belongs to `alex` — must never appear for `demo` |
| Alex inbox item | — | ditto, in Alex's inbox |

Coverage this produces for `demo`: 19 tasks, all three statuses, all three priorities, all
four Eisenhower quadrants, 8 tasks carrying labels, 4 with time ranges, and every view
populated — `ALL` 19, `INBOX` 3, `TODAY` 5, `OVERDUE` 2, `UPCOMING` 11.

> `UPCOMING` is `between(today, today + 7)`, so it **includes** today and does not exclude
> completed tasks. That is why it is 11 and not 6. See
> [`TaskSpecifications`](../src/main/java/com/todolist/modules/task/repository/TaskSpecifications.java).

The two `alex` rows exist for the data-isolation scenario in
[TESTING.md](TESTING.md) §S2: signed in as `demo`, `GET /api/v1/tasks/9320` returns **404**,
not 403 — an unowned row is indistinguishable from a missing one.

---

## 4. Re-running, resetting, removing

**Re-running is safe.** Both scripts delete the reserved id range before inserting, so they
converge on the same state rather than duplicating rows. Flyway re-applies the PostgreSQL
version whenever its checksum changes; edit the file and restart.

Force a re-apply without editing:

```bash
psql -d todolist -c "delete from flyway_schema_history where script like '%demo_seed%';"
```

Remove the demo data and keep your own:

```bash
psql -d todolist -c "delete from task_label where task_id between 9301 and 9399;
                     delete from task     where id      between 9301 and 9399;
                     delete from label    where id      between 9201 and 9299;
                     delete from project  where id      between 9101 and 9199;
                     delete from app_user where id      between 9001 and 9099;"
```

For H2 nothing is needed — the database is in-memory and dies with the process.

---

## 5. Reserved ids, and why the sequences jump to 10050

The seed uses fixed ids so the `task_label` links and the test expectations can reference
them. Each table has a reserved band, and **nothing else may use these**:

| Table | Reserved |
|--|--|
| `app_user` | 9001–9099 |
| `project` | 9101–9199 |
| `label` | 9201–9299 |
| `task` | 9301–9399 |

Both scripts then push the sequences to **10050**, not 10000. The entities generate ids with
`SEQUENCE` and `allocationSize = 50`, and Hibernate's pooled optimizer takes one `nextval`
as the *high* value of a block and hands out `(hi − 50, hi]`. Restarting at 10000 therefore
produces ids **9951–10000** on the first insert — directly below the sequence value and
uncomfortably close to the reserved band. Restarting at 10050 keeps generated ids at 10001
and above. Verified: the first task created after seeding gets id `10001`.

The PostgreSQL version wraps this in `greatest(last_value, 10050)` so a re-run never *lowers*
a sequence. Lowering it would re-issue ids that already exist and fail on the primary key.

---

## 6. Why this cannot reach production

Three independent reasons, any one of which is sufficient:

1. The seed lives in `classpath:db/seed`, and only `application-dev.properties` adds that
   location to `spring.flyway.locations`. The default and `docker` profiles read
   `classpath:db/migration` alone.
2. The H2 script is referenced only from `application-h2.properties`.
3. `application-dev.properties` is the only file with a hardcoded password, and
   `application.properties` still resolves `${DB_PASSWORD}` with **no default** — a deployed
   environment that forgets the variable fails to start rather than falling back.

The committed BCrypt hash is a hash of the known string `Demo1234!` at strength 12. It is a
development fixture, not a credential for any reachable system.
