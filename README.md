# OrderTracker

[![CI](https://github.com/Seyidli06/order-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/Seyidli06/order-tracker/actions/workflows/ci.yml)

OrderTracker is a Spring Boot backend for managing e-commerce orders and processing payment and shipment webhooks. It combines JWT authentication, role-based authorization, order ownership, strict order-status transitions, webhook audit logging, HMAC signature verification, rate limiting, asynchronous email notifications, PostgreSQL persistence, Flyway migrations, Docker Compose, Testcontainers, and CI verification.

## Features

- JWT-based registration and login
- USER and ADMIN role separation
- Authenticated order ownership
- Create, list, view, cancel, and inspect order history
- Paginated order and history APIs
- Strict order-status transition validation
- Optimistic locking for concurrent order updates
- Payment and shipment webhook endpoints
- HMAC-SHA256 webhook signature verification
- Timestamp tolerance to reduce replay attacks
- Separate secrets for payment and shipment webhooks
- Webhook audit log with `PENDING`, `PROCESSED`, and `FAILED` states
- Duplicate webhook-event protection
- Async order-status email notifications with Spring Mail
- Rate limiting for auth, webhook, and order APIs
- ADMIN-only audit APIs
- Consistent JSON error responses
- Swagger / OpenAPI documentation
- PostgreSQL + Flyway migrations
- Docker Compose development stack with Mailpit
- Unit, MVC, integration, security, and Testcontainers tests
- GitHub Actions CI with `mvn clean verify`

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Web | Spring MVC |
| Security | Spring Security, JWT (JJWT) |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| Migrations | Flyway |
| Webhook security | HMAC-SHA256 |
| Rate limiting | Bucket4j + Caffeine |
| Email | Spring Mail |
| API docs | Springdoc OpenAPI / Swagger UI |
| Testing | JUnit 5, Mockito, MockMvc, Awaitility, Testcontainers |
| Containers | Docker, Docker Compose |
| CI | GitHub Actions |

## Architecture

```text
                            +----------------------+
                            |       Client         |
                            +----------+-----------+
                                       |
                                       | JWT
                                       v
+------------------+        +----------+-----------+
| Payment Provider |------->|                      |
+------------------+ HMAC   |    Spring Security   |
                            |                      |
+------------------+ HMAC   | JWT -> Rate Limit ->|
| Shipping Provider|------->| Webhook Signature   |
+------------------+        +----------+-----------+
                                       |
                                       v
                            +----------+-----------+
                            |      Controllers     |
                            +----------+-----------+
                                       |
                      +----------------+----------------+
                      |                                 |
                      v                                 v
             +--------+---------+              +--------+---------+
             |   OrderService   |              |  WebhookService  |
             +--------+---------+              +--------+---------+
                      |                                 |
                      v                                 v
             +--------+---------+              +--------+---------+
             | Orders + History |              | Audit + Status   |
             |   PostgreSQL     |              | Processing       |
             +------------------+              +--------+---------+
                                                        |
                                                        v
                                               +--------+---------+
                                               | NotificationSvc  |
                                               |     @Async       |
                                               +--------+---------+
                                                        |
                                                        v
                                                   SMTP / Mailpit
```

The project uses feature-oriented packages while keeping controllers, services, repositories, DTOs, security filters, and integration logic separated by responsibility.

## Security Model

### JWT Authentication

`POST /api/auth/register` and `POST /api/auth/login` are public. Registration always creates a `USER` account and returns a JWT access token.

Protected endpoints use:

```http
Authorization: Bearer <token>
```

Order resources are ownership-aware: regular users can access only their own orders, while ADMIN users can access privileged audit endpoints.

There is no public endpoint that grants the `ADMIN` role. Admin accounts must be provisioned through a trusted administrative mechanism.

### Webhook HMAC Verification

Webhook endpoints do not require JWT because they are intended for external providers. Instead, every payment and shipment webhook must contain:

```http
X-Webhook-Timestamp: <unix-epoch-seconds>
X-Webhook-Signature: sha256=<hex-signature>
```

The signed message is:

```text
timestamp + "." + rawRequestBody
```

and the signature is:

```text
HMAC-SHA256(providerSecret, signedMessage)
```

Payment and shipment webhooks use separate secrets. The default timestamp tolerance is 300 seconds.

Invalid, missing, expired, cross-provider, or body-tampered signatures are rejected with HTTP `401` before the controller processes the payload.

### Rate Limits

| Endpoint group | Limit | Key |
| --- | ---: | --- |
| `POST /api/auth/login` | 5 requests/min | IP |
| `POST /api/auth/register` | 3 requests/min | IP |
| `POST /api/webhooks/**` | 60 requests/min | IP |
| `/api/orders/**` | 120 requests/min | authenticated user, IP fallback |

Rate-limited responses use HTTP `429` and may include:

```http
X-RateLimit-Limit
X-RateLimit-Remaining
Retry-After
```

## Order Lifecycle

Order status changes are validated centrally. Invalid transitions are rejected.

```text
CREATED
  -> PAYMENT_PENDING
  -> PAID
  -> PAYMENT_FAILED
  -> CANCELLED

PAYMENT_PENDING
  -> PAID
  -> PAYMENT_FAILED
  -> CANCELLED

PAYMENT_FAILED
  -> PAYMENT_PENDING
  -> PAID
  -> CANCELLED

PAID
  -> PROCESSING
  -> SHIPPED
  -> CANCELLED
  -> REFUNDED

PROCESSING
  -> SHIPPED
  -> CANCELLED
  -> REFUNDED

SHIPPED
  -> OUT_FOR_DELIVERY
  -> DELIVERED
  -> RETURNED

OUT_FOR_DELIVERY
  -> DELIVERED
  -> RETURNED

DELIVERED
  -> RETURNED
  -> REFUNDED

RETURNED
  -> REFUNDED

CANCELLED
  -> REFUNDED

REFUNDED
  -> terminal
```

Every successful status change is stored in `order_status_history` with the previous status, new status, source, reference ID, and timestamp.

## API Overview

### Authentication

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Public | Register a USER and return JWT |
| POST | `/api/auth/login` | Public | Authenticate and return JWT |

### Orders

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| POST | `/api/orders` | Authenticated | Create an order |
| GET | `/api/orders?page=0&size=20` | Authenticated | List current user's orders |
| GET | `/api/orders/{orderId}` | Owner / authorized user | Get order details |
| PATCH | `/api/orders/{orderId}/cancel` | Owner / authorized user | Cancel an order when allowed |
| GET | `/api/orders/{orderId}/history?page=0&size=20` | Owner / authorized user | Get status history |

### Webhooks

| Method | Endpoint | Security | Description |
| --- | --- | --- | --- |
| POST | `/api/webhooks/payment` | HMAC signature | Process payment status event |
| POST | `/api/webhooks/shipment` | HMAC signature | Process shipment status event |

### Audit

All audit endpoints require `ADMIN`.

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/api/audit/logs/{eventId}` | Find audit event by provider event ID |
| GET | `/api/audit/logs?status=PROCESSED` | Filter events by processing status |
| GET | `/api/audit/logs/date-range?startDate=...&endDate=...` | Filter by received time |
| GET | `/api/audit/logs/failed/retry?maxRetries=3` | Find failed events eligible for retry |
| GET | `/api/audit/logs/count?status=FAILED` | Count events by status |

## Quick Start with Docker Compose

### 1. Clone the repository

```bash
git clone https://github.com/Seyidli06/order-tracker.git
cd order-tracker
```

### 2. Create the environment file

Linux/macOS:

```bash
cp .env.example .env
```

PowerShell:

```powershell
Copy-Item .env.example .env
```

### 3. Configure secrets

Generate a Base64 JWT key, for example:

```bash
openssl rand -base64 32
```

Generate independent webhook secrets:

```bash
openssl rand -hex 32
openssl rand -hex 32
```

Put the generated values in `.env`:

```dotenv
JWT_SECRET=<base64-jwt-secret>
WEBHOOK_PAYMENT_SECRET=<payment-secret>
WEBHOOK_SHIPMENT_SECRET=<shipment-secret>
```

Never commit `.env` or production secrets.

### 4. Start the stack

```bash
docker compose up --build
```

Default services:

| Service | URL / Port |
| --- | --- |
| OrderTracker API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| Health check | `http://localhost:8080/actuator/health` |
| PostgreSQL host port | `localhost:55432` |
| Mailpit SMTP | `localhost:1025` |
| Mailpit UI | `http://localhost:8025` |

Stop the stack with:

```bash
docker compose down
```

To also remove the PostgreSQL volume:

```bash
docker compose down -v
```

## Environment Variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `APP_HOST_PORT` | `8080` | API host port |
| `POSTGRES_DB` | `order_tracker` | PostgreSQL database |
| `POSTGRES_USER` | `order_tracker` | PostgreSQL user |
| `POSTGRES_PASSWORD` | `order_tracker_dev` | Local DB password |
| `POSTGRES_HOST_PORT` | `55432` | PostgreSQL host port |
| `JWT_SECRET` | required | Base64 JWT signing key |
| `JWT_EXPIRATION` | `3600000` | JWT lifetime in milliseconds |
| `WEBHOOK_PAYMENT_SECRET` | required in Compose | Payment webhook HMAC secret |
| `WEBHOOK_SHIPMENT_SECRET` | required in Compose | Shipment webhook HMAC secret |
| `WEBHOOK_TIMESTAMP_TOLERANCE_SECONDS` | `300` | Allowed webhook clock skew/replay window |
| `MAILPIT_SMTP_HOST_PORT` | `1025` | Mailpit SMTP host port |
| `MAILPIT_UI_HOST_PORT` | `8025` | Mailpit UI host port |

## Example Requests

### Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "StrongPass123"
  }'
```

Example response:

```json
{
  "token": "<jwt>"
}
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "StrongPass123"
  }'
