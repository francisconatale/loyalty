# Constitución y Principios de Desarrollo

La constitución del proyecto en `.specify/memory/constitution.md` establece las reglas de arquitectura no negociables para `loyalty-engine`. Cualquier código generado o revisado debe respetar estos cinco principios.

---

## Principio I: Zero-Boilerplate Immutability (Java 17 Records)

Todos los payloads de solicitud (request) y respuesta (response) REST **deben** ser Java 17 `record`.

- Quedan estrictamente prohibidas las clases con getters/setters manuales para DTOs.
- Las validaciones declarativas de Jakarta Validation (`@NotNull`, `@NotBlank`, `@Positive`, etc.) se colocan directamente en los componentes del record.
- Los DTOs deben proveer métodos estáticos de fábrica `from(Entity entity)` para aislar el modelo persistente.
- **Nunca exponer entidades JPA directamente en `@RestController`** (evita `LazyInitializationException` y fugas del esquema de base de datos).

```java
// Ejemplo canónico de DTO inmutable
public record AccruePointsRequestDTO(
    @NotNull(message = "Account ID is required")
    Long accountId,

    @NotNull(message = "Points amount is required")
    @Positive(message = "Points to accrue must be strictly positive")
    Integer points,

    @NotBlank(message = "Transaction reference is required")
    @Size(max = 64, message = "Reference must not exceed 64 characters")
    String transactionReference,

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    String reason
) {}
```

---

## Principio II: Rich Domain Model & Encapsulación

Las entidades JPA representan modelos de dominio ricos que protegen sus invariantes.

- Usar Lombok `@Getter` a nivel de clase. **PROHIBIDO usar `@Data`** (rompe contratos de Hibernate y causa desbordamiento de pila por llamadas recursivas en asociaciones perezosas).
- **PROHIBIDO `@Setter` a nivel de clase**: Las entidades no son bolsas anémicas de propiedades.
- Las transiciones de estado ocurren mediante métodos de dominio explícitos (ej. `account.accrue(...)`, `account.redeem(...)`).
- Asociaciones `@ManyToOne` y `@OneToOne` siempre con `FetchType.LAZY`.
- Mapeo PostgreSQL JSONB con Hibernate 6 vía `@JdbcTypeCode(SqlTypes.JSON)`.

---

## Principio III: Fail-Fast & Manejo Centralizado de Excepciones

- Los límites de controlador anotan el cuerpo con `@Valid @RequestBody`.
- Las validaciones de negocio en el dominio lanzan excepciones en tiempo de ejecución explícitas (`InsufficientPointsException`, `ResourceNotFoundException`, `IllegalArgumentException`).
- Un `@RestControllerAdvice` centralizado (`GlobalExceptionHandler`) intercepta todas las excepciones y devuelve un payload uniforme `ApiErrorResponse`:
  - `errorCode`: Código semántico estable.
  - `message`: Descripción legible del problema.
  - `path`: URI de la petición.
  - `timestamp`: Marca de tiempo UTC.

---

## Principio IV: Aislamiento de Seguridad y Desacoplamiento de Transporte

- Tres cadenas de filtros independientes y ordenadas en Spring Security 6 (`Order(1)` integración POS con `X-API-KEY`, `Order(2)` REST con login JSON y 401 unauthenticated, `Order(3)` portal web).
- La capa de servicios (`@Service`) **nunca** inyecta ni referencia clases de transporte HTTP (`HttpServletRequest`, `HttpServletResponse`, `HttpSession`).
- Roles centralizados en la clase de constantes `Roles`.

---

## Principio V: Disciplina de Pruebas de Persistencia

- **Prohibido mockear repositorios JPA internos** en tests de servicios de negocio.
- Usar `@DataJpaTest` con base de datos real (H2 en modo PostgreSQL o Testcontainers) para verificar constraints, mapeos JSONB y queries SQL reales.
- Pruebas de seguridad verifican que las peticiones REST no autenticadas devuelvan HTTP 401 (y nunca 302 redirect).
