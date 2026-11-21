<!--
SYNC IMPACT REPORT
==================
Version Change: Template / Unratified -> 1.0.0
Type of Change: MAJOR (Initial formal ratification from repository guidelines)

Modified Principles:
- [PRINCIPLE_1_NAME] -> I. Zero-Boilerplate Immutability (Java 17 Records for DTOs)
- [PRINCIPLE_2_NAME] -> II. Rich Domain Model & Strict Entity Encapsulation
- [PRINCIPLE_3_NAME] -> III. Fail-Fast Validation & Centralized Error Handling
- [PRINCIPLE_4_NAME] -> IV. Multi-Tier Security Isolation & Transport Decoupling
- [PRINCIPLE_5_NAME] -> V. Realistic Persistence & Testing Discipline

Added Sections:
- Architecture & Technology Stack Standards
- Development Workflow & Quality Gates
- Governance & Architectural Compliance

Removed Sections:
- None (template placeholder sections replaced with concrete governance)

Templates Requiring Updates:
- .specify/templates/plan-template.md: ✅ Aligned (Constitution Check gate validates against Core Principles)
- .specify/templates/spec-template.md: ✅ Aligned (User stories & acceptance scenarios compatible)
- .specify/templates/tasks-template.md: ✅ Aligned (Phased implementation & testing tasks compatible)

Follow-up TODOs:
- None. All placeholders derived and ratified from AGENTS.md, KICKSTART.md, and pom.xml.
-->

# Loyalty Engine Constitution

## Core Principles

### I. Zero-Boilerplate Immutability (Java 17 Records for DTOs)
All REST request and response payloads MUST be declared as Java 17 `record` types.
- Manual getters, setters, `equals`, `hashCode`, or mutable DTO classes are strictly forbidden.
- Declarative Jakarta Validation annotations (`@NotNull`, `@NotBlank`, `@Positive`, `@Size`, `@Valid`) MUST be placed directly on record components.
- DTOs MUST provide static factory methods (e.g., `ResponseDTO.from(Entity entity)`) to isolate domain entities from the web layer.
- JPA entities MUST NEVER be returned directly from `@RestController` endpoints to avoid accidental data leaks and `LazyInitializationException`.

