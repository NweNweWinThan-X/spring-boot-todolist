---
name: java-clean-code
description: Review or write Java/Spring code for this project. Auto-trigger when the user asks to write, refactor, fix, or review Java/Spring code. Enforces project conventions for exceptions, controllers, services, DTOs, repositories, and project architecture. JPA rules are defined in `jpa-best-practices.md`. Code style rules are defined in `java-code-style.md`.
user-invocable: true
argument-hint: [file-path-or-class-name]
allowed-tools: Read, Grep, Glob, Edit, Write
---

# Java Clean Code Skill

## Workflow

When invoked:

1. **Read** the target file(s)
2. Read `java-code-style.md`
3. If the file contains **Entity or Repository code**, read **`jpa-best-practices.md`**
4. **Scan** against the Violation Checklist
5. **Fix violations in this order**

```
Exceptions → Controller → Service → Entity/Repository → DTO → Style
```

6. **Report every change and explain why**

---

# Violation Checklist

Run through these before finishing any code change.

---

## 1. Exceptions (CRITICAL)

- [ ] No `IllegalArgumentException`, `RuntimeException`, or `new Exception(...)`
- [ ] Use `AppException` or `Preconditions`
- [ ] No `.orElseThrow()` without explicit exception
- [ ] No try-catch blocks in controllers
- [ ] If error code missing → add to `AppErrorCode`

---

## 2. Controller

- [ ] No repository injection in controller
- [ ] No business logic
- [ ] No try-catch (handled by `GlobalExceptionHandler`)
- [ ] No formatting or display logic
- [ ] Admin endpoints must have `@PreAuthorize("hasAnyRole('ROLE_ADMIN')")`

Controller must only handle:

```
HTTP
Request/Response
View binding
```

All business logic belongs in services.

---

## 3. Service

Service layer contains business logic and transaction boundaries.

### Transaction rules

- [ ] Service classes should default to `@Transactional(readOnly = true)`
- [ ] Write methods must override with `@Transactional`
- [ ] Do not place `@Transactional` on private methods

Example:

```java
@Service
@Transactional(readOnly = true)
public class OrderService {

  public List<OrderDto> list() {
    return repository.findAllDto();
  }

  @Transactional
  public void create(OrderCommand command) {
    repository.save(new Order(command));
  }
}
```

---

### Service structure

- [ ] Complex services use **interface + implementation**
- [ ] Simple services may use a **single concrete class**

Example:

```
UserService
UserServiceImpl
```

---

### N+1 prevention

Service logic must not trigger N+1 queries.

Typical problem:

```java
orders.forEach(o -> o.getUser().getName());
```

Fix in the **repository query**, not in the service:

- `JOIN FETCH`
- DTO projection

---

## 4. Entity / Repository

Before reviewing entities or repositories:

- [ ] Read `JPA Best Practices`
- [ ] Ensure ORM design follows `JPA Best Practices`
- [ ] Fix violations related to:
  - entity mapping
  - associations
  - fetch strategy
  - composite keys
  - ID generation
  - query loading strategy

JPA rules are defined in `JPA Best Practices`.

Before reviewing entities or repositories, read `JPA Best Practices` and apply those rules.

---

## 5. DTO Rules

Use these rules when generating or refactoring DTOs.

### 1. Purpose

DTO (Data Transfer Object) is used for **data transport between layers**.

Typical usage:

- Controller ↔ Service
- Service ↔ UI
- Service ↔ External API

DTO must **not contain domain behavior**.

Domain logic belongs to **Entities in the domain layer**.

---

### 2. Naming

Use these suffixes.

| Suffix | Purpose |
|------|------|
| `Dto` | Read model / API response |
| `Command` | Write operation input |
| `Query` | Query parameters |
| `Form` | UI form input |

Example:

```java
public record OrderDto(Long id, String status) {}
```

---

### 3. Mapping

DTO should be created from **Domain Entities**.

Example:

```java
public static OrderDto from(Order order) {
  return new OrderDto(
      order.getId().getValue(),
      order.getStatus().name()
  );
}
```

Mapping logic stays **inside DTO**.

---

### 4. JPA Usage

DTO **can be used for query projection**.

Example:

```java
@Query("""
    select new com.example.OrderDto(
      o.id,
      o.status
    )
    from Order o
    """)
List<OrderDto> findOrders();
```

DTO must **not participate in persistence lifecycle**.

DTO must NOT:

- be annotated with `@Entity`
- be persisted with `persist` or `merge`

---

### Rule

Keep DTO **simple, immutable, and stable**.

---

## 6. Package Structure

```
modules/{name}/
  domain/
    JPA entities
  dto/
    Dto / Command / Form
  repository/
    Spring Data repositories (domain interfaces)
  service/
    Business logic
  controller/
    HTTP layer
```

### Rules

- Controllers must not access repositories directly
- Business logic belongs in services
- DTOs isolate API/view models from entities

---

## 7. Repository Conventions

Prefer DTO projection for read queries.

Use text blocks for multi-line JPQL.

Example:

```java
@Query("""
    select new com.example.dto.DepositDto(
      g.id,
      a.companyName
    )
    from MoneylookGoods g
    join g.account a
    where g.deleted = false
    """)
List<DepositDto> findAllActive();
```

---

## 8. Magic Values

Replace magic values with constants when they improve clarity.

Avoid extracting constants for values used only once.

Example:

```java
// NG
Sort.by("username")

// OK
private static final String SORT_FIELD_USERNAME = "username";
Sort.by(SORT_FIELD_USERNAME);
```

---

## 9. Security

Use method-level authorization for protected endpoints.

Example:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Never expose administrative operations without authorization.

## 10. Code Style

Follow rules defined in **`java-code-style.md`**.

Check for violations related to:

- file structure
- import ordering
- indentation
- whitespace
- naming conventions
- braces
- modifier order
- annotations
- comments
- Javadoc

Fix style violations automatically when safe.
Avoid unnecessary formatting changes.
