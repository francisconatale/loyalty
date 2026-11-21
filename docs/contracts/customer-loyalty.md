# API REST de Fidelización y Consultas de Cliente

Expone el saldo de puntos y el extracto de movimientos para aplicaciones móviles, portales de cliente o pantallas de consulta en el local.

---

## 1. Consulta de Saldo de Puntos

- **Método y Ruta**: `GET /api/v1/customers/{id}/loyalty`
- **Seguridad**: Token de sesión o Bearer Token en cadena `@Order(2)`.

### Respuesta (`CustomerLoyaltyResponse`)

```json
{
  "customerId": "8f3b6c20-7f21-4f11-9a70-8b1e0a294821",
  "programName": "Puntos Kioskito",
  "pointsBalance": 350,
  "lifetimePoints": 1200
}
```

---

## 2. Extracto de Movimientos del Libro Mayor

- **Método y Ruta**: `GET /api/v1/customers/{id}/transactions`
- **Propósito**: Devuelve la lista cronológica e inmutable de movimientos de la cuenta.

### Respuesta

```json
[
  {
    "id": "e4f8d2a1-3c4b-4a5f-b987-123456789abc",
    "type": "EARN",
    "amount": 250,
    "description": "Puntos acreditados por compra sale-10023",
    "createdAt": "2026-11-21T14:30:05Z"
  },
  {
    "id": "7b1c3e4d-5f6a-4b7c-8d9e-012345678def",
    "type": "REDEEM",
    "amount": -200,
    "description": "Canje por cupón 15% Descuento",
    "createdAt": "2026-11-21T15:00:10Z"
  }
]
```
