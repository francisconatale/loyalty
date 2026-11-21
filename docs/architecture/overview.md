# Visión y Arquitectura General

Loyalty Engine opera como un **Monolito Modular** construido sobre **Java 17** y **Spring Boot 3.2.5**, desacoplado de los sistemas de Punto de Venta (POS) externos.

---

## 1. Responsabilidad y Límites del Sistema

- **Qué hace**: Recibe eventos de negocio desde el backend de los POS (ej. Kioskito), construye perfiles de comportamiento de clientes al vuelo, gestiona saldos y transacciones de puntos en un libro mayor inmutable, valida cupones de descuento en caja y ejecuta reglas de automatización configurables.
- **Qué NO hace**: No reemplaza al POS, no administra inventario ni catálogo de productos comercializables, no maneja cajas registradoras ni facturación fiscal.

```text
                  BACKEND POS (Kioskito)
                            │
              POST /api/v1/events (X-API-KEY)
                            │
                            ▼
                     INTEGRATION MODULE
                            │
                    Event Ingestion &
                 Direct Customer Upsert
                            │
                            ▼
                    EVENT INBOX (Queue)
                            │
                     Event Processor
                            │
        +-------------------+-------------------+
        |                   |                   |
        v                   v                   v
     CUSTOMER            PURCHASE            LOYALTY
        |                   |                   |
        +-------------------+-------------------+
                            │
                            ▼
                     AUTOMATION ENGINE
                            │
                   +--------+--------+
                   |                 |
                   v                 v
               EMAIL SERVICE    WHATSAPP (Post-MVP)
                            │
                            ▼
                         TRACKING
```

---

## 2. Organización Modular

El backend se organiza en paquetes independientes con límites de dominio claros:

| Módulo | Responsabilidad | Dependencias Principales |
|---|---|---|
| `tenant` | Gestión del ciclo de vida y estado de los comercios minoristas. | `shared` |
| `integration` | Ingesta directa de eventos (`POST /api/v1/events`), encolado en `IntegrationEvent` y adaptadores. | `tenant`, `shared` |
| `customer` | Identidad canónica de clientes y mapeo de IDs externos (`ExternalCustomerReference`). Resuelto al vuelo por los eventos. | `tenant`, `shared` |
| `purchase` | Ingesta normalizada de ventas, devoluciones y cancelaciones. | `tenant`, `customer`, `integration` |
| `analytics` | Proyecciones de métricas (`CustomerStats`: gasto acumulado, ticket promedio, frecuencia). | `customer`, `purchase` |
| `loyalty` | Programas de fidelización (`LoyaltyProgram`), cuentas (`LoyaltyAccount`) y libro mayor (`PointTransaction`). | `tenant`, `customer` |
| `reward` | Catálogo de recompensas y beneficios canjeables por puntos. | `tenant` |
| `coupon` | Emisión, validación y redención de cupones de descuento de un solo uso. | `tenant`, `customer`, `reward` |
| `automation` | Reglas reactivas (triggers, condiciones y acciones ejecutables). | `customer`, `coupon`, `loyalty`, `notification` |
| `notification` | Adaptadores de envío transaccional (Email implementado en MVP; WhatsApp preparado para fase 2). | `shared` |
| `tracking` | Auditoría de ejecuciones de automatizaciones y efectividad de campañas. | `automation` |

---

## 3. Modelo Simplificado de Eventos (Kioskito)

En lugar de requerir que Kioskito configure webhooks con URLs de callback y endpoints separados de sincronización:
1. Cuando ocurre una venta en Kioskito, su backend simplemente hace un `POST /api/v1/events` con los datos de la compra y del cliente juntos.
2. Loyalty Engine encola el evento en la bandeja `integration_events` y responde `202 Accepted` de inmediato.
3. El procesador asíncrono toma el evento:
   - Si el cliente no existía, lo crea y lo asocia a su ID externo de Kioskito. Si ya existía, actualiza sus datos de contacto.
   - Registra la venta `Purchase`.
   - Recalcula `CustomerStats`.
   - Acredita los puntos en `LoyaltyAccount` mediante un movimiento inmutable `PointTransaction` (`EARN`).
   - Evalúa si se disparan reglas de automatización (ej. primer compra o cliente inactivo).
