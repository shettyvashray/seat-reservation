# Seat Reservation at Scale - Writeup

## 1. Atomic Decision

The critical reservation decision lives in **PostgreSQL**, inside a single database transaction.

For a reservation, the service:

1. Normalizes and sorts the requested seat numbers.
2. Acquires the `(show_id, user_id)` lock row.
3. Checks the user's existing confirmed seat count.
4. Locks the requested seat rows with PostgreSQL `FOR UPDATE`.
5. Verifies that every requested seat exists and is `AVAILABLE`.
6. Inserts the reservation.
7. Marks the seats `CONFIRMED`.
8. Commits the transaction.

The seat lock is the mechanism that prevents double selling.

Conceptually:

```sql
SELECT *
FROM seats
WHERE show_id = ?
  AND seat_number IN (...)
ORDER BY seat_number
FOR UPDATE;
```

Two concurrent transactions cannot both acquire the same seat row at the same time.

For a hot seat such as A12:

one transaction obtains the lock, sees `AVAILABLE`, and confirms the seat.

The remaining transactions wait for the lock, then observe that the seat is no longer available and return `409 Conflict`.

There is therefore no application level "check then write" race.

## 2. Why PostgreSQL Locking?

The database is the correct place for this decision because it remains authoritative across application instances.

A Java `synchronized` block or JVM lock would only coordinate requests within one process. With multiple application instances, two instances could still attempt to reserve the same seat.

PostgreSQL row locking provides the same correctness property regardless of how many application instances are running, as long as they share the same database.

The design intentionally prefers correctness and a simple failure model over adding Redis or another distributed locking system.

## 3. Multi-seat Requests and Deadlock Avoidance

Multi-seat reservations are **all-or-nothing**.

For:

```text
["A12", "A13"]
```

both seats must be available or the complete request is rejected.

Before acquiring any seat locks, the service canonicalizes the list:

```text
distinct & sorted
```

This gives deterministic lock ordering.

For example:

```text
Request A: A12, A13
Request B: A13, A12
```

both acquire locks as:

```text
A12 → A13
```

rather than each following the caller's original order.

This reduces the possibility of circular lock waits and makes the locking behaviour deterministic.

## 4. Per-user Limit

The default limit is four seats per user per show.

Simply counting reservations without locking would be unsafe:

```text
current count = 2

Request A checks → 2
Request B checks → 2

both decide 2 + 2 <= 4
```

Both could then succeed and exceed the limit.

To prevent this, the database contains a `show_user_locks` table with one row per:

```text
(show_id, user_id)
```

The service creates the row if necessary and pessimistically locks it before checking the user's current confirmed seats.

Therefore concurrent reservation requests from the same user for the same show serialize around the limit check.

Different users do not share that lock, so unrelated reservations can still proceed concurrently.

## 5. Idempotency

Each reservation request requires an `Idempotency-Key`.

The service stores the key together with:

```text
show_id
user_id
request_hash
reservation
```

The requested seat list is canonicalized before generating a SHA-256 request hash.

The database also has a unique constraint on:

```text
(show_id, user_id, idempotency_key)
```

The behaviour is:

### Same key + same request

The existing reservation is returned.

This means a retry after a network timeout does not create another reservation.

### Same key + different body

The stored request hash does not match.

The service returns:

```text
409 IDEMPOTENCY_CONFLICT
```

No new reservation is created.

### Concurrent identical requests

The `(show_id, user_id)` lock serializes requests from the same user for the show, while the database uniqueness constraint provides an additional final guarantee.

## 6. Holds and Expiry

I chose the **explicit cancellation** model.

The API provides:

```text
POST /shows/reservations/{id}/cancel
```

Only the owner can cancel the reservation.

Cancellation locks the reservation and its associated seats, then atomically changes:

```text
reservation: CONFIRMED → CANCELLED
seat:        CONFIRMED → AVAILABLE
```

The operation is transactional, so a partial release cannot be committed.

Timed holds and automatic expiry were intentionally not implemented because the assignment allows either model and explicit cancellation keeps the implementation focused on the core concurrency problem.

The seat-state model still supports `AVAILABLE`, `HELD`, and `CONFIRMED`, but this implementation does not create temporary holds.

