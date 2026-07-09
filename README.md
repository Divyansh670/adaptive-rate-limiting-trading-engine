# Adaptive Rate-Limiting Trading Engine

A real-time order-matching engine that simulates the core infrastructure of a stock exchange — built from scratch to demonstrate distributed systems design, concurrency-safe algorithms, and low-level design (LLD) patterns used in high-frequency trading platforms.

---

## What This Project Is

Most portfolio projects that touch "trading" or "high-throughput systems" stop at building a **gatekeeper**: validate an incoming request, check a balance, forward it along, and log it somewhere. That's a useful but relatively shallow problem.

This project goes further. It implements an actual **order-matching engine** — the component that sits at the heart of every real exchange (NYSE, NASDAQ, NSE) — which takes incoming buy/sell orders and matches them against each other in real time, enforcing **price-time priority**, handling **partial fills**, and maintaining a live, correctly-sorted order book entirely in memory for sub-millisecond access.

On top of that core engine, the system adds the operational concerns a real trading platform needs: adaptive rate limiting that treats different client types differently, automatic order expiry via a scheduled background job, and (in later steps) a circuit breaker for downstream resilience and a live WebSocket dashboard.

---

## The Problem It Solves

Financial exchanges and high-frequency trading platforms face a specific engineering challenge: **thousands of orders can arrive per second, and each one must be validated, matched, and recorded correctly — with zero tolerance for race conditions, lost orders, or incorrect fills.**

Traditional approaches that route every order straight through a relational database hit locking bottlenecks under this kind of concurrent load. This project's architecture avoids that by:

- Keeping the **live order book entirely in memory**, using thread-safe data structures, for fast matching
- Using the **database as a durable audit trail**, written to asynchronously alongside the in-memory operations, not as a bottleneck in the hot path
- Enforcing **fairness and safety** (correct price-time priority, no double-fills, no lost orders) through careful data structure choices rather than database locks

---

## What's Unique About This Project

Compared to typical "rate limiter + gateway" portfolio projects, this one is differentiated by:

1. **A real matching algorithm, not just validation.** The order book uses a `TreeMap`-based structure (via `ConcurrentSkipListMap`) to maintain O(log n) insertion and O(1) best-price lookup, with FIFO queues at each price level to preserve time priority — the same core data structure real exchanges use.

2. **Adaptive, tiered rate limiting.** Instead of one blanket rule for every client, this system implements the **Strategy design pattern** to apply different rate-limiting algorithms based on client type:
   - **Token Bucket** (Redis-backed, fixed-window) for retail clients — steady rate, small bursts
   - **Sliding Window Log** (Redis-backed, sorted set) for market-maker clients — tolerates larger bursts but strictly caps volume over any rolling window

3. **Genuine concurrency safety**, verified by tests — not just claimed. The order book and rate limiters are built to be correct under concurrent access from the ground up (`ConcurrentSkipListMap`, atomic Redis operations), rather than retrofitted afterward.

4. **A believable, realistic domain model** — Time-in-Force semantics (GTC/IOC/FOK), a min-heap-based expiry scheduler, and a full order lifecycle state machine (`CREATED → OPEN → PARTIALLY_FILLED → FILLED/CANCELLED/EXPIRED`) — mirroring how real trading systems are actually built.

