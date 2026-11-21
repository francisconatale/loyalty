# Motor de Automatizaciones

El motor de automatizaciones convierte la información estática de compras y puntos en un generador proactivo de ventas y retención.

---

## 1. Arquitectura del Motor

El motor no utiliza lenguajes de scripting arbitrarios ni intérpretes dinámicos que comprometan la seguridad o la estabilidad. Opera mediante el patrón **Strategy fuertemente tipado**:

```text
                  PROGRAMADOR / WORKER
                            │
                            ▼
                     TRIGGER EVALUATOR
            (Evalúa reglas activas contra métricas)
                            │
                            ▼
                    CONDITION EVALUATOR
            (Valida condiciones: email presente, días)
                            │
                            ▼
                    ACTION EXECUTOR
          (Genera cupones, añade puntos, envía emails)
                            │
                            ▼
                   AUTOMATION AUDIT LOG
          (AutomationExecution + AutomationAction)
```

---

## 2. Triggers Predefinidos (MVP)

| Trigger | Tipo | Descripción |
|---|---|---|
| `CUSTOMER_INACTIVE` | Programado | Se activa cuando un cliente supera N días sin compras (`lastPurchaseAt`). |
| `FIRST_PURCHASE` | Basado en evento | Se activa inmediatamente después de registrarse la primera compra del cliente. |
| `SECOND_PURCHASE` | Basado en evento | Incentivo clave para convertir compradores ocasionales en recurrentes. |
| `PURCHASE_COMPLETED`| Basado en evento | Se activa ante cualquier compra que supere ciertas condiciones de ticket. |
| `BIRTHDAY` | Programado | Se dispara en la fecha de cumpleaños del cliente (`birthDate`). |
| `SPENDING_THRESHOLD`| Basado en evento | Se activa cuando el gasto total acumulado alcanza una meta. |

---

## 3. Ejemplo de Configuración JSONB

Automatización canónica del MVP: **Recuperación de clientes inactivos por 30 días**.

```json
{
  "trigger": "CUSTOMER_INACTIVE",
  "parameters": {
    "inactivityDays": 30
  },
  "conditions": [
    {
      "type": "HAS_EMAIL",
      "value": true
    }
  ],
  "actions": [
    {
      "type": "GENERATE_COUPON",
      "parameters": {
        "discountType": "PERCENTAGE",
        "discountValue": 15.0,
        "validityDays": 7
      }
    },
    {
      "type": "SEND_EMAIL",
      "parameters": {
        "template": "customer-reactivation"
      }
    }
  ]
}
```

---

## 4. Trazabilidad e Idempotencia de Ejecución

Cada ejecución queda registrada en dos tablas de auditoría:

1. **`automation_executions`**:
   - `idempotencyKey`: Garantiza que el cliente no reciba dos correos de reactivación en la misma ventana de tiempo (ej. clave `auto-30d-{customerId}-2026-11`).
   - `status`: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`.
2. **`automation_actions`**:
   - Detalle de cada paso ejecutado. Si la generación del cupón tuvo éxito pero el envío de email falló transitoriamente, el reintento ejecuta únicamente el envío del correo sin volver a duplicar el cupón.
