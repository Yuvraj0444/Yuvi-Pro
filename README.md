# ✈️ SkyWays Airlines

A production-grade flight booking platform built with Java Spring Boot microservices, Apache Kafka saga orchestration, and a React frontend.

## Overview

SkyWays Airlines handles the complete booking flow — flight search, passenger validation, payment processing, and email notifications — across 9 independently deployable services. Each service owns its database, communicates through Kafka events, and is routed through a central API gateway with JWT authentication.

## Architecture

```
React Frontend (port 3000)
        ↓
API Gateway (port 8080) — JWT validation, rate limiting, routing
        ↓
Eureka Service Registry — dynamic service discovery
        ↓
┌─────────────┬──────────────┬─────────────────┬─────────────────┐
│ User Service│Flight Service│ Booking Service │ Payment Service │
│   (8081)    │   (8082)     │    (8083)       │    (8084)       │
└─────────────┴──────────────┴─────────────────┴─────────────────┘
                                    ↓
                             Apache Kafka
                                    ↓
                         Saga Orchestrator (8086)
                                    ↓
                       Notification Service (8085)
```

### Booking Saga Flow

```
booking-initiated → seat-reservation-requested → payment-initiation-requested → booking-confirmed → notification-requested
```

**Compensation on failure:**
```
payment-failed → seat-release-requested → booking-cancelled → cancellation email sent
```

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Spring Cloud 2023 |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway + JWT filter |
| Messaging | Apache Kafka (Saga choreography) |
| Database | PostgreSQL 16 (per-service isolation) |
| Payments | Razorpay & Stripe API |
| Email | SendGrid |
| Resilience | Resilience4j (circuit breaker, retry, timeout) |
| Observability | ELK Stack 7.17 + Log4j2 JSON structured logs |
| Security | JWT (JJWT 0.12), BCrypt, 3-DES PII encryption |
| Frontend | React 18, Vite, Tailwind CSS, React Router |
| Containers | Docker Compose (16 services) |

## Services

| Service | Port | Description |
|---|---|---|
| eureka-registry | 8761 | Service discovery |
| config-server | 8888 | Centralized configuration |
| api-gateway | 8080 | JWT auth, routing, rate limiting |
| user-service | 8081 | Registration, login, JWT issuance |
| flight-service | 8082 | Flight search, seat inventory |
| booking-service | 8083 | Reservations, overbooking guard, saga initiation |
| payment-service | 8084 | Razorpay & Stripe integration, refunds |
| notification-service | 8085 | SendGrid email with retry and dead-letter queue |
| saga-orchestrator | 8086 | Distributed transaction coordination and compensation |
| frontend | 3000 | React UI served by Nginx |

## Key Features

- **Saga Pattern** — Kafka-driven distributed transactions with automatic rollback on failure
- **Overbooking Guard** — Pessimistic locking prevents double-booking under concurrent load
- **3-DES Encryption** — All PII data (passport numbers, DOB, contact details) encrypted at rest
- **Circuit Breakers** — Resilience4j wraps external flight APIs with graceful fallback
- **Idempotent Payments** — Razorpay & Stripe idempotency keys prevent duplicate charges on retries
- **Centralized Logging** — ELK Stack with trace ID correlation across all services
- **JWT Security** — Stateless authentication validated at the gateway layer

## Quick Start

**Prerequisites:** Docker Desktop

```bash
git clone https://github.com/Yuvraj0444/Yuvi-Pro.git
cd Yuvi-Pro
docker compose up -d
```

Open **http://localhost:3000**

All 16 containers start automatically. First startup takes ~2 minutes.

## Observability

| Dashboard | URL |
|---|---|
| Kibana (logs) | http://localhost:5601 |
| Eureka (services) | http://localhost:8761 |

## Project Structure

```
skyways-airlines/
├── skyways-common/               # Shared DTOs, exceptions, 3-DES, Kafka envelopes
├── skyways-registry/             # Eureka Server
├── skyways-config-server/        # Spring Cloud Config Server
├── skyways-gateway/              # API Gateway + JWT filter
├── skyways-user-service/         # User auth and profiles
├── skyways-flight-service/       # Flight search and inventory
├── skyways-booking-service/      # Booking management and saga initiation
├── skyways-payment-service/      # Payment processing
├── skyways-notification-service/ # Email notifications
├── skyways-saga-orchestrator/    # Saga coordination
├── skyways-frontend/             # React + Vite frontend
├── docker/                       # Dockerfiles per service
├── elk/                          # Logstash config
├── kafka/                        # Topic initialization
├── k8s/                          # Kubernetes manifests
└── scripts/                      # Database seed scripts
```

## License

MIT
