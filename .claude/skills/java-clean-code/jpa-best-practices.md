# JPA Best Practices
Based on Vlad Mihalcea — High-Performance Java Persistence

Core rules for designing JPA entities and repositories.

---

## 1. Entity identity

Entities must survive all lifecycle states.

Rules:
- `equals` compares **non-null id**
- `hashCode` must be **constant**
- Never use mutable fields in `hashCode`

Replace `Entity` with the concrete entity class.

```java
@Override
public boolean equals(Object o) {
  if (this == o) return true;
  if (!(o instanceof Post other)) return false;
  return id != null && id.equals(other.id);
}

@Override
public int hashCode() {
  return getClass().hashCode();
}
```

---

## 2. Entity design

Avoid exposing public setters.

Prefer:

* constructors
* domain methods

Example:

```java
public void changeName(String name) {
  this.name = name;
}
```

Reason:

* prevents invalid states
* keeps entity invariants consistent

---

## 3. Audit fields

Every table should contain audit metadata.

Minimum:

* `created_at`
* `updated_at`

Optional:

* `created_by`
* `updated_by`
* `deleted` (soft delete)

---

## 4. Association design

### OneToMany

Use **bidirectional mapping**.

Rules:

* parent uses `mappedBy`
* child owns the foreign key
* maintain both sides with helper methods

Large collections should **not be mapped**.
Query children directly instead.

---

### ManyToMany

Rules:

* Always use `Set`
* Cascade only `{PERSIST, MERGE}`
* Never use `CascadeType.REMOVE` or `CascadeType.ALL`

Reason:
`List` causes full delete + reinsert operations.

Prefer replacing `ManyToMany` with a **join entity**
when the relationship may contain metadata.

Example:

```
UserRole
PostTag
```

---

### OneToOne

Use **child → parent mapping with `@MapsId`**.

Rules:

* parent should not own the relation
* child PK = parent FK
* avoid parent-side `@OneToOne`

Reason:
parent-side mapping often triggers extra SELECTs.

---

## 5. Fetch strategy

Always declare fetch type explicitly.

```java
@ManyToOne(fetch = FetchType.LAZY)
@OneToOne(fetch = FetchType.LAZY)
```

Reason:
Default is `EAGER`, which often causes N+1 problems.

---

## 6. N+1 prevention

Typical N+1 pattern:

```java
list.forEach(e -> e.getRelation().getName());
```

Solutions:

1. `JOIN FETCH`
2. DTO projection

DTO projection is preferred for read-only queries.

---

## 7. Query design

Avoid loading full entities when not necessary.

Prefer:

* DTO projection
* pagination (`Pageable`)
* specific queries

Avoid:

```
repository.findAll()
```

Use `findAll()` only for small master tables.

---

## 8. `getReferenceById`

Use when only a foreign key reference is required.

```java
User ref = userRepository.getReferenceById(id);
entity.setUser(ref);
```

Reason:
No SELECT is executed.

Note:
Accessing a non-existing reference may throw

```
EntityNotFoundException
```

Use `findById` when entity fields must be validated.

---

## 9. Composite keys

Use:

* `@Embeddable`
* `@EmbeddedId`

Never use:

* `@IdClass`

Reason:
`@IdClass` duplicates fields and complicates mappings.

---

## 10. ID generation strategy

`IDENTITY`

* simple
* disables batch insert

`SEQUENCE`

* supports batch inserts
* preferred for high-volume insert operations

Example:

```java
@GeneratedValue(strategy = GenerationType.SEQUENCE)
```

---

## 11. API design

Never expose entities in API responses.

Use DTO objects instead.

Reason:

* avoids lazy loading issues
* prevents N+1 queries
* protects internal domain model

---

## Core principles

1. Explicit fetch strategy
2. Avoid unnecessary entity loading
3. Prefer DTO queries for read operations
4. Minimize entity collections
5. Design relations based on SQL efficiency
