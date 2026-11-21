# AGENTS.md — Development Guidelines & Architectural Rules

Guidelines for AI agents and developers working on the `loyalty-engine` project.

---

## 1. Architectural Role & Context

`loyalty-engine` is a **Loyalty & Rewards Core Backend Service** built on **Spring Boot 3.2+** and **Java 17**.
- Manages loyalty accounts, customer tiers, point accruals, ledger transactions, and reward redemptions.
- Provides institutional and third-party persistence via **PostgreSQL** (leveraging JSONB for dynamic tier rules and reward metadata).
- Handles multi-source authentication: external e-commerce / POS terminals (M2M via API Key), backoffice administrators, and client portals.
- Emits transactional receipts/reports via OpenPDF.
- Exposes REST APIs documented via OpenAPI 3 / Swagger UI.

---

## 2. Zero-Boilerplate Standards

To keep the codebase maintainable, secure, and concise, boilerplate code is strictly forbidden.

### 2.1 DTOs (Data Transfer Objects)
- **Always use Java 17 `record`** for all REST request and response payloads.
- **Never create classes with manual getters, setters, equals, or hashCode for DTOs**.
- **Immutability by default**: Records guarantee thread-safety and shallow immutability.
- **Declarative Jakarta Validation directly on record components**:
  ```java
  public record AccruePointsRequestDTO(
      @NotNull(message = "Account ID is required")
      Long accountId,

      @NotNull(message = "Points amount is required")
      @Positive(message = "Points to accrue must be strictly positive")
      Integer points,

      @NotBlank(message = "Transaction reference is required")
      @Size(max = 64, message = "Reference must not exceed 64 characters")
      String transactionReference,

      @Valid
      TransactionMetadataDTO metadata
  ) {}
  ```
- **Entity to DTO isolation (Static Factory Methods)**:
  - DTOs must provide a static `from(Entity entity)` method when mapping from JPA entities.
  - **Never expose JPA entities directly in `@RestController` endpoints** (avoids accidental `LazyInitializationException` and leaking database structure).
  ```java
  public record LoyaltyAccountResponseDTO(
      Long id,
      String accountNumber,
      Integer currentBalance,
      String tierLevel,
      String customerName
  ) {
      public static LoyaltyAccountResponseDTO from(LoyaltyAccount account) {
          return new LoyaltyAccountResponseDTO(
              account.getId(),
              account.getAccountNumber(),
              account.getCurrentBalance(),
              account.getTier() != null ? account.getTier().getName() : null,
              account.getCustomer() != null ? account.getCustomer().getFullName() : null
          );
      }
  }
  ```

### 2.2 JPA Entities
- **Selective Lombok Usage**:
  - Use `@Getter` at the class level.
  - **NEVER use `@Data` on JPA entities**. `@Data` generates `equals()`, `hashCode()`, and `toString()` across all fields, which breaks Hibernate identity contracts and causes infinite recursion / `LazyInitializationException` on lazy associations.
  - **Avoid class-level `@Setter`**. Entities must not be anemic bags of setters.
- **Constructors**:
  - `protected EntityName() {}` or `@NoArgsConstructor(access = AccessLevel.PROTECTED)` to satisfy JPA/Hibernate requirements while preventing unvalidated public instantiation.
  - Provide explicit domain constructors that enforce required invariants (throw `IllegalArgumentException` on missing required attributes).
- **Rich Domain Model & Encapsulation**:
  - State changes must occur via explicit domain methods (e.g., `account.accruePoints(amount, reference)`, `account.redeem(amount)`, `account.upgradeTier(newTier)`).
  - Business rules must be verified inside the entity, throwing domain-specific runtime exceptions (e.g., `InsufficientPointsException`, `InvalidAccountStateException`).
- **JPA Associations**:
  - **Always specify `FetchType.LAZY`** on `@ManyToOne` and `@OneToOne` relationships.
  - Never use default `FetchType.EAGER`.
  - Use `cascade = CascadeType.ALL` and `orphanRemoval = true` only for strict aggregate compositions (e.g., `LoyaltyAccount` owning its `List<PointTransaction>`).
