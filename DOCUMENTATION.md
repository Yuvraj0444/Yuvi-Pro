# SkyWays Airlines — Project Documentation

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [File & Directory Structure](#3-file--directory-structure)
4. [Service Architecture & Connections](#4-service-architecture--connections)
5. [End-to-End Booking Workflow](#5-end-to-end-booking-workflow)
6. [Service Details](#6-service-details)
   - [skyways-common](#61-skyways-common)
   - [skyways-registry](#62-skyways-registry-eureka)
   - [skyways-config-server](#63-skyways-config-server)
   - [skyways-gateway](#64-skyways-gateway)
   - [skyways-user-service](#65-skyways-user-service)
   - [skyways-flight-service](#66-skyways-flight-service)
   - [skyways-booking-service](#67-skyways-booking-service)
   - [skyways-payment-service](#68-skyways-payment-service)
   - [skyways-notification-service](#69-skyways-notification-service)
   - [skyways-saga-orchestrator](#610-skyways-saga-orchestrator)
   - [skyways-frontend](#611-skyways-frontend)
7. [Database Schema](#7-database-schema)
8. [Kafka Topics & Event Flow](#8-kafka-topics--event-flow)
9. [Security](#9-security)
10. [Infrastructure & Docker](#10-infrastructure--docker)
11. [Running Locally](#11-running-locally)
12. [Mock Flight Data](#12-mock-flight-data)

---

## 1. Project Overview

SkyWays Airlines is a production-grade cloud-native flight booking platform built with a **Java/Spring Boot microservices architecture**. It handles the complete airline booking lifecycle — from searching flights and entering passenger details to processing payments and sending confirmation emails — all coordinated via a **Kafka-driven Saga pattern** to ensure distributed transaction consistency.

**Core problem it solves:**
- Prevents overbooking via atomic seat reservation
- Guarantees payment never succeeds if seat reservation fails (and vice versa) — compensating transactions roll back the full flow
- Encrypts all passenger PII (passport, DOB, names) with Triple-DES at rest
- Correlates logs across all services with a shared `traceId`

---

## 2. Technology Stack

| Layer | Technology |
|---|---|
| Backend Language | Java 17 |
| Backend Framework | Spring Boot 3.3.0 |
| Service Discovery | Spring Cloud Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Config Management | Spring Cloud Config Server |
| Async Messaging | Apache Kafka (Confluent 7.6.0) |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Circuit Breaker | Resilience4j 2.2.0 |
| Security / JWT | JJWT 0.12.5 + BCrypt |
| Encryption (PII) | Triple-DES (DESede/CBC/PKCS5Padding) |
| Payment Gateway | Razorpay |
| Email | SendGrid |
| Logging | Log4j2 (JSON) → Logstash → Elasticsearch → Kibana |
| Secret Management | Google Cloud Secret Manager (local fallback: secrets.env) |
| Frontend | React 18 + Vite + Tailwind CSS |
| HTTP Client | Axios |
| Containerization | Docker + Docker Compose |
| Orchestration (prod) | Kubernetes |

---

## 3. File & Directory Structure

```
d:\Skyways\skyways-airlines\
│
├── pom.xml                          ← Parent Maven POM (manages all versions)
├── docker-compose.yml               ← Local infrastructure (Postgres, Kafka, ELK)
├── DOCUMENTATION.md                 ← This file
│
├── skyways-common/                  ← Shared library (NO runnable service)
│   └── src/main/java/com/skyways/common/
│       ├── dto/
│       │   ├── ApiResponse.java     ← Universal {success, data, timestamp} wrapper
│       │   ├── ErrorResponse.java   ← Error payload with errorCode, message, traceId
│       │   ├── ErrorDetail.java     ← Field-level validation detail
│       │   ├── PageResponse.java    ← Paginated result wrapper
│       │   └── KafkaEventEnvelope.java ← Wraps all Kafka messages with eventId, sagaId, traceId
│       ├── exception/               ← Full exception hierarchy (30+ typed exceptions)
│       │   ├── SkyWaysBaseException.java  ← Abstract root (errorCode, httpStatus, traceId)
│       │   ├── auth/                ← AuthenticationException, TokenExpiredException
│       │   ├── booking/             ← BookingNotFoundException, InvalidPassengerDetailsException, PassportExpiredException
│       │   ├── flight/              ← FlightNotFoundException, FlightOverBookedException, GDSConnectionException
│       │   ├── payment/             ← PaymentFailedException, DuplicatePaymentException, RefundFailedException
│       │   └── notification/        ← NotificationDeliveryException
│       ├── enums/
│       │   └── KafkaTopics.java     ← All Kafka topic name constants
│       ├── security/
│       │   ├── SecretManagerService.java  ← 4-tier secret resolution (env → JVM → file → GCP)
│       │   └── TripleDESEncryptor.java    ← PII encryption/decryption
│       └── filter/
│           └── MDCTraceFilter.java  ← Injects traceId into every HTTP request's MDC
│
├── skyways-registry/                ← Eureka Service Discovery (Port 8761)
│   └── src/main/java/com/skyways/registry/
│       └── SkyWaysRegistryApplication.java  ← @EnableEurekaServer
│
├── skyways-config-server/           ← Spring Cloud Config Server (Port 8888)
│   └── src/main/java/com/skyways/configserver/
│       └── SkyWaysConfigServerApplication.java  ← @EnableConfigServer
│
├── skyways-gateway/                 ← API Gateway — single entry point (Port 8080)
│   └── src/main/java/com/skyways/gateway/
│       ├── config/
│       │   └── GatewayRoutingConfig.java   ← Route definitions (lb://service-name URIs)
│       ├── filter/
│       │   ├── JwtAuthenticationFilter.java ← Validates Bearer token, injects X-User-Id header
│       │   └── LoggingFilter.java           ← Request/response logging with latency
│       └── exception/
│           └── GatewayExceptionHandler.java ← Returns ErrorResponse JSON for gateway-level errors
│
├── skyways-user-service/            ← Auth + User management (Port 8081)
│   └── src/main/java/com/skyways/user/
│       ├── controller/
│       │   └── AuthController.java  ← POST /register, POST /login
│       ├── service/
│       │   ├── UserService.java     ← Registration, login, BCrypt password handling
│       │   └── JwtService.java      ← JWT generation and validation
│       ├── entity/
│       │   ├── User.java, UserRole.java
│       │   ├── PassengerProfile.java
│       │   └── AuditLog.java
│       ├── dto/
│       │   ├── RegisterRequest.java, LoginRequest.java, LoginResponse.java
│       ├── kafka/
│       │   └── UserEventProducer.java  ← Publishes to user-events topic
│       ├── repository/
│       │   └── UserRepository.java, PassengerProfileRepository.java
│       ├── exception/
│       │   └── UserExceptionHandler.java  ← @RestControllerAdvice
│       └── config/
│           └── SecurityConfig.java  ← Spring Security (stateless, JWT-based)
│
├── skyways-flight-service/          ← Flight search + seat management (Port 8082)
│   └── src/main/java/com/skyways/flight/
│       ├── controller/
│       │   └── FlightSearchController.java  ← GET /api/v1/flights/search
│       ├── service/
│       │   ├── FlightAggregatorService.java    ← Aggregates results, 5-min cache
│       │   ├── MockFlightDataService.java      ← Generates realistic flight data in-memory
│       │   ├── GDSIntegrationService.java      ← (Real GDS — circuit breaker, retry)
│       │   ├── SkyscannerIntegrationService.java
│       │   ├── AmadeusIntegrationService.java
│       │   └── SeatService.java                ← Atomic seat reservation + release
│       ├── entity/
│       │   ├── Flight.java, FlightStatus.java
│       │   ├── Airline.java, Airport.java
│       │   ├── Seat.java, FareClass.java
│       ├── dto/
│       │   ├── FlightDto.java, FlightSearchRequest.java
│       ├── kafka/
│       │   └── SeatReservationConsumer.java  ← Listens seat-reservation-requested / seat-release-requested
│       ├── repository/
│       │   └── FlightRepository.java, SeatRepository.java, FareClassRepository.java
│       └── exception/
│           └── FlightExceptionHandler.java
│
├── skyways-booking-service/         ← Booking creation and lifecycle (Port 8083)
│   └── src/main/java/com/skyways/booking/
│       ├── controller/
│       │   └── BookingController.java  ← POST /api/v1/bookings, GET /api/v1/bookings/{ref}
│       ├── service/
│       │   ├── BookingService.java              ← Creates booking, encrypts PII, initiates saga
│       │   └── PassengerValidationService.java  ← Validates passport, DOB, email format
│       ├── entity/
│       │   ├── Booking.java, BookingStatus.java
│       │   ├── BookingItem.java, Passenger.java
│       │   └── BookingStatusHistory.java
│       ├── dto/
│       │   ├── CreateBookingRequest.java, PassengerDto.java
│       ├── kafka/
│       │   ├── BookingEventProducer.java  ← Publishes booking-initiated / confirmed / cancelled
│       │   └── SagaEventConsumer.java     ← Listens booking-confirmed / booking-cancelled
│       ├── repository/
│       │   └── BookingRepository.java, BookingStatusHistoryRepository.java
│       └── exception/
│           └── BookingExceptionHandler.java
│
├── skyways-payment-service/         ← Payment processing via Razorpay (Port 8084)
│   └── src/main/java/com/skyways/payment/
│       ├── controller/
│       │   └── PaymentController.java  ← GET /orders/{bookingId}, POST /verify, GET /{bookingId}
│       ├── service/
│       │   ├── PaymentService.java     ← Order creation, signature verification, refunds
│       │   └── RazorpayService.java    ← Razorpay SDK wrapper
│       ├── entity/
│       │   ├── Payment.java, PaymentStatus.java
│       │   ├── PaymentTransaction.java, Refund.java
│       ├── kafka/
│       │   ├── PaymentEventConsumer.java  ← Listens payment-initiation-requested / booking-cancelled
│       │   └── PaymentEventProducer.java  ← Publishes payment-processed / payment-failed
│       ├── repository/
│       │   └── PaymentRepository.java
│       └── exception/
│           └── PaymentExceptionHandler.java
│
├── skyways-notification-service/    ← Email notifications via SendGrid (Port 8085)
│   └── src/main/java/com/skyways/notification/
│       ├── service/
│       │   ├── NotificationService.java      ← Orchestrates email sending
│       │   ├── SendGridService.java          ← SendGrid API wrapper
│       │   └── EmailTemplateService.java     ← HTML email templates
│       ├── kafka/
│       │   └── NotificationEventConsumer.java  ← Listens notification-requested
│       ├── retry/
│       │   └── NotificationRetryHandler.java   ← 3 retries, then publishes to DLQ
│       └── dto/
│           └── BookingConfirmationDto.java
│
├── skyways-saga-orchestrator/       ← Distributed transaction coordinator (Port 8086)
│   └── src/main/java/com/skyways/saga/
│       ├── service/
│       │   └── SagaOrchestrationService.java  ← Drives the full saga state machine
│       ├── kafka/
│       │   └── SagaEventRouter.java           ← 6 KafkaListeners routing events to service
│       ├── entity/
│       │   ├── SagaState.java, SagaStatus.java
│       └── repository/
│           └── SagaStateRepository.java
│
├── skyways-frontend/                ← React + Vite SPA (Port 3000)
│   ├── vite.config.js               ← Proxy: /api → http://localhost:8080
│   ├── src/
│   │   ├── main.jsx                 ← Entry point
│   │   ├── App.jsx                  ← Route definitions
│   │   ├── context/
│   │   │   └── AuthContext.jsx      ← Global auth state (user, login, logout)
│   │   ├── hooks/
│   │   │   └── useAuth.js           ← Hook to access AuthContext
│   │   ├── api/
│   │   │   ├── axios.js             ← Axios instance (JWT interceptor, 401 handler)
│   │   │   ├── authApi.js           ← login, register, getProfile
│   │   │   ├── flightApi.js         ← search, getById
│   │   │   ├── bookingApi.js        ← create, getById, getMyBookings, cancel
│   │   │   └── paymentApi.js        ← getOrder, verify, getStatus
│   │   ├── pages/
│   │   │   ├── HomePage.jsx
│   │   │   ├── LoginPage.jsx        ← Redirects back to original page after login
│   │   │   ├── RegisterPage.jsx
│   │   │   ├── FlightSearchPage.jsx
│   │   │   ├── BookingFlowPage.jsx  ← Passenger form + creates booking via API
│   │   │   ├── PaymentPage.jsx      ← Razorpay Checkout popup integration
│   │   │   ├── BookingConfirmationPage.jsx
│   │   │   ├── MyBookingsPage.jsx
│   │   │   ├── BookingDetailPage.jsx
│   │   │   └── ProfilePage.jsx
│   │   ├── components/
│   │   │   ├── common/
│   │   │   │   ├── ProtectedRoute.jsx   ← Redirects to /login if no token
│   │   │   │   ├── LoadingSpinner.jsx
│   │   │   │   ├── Alert.jsx
│   │   │   │   ├── StatusBadge.jsx
│   │   │   │   └── AirportSearch.jsx    ← Airport autocomplete
│   │   │   ├── layout/
│   │   │   │   ├── Navbar.jsx
│   │   │   │   └── Footer.jsx
│   │   │   ├── flights/
│   │   │   │   ├── FlightSearchForm.jsx
│   │   │   │   └── FlightCard.jsx       ← Displays one flight result with Book Now button
│   │   │   └── booking/
│   │   │       ├── PassengerForm.jsx    ← Multi-passenger form with react-hook-form
│   │   │       └── BookingSummary.jsx   ← Price breakdown sidebar
│   │   └── data/
│   │       └── airports.js             ← IATA code list for autocomplete
│
├── docker/
│   ├── init-db.sql                  ← Creates all PostgreSQL databases + tables on first run
│   └── Dockerfile.*                 ← Multi-stage Dockerfiles per service
│
├── kafka/
│   └── create-topics.sh             ← Script to pre-create all 12 Kafka topics
│
├── elk/
│   ├── elasticsearch.yml
│   ├── logstash.conf                ← Parses JSON logs, routes to Elasticsearch
│   └── logstash.yml
│
├── k8s/                             ← Kubernetes manifests (Deployments, Services, HPAs)
│
└── scripts/
    ├── secrets.env                  ← Local dev secrets (JWT_SECRET, DB_PASS, API keys)
    └── load-secrets.ps1             ← Exports secrets.env into PowerShell environment
```

---

## 4. Service Architecture & Connections

```
                          ┌─────────────┐
                          │   Browser   │
                          │  :3000      │
                          └──────┬──────┘
                                 │ HTTP (Vite proxy /api → :8080)
                          ┌──────▼──────────────────┐
                          │    API GATEWAY :8080     │
                          │  JwtAuthenticationFilter │
                          │  (validates JWT,         │
                          │   injects X-User-Id)     │
                          └──┬────┬────┬────┬────────┘
                             │    │    │    │  lb:// (Eureka)
              ┌──────────────┘    │    │    └──────────────────┐
              │                   │    │                       │
       ┌──────▼──────┐   ┌────────▼────┐   ┌──────────────┐   ┌───────────┐
       │ User Service│   │Flight Service│   │Booking Service│   │Payment Svc│
       │    :8081    │   │    :8082    │   │    :8083     │   │   :8084   │
       └──────┬──────┘   └────────┬────┘   └──────┬───────┘   └─────┬─────┘
              │                   │               │                  │
              │            ┌──────▼──────┐        │                  │
              │            │ MockFlight  │        │                  │
              │            │ DataService │        │                  │
              │            │ (in-memory) │        │                  │
              │            └─────────────┘        │                  │
              │                                   │                  │
              └─────────────────┬─────────────────┘                  │
                                │           ┌────────────────────────┘
                                │           │
                          ┌─────▼───────────▼──────────────┐
                          │         Apache Kafka            │
                          │         :29092 (host)           │
                          │         :9092  (internal)       │
                          └──────────────┬──────────────────┘
                                         │
               ┌─────────────────────────┼──────────────────────┐
               │                         │                      │
      ┌────────▼──────────┐   ┌──────────▼──────────┐   ┌──────▼──────────┐
      │  Saga Orchestrator │   │  Booking Service    │   │ Notification Svc │
      │      :8086         │   │  (SagaEventConsumer)│   │     :8085        │
      │ (drives the state  │   │  updates status     │   │ SendGrid emails  │
      │  machine for each  │   └─────────────────────┘   └─────────────────┘
      │  transaction)      │
      └────────────────────┘
               │
               │ Service Discovery
      ┌────────▼──────────┐
      │  Eureka Registry  │
      │      :8761        │
      └────────────────────┘
               │
               │ Config Fetch (startup)
      ┌────────▼──────────┐
      │  Config Server    │
      │      :8888        │
      └────────────────────┘
```

### How Services Find Each Other

Every service registers with **Eureka** at startup. The Gateway uses `lb://skyways-booking-service` (load-balanced URI) so Eureka resolves the actual host:port. No service has hardcoded URLs to other services.

### What the Gateway Does

Every frontend request to `/api/**` goes through the gateway first:
1. Reads the `Authorization: Bearer <token>` header
2. Validates the JWT signature using the same `JWT_SECRET` as the user-service
3. Extracts the `userId` from the token's `sub` claim
4. Injects `X-User-Id: <userId>` header into the forwarded request
5. Routes to the correct downstream service via Eureka load balancing

Public endpoints (login, register, flight search) bypass JWT validation.

---

## 5. End-to-End Booking Workflow

### Step 1 — Search Flights (No Login Required)

```
User fills search form
  → FlightSearchPage calls GET /api/v1/flights/search?origin=DEL&destination=BOM&...
  → Gateway forwards (no JWT check — public path)
  → FlightSearchController → FlightAggregatorService
  → MockFlightDataService.searchFlights() generates flights in-memory
      (calculates distance via Haversine formula, picks airlines by region,
       generates realistic INR prices, adds connecting flight for long-haul)
  → Returns List<FlightDto> sorted by price
  → FlightCard components render results
```

### Step 2 — Book a Flight (Login Required)

```
User clicks "Book Now" on a FlightCard
  → ProtectedRoute checks localStorage for token
  → If not logged in: redirect to /login with {state: {from: /book/:flightId}}
  → After login: LoginPage navigates back to original URL with flight state intact
  → BookingFlowPage loads with flight data from router state
```

### Step 3 — Passenger Details Form

```
User fills PassengerForm (firstName, lastName, passportNo, passportExpiry, DOB, nationality, email, phone)
  → react-hook-form validates client-side (passport pattern, email format, required fields)
  → User clicks "Continue to Payment"
  → BookingFlowPage.handlePassengerSubmit() calls POST /api/v1/bookings
       payload: { flightId, passengers[], totalAmount, currency }
  → Gateway validates JWT → injects X-User-Id → forwards to Booking Service (:8083)
  → BookingController reads X-User-Id header
  → PassengerValidationService validates:
       - passport format [A-Z0-9]{6-9}
       - passportExpiry must be > 6 months in future
       - dateOfBirth must make passenger ≥ 2 years old
       - email format
       - name ≥ 2 chars, no special chars
  → BookingService.createBooking():
       - Encrypts all PII with TripleDESEncryptor (firstName, lastName, passportNo, DOB, email, phone)
       - Persists Booking (status = INITIATED) + BookingItems + Passengers in booking_db
       - Publishes booking-initiated event to Kafka (non-blocking, fails fast after 5s)
  → Returns { bookingId, bookingRef, status, message }
  → Frontend navigates to /payment/:bookingId
```

### Step 4 — Payment (Razorpay)

```
PaymentPage loads
  → Razorpay checkout.js script loaded dynamically
  → User clicks "Pay ₹X via Razorpay"
  → Razorpay native popup opens (UPI, Cards, Netbanking, Wallets)
  → User completes payment in popup
  → On success: Razorpay calls handler({ razorpay_payment_id, razorpay_order_id, razorpay_signature })
  → Frontend calls POST /api/v1/payments/verify with those values
  → PaymentController → PaymentService.verifyAndCapture()
       - Validates HMAC-SHA256 signature
       - Updates Payment to COMPLETED in payment_db
       - Publishes payment-processed to Kafka
  → Frontend navigates to /confirmation/:bookingId
```

### Step 5 — Saga Completes (Async, behind the scenes)

```
[Kafka Events flowing in parallel with Steps 3-4]

booking-initiated (from BookingService)
  → SagaOrchestrator.handleBookingInitiated()
      - Creates SagaState (STARTED) in saga_db
      - Publishes seat-reservation-requested

seat-reservation-requested (from SagaOrchestrator)
  → FlightService.SeatReservationConsumer
      - SeatService.reserveSeats() — atomically marks N seats as RESERVED
      - If seats available: publishes seat-reservation-confirmed
      - If overbooked: publishes seat-reservation-failed

seat-reservation-confirmed (from FlightService)
  → SagaOrchestrator.handleSeatReservationConfirmed()
      - SagaState → PAYMENT_PENDING
      - Publishes payment-initiation-requested

payment-processed (from PaymentService after Step 4)
  → SagaOrchestrator.handlePaymentProcessed()
      - SagaState → COMPLETED
      - Publishes booking-confirmed
      - Publishes notification-requested

booking-confirmed (from SagaOrchestrator)
  → BookingService.SagaEventConsumer
      - Updates Booking.status = CONFIRMED in booking_db

notification-requested (from SagaOrchestrator)
  → NotificationService.NotificationEventConsumer
      - Renders HTML email template
      - Sends via SendGrid
      - Retries up to 3 times on failure
      - On 3rd failure: publishes to notification-dlq
```

### Failure / Compensation Flow

```
If seat-reservation-failed:
  SagaOrchestrator → publishes booking-cancelled
                   → publishes notification-requested (cancellation email)
  BookingService   → updates Booking.status = CANCELLED
  SagaState        → COMPENSATED

If payment-failed (after seats reserved):
  SagaOrchestrator → publishes seat-release-requested
  FlightService    → releases seat reservations
  SagaOrchestrator → publishes booking-cancelled
  BookingService   → updates Booking.status = CANCELLED
  PaymentService   → issues Razorpay refund if charge was made
  SagaState        → COMPENSATED
```

---

## 6. Service Details

### 6.1 skyways-common

Shared library imported by every service. Not a runnable application.

**Key classes:**

| Class | Purpose |
|---|---|
| `ApiResponse<T>` | Wraps every HTTP response: `{success, data, timestamp}` |
| `ErrorResponse` | Error payload with `errorCode`, `message`, `path`, `traceId`, `details[]` |
| `KafkaEventEnvelope<T>` | Wraps every Kafka message with `eventId`, `eventType`, `sagaId`, `traceId`, `payload` |
| `SkyWaysBaseException` | Root of all custom exceptions — carries `errorCode`, `httpStatus`, `traceId` |
| `SecretManagerService` | Resolves secrets: env var → JVM `-D` → `scripts/secrets.env` → GCP Secret Manager |
| `TripleDESEncryptor` | Encrypts/decrypts passenger PII. Key: `TRIPLE_DES_KEY` from SecretManagerService |
| `MDCTraceFilter` | Injects `X-Trace-Id` into every request's Log4j2 MDC for cross-service log correlation |

### 6.2 skyways-registry (Eureka)

**Port:** 8761  
**Role:** Service registry — all other services register here on startup and query it for peer addresses.

### 6.3 skyways-config-server

**Port:** 8888  
**Role:** Centralised configuration — serves `application.yml` per service from a Git repo. Services fetch config at boot with `spring.config.import: optional:configserver:http://...`.

### 6.4 skyways-gateway

**Port:** 8080 — the **only port the frontend talks to**.

**Routes:**

| Path | Downstream Service |
|---|---|
| `/api/v1/auth/**`, `/api/v1/users/**` | skyways-user-service |
| `/api/v1/flights/**` | skyways-flight-service |
| `/api/v1/bookings/**` | skyways-booking-service |
| `/api/v1/payments/**` | skyways-payment-service |

**Public paths** (no JWT): `/api/v1/auth/login`, `/api/v1/auth/register`, `/api/v1/flights/search`

**JWT filter** (`JwtAuthenticationFilter`):
- Reads `JWT_SECRET` using the same 4-tier cascade as `SecretManagerService` (env → JVM → `secrets.env` → fallback)
- Parses the Bearer token, extracts `sub` (userId) and `role`
- Adds `X-User-Id` and `X-User-Role` headers to the forwarded request

### 6.5 skyways-user-service

**Port:** 8081  
**Database:** `user_db`

**Endpoints:**

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Creates user account (BCrypt password) |
| POST | `/api/v1/auth/login` | Returns JWT token (24-hour expiry) |
| GET | `/api/v1/users/profile` | Returns current user's profile |

**JWT Token format:**
```json
{
  "sub": "<userId UUID>",
  "role": "CUSTOMER",
  "email": "user@example.com",
  "iat": 1716000000,
  "exp": 1716086400
}
```
Signed with HMAC-SHA384 using `JWT_SECRET` from `SecretManagerService`.

### 6.6 skyways-flight-service

**Port:** 8082  
**Database:** `flight_db` (schema exists; flights are currently in-memory)

**Endpoints:**

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/flights/search` | Search flights (no auth) |

**Flight data source:**

`MockFlightDataService` at:
```
skyways-flight-service/src/main/java/com/skyways/flight/service/MockFlightDataService.java
```

It generates realistic flights on every search call:
- **72 airports** with real GPS coordinates (India, Middle East, Europe, Americas, Asia, Africa)
- **30 airlines** with regional preference scoring (IndiGo/Air India prioritised for Indian routes)
- **Pricing in INR**: domestic ₹1,500–₹16,000, international ₹15,000–₹42,000, long-haul ₹30,000–₹75,000
- **Haversine formula** for realistic distance-based pricing
- **Deterministic UUIDs** — same route+date always produces the same flight IDs
- **5-minute response cache** per route in `FlightAggregatorService`

To plug in real APIs, edit `FlightAggregatorService.java` — it has TODO markers where `SkyscannerIntegrationService` and `AmadeusIntegrationService` connect.

**Resilience4j circuit breakers** (for real APIs when enabled):
- GDS: 3 retries, 2s wait, 5s timeout, opens at 50% failure rate
- Skyscanner: 2 retries, 1s wait, opens at 50% failure rate

### 6.7 skyways-booking-service

**Port:** 8083  
**Database:** `booking_db`

**Endpoints:**

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/bookings` | Create booking (requires JWT) |
| GET | `/api/v1/bookings/{bookingRef}` | Get booking by reference (e.g. `SW-A3K9X`) |

**Passenger validation rules** (`PassengerValidationService`):
- Passport number: `[A-Z0-9]{6,9}` (uppercase alphanumeric)
- Passport expiry: must be YYYY-MM-DD and > 6 months from today
- Date of birth: must be YYYY-MM-DD, passenger must be ≥ 2 years old
- Email: standard format
- Names: ≥ 2 characters, no `< > " ' % ; ( ) & +`

**PII encryption:** All sensitive passenger fields are encrypted with TripleDES before being written to `booking_db`.

**Booking reference format:** `SW-XXXXX` (5 random uppercase alphanumeric characters)

### 6.8 skyways-payment-service

**Port:** 8084  
**Database:** `payment_db`

**Endpoints:**

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/payments/orders/{bookingId}` | Get Razorpay order details |
| POST | `/api/v1/payments/verify` | Verify Razorpay signature after payment |
| GET | `/api/v1/payments/{bookingId}` | Get payment status |

**Razorpay flow:**
1. Payment service creates a Razorpay Order (with idempotency key `SW-{bookingId}`)
2. Frontend opens Razorpay Checkout popup with the `orderId`
3. After user pays, Razorpay returns `{ razorpay_payment_id, razorpay_order_id, razorpay_signature }`
4. Frontend calls `/verify` — service validates HMAC-SHA256 signature, updates status to COMPLETED

**Test card:** `4111 1111 1111 1111` (any future expiry, any CVV)

### 6.9 skyways-notification-service

**Port:** 8085  
**No HTTP endpoints** — purely Kafka-driven.

Consumes `notification-requested` events and sends emails via SendGrid.  
Retries up to 3 times on failure; after 3 failures publishes to `notification-dlq`.

### 6.10 skyways-saga-orchestrator

**Port:** 8086  
**Database:** `saga_db`

The brain of distributed transactions. Listens to Kafka events and drives the saga state machine:

```
STARTED
  → (seat-reservation-requested published)
SEAT_RESERVATION_PENDING
  → on confirmed: (payment-initiation-requested published)
PAYMENT_PENDING
  → on processed: (booking-confirmed + notification-requested published)
COMPLETED

On any failure: COMPENSATING → COMPENSATED
```

Each saga step is idempotent — the `sagaId` is checked before processing to handle Kafka at-least-once delivery.

### 6.11 skyways-frontend

**Port:** 3000 (Vite dev server)  
**Proxy:** `/api/*` → `http://localhost:8080`

**Authentication flow:**
1. Login stores JWT in `localStorage` as `skyways_token`
2. Every axios request attaches `Authorization: Bearer <token>`
3. `ProtectedRoute` checks `localStorage` — if no token, saves current location and redirects to `/login`
4. After login, `LoginPage` navigates back to the saved location (including router state) so the booking flow continues seamlessly

**Key pages and what they do:**

| Page | Route | What it does |
|---|---|---|
| `HomePage` | `/` | Landing, links to search |
| `LoginPage` | `/login` | JWT login, redirect-after-login |
| `RegisterPage` | `/register` | Account creation |
| `FlightSearchPage` | `/flights` | Search form + FlightCard results |
| `BookingFlowPage` | `/book/:flightId` | Passenger form, calls POST /bookings |
| `PaymentPage` | `/payment/:bookingId` | Opens Razorpay popup, calls POST /payments/verify |
| `BookingConfirmationPage` | `/confirmation/:bookingId` | Shows booking reference and summary |
| `MyBookingsPage` | `/bookings` | Lists all user bookings |
| `BookingDetailPage` | `/bookings/:id` | Full booking detail |

---

## 7. Database Schema

### user_db

```sql
users           (userId UUID PK, email UNIQUE, passwordHash, role, isVerified, createdAt, updatedAt)
passenger_profiles (profileId UUID PK, userId FK, firstName, lastName, passportNo, dateOfBirth, nationality, preferences JSON)
audit_log       (logId BIGSERIAL PK, userId, action, ipAddress, userAgent, timestamp)
```

### flight_db

```sql
airports    (iataCode CHAR(3) PK, name, city, country, timezone)
airlines    (airlineId UUID PK, iataCode CHAR(2), name, country)
flights     (flightId UUID PK, airlineId FK, flightNumber, originIata FK, destinationIata FK,
             departureTime, arrivalTime, status, totalSeats, availableSeats, createdAt)
seats       (seatId UUID PK, flightId FK, seatNumber, fareClass, status, bookingId, createdAt, updatedAt)
fare_classes (fareId UUID PK, flightId FK, classType, basePrice NUMERIC(10,2), currency, available)
```
> Note: Flights are generated in-memory by `MockFlightDataService`. The `flights` table is defined but not used for search.

### booking_db

```sql
bookings         (bookingId UUID PK, userId UUID, bookingRef UNIQUE, totalAmount NUMERIC(10,2),
                  currency, status, sagaId UUID, createdAt, updatedAt)
booking_items    (itemId UUID PK, bookingId FK, flightId UUID, fareClass, price NUMERIC(10,2))
passengers       (passengerId UUID PK, bookingId FK, firstName*, lastName*, passportNo*,
                  nationality, dateOfBirth*, email*, phone*)   (* = 3DES encrypted)
booking_status_history (historyId BIGSERIAL PK, bookingId UUID, oldStatus, newStatus, reason, changedAt)
```

### payment_db

```sql
payments             (paymentId UUID PK, bookingId UUID UNIQUE, amount NUMERIC(10,2), currency,
                      status, razorpayOrderId, gatewayPaymentId, idempotencyKey UNIQUE, sagaId, createdAt, updatedAt)
payment_transactions (txnId UUID PK, paymentId FK, gatewayEvent, rawResponse TEXT, occurredAt)
refunds              (refundId UUID PK, paymentId FK, refundAmount, gatewayRefundId, reason, status, createdAt)
```

### saga_db

```sql
saga_state (sagaId UUID PK, bookingId UUID INDEXED, status, currentStep,
            failureReason, compensationStep, createdAt, updatedAt)
```

---

## 8. Kafka Topics & Event Flow

All producers use `acks=all` and `enable.idempotence=true`.

| Topic | Published By | Consumed By |
|---|---|---|
| `booking-initiated` | Booking Service | Saga Orchestrator |
| `seat-reservation-requested` | Saga Orchestrator | Flight Service |
| `seat-reservation-confirmed` | Flight Service | Saga Orchestrator |
| `seat-reservation-failed` | Flight Service | Saga Orchestrator |
| `seat-release-requested` | Saga Orchestrator | Flight Service |
| `payment-initiation-requested` | Saga Orchestrator | Payment Service |
| `payment-processed` | Payment Service | Saga Orchestrator |
| `payment-failed` | Payment Service | Saga Orchestrator |
| `booking-confirmed` | Saga Orchestrator | Booking Service |
| `booking-cancelled` | Saga Orchestrator | Booking Service, Payment Service |
| `notification-requested` | Saga Orchestrator | Notification Service |
| `notification-dlq` | Notification Service | (manual review) |
| `user-events` | User Service | (analytics/future use) |

---

## 9. Security

### Authentication
- **Algorithm:** HMAC-SHA384 JWT
- **Expiry:** 24 hours
- **Storage:** Browser `localStorage` (key: `skyways_token`)
- **Validation:** Gateway validates on every non-public request

### PII Encryption
Passenger personal data stored encrypted in `booking_db`:
- **Algorithm:** Triple-DES (DESede/CBC/PKCS5Padding)
- **IV:** Random 8-byte, prepended to ciphertext, stored as `Base64(iv + ciphertext)`
- **Encrypted fields:** firstName, lastName, passportNo, dateOfBirth, email, phone
- **Key source:** `TRIPLE_DES_KEY` (Base64-encoded 24 bytes) from SecretManagerService

### Secret Resolution Order (SecretManagerService)
1. OS environment variable
2. JVM system property (`-DJWT_SECRET=...`)
3. `scripts/secrets.env` file (auto-discovered relative to working directory)
4. Google Cloud Secret Manager (only if `skyways.secrets.gcp.enabled=true`)

---

## 10. Infrastructure & Docker

### docker-compose.yml services

| Container | Image | Port | Purpose |
|---|---|---|---|
| `skyways-postgres` | postgres:16-alpine | 5432 | All PostgreSQL databases |
| `skyways-zookeeper` | confluentinc/cp-zookeeper:7.6.0 | 2181 | Kafka coordination |
| `skyways-kafka` | confluentinc/cp-kafka:7.6.0 | 9092 (internal), **29092 (host)** | Message broker |
| `skyways-elasticsearch` | docker.elastic.co/elasticsearch:8.13.0 | 9200 | Log storage |
| `skyways-logstash` | docker.elastic.co/logstash:8.13.0 | 5000, 5044, 9600 | Log pipeline |
| `skyways-kibana` | docker.elastic.co/kibana:8.13.0 | 5601 | Log visualisation |

> All Spring Boot services run as local JAR processes (not Docker containers) in the default dev setup.

### Important: Kafka Port

Services connect to Kafka on **`localhost:29092`** when running on the host machine (the Docker container maps internal port 9092 to host port 29092). The `application.yml` defaults use `${KAFKA_BOOTSTRAP:localhost:29092}`.

---

## 11. Running Locally

### Prerequisites
- Java 17+, Maven 3.9+, Node.js 18+, Docker Desktop

### Start Infrastructure

```bash
cd d:\Skyways\skyways-airlines
docker-compose up -d
```

### Build All Services

```bash
mvn clean package -DskipTests
```

### Start Backend Services (in order)

```bash
# 1. Eureka first
java -jar skyways-registry/target/skyways-registry-1.0.0-SNAPSHOT.jar

# 2. Then all others (in parallel or sequence)
java -jar skyways-gateway/target/skyways-gateway-1.0.0-SNAPSHOT.jar
java -jar skyways-user-service/target/skyways-user-service-1.0.0-SNAPSHOT.jar
java -jar skyways-flight-service/target/skyways-flight-service-1.0.0-SNAPSHOT.jar
java -jar skyways-booking-service/target/skyways-booking-service-1.0.0-SNAPSHOT.jar
java -jar skyways-payment-service/target/skyways-payment-service-1.0.0-SNAPSHOT.jar
java -jar skyways-notification-service/target/skyways-notification-service-1.0.0-SNAPSHOT.jar
```

### Start Frontend

```bash
cd skyways-frontend
npm install
npm run dev
```

### Access Points

| URL | Description |
|---|---|
| http://localhost:3000 | Frontend application |
| http://localhost:8761 | Eureka dashboard |
| http://localhost:8080/swagger-ui.html | API docs (aggregated) |
| http://localhost:5601 | Kibana (logs) |

---

## 12. Mock Flight Data

Since real GDS/Skyscanner API keys are not configured, flight data is generated by:

```
skyways-flight-service/src/main/java/com/skyways/flight/service/MockFlightDataService.java
```

**To add/modify airports:** Edit the `AIRPORTS` static block (line ~30) — each entry is:
```java
add("IATA", "Airport Name", "City", "Country", latitude, longitude);
```

**To change airlines:** Edit the `AIRLINES` list (line ~120).

**To adjust pricing:** Edit `generatePrice()` method — current INR tiers:
```
< 500 km   → ₹1,500 + km × 3    (ultra-short domestic)
< 1,500 km → ₹2,500 + km × 3.5  (short domestic)
< 3,000 km → ₹4,000 + km × 4    (medium domestic/regional)
< 6,000 km → ₹15,000 + km × 4.5 (international)
≥ 6,000 km → ₹30,000 + km × 3.5 (long-haul)
```

**To enable real APIs:** Open `FlightAggregatorService.java` and replace the `mockService.searchFlights(req)` call with parallel calls to `SkyscannerIntegrationService` and `AmadeusIntegrationService`. Add your API keys to `scripts/secrets.env`.