A released seat can subsequently be booked again.

## 7. Identity and Authorization

The authenticated user's identity comes from the Bearer token.

The reservation API does not accept a user ID from the request body.

For example:

```text
Authorization: Bearer alice
```

means the request acts as `alice`.

A caller cannot submit a different user ID to reserve or cancel on another user's behalf.

Cancellation additionally verifies that the authenticated user owns the reservation.

This directly addresses the identity spoofing requirement.

## 8. Consistency vs Availability

The system deliberately prefers **consistency over availability** for the reservation decision.

The reason is simple: if the database cannot be reached, the service cannot safely determine whether a seat is available.

It therefore does not attempt to reserve from a local cache or use an eventually consistent source of inventory.

The readiness endpoint includes the PostgreSQL dependency, so a database outage causes readiness to fail and the instance should not be considered ready to serve traffic.

The trade-off is that a database outage can make reservations unavailable. This is intentional: accepting a reservation without a reliable source of truth would risk violating the primary requirement of never double selling a seat.

For a production implementation, dependency failures would also be mapped explicitly to `503 Service Unavailable` rather than exposing infrastructure failures as generic server errors.

## 9. Observability

The service exposes three main categories of operational signals.

### Metrics

Prometheus metrics include:

```text
reservations_confirmed_total
reservations_declined_total{reason="seat_taken"}
reservations_declined_total{reason="per_user_limit"}
reservations_declined_total{reason="idempotent_replay"}
reservations_declined_total{reason="idempotency_conflict"}
seats_available
```

The counters measure events over time, while `seats_available` reflects current inventory.

The current inventory is reconciled against:

```text
GET /shows/{id}
```

using:

```text
available + held + confirmed = total_seats
```

### Logs

Each request receives an `X-Request-ID`.

The ID is placed in the logging context and returned to the caller, making it possible to correlate a response with its structured application log entry.

Authentication credentials and idempotency keys are not logged.

### Health

Liveness verifies that the application process is alive.

Readiness includes the PostgreSQL dependency check.

## 10. What I Would Page On at 2 AM

The most important alerts would be:

### Any unexpected 5xx increase

A reservation decline should be a controlled `4xx` domain outcome, not an application error.

A sudden increase in `5xx` responses during an on-sale burst would be an immediate incident.

### Database health / readiness failures

The reservation decision depends on PostgreSQL, so loss of database connectivity directly affects the service's ability to make correct decisions.

### Reconciliation mismatch

Any violation of:

```text
available + held + confirmed = total_seats
```

would indicate a severe correctness problem.

### Unexpected duplicate confirmation detection

A seat appearing as confirmed for more than one reservation would be the highest severity correctness incident.

### Reservation latency / database lock contention

A major increase in reservation latency during a hot-seat event could indicate database contention, connection pool exhaustion, or another scaling bottleneck.

## 11. What I Would Do Next

With more time, I would focus on production hardening rather than adding architectural complexity.

First, I would add a proper integration/concurrency test suite that runs against PostgreSQL and repeatedly exercises the hot-seat and idempotency races.

I would then add explicit `503` handling for database/dependency failures, tune database connection pooling, add rate limiting for abusive traffic, and introduce stronger authentication such as JWT/OIDC.

For a very high scale production system, I would also investigate partitioning strategies, hot-seat mitigation, and more detailed distributed tracing.

## 12. AI Usage

AI tools were deliberately used during development, as encouraged by the exercise.

### Directed by me

I defined the primary constraints and selected the overall design:

- PostgreSQL as the source of truth
- transactional reservation
- row-level pessimistic locking
- deterministic seat lock ordering
- per-user locking
- idempotency key + request hash
- all-or-nothing multi-seat behaviour
- explicit cancellation
- simple Bearer authentication

I also determined the scope boundaries: no frontend, no Kafka/Redis, no timed holds, and no external authentication provider.

### Assisted by AI

AI was used for:

- discussing alternative concurrency approaches
- reviewing race conditions and deadlock scenarios
- generating/refining implementation code
- debugging Spring Security and Docker issues
- designing the burst-test scenarios
- reviewing error handling
- improving documentation

The resulting code and architecture were tested and reviewed against the assignment requirements rather than being accepted blindly from the AI output.
