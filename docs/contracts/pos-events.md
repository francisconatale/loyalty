# Ingesta Directa de Eventos (Event API)

En lugar de requerir webhooks o sincronizaciones pesadas, el sistema expone un **Endpoint Unificado de Eventos** (`POST /api/v1/events`).

El identificador de cliente (`customerId`) es **lo único obligatorio**. El perfil con datos personales es completamente opcional.

---

## 1. Especificación del Endpoint

- **Método y Ruta**: `POST /api/v1/events`
- **Autenticación**: `X-API-KEY: <tu_api_key>`
- **Comportamiento**: Encola el evento inmediatamente en la base de datos (`integration_events`) y responde `HTTP 202 Accepted` en < 500 ms.

---

## 2. Compra Habitual: Evento Liviano (Thin Event)

Cuando el cliente ya existe en el sistema o no necesitas actualizar sus datos, solo mandás su ID externo. Cero sobrecarga de red:

```json
{
  "type": "purchase.completed",
  "eventId": "evt-kioskito-5501",
  "occurredAt": "2026-11-21T15:30:00Z",
  "data": {
    "purchaseId": "sale-1004",
    "customerId": "kioskito-user-1",
    "total": 25000.00,
    "currency": "ARS"
  }
}
```

El motor resuelve la cuenta de puntos del cliente con `kioskito-user-1` y registra la compra.

---

## 3. Primera Compra o Actualización de Contacto (Fat Event / Upsert)

Si es la primera vez que el cliente compra o querés actualizarle el email para automatizaciones, incluís el bloque opcional `customer`:

```json
{
  "type": "purchase.completed",
  "eventId": "evt-kioskito-5502",
  "occurredAt": "2026-11-21T15:35:00Z",
  "data": {
    "purchaseId": "sale-1005",
    "customerId": "kioskito-user-2",
    "total": 18000.00,
    "currency": "ARS",
    "customer": {
      "name": "Maria Gonzalez",
      "email": "maria.gonzalez@example.com",
      "phone": "+5491144443333"
    }
  }
}
```

El motor actualiza (o crea) el perfil del cliente en la misma transacción y acredita los puntos correspondientes.

---

## 4. Anulación o Devolución (`purchase.cancelled` / `purchase.refunded`)

```json
{
  "type": "purchase.cancelled",
  "eventId": "evt-kioskito-5503",
  "occurredAt": "2026-11-21T16:00:00Z",
  "data": {
    "purchaseId": "sale-1004",
    "reason": "Devolución en caja"
  }
}
```
El motor localiza la compra, actualiza su estado a `CANCELLED` o `REFUNDED` y genera la compensación (`REVERSAL`) en el libro mayor de puntos.
