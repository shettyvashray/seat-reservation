# Seat Reservation at Scale

A concurrent seat reservation service built with Java, Spring Boot and PostgreSQL, designed to maintain correctness under concurrent reservation load.

## Live Deployment

**Live URL:** `https://seat-reservation-vr5d.onrender.com`

**Health**
- `https://seat-reservation-vr5d.onrender.com/actuator/health/liveness`
- `https://seat-reservation-vr5d.onrender.com/actuator/health/readiness`

**Metrics**
- `https://seat-reservation-vr5d.onrender.comactuator/prometheus`

## Tech Stack

- Java 21
- Spring Boot 4.1.1
- PostgreSQL 16
- Spring Data JPA / Hibernate
- Flyway
- Spring Security
- Micrometer / Prometheus
- Docker / Docker Compose
- Python
- Render

## API

### 1. Create a show

`POST /shows`

Admin authentication required.

```json
{
  "name": "friday-night",
  "seats": ["A1", "A2", "A3"],
  "price_paise": 25000
}
```

Returns the created show with every seat initially `available`.

### 2. Reserve seats

`POST /shows/{id}/reserve`

Authenticated user required.

The request must include an `Idempotency-Key` header.

```json
{
  "seats": ["A1", "A2"]
}
```

A successful reservation returns `201 Created` with:

```json
{
  "reservation_id": "…",
  "show_id": "…",
  "user_id": "user-123",
  "seats": ["A1", "A2"],
  "amount_paise": 50000,
  "status": "confirmed"
}
```

Identity comes from the Bearer token; there is no user ID field accepted from the request body.

### 3. Cancel a reservation

`POST /shows/reservations/{id}/cancel`

Only the reservation owner can cancel it.

Cancelled seats become `available` and can be reserved again.

This implementation uses explicit cancellation rather than time-based expiry.

### 4. Show state

`GET /shows/{id}`

Returns every seat and aggregate counts:

```text
available + held + confirmed = total_seats
```

This is the reconciliation invariant used by the burst test.

## Reservation Behaviour

### Concurrency

Reservations are transactional.

Requested seat rows are locked in PostgreSQL using `SELECT ... FOR UPDATE` before checking and changing seat state.

For a hot-seat race:

```text
500 requests → A12
```

exactly one request can successfully confirm A12. The remaining requests receive `409 Conflict`.

Requested seat numbers are normalized to `distinct + sorted` before locking so concurrent multi-seat requests acquire locks in a deterministic order.

### Per-user limit

The default limit is 4 seats per user per show.

A database row for `(show_id, user_id)` is pessimistically locked before the current confirmed seat count is checked. This prevents concurrent requests from bypassing the limit.

### Idempotency

Each reservation stores:

- show ID
- user ID
- idempotency key
- request hash
- reservation

The database enforces uniqueness on:

```text
(show_id, user_id, idempotency_key)
```

Behaviour:

```text
same key + same request
    → original reservation is returned

same key + different seats
    → 409 IDEMPOTENCY_CONFLICT

key previously used by a cancelled reservation
    → 409 IDEMPOTENCY_KEY_CANCELLED
```

### Multi-seat policy

Multi-seat requests are **all-or-nothing**.

For:

```text
["A12", "A13"]
```

if either seat is unavailable, neither seat is reserved.

## Authentication

This exercise uses a deliberately simple Bearer-token model.

A normal user token represents the user ID:

```text
Authorization: Bearer user-123
```

Admin operations require the configured `ADMIN_TOKEN`.

This is intentionally simpler than a production JWT/OIDC setup so the implementation can focus on reservation correctness.

## Health

Liveness:

```text
GET /actuator/health/liveness
```

Readiness:

```text
GET /actuator/health/readiness
```

Readiness includes the PostgreSQL dependency check.

## Metrics

Prometheus metrics are exposed at:

```text
GET /actuator/prometheus
```

The service exposes:

- confirmed reservations counter
- declined reservations counter by reason
- available seats gauge

Decline reasons include:

```text
seat_taken
per_user_limit
idempotent_replay
idempotency_conflict
```

## Logs

Application logs are structured and include an `X-Request-ID` correlation ID.

The request ID is generated when not supplied and is returned in the response.

Sensitive `Authorization` and `Idempotency-Key` values are not logged.

**Live logs:** Render Dashboard

A short recording of the live logs during the concurrency burst is provided with the submission because the Render runtime logs are not exposed as a public unauthenticated endpoint.

## Run Locally

### Prerequisites

- Docker
- Docker Compose
- Python 3

Start the service:

```bash
docker compose up --build
```

The API runs at:

```text
http://localhost:8080
```

Check readiness:

```bash
curl http://localhost:8080/actuator/health/readiness
```

## One-command burst test

The repository includes a concurrent burst test covering:

- hot-seat storm
- idempotency storm
- same-key/different-body conflict
- per-user concurrency limit
- cancellation + rebooking
- final reconciliation

Run locally:

```bash
./burst.sh http://localhost:8080
```

Run against the live deployment:

```bash
ADMIN_TOKEN="<LIVE_ADMIN_TOKEN>" ./burst.sh <LIVE_URL>
```

The script prints the outcome distribution and final seat reconciliation.

## Environment Variables

For deployment:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
ADMIN_TOKEN
```

Local Docker Compose provides disposable development defaults. Production credentials are supplied through the deployment environment and are not committed to the repository.

## Project Structure

```text
src/main/java/com/ashray/seatreservation
├── controller
├── dto
├── entity
├── repository
├── service
├── exception
├── config
├── security
├── filter
└── metrics

src/main/resources
└── db/migration
    └── V1__initial_schema.sql

burst/
├── burst.py
└── requirements.txt

Dockerfile
docker-compose.yml
burst.sh
WRITEUP.md
```

## AI Usage

AI tools were used as a development assistant for architecture discussions, debugging, implementation support, and documentation.

The technical decisions and final implementation were reviewed and validated against the assignment requirements.