- **PostgreSQL JSONB Mapping**:
  - Use Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)` for JSON columns (e.g. flexible metadata, reward rule configuration).

---

## 3. Validation & Exception Handling

- **Fail-Fast Controller Boundary**:
  - Annotate request bodies with `@Valid @RequestBody`.
  - Use `@Valid` on nested objects and validation annotations inside collections (`List<@Valid ...>`).
- **Domain Invariants**:
  - Validate parameters inside entity constructors and domain methods; throw `IllegalArgumentException` or domain runtime exceptions.
- **Centralized Error Responses**:
  - All REST error responses are intercepted by `@RestControllerAdvice` (`GlobalExceptionHandler`).
  - Emit standardized `ApiErrorResponse` payloads containing:
    - `errorCode`: Semantic string (e.g., `"INSUFFICIENT_POINTS"`, `"RESOURCE_NOT_FOUND"`).
    - `message`: Human-readable error description.
    - `path`: Request URI.
    - `timestamp`: UTC timestamp.

---

## 4. Spring Boot Security Best Practices

Security in `loyalty-engine` uses Spring Security 6 with three distinct, ordered filter chains:

```
[Incoming Request]
       │
       ├── /api/v1/events + /api/v1/coupons/** ──> Order(1): integrationSecurity (Stateless, ApiKeyFilter, ROLE_INTEGRATION_SERVICE)
       │
       ├── /api/**             ──> Order(2): applicationSecurity (REST API, JsonAuthenticationFilter, 401 EntryPoint)
       │
       └── /**                 ──> Order(3): portalSecurity (Backoffice Web, formLogin, Session)
```

### 4.1 Security Filter Chains
1. **Chain 1 (`@Order(1)` — `integrationSecurity`)**:
   - Matches: `/api/v1/events` and `/api/v1/coupons/**` (POS event ingestion and coupon validation/redemption).
   - Authentication: Custom `ApiKeyFilter` evaluating header `X-API-KEY`.
   - Session policy: `SessionCreationPolicy.STATELESS`.
   - CSRF: Disabled.
2. **Chain 2 (`@Order(2)` — `applicationSecurity`)**:
   - Matches: `/api/**`.
   - Authentication: Custom `JsonAuthenticationFilter` handling JSON POST to `/api/auth/login`.
   - Session policy: `SessionCreationPolicy.IF_REQUIRED`.
   - Context persistence: `HttpSessionSecurityContextRepository`.
   - Entry point: `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)`. **Crucial**: REST endpoints must return HTTP 401 Unauthorized status on unauthenticated access, NEVER a 302 redirect to HTML login.
   - Logout: `/api/auth/logout` returning HTTP 204 No Content.
3. **Chain 3 (`@Order(3)` — `portalSecurity`)**:
   - Matches: Any other path (`/**`).
   - Purpose: Management/Backoffice SSR portal.
   - Form Login: Configured on `/login` redirecting to `/dashboard`.

### 4.2 Security Layer Separation
- **Decouple HTTP from Business Services**:
  - `JsonAuthenticationFilter` extends `AbstractAuthenticationProcessingFilter` and parses JSON credentials via `ObjectMapper`.
  - `SiteAuthenticationProvider` handles `AuthenticationProvider` authentication logic.
  - `AuthenticationService` is a pure Spring `@Service`. It **never** injects or references `HttpServletRequest`, `HttpServletResponse`, or `HttpSession`.
- **Role Constants**:
  - All roles are centralized in a `Roles` class (e.g., `Roles.ROLE_ADMIN`, `Roles.ROLE_CUSTOMER`, `Roles.ROLE_INTEGRATION_SERVICE`).

---

## 5. Dependency Rules (`pom.xml`)

Only add dependencies aligned with the architecture:
- `spring-boot-starter-web`: REST endpoints and Tomcat.
- `spring-boot-starter-data-jpa`: Hibernate 6 / JPA repository layer.
- `postgresql`: Database driver with JSONB support.
- `spring-boot-starter-security`: Spring Security 6.
- `spring-boot-starter-validation`: Jakarta Bean Validation.
- `spring-retry` & `spring-boot-starter-aop`: Fault-tolerant external calls.
- `com.github.librepdf:openpdf`: Programmatic PDF generation.
- `org.projectlombok:lombok`: Boilerplate reduction (`@Getter`, `@RequiredArgsConstructor`).
- `springdoc-openapi-starter-webmvc-ui`: Swagger UI and OpenAPI documentation.
- `spring-boot-starter-thymeleaf`: SSR backoffice portal (when needed).

---

## 6. Testing Conventions

- **No Mocking Internal JPA Repositories in Service Tests**:
  - Use `@DataJpaTest` with H2 or PostgreSQL test containers to verify real SQL queries, constraints, and JSONB mapping.
- **Security Testing**:
  - Use `spring-security-test` (`@WithMockUser`, `SecurityMockMvcRequestPostProcessors.csrf()`).
  - Verify that REST endpoints return 401 (not 302) when unauthenticated.
- **REST Controller Tests**:
  - Use `MockMvc` or `@WebMvcTest` with `@Valid` payload validation checks.

---

## 7. Git & Commit Workflow

- **Commit per Completed Task / Feature**: Each completed task or feature MUST be committed immediately before advancing to the next one.
- **Conventional Commits**: Commit messages MUST strictly follow Conventional Commits standard (e.g., `feat: ...`, `fix: ...`, `refactor: ...`, `test: ...`, `docs: ...`, `chore: ...`).
- **No AI Attribution**: NEVER include `Co-Authored-By`, AI watermarks, or AI attributions in commit messages.
- **Atomic Work Units**: Never bundle multiple unrelated tasks or features into a single monolithic commit.

---

## 8. Rules Checklist for Agents

Before completing any task in `loyalty-engine`, verify:
- [ ] DTOs are declared as Java 17 `record`.
- [ ] No manual getters/setters in DTOs.
- [ ] Jakarta Validation annotations applied declaratively.
- [ ] JPA entities use `@Getter` (never `@Data`, avoid `@Setter`).
- [ ] JPA associations use `FetchType.LAZY`.
- [ ] No JPA entities returned directly by REST controllers (use `RecordDTO.from(entity)`).
- [ ] Services do not reference Servlet or HTTP transport classes.
- [ ] Security configurations preserve multi-chain separation and 401 entry point for REST.
- [ ] Each completed task or feature is committed immediately using Conventional Commits without AI attribution.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
at specs/001-core-loyalty-mvp/plan.md
<!-- SPECKIT END -->
