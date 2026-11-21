# Receta y Estándares Zero-Boilerplate

Normas obligatorias para desarrolladores y agentes de IA al crear o modificar código en `loyalty-engine`.

---

## 1. DTOs: Obligatoriamente Java 17 `record`

- **NUNCA** utilices `class` con getters y setters para contratos REST.
- Los records garantizan inmutabilidad superficial y seguridad en hilos.
- Toda validación Jakarta se declara directamente en los componentes del record:

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

### Aislamiento de Entidades con Fábricas Estáticas
Para evitar filtrar detalles de persistencia y prevenir la infame `LazyInitializationException`, los DTOs de salida proveen un método `from(Entity entity)`:

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

---

## 2. Entidades JPA: Modelos Ricos y Encapsulados

- **Usar únicamente `@Getter`** a nivel de clase.
- **PROHIBIDO usar `@Data`**: Genera `equals()`, `hashCode()` y `toString()` automáticos que rompen contratos de Hibernate y desbordan la pila en asociaciones LAZY.
- **PROHIBIDO `@Setter` de clase**: Toda mutación se hace mediante métodos de dominio que validan invariantes de negocio.
- **Asociaciones**: Siempre especificar `FetchType.LAZY` en `@ManyToOne` y `@OneToOne`.
- **Constructores**: Constructor `protected` sin argumentos para Hibernate + constructor explícito con validación de invariantes.

```java
@Entity
@Table(name = "loyalty_accounts")
@Getter
public class LoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private Integer currentBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PointTransaction> transactions = new ArrayList<>();

    protected LoyaltyAccount() {}

    public LoyaltyAccount(String accountNumber, Customer customer) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be blank");
        }
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        this.accountNumber = accountNumber;
        this.customer = customer;
        this.currentBalance = 0;
    }

    public PointTransaction accrue(int points, String reference, String reason) {
        if (points <= 0) {
            throw new IllegalArgumentException("Accrual points must be positive");
        }
        this.currentBalance += points;
        PointTransaction tx = new PointTransaction(this, points, TransactionType.ACCRUAL, reference, reason);
        this.transactions.add(tx);
        return tx;
    }
}
```

---

## 3. Checklist Pre-Commit

Antes de considerar terminada una tarea, verificar:
- [ ] Todos los DTOs nuevos o modificados son `record`.
- [ ] No existen getters/setters manuales en DTOs.
- [ ] Entidades JPA usan `@Getter` (nunca `@Data`, sin `@Setter` de clase).
- [ ] Asociaciones JPA usan `FetchType.LAZY`.
- [ ] Ningún controlador devuelve directamente una entidad JPA.
- [ ] Los servicios `@Service` no inyectan `HttpServletRequest` ni `HttpSession`.
- [ ] Los endpoints REST no autenticados devuelven HTTP 401.
