# Adaptive Rate-Limiting Trading Engine

A real-time order-matching engine that simulates the core infrastructure of a stock exchange — built from scratch to demonstrate distributed systems design, concurrency-safe algorithms, and low-level design (LLD) patterns used in high-frequency trading platforms.

---

## What This Project Is

Most portfolio projects that touch "trading" or "high-throughput systems" stop at building a **gatekeeper**: validate an incoming request, check a balance, forward it along, and log it somewhere. That's a useful but relatively shallow problem.

This project goes further. It implements an actual **order-matching engine** — the component that sits at the heart of every real exchange (NYSE, NASDAQ, NSE) — which takes incoming buy/sell orders and matches them against each other in real time, enforcing **price-time priority**, handling **partial fills**, and maintaining a live, correctly-sorted order book entirely in memory for sub-millisecond access.

On top of that core engine, the system adds the operational concerns a real trading platform needs: adaptive rate limiting that treats different client types differently, a circuit breaker for downstream resilience, automatic order expiry via a scheduled background job, and asynchronous event streaming via Kafka.

---

## The Problem It Solves

Financial exchanges and high-frequency trading platforms face a specific engineering challenge: **thousands of orders can arrive per second, and each one must be validated, matched, and recorded correctly — with zero tolerance for race conditions, lost orders, or incorrect fills.**

Traditional approaches that route every order straight through a relational database hit locking bottlenecks under this kind of concurrent load. This project's architecture avoids that by:

- Keeping the **live order book entirely in memory**, using thread-safe data structures, for fast matching
- Using the **database as a durable audit trail**, written to alongside the in-memory operations, and **also** streamed asynchronously via Kafka so downstream consumers aren't on the matching engine's critical path
- Enforcing **fairness and safety** (correct price-time priority, no double-fills, no lost orders) through careful data structure choices rather than database locks
- Protecting the system itself under stress, via adaptive rate limiting and a circuit breaker around unreliable downstream dependencies

---

## What's Unique About This Project

Compared to typical "rate limiter + gateway" portfolio projects, this one is differentiated by:

1. **A real matching algorithm, not just validation.** The order book uses a `TreeMap`-based structure (via `ConcurrentSkipListMap`) to maintain O(log n) insertion and O(1) best-price lookup, with FIFO queues at each price level to preserve time priority — the same core data structure real exchanges use. This was tested to the point of catching and fixing a genuine bug (stale empty price levels corrupting best-price lookups after a full match) with a permanent regression test.

2. **Adaptive, tiered rate limiting.** Instead of one blanket rule for every client, this system implements the **Strategy design pattern** to apply different rate-limiting algorithms based on client type:
   - **Token Bucket** (Redis-backed, fixed-window) for retail clients — steady rate, small bursts
   - **Sliding Window Log** (Redis-backed, sorted set) for market-maker clients — tolerates larger bursts but strictly caps volume over any rolling window
   - Limits **automatically tighten** when a live volatility signal (computed from a rolling window of recent trade prices) detects a sharp price swing — no manual intervention required.

3. **A genuine circuit breaker**, not just a try/catch. Implements the full CLOSED → OPEN → HALF_OPEN state machine around a simulated downstream dependency, verified end-to-end: trips after consecutive failures, rejects fast while open, and self-tests recovery via a single trial request before fully closing again.

4. **Decoupled event streaming via Kafka.** Every matched trade is published to a Kafka/Redpanda topic as a flat, JSON-serializable event, consumed independently by a dedicated listener — demonstrating the classic pattern of separating the low-latency matching path from downstream processing.

5. **Genuine concurrency safety**, verified by tests — not just claimed. The order book and rate limiters are built to be correct under concurrent access from the ground up (`ConcurrentSkipListMap`, atomic Redis operations), rather than retrofitted afterward.

6. **A believable, realistic domain model** — Time-in-Force semantics (GTC/IOC/FOK), a min-heap-based expiry scheduler, and a full order lifecycle state machine (`CREATED → OPEN → PARTIALLY_FILLED → FILLED/CANCELLED/EXPIRED`) — mirroring how real trading systems are actually built.