### II. Rich Domain Model & Strict Entity Encapsulation
JPA entities MUST represent rich domain models that protect business invariants; anemic domain models are forbidden.
- Entities MUST use Lombok `@Getter` at class level. `@Data` is strictly prohibited on JPA entities due to broken identity contracts and recursive evaluation on lazy associations. Class-level `@Setter` is strictly prohibited.
- Entities MUST declare protected no-arg constructors for JPA/Hibernate compliance and explicit domain constructors that enforce non-null and domain invariants.
- State transitions MUST occur exclusively through expressive domain methods (e.g., `account.accrue(points, ref, reason)`, `account.redeem(points, ref, reason)`), throwing explicit domain exceptions on rule violations.
- All `@ManyToOne` and `@OneToOne` associations MUST explicitly specify `FetchType.LAZY`. Default `FetchType.EAGER` is forbidden.
- Cascade operations (`CascadeType.ALL`, `orphanRemoval = true`) are restricted to strict aggregate root compositions (e.g., `LoyaltyAccount` owning its `PointTransaction` ledger).
- Dynamic metadata and flexible tier configurations MUST use PostgreSQL JSONB via Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)`.

### III. Fail-Fast Validation & Centralized Error Handling
The application MUST enforce defensive, fail-fast input validation at all system boundaries.
- Controller boundaries MUST annotate all incoming request bodies with `@Valid @RequestBody`, including validation across nested objects and collection elements (`List<@Valid ...>`).
- Business domain preconditions MUST be checked inside entities and services, throwing descriptive runtime exceptions (`IllegalArgumentException`, `InsufficientPointsException`, `ResourceNotFoundException`).
- All REST errors MUST be captured by a centralized `@RestControllerAdvice` (`GlobalExceptionHandler`) emitting standardized `ApiErrorResponse` payloads containing `errorCode`, `message`, `path`, and `timestamp`.

### IV. Multi-Tier Security Isolation & Transport Decoupling
Security MUST be partitioned into three dedicated, ordered filter chains preserving strict transport decoupling:
1. **Chain 1 (`@Order(1)` — `integrationSecurity`)**: Protects `/api/v1/events` and `/api/v1/coupons/**` for POS event ingestion and checkout callbacks. Enforces stateless session management, CSRF disabled, and `X-API-KEY` header evaluation granting `ROLE_INTEGRATION_SERVICE`.
2. **Chain 2 (`@Order(2)` — `applicationSecurity`)**: Protects application REST endpoints at `/api/**`. Uses `JsonAuthenticationFilter` for credentials posted to `/api/auth/login`, `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)` returning HTTP 401 (never a 302 redirect), and `/api/auth/logout` returning HTTP 204.
3. **Chain 3 (`@Order(3)` — `portalSecurity`)**: Protects administrative backoffice portal routes (`/**`) using standard form login redirecting to `/dashboard`.
- Business services (`@Service`) MUST remain decoupled from the HTTP transport layer and MUST NEVER inject or reference `HttpServletRequest`, `HttpServletResponse`, or `HttpSession`.
- Security roles MUST be centralized in a dedicated `Roles` constant class (e.g., `Roles.ROLE_ADMIN`, `Roles.ROLE_CUSTOMER`, `Roles.ROLE_INTEGRATION_SERVICE`).

### V. Realistic Persistence & Testing Discipline
Testing MUST validate real runtime semantics rather than superficial mock interactions.
- Mocking internal Spring Data JPA repositories in service tests is strictly prohibited. Persistence semantics, transactional rollback, unique constraints, and PostgreSQL JSONB mappings MUST be tested via `@DataJpaTest` using real database engines (H2 in PostgreSQL compatibility mode or Testcontainers).
- Security configurations MUST be verified with `spring-security-test`, explicitly validating that unauthenticated REST calls return HTTP 401 instead of 302 redirects.
- REST controllers MUST be tested using `MockMvc` or `@WebMvcTest` with invalid payloads to guarantee declarative Bean Validation triggers fail-fast error responses.

## Architecture & Technology Stack Standards

- **Runtime & Language**: Spring Boot 3.2.5 on Java 17 (records, pattern matching, sealed types).
- **Persistence**: PostgreSQL relational database with JSONB support; Hibernate 6 / Spring Data JPA.
- **Reporting & Documents**: OpenPDF (`com.github.librepdf:openpdf`) for transactional receipts, statements, and vouchers.
- **API Documentation**: OpenAPI 3 / Swagger UI via `springdoc-openapi-starter-webmvc-ui`.
- **Dependency Hygiene**: Only dependencies aligned with the established architectural roles are permitted in `pom.xml`. Unauthorized third-party libraries or duplicate utility packages are prohibited.

## Development Workflow & Quality Gates

Before declaring any feature or bugfix complete, developers and AI agents MUST verify:
- [ ] DTOs are declared as Java 17 `record` types with declarative Jakarta Validation.
- [ ] No manual getters, setters, `equals`, or `hashCode` in DTOs.
- [ ] No JPA entities exposed directly from controllers (`RecordDTO.from(entity)` pattern used).
- [ ] JPA entities use `@Getter` only (no `@Data`, no class-level `@Setter`).
- [ ] JPA associations use `FetchType.LAZY`.
- [ ] Domain invariants enforced via rich methods throwing domain runtime exceptions.
- [ ] Business services do not reference Servlet or HTTP transport classes.
- [ ] Multi-chain security rules and HTTP 401 entry points preserved.
- [ ] Git commit messages follow Conventional Commits without AI co-authorship tags.

## Governance

- This Constitution is the authoritative architectural contract for the `loyalty-engine` project and supersedes any conflicting local patterns or legacy practices.
- Runtime development guidance and operational context are detailed in `AGENTS.md` and `KICKSTART.md`.
- **Amendment Policy**: Amendments require consensus, updating this constitution document, and recording changes in the Sync Impact Report.
  - **MAJOR** version bumps: Breaking architectural shifts, removal of core principles, or altered security baselines.
  - **MINOR** version bumps: Added principles, new mandatory sections, or expanded governance rules.
  - **PATCH** version bumps: Non-breaking clarifications, corrections, or wording refinements.

**Version**: 1.0.0 | **Ratified**: 2026-11-21 | **Last Amended**: 2026-11-21