```

### Create an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "totalAmount": 99.99,
    "currency": "USD"
  }'
```

### Paginated Orders

```bash
curl "http://localhost:8080/api/orders?page=0&size=20" \
  -H "Authorization: Bearer <jwt>"
```

Pagination responses use this structure:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

## Payment Webhook Example

Example body:

```json
{
  "event_id": "pay_evt_12345",
  "event_type": "payment.completed",
  "source": "stripe",
  "timestamp": "2026-08-25T08:00:00Z",
  "payment_data": {
    "payment_id": "pi_12345",
    "order_id": "ord_example",
    "amount": 99.99,
    "currency": "USD",
    "status": "PAYMENT_SUCCEEDED",
    "transaction_id": "txn_12345",
    "payment_method": "visa"
  },
  "metadata": {
    "customer_email": "customer@example.com"
  }
}
```

To sign a webhook in Bash:

```bash
BODY='{"event_id":"pay_evt_12345","event_type":"payment.completed","source":"stripe","timestamp":"2026-08-25T08:00:00Z","payment_data":{"payment_id":"pi_12345","order_id":"ord_example","amount":99.99,"currency":"USD","status":"PAYMENT_SUCCEEDED"}}'
TIMESTAMP=$(date +%s)
SIGNATURE=$(printf '%s.%s' "$TIMESTAMP" "$BODY" \
  | openssl dgst -sha256 -hmac "$WEBHOOK_PAYMENT_SECRET" -hex \
  | awk '{print $2}')

curl -X POST http://localhost:8080/api/webhooks/payment \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Timestamp: $TIMESTAMP" \
  -H "X-Webhook-Signature: sha256=$SIGNATURE" \
  -d "$BODY"
```

