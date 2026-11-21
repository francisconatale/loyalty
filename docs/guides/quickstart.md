# Guía de Validación Rápida (Quickstart)

Guía paso a paso para levantar el entorno y verificar los flujos de punta a punta con llamadas `curl`.

---

## 1. Requisitos y Arranque Local

- **Java**: 17+
- **Maven**: 3.8+
- **PostgreSQL**: Levantado localmente o vía contenedor Docker:
  ```bash
  docker run --name loyalty-postgres \
    -e POSTGRES_DB=loyalty_engine \
    -e POSTGRES_USER=postgres \
    -e POSTGRES_PASSWORD=postgres \
    -p 5432:5432 -d postgres:16-alpine
  ```
- **Compilación e Inicio del Proyecto**:
  ```bash
  mvn spring-boot:run
  ```

---

## 2. Flujo Completo de Prueba Simplificado

### Paso A: Pegarle al Motor con una Compra Habitual (Evento Liviano)
Si el cliente ya existe, solo necesitás mandar el `customerId` y omitir el bloque de perfil. En la primera compra, incluí `data.customer` para que el motor cree el perfil y su referencia externa.

```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: test-kioskito-api-key" \
  -d '{
    "type": "purchase.completed",
    "eventId": "evt-kioskito-1001",
    "occurredAt": "2026-11-21T15:30:00Z",
    "data": {
      "purchaseId": "sale-5001",
      "customerId": "kioskito-cust-001",
      "total": 35000.00,
      "currency": "ARS",
      "customer": {
        "name": "Maria Gonzalez",
        "email": "maria.gonzalez@example.com",
        "phone": "+5491144443333",
        "birthDate": "1992-08-20"
      }
    }
  }'
```
*Respuesta esperada*: `HTTP 202 Accepted` en menos de 500 ms con `inboxId` y estado `ACCEPTED`.

---

### Paso B: Consultar el Saldo de Puntos Acreditados
El worker en segundo plano procesa la cola, resuelve el cliente, registra la venta y acredita los puntos.

```bash
curl -X GET http://localhost:8080/api/v1/customers/<CUSTOMER_ID>/loyalty
```
*Respuesta esperada*: Saldo actualizado con 350 puntos (calculado a razón de 1 punto cada $100 gastados).

---

### Paso C: Validar y Canjear un Cupón en Caja
```bash
# 1. Validar validez del cupón
curl -X POST http://localhost:8080/api/v1/coupons/validate \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: test-kioskito-api-key" \
  -d '{"code": "DESC15-PROMO"}'

# 2. Confirmar redención
curl -X POST http://localhost:8080/api/v1/coupons/redeem \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: test-kioskito-api-key" \
  -d '{
    "code": "DESC15-PROMO",
    "transactionReference": "ticket-5002"
  }'

# 3. Reintento de canje (debe ser rechazado)
curl -X POST http://localhost:8080/api/v1/coupons/redeem \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: test-kioskito-api-key" \
  -d '{
    "code": "DESC15-PROMO",
    "transactionReference": "ticket-5003"
  }'
```
*Respuesta esperada del paso 3*: `HTTP 409 Conflict` (protección contra doble canje).