7. **100% free and local.** No cloud hosting, no paid services, no credit card required, ever. Everything runs via Docker Compose on a local machine — see [Tech Stack](#tech-stack--and-why-its-free) below.

---

## Architecture Overview

Client Request
             │
             ▼
┌──────────────────────────┐
│    Rate Limit Filter     │ ──► [429 Too Many Requests]
│  (Redis, strategy pattern,│
│   tightens on volatility)│
└────────────┬─────────────┘
             │ (Valid Request)
             ▼
┌──────────────────────────┐
│     Order Controller     │
│        (REST API)        │
└────────────┬─────────────┘
             │
      ┌──────┴────────────────────────┐
      ▼                               ▼
┌──────────────────────────┐    ┌──────────────────────────┐
│     Matching Engine      │    │     Order Repository     │
│  (Price-time priority,   │◄───┤ (Postgres via Flyway DB) │
│   handles partial fills) │    └──────────────────────────┘
└────────────┬─────────────┘
             │
             ├─► Volatility Tracker (Rolling window)
             ├─► Kafka Producer ──► [trade-events] ──► Audit Log
             │
             ▼
┌──────────────────────────┐
│        Order Book        │
│  (In-memory per symbol,  │
│   SkipListMap + Deque)   │
└────────────┬─────────────┘
             │
      ┌──────┴────────────────────────┐
      ▼                               ▼
┌──────────────────────────┐    ┌──────────────────────────┐
│     Expiry Scheduler     │    │     Circuit Breaker      │
│  (Min-Heap @Scheduled,   │    │ (CLOSED/OPEN/HALF_OPEN,  │
│   polls & evicts 2s)     │    │  protects downstream)    │
└──────────────────────────┘    └──────────────────────────┘
**Data flow for a single order:**
1. Request hits the **Rate Limit Filter** — rejected with `429` if the client's tier-specific limit is exceeded. The limit itself is tighter automatically if recent trades show high price volatility.
2. Passes to the **Order Controller**, which persists the order to Postgres and hands it to the **Matching Engine**.
3. The Matching Engine checks the in-memory **Order Book** for a matching counterparty at a crossing price. Matches produce **Trade** records (saved to Postgres as the permanent audit trail), update both orders' fill status, feed the **Volatility Tracker**, and publish a **Kafka event** consumed asynchronously by a dedicated listener.
4. Any unmatched remainder either rests in the book (GTC) or is cancelled (IOC/FOK). If the order has a TTL, it's scheduled in the **Expiry Queue** (a min-heap sorted by expiry time).
5. A background **Expiry Scheduler** runs every 2 seconds, polling the min-heap and auto-cancelling any orders whose time has run out, cleanly removing them from the order book.
6. A separate **Circuit Breaker** guards calls to a simulated downstream exchange dependency, tripping open after repeated failures and self-testing recovery — independent of the main order flow, demonstrating a standard resilience pattern.

---

## Tech Stack — and why it's free

| Layer | Technology | Why |
|---|---|---|
| Backend | Java 21 + Spring Boot 4.1 | Mature ecosystem for building the REST API, scheduling, and dependency injection |
| Persistence | PostgreSQL 16 | Durable audit trail for every order and trade; schema managed via Flyway migrations |
| Caching / Rate Limiting | Redis 7.2 | In-memory atomic operations (`INCR`, sorted sets) back both rate-limiting strategies |
| Messaging | Redpanda (Kafka-API compatible) | Lightweight local alternative to Apache Kafka for streaming trade events |
| Containerization | Docker Compose | Runs the entire stack locally with one command — no cloud account needed |
| Frontend *(planned)* | React + TypeScript + Framer Motion | Live order book and trade tape, driven by WebSocket updates |

**Everything runs 100% locally via Docker Compose.** There is no cloud deployment, no managed database, no paid API tier, and no domain name — by design. This keeps the project free to build, run, and maintain indefinitely, with zero risk of an unexpected bill. The project is demonstrated via a recorded demo and this GitHub repository rather than a live-hosted link.

---

## What's Built So Far

- ✅ **Step 1 — Infrastructure:** Docker Compose setup for Postgres, Redis, and Redpanda, all running locally.
- ✅ **Step 2 — Core Spring Boot app:** Connected to Postgres via Flyway-managed migrations (`symbol`, `orders`, `trade` tables).
- ✅ **Step 3 — Order domain model & Order Book:** JPA entities, enums (`Side`, `OrderStatus`, `TimeInForce`), and an in-memory `OrderBook` with verified price-time priority.
- ✅ **Step 4 — Matching Engine:** Full matching algorithm supporting partial fills, trade recording, and TIF-aware handling of unmatched remainders.
- ✅ **Step 5 — Order Expiry Engine:** TTL support on orders, a min-heap-based `ExpiryQueue`, and a background `@Scheduled` job that auto-cancels overdue orders and cleans them out of the live book.
- ✅ **Step 6 — Adaptive Rate Limiting:** Strategy-pattern rate limiting with Token Bucket (retail) and Sliding Window Log (market-maker) implementations, Redis-backed, enforced via a servlet filter at the API gateway layer.
- ✅ **Step 7 — Volatility-Aware Limits + Circuit Breaker:** A rolling-window volatility tracker automatically tightens rate limits under sharp price swings; a full CLOSED/OPEN/HALF_OPEN circuit breaker protects against a simulated failing downstream dependency. Includes a real bug fix: fully-matched resting orders were leaving stale empty price levels in the book, corrupting best-price lookups — caught via testing and fixed with a permanent regression test.
- ✅ **Step 8 — Kafka Event Streaming:** Every matched trade publishes a JSON event to a `trade-events` Kafka/Redpanda topic; a dedicated consumer processes events asynchronously, decoupling the matching engine's hot path from downstream audit processing.

**Test coverage:** 6 automated unit tests covering order book correctness (price-time priority, FIFO ordering, empty-book edge cases, and the price-level cleanup regression), all passing.

---

## Planned Work

- **Step 9 — Real-time WebSocket dashboard:** A React/TypeScript frontend showing a live order book depth chart, a scrolling trade tape, and controls to simulate order bursts — all driven by a WebSocket connection to the backend.
- **Step 10 — Metrics & polish:** Matching latency (p99), throughput (orders/sec), and rate-limit rejection rate surfaced on the dashboard; final README polish and a recorded demo video.

---

## Running This Project Locally

```bash
# 1. Start infrastructure
cd infra
docker-compose up -d

# 2. Start the backend
cd ../trading-engine
./mvnw spring-boot:run

# 3. Verify
curl http://localhost:8081/health
```

Full setup, environment details, and troubleshooting notes are documented in the commit history of this repository.

---

## Author

Built by Divyansh Srivastav as a portfolio project demonstrating distributed systems design, concurrent data structures, and low-level system design patterns relevant to high-frequency trading infrastructure.