The signature must be calculated from the exact raw body sent in the request.

## Error Response Format

API errors use a consistent structure:

```json
{
  "timestamp": "2026-08-30T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/orders",
  "validationErrors": {
    "currency": "Currency must be a 3-letter code"
  }
}
```

Security errors such as `401`, `403`, and `429` follow the same API error contract.

## Database Design

Main tables:

```text
users
  |
  +----< orders
            |
            +----< order_status_history

webhook_audit_log
```

Important database guarantees include:

- unique user email
- unique external order ID
- unique webhook event ID
- foreign keys between orders, users, and status history
- `NUMERIC(19,2)` order amounts
- JSONB storage for webhook payloads and headers
- indexes for order ownership/history and webhook audit queries
- optimistic locking through the order `version` column

Flyway manages the schema under:

```text
src/main/resources/db/migration
```

## Testing

The project includes:

- service unit tests
- DTO validation tests
- controller / MockMvc tests
- JWT and security tests
- rate-limit filter tests
- webhook HMAC verifier/filter tests
- HTTP-level webhook signature tests
- payment and shipment integration tests
- order-to-webhook end-to-end flow tests
- email notification integration tests
- repository tests
- PostgreSQL Testcontainers tests

Run the full verification suite:

Linux/macOS:

```bash
./mvnw clean verify
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

Check whitespace errors:

```bash
git diff --check
```

## CI

GitHub Actions runs on pushes and pull requests targeting `main`.

The pipeline:

1. checks out the repository
2. configures Temurin Java 21
3. caches Maven dependencies
4. runs `./mvnw clean verify`
5. runs `git diff --check`

## Project Structure

```text
src/main/java/com/ordertracker
├── audit
│   ├── controller
│   ├── dto
│   ├── AuditService.java
│   ├── WebhookAuditLog.java
│   └── WebhookAuditLogRepository.java
├── auth
│   ├── controller
│   ├── dto
│   └── service
├── common
│   ├── dto
│   └── enums
├── config
├── exception
├── notification
│   ├── dto
│   └── service
├── order
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── integration
│   ├── mapper
│   ├── repository
│   └── service
├── security
│   ├── config
│   ├── handler
│   ├── jwt
│   ├── ratelimit
│   ├── service
│   └── webhook
├── user
│   ├── entity
│   └── repository
└── webhook
    ├── controller
    ├── dto
    └── service
```

## Production Hardening Ideas

The current implementation satisfies the project scope. For a higher-scale production deployment, useful next steps would include:

- durable webhook inbox / queue before asynchronous processing
- automatic retry worker for failed webhook events
- paginated audit-log queries
- bounded custom async executor and rejection policy
- distributed rate limiting with Redis
- trusted-proxy-aware client IP resolution
- webhook key rotation / key IDs
- metrics and distributed tracing

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
