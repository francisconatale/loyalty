# Contratos de Cupones y Canje en Caja

Endpoints utilizados por las terminales POS para consultar la validez de un cupón y registrar su aplicación en una venta.

> **Seguridad Crítica**: Estos endpoints están protegidos obligatoriamente por la cabecera `X-API-KEY`. El sistema aplica limitación de tasa (rate limiting) y búsqueda acotada exclusivamente al `tenantId` autenticado para evitar ataques de fuerza bruta y enumeración de cupones (*coupon fuzzing*).

---

## 1. Validación de Cupón

- **Método y Ruta**: `POST /api/v1/coupons/validate`
- **Autenticación**: `X-API-KEY: <tu_api_key>` (Obligatorio)
- **Propósito**: Verificar si un código ingresado por el cliente es aplicable antes de totalizar la venta.

### Solicitud

```json
{
  "code": "DESC15-XYZ89"
}
```

### Respuesta Exitosa

```json
{
  "code": "DESC15-XYZ89",
  "status": "AVAILABLE",
  "isValid": true,
  "discountType": "PERCENTAGE",
  "discountValue": 15.0,
  "expiresAt": "2026-11-28T23:59:59Z"
}
```

---

## 2. Canje Definitivo (Redención)

- **Método y Ruta**: `POST /api/v1/coupons/redeem`
- **Propósito**: Marcar el cupón como utilizado una vez concretado el ticket de compra.

### Solicitud

```json
{
  "code": "DESC15-XYZ89",
  "transactionReference": "ticket-kioskito-2026-99"
}
```

### Respuesta Exitosa

- **HTTP 200 OK**:
  ```json
  {
    "code": "DESC15-XYZ89",
    "status": "REDEEMED",
    "redeemedAt": "2026-11-21T15:45:10Z",
    "transactionReference": "ticket-kioskito-2026-99"
  }
  ```

### Errores Comunes

- **HTTP 409 Conflict**: El cupón ya fue canjeado en otra venta o está vencido.
- **HTTP 404 Not Found**: El código de cupón no existe en este comercio.
