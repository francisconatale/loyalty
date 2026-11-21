# Contributing to Loyalty Engine

Thank you for your interest in contributing to **Loyalty Engine**! This document provides the guidelines, architectural rules, and workflows you need to know to contribute effectively.

---

## 1. Development Setup

### Prerequisites
- **Java 17+** (OpenJDK or GraalVM recommended)
- **Maven 3.8+**
- **Docker** (for local PostgreSQL instance)

### Local Environment
1. Start the PostgreSQL container:
   ```bash
   docker run --name loyalty-postgres \
     -e POSTGRES_DB=loyalty_engine \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=postgres \
     -p 5432:5432 -d postgres:16-alpine
   ```

2. Compile and run the service:
   ```bash
   mvn clean spring-boot:run
   ```

3. Run the automated test suite:
   ```bash
   mvn test
   ```

---

## 2. Zero-Boilerplate Standards

To keep the codebase maintainable, concise, and type-safe, adhere strictly to the following conventions:

### Data Transfer Objects (DTOs)
- **Always use Java 17 `record`** for request and response payloads.
- **Never create classes with manual getters, setters, equals, or hashCode for DTOs**.
- Declare Jakarta Validation annotations directly on record components (`@NotNull`, `@NotBlank`, `@Positive`).
- Provide static factory methods `RecordResponseDTO.from(Entity entity)` to map JPA entities to DTOs without leaking database structures or triggering `LazyInitializationException`.

### JPA Entities
- Use `@Getter` at the class level. **Never use `@Data` or class-level `@Setter`**.
- Keep default constructors protected (`protected EntityName() {}`).
- Maintain rich domain models: business state transitions must happen through explicit domain methods (e.g., `account.accrue(points, ref)`), validating invariants and throwing domain runtime exceptions.
- Always specify `FetchType.LAZY` on `@ManyToOne` and `@OneToOne` relationships.

### Service Layer
- Use Lombok `@RequiredArgsConstructor` for constructor injection.
- Keep `@Service` classes pure domain logic: never inject or reference `HttpServletRequest`, `HttpServletResponse`, or other HTTP transport classes.

---

## 3. Extending the Engine

The engine provides Service Provider Interfaces (SPIs) that make adding new POS vendors and automation rules straightforward:

| Extension Point | Interface | Package |
|---|---|---|
| **POS Adapter** | `PosNormalizationAdapter` | `com.loyalty.integration.adapters` |
| **Automation Trigger** | `TriggerEvaluator` | `com.loyalty.automation.trigger` |
| **Automation Action** | `ActionExecutor` | `com.loyalty.automation.action` |
| **Notification Channel** | `EmailProviderAdapter` / SPIs | `com.loyalty.notification` |

For detailed walkthroughs and code examples, consult the [Extension Guide](docs/guides/extending.md).

---

## 4. Git & Commit Workflow

- **Conventional Commits**: Every commit message must follow the Conventional Commits format:
  ```
  feat: add square pos normalization adapter
  fix: handle null phone number in customer resolution
  test: add integration test for coupon burn idempotency
  docs: update pos events contract reference
  ```
- **No AI Attribution**: Never include `Co-Authored-By`, AI watermarks, or AI tool references in commit messages.
- **Atomic Commits**: Each commit should represent a single logical, buildable change with passing tests.

---

## 5. Pull Request Checklist

Before submitting your pull request, please verify:
- [ ] Code compiles without warnings (`mvn clean test-compile`).
- [ ] All unit and integration tests pass (`mvn test`).
- [ ] DTOs are declared as Java 17 `record` with declarative validation.
- [ ] JPA entities do not use `@Data` or `@Setter`.
- [ ] REST endpoints return `HTTP 401 Unauthorized` for unauthenticated requests.
- [ ] Documentation has been updated if new endpoints or adapters were introduced (`mkdocs build --strict`).
