# Guía de Extensión y Contribución

Esta guía detalla cómo extender **Loyalty Engine** con nuevas integraciones de puntos de venta (POS), reglas de automatización, canales de notificación y nuevas funcionalidades de negocio, respetando la arquitectura limpia y los estándares zero-boilerplate.

---

## 1. Puntos de Extensión del Sistema

El motor expone interfaces de proveedor de servicios (SPI) implementadas como componentes de Spring (`@Component`):

| Punto de Extensión | Interfaz | Paquete | Rol |
|---|---|---|---|
| **Adaptador POS** | `PosNormalizationAdapter` | `com.loyalty.integration.adapters` | Traduce el payload del POS externo al modelo de dominio canónico. |
| **Evaluador de Disparador** | `TriggerEvaluator` | `com.loyalty.automation.trigger` | Detecta clientes elegibles (ej. inactividad, frecuencia de compra). |
| **Ejecutor de Acción** | `ActionExecutor` | `com.loyalty.automation.action` | Ejecuta la tarea cuando se dispara la regla (ej. emitir cupón, acreditar puntos). |
| **Canal de Notificación** | `EmailProviderAdapter` / SPIs | `com.loyalty.notification` | Despacha mensajes mediante proveedores externos (SendGrid, WhatsApp, etc.). |

---

## 2. Crear un Nuevo Adaptador POS

Para incorporar una terminal de punto de venta (ej. Kioskito, Clover, un ERP a medida):

1. **Implementar `PosNormalizationAdapter`**:

```java
@Component
@RequiredArgsConstructor
public class CustomPosNormalizationAdapter implements PosNormalizationAdapter {

    private final PurchaseService purchaseService;

    @Override
    public String getProvider() {
        return "CUSTOM_POS"; // Coincide con PosIntegration.provider
    }

    @Override
    public Purchase normalizeAndProcess(UUID tenantId, UUID integrationId, IngestEventRequestDTO event) {
        // 1. Extraer campos del evento crudo
        // 2. Resolver o aprovisionar el perfil del cliente
        // 3. Registrar la venta y acreditar puntos
        return purchaseService.recordPurchase(tenantId, ...);
    }
}
```

2. **Registrar el proveedor**:
Configurá el valor en `PosIntegration.provider` para el tenant. El router del Inbox asíncrono delega automáticamente según este identificador.

---

## 3. Crear Nuevas Automatizaciones

### Disparador (`TriggerEvaluator`)

```java
@Component
public class HighSpenderTriggerEvaluator implements TriggerEvaluator {

    @Override
    public boolean supports(TriggerType triggerType) {
        return triggerType == TriggerType.HIGH_SPENDER;
    }

    @Override
    public List<UUID> evaluate(Automation automation) {
        return customerStatsRepository.findHighSpenders(automation.getTenantId(), ...);
    }
}
```

### Acción (`ActionExecutor`)

```java
@Component
public class BonusPointsActionExecutor implements ActionExecutor {

    @Override
    public boolean supports(ActionType actionType) {
        return actionType == ActionType.AWARD_BONUS_POINTS;
    }

    @Override
    public String execute(Automation automation, UUID customerId) {
        loyaltyAccountService.accruePoints(automation.getTenantId(), customerId, 500, "Bono VIP");
        return "Acreditados 500 puntos bono";
    }
}
```

---

## 4. Estándares para Nuevas Funcionalidades

1. **DTOs como Java 17 `record`**: Inmutabilidad garantizada, validaciones Jakarta declarativas en componentes y fábrica estática `from(Entity entity)` para desacoplar el modelo JPA de los controladores.
2. **Entidades JPA Ricas**: `@Getter` único a nivel de clase, constructores de dominio que validen invariantes y métodos explícitos para transiciones de estado.
3. **Servicios Puros**: Sin acoplamiento a Servlets ni capas HTTP.
4. **Respuesta 401 en REST**: Cualquier endpoint en `/api/**` responde HTTP 401 ante accesos no autenticados, nunca redirecciones HTML 302.