5. **100% free and local.** No cloud hosting, no paid services, no credit card required, ever. Everything runs via Docker Compose on a local machine — see [Tech Stack](#tech-stack--why-its-free) below.

---

## Architecture Overview
┌─────────────────────┐
Client Request → │   Rate Limit Filter  │  (Token Bucket / Sliding Window,
│  (Strategy Pattern)  │   Redis-backed, per client tier)
└──────────┬───────────┘
│ (429 if over limit)
▼
┌─────────────────────┐
│   Order Controller   │  (REST API)
└──────────┬───────────┘
│
┌─────────────┴─────────────┐
▼                             ▼
┌─────────────────┐         ┌───────────────────┐
│  Matching Engine  │         │   Order Repository │
│ (price-time match, │◄───────┤   (Postgres, via   │
│  partial fills)    │  saves  │   Flyway-managed   │
└────────┬───────────┘         │      schema)       │
│                     └───────────────────┘
▼
┌─────────────────┐
│    Order Book     │  (in-memory, per symbol,
│ (ConcurrentSkip-   │   ConcurrentSkipListMap +
│  ListMap, FIFO     │   ArrayDeque, price-time
│  queues per price) │   priority)
└────────┬───────────┘
│
▼
┌─────────────────┐
│  Expiry Scheduler  │  (background job, polls a
│  (Min-Heap +       │   min-heap of TTL orders,
│   @Scheduled)       │   auto-cancels overdue ones)
└────────────────────┘
**Data flow for a single order:**
1. Request hits the **Rate Limit Filter** — rejected with `429` if the client's tier-specific limit is exceeded.
2. Passes to the **Order Controller**, which persists the order to Postgres and hands it to the **Matching Engine**.
3. The Matching Engine checks the in-memory **Order Book** for a matching counterparty at a crossing price. Matches produce **Trade** records (saved to Postgres as the permanent audit trail) and update both orders' fill status.
4. Any unmatched remainder either rests in the book (GTC) or is cancelled (IOC/FOK). If the order has a TTL, it's scheduled in the **Expiry Queue** (a min-heap sorted by expiry time).
5. A background **Expiry Scheduler** runs every 2 seconds, polling the min-heap and auto-cancelling any orders whose time has run out, cleanly removing them from the order book.

---

## Tech Stack — and why it's free

| Layer | Technology | Why |
|---|---|---|
| Backend | Java 21 + Spring Boot 4.1 | Mature ecosystem for building the REST API, scheduling, and dependency injection |
| Persistence | PostgreSQL 16 | Durable audit trail for every order and trade; schema managed via Flyway migrations |
| Caching / Rate Limiting | Redis 7.2 | In-memory atomic operations (`INCR`, sorted sets) back both rate-limiting strategies |
| Messaging *(planned)* | Redpanda (Kafka-API compatible) | Lightweight local alternative to Apache Kafka for streaming trade events |
| Containerization | Docker Compose | Runs the entire stack locally with one command — no cloud account needed |
| Frontend *(planned)* | React + TypeScript + Framer Motion | Live order book and trade tape, driven by WebSocket updates |

**Everything runs 100% locally via Docker Compose.** There is no cloud deployment, no managed database, no paid API tier, and no domain name — by design. This keeps the project free to build, run, and maintain indefinitely, with zero risk of an unexpected bill. The project is demonstrated via a recorded demo and this GitHub repository rather than a live-hosted link.

---

## What's Built So Far

- ✅ **Step 1 — Infrastructure:** Docker Compose setup for Postgres, Redis, and Redpanda, all running locally.
- ✅ **Step 2 — Core Spring Boot app:** Connected to Postgres via Flyway-managed migrations (`symbol`, `orders`, `trade` tables).
- ✅ **Step 3 — Order domain model & Order Book:** JPA entities, enums (`Side`, `OrderStatus`, `TimeInForce`), and an in-memory `OrderBook` with verified price-time priority (5 passing unit tests).
- ✅ **Step 4 — Matching Engine:** Full matching algorithm supporting partial fills, trade recording, and TIF-aware handling of unmatched remainders.
- ✅ **Step 5 — Order Expiry Engine:** TTL support on orders, a min-heap-based `ExpiryQueue`, and a background `@Scheduled` job that auto-cancels overdue orders and cleans them out of the live book.
- ✅ **Step 6 — Adaptive Rate Limiting:** Strategy-pattern rate limiting with Token Bucket (retail) and Sliding Window Log (market-maker) implementations, Redis-backed, enforced via a servlet filter at the API gateway layer.

---

## Planned Work

- **Step 7 — Volatility-aware limits + Circuit Breaker:** Rate limits automatically tighten during simulated high-volatility periods; a circuit breaker (CLOSED/OPEN/HALF_OPEN) protects against a failing downstream dependency.
- **Step 8 — Kafka event streaming:** Every trade publishes to a Kafka/Redpanda topic; a dedicated consumer persists it to the audit trail asynchronously, decoupling matching latency from database write latency.
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