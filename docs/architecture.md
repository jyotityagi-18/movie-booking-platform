# Architecture & Design Document

## Table of Contents
1. [System Overview](#1-system-overview)
2. [Technology Choices](#2-technology-choices)
3. [Database & Data Modelling](#3-database--data-modelling)
4. [Transactional Design](#4-transactional-design)
5. [Non-Functional Requirements](#5-non-functional-requirements)
6. [Security — OWASP Top 10](#6-security--owasp-top-10)
7. [Integration with Theatre Partners](#7-integration-with-theatre-partners)
8. [Payment Gateway Integration](#8-payment-gateway-integration)
9. [Platform Monetization](#9-platform-monetization)
10. [Hosting, Sizing & Cloud Strategy](#10-hosting-sizing--cloud-strategy)
11. [Release Management](#11-release-management)
12. [Monitoring & Observability](#12-monitoring--observability)
13. [KPIs](#13-kpis)
14. [AI Enhancements](#14-ai-enhancements)
15. [Compliance](#15-compliance)
16. [High-Level Project Plan](#16-high-level-project-plan)

---

## 1. System Overview

```
┌─────────────────────────────────────────────────────┐
│                  API Gateway / LB                    │
│           (rate-limit, auth, TLS termination)        │
└──────┬──────────────────────────────┬────────────────┘
       │                              │
  ┌────▼─────────────┐    ┌──────────▼──────────┐
  │  ShowService     │    │  BookingService     │
  │  ─────────────── │    │  ────────────────── │
  │  Browse shows    │    │  Book tickets       │
  │  Seat map        │    │  Cancel booking     │
  │  Admin CRUD      │    │  Pricing + discounts│
  └────────┬─────────┘    └──────────┬──────────┘
           │                         │
           └────────┬────────────────┘
                    ▼
           ┌──────────────┐
           │  PostgreSQL  │  (H2 for demo)
           └──────────────┘
```

### Architecture Style
**Modular Monolith** for MVP → decompose into microservices as domains stabilize.

Rationale:
- Faster delivery, simpler deployment, easier debugging
- Clear package boundaries (catalog, booking, admin) make future extraction straightforward
- Avoids premature distributed-system complexity

---

## 2. Technology Choices

| Area | Choice | Key Driver |
|------|--------|-----------|
| Language | Java 17 | Enterprise maturity, strong typing, vast ecosystem |
| Framework | Spring Boot 4 | Production-grade, auto-config, test support |
| ORM | Spring Data JPA / Hibernate | Rapid CRUD, JPQL for custom queries |
| Database (demo) | H2 | Zero-config, instant demo |
| Database (prod) | PostgreSQL | ACID, JSONB support, row-level locking, proven scale |
| Cache | Redis | Low-latency reads for show listings and seat counts |
| Messaging | Kafka / RabbitMQ | Async notifications, partner sync, event sourcing |
| API Style | RESTful JSON | Ubiquitous, tooling-rich, easy to consume |
| Auth | Spring Security + OAuth 2.0 / JWT | Stateless, standard, integrates with IdPs |
| Container | Docker | Portable, CI/CD friendly |
| Orchestration | Kubernetes (EKS/GKE/AKS) | Auto-scaling, self-healing, rolling deploys |
| Build & CI | Maven + GitHub Actions / Jenkins | Mature, reproducible builds |

---

## 3. Database & Data Modelling

### Core Entities
| Entity | Purpose |
|--------|---------|
| `Movie` | Catalog of available movies |
| `Theatre` | Onboarded partner theatres |
| `Show` | A screening of a movie at a theatre on a date/time |
| `Seat` | Individual seat with type, status, booking reference |
| `Booking` | Customer reservation with pricing snapshot |

### Key Design Decisions
1. **Denormalized `availableSeats`** on `Show` — enables fast filtering without counting seats per query.
2. **`@Version`** on `Show` — optimistic locking prevents overselling during concurrent bookings.
3. **Seat-level status** — supports preferred-seat selection and visual seat maps.
4. **Pricing snapshot in Booking** — total, discount, and final amounts are stored at booking time so historical accuracy is preserved even if pricing rules change.

### Indexes (Production)
```sql
CREATE INDEX idx_show_city_date ON shows (theatre_id, show_date);
CREATE INDEX idx_seat_show_status ON seats (show_id, status);
CREATE INDEX idx_booking_customer ON bookings (customer_email);
CREATE INDEX idx_theatre_city ON theatres (city);
```

---

## 4. Transactional Design

### Booking Flow
```
Client                 BookingService                 DB
  │ POST /bookings        │                           │
  │──────────────────────►│                           │
  │                       │ BEGIN TX                  │
  │                       │──────────────────────────►│
  │                       │ SELECT seats WHERE        │
  │                       │   id IN (…) AND status =  │
  │                       │   AVAILABLE AND show_id=X │
  │                       │◄──────────────────────────│
  │                       │                           │
  │                       │ ✓ validate count matches  │
  │                       │                           │
  │                       │ calculate pricing         │
  │                       │                           │
  │                       │ INSERT booking            │
  │                       │──────────────────────────►│
  │                       │ UPDATE seats SET status=  │
  │                       │   BOOKED, booking_id=…    │
  │                       │──────────────────────────►│
  │                       │ UPDATE show SET           │
  │                       │   available_seats -= N,   │
  │                       │   version = version + 1   │
  │                       │──────────────────────────►│
  │                       │ COMMIT                    │
  │◄──────────────────────│                           │
  │ 201 BookingResponse   │                           │
```

### Concurrency Safety
| Mechanism | Protection |
|-----------|-----------|
| `@Transactional` | Atomic seat-update + booking creation |
| `@Version` (optimistic lock) | Prevents two threads from decrementing `availableSeats` simultaneously |
| Status check in SELECT | Only AVAILABLE seats are returned, so double-booking is prevented |
| Database constraint | `seat.status` ensures integrity at DB level |

### Failure Scenarios
| Scenario | Handling |
|----------|---------|
| Concurrent booking of same seat | Second TX gets `OptimisticLockException` → 409 Conflict |
| Payment timeout (future) | Reservation expires; scheduled job releases BLOCKED seats after TTL |

---

## 5. Non-Functional Requirements

### Scalability (99.99% Availability)
| Strategy | Details |
|----------|---------|
| **Stateless services** | No server-side session; any instance handles any request |
| **Horizontal auto-scaling** | K8s HPA scales pods based on CPU/request-rate |
| **Multi-AZ deployment** | At least 2 availability zones to survive AZ failure |
| **Read replicas** | Read-heavy show browsing directed to replicas |
| **Redis caching** | Cache show listings (TTL 30s), invalidate on booking |
| **CDN** | Static assets, movie posters, theatre images |
| **Database failover** | Managed PostgreSQL with automated failover (RDS Multi-AZ) |
| **Circuit breaker** | Resilience4j for external integrations (payment, partner APIs) |
| **Graceful degradation** | If cache is down, fall back to DB; if payment gateway is slow, queue the request |

### Availability Budget (99.99%)
- ~52 minutes downtime per year
- Requires: multi-AZ, rolling deploys, health checks, auto-restart, no single point of failure

### Performance Targets
| Metric | Target |
|--------|--------|
| Show search API p95 | < 200 ms |
| Booking API p95 | < 500 ms |
| Concurrent bookings | 1000+ per second per city |

---

## 6. Security — OWASP Top 10

| OWASP Threat | Mitigation |
|-------------|-----------|
| **A01 Broken Access Control** | Role-based auth (CUSTOMER, THEATRE_ADMIN, PLATFORM_ADMIN); endpoint-level `@PreAuthorize` |
| **A02 Cryptographic Failures** | TLS everywhere; sensitive data encrypted at rest; no secrets in code |
| **A03 Injection** | JPA parameterized queries; input validation on all DTOs |
| **A04 Insecure Design** | Threat modelling; principle of least privilege; seat-status checks |
| **A05 Security Misconfiguration** | Spring Security defaults; disable H2 console in prod; CORS whitelist |
| **A06 Vulnerable Components** | Maven dependency check plugin; GitHub Dependabot |
| **A07 Auth Failures** | OAuth 2.0 / JWT with short-lived tokens; rate limiting on login |
| **A08 Software/Data Integrity** | CI/CD pipeline with signed artifacts; DB migrations versioned |
| **A09 Logging & Monitoring** | Structured logging (JSON); audit trail for bookings; alert on anomalies |
| **A10 SSRF** | No user-controlled URLs fetched server-side; network segmentation |

### Additional
- **CSRF**: Stateless JWT → CSRF not applicable for API-only
- **Rate Limiting**: API gateway enforces per-client rate limits
- **Input Sanitization**: Reject unexpected fields; max-length constraints

---

## 7. Integration with Theatre Partners

### Partner Types

| Type | Integration Mode |
|------|-----------------|
| **Digital-first theatres** (existing IT) | REST API / webhook bidirectional sync |
| **New / non-digital theatres** | Admin portal (B2B API) for manual management |

### Integration Architecture
```
┌──────────────┐    REST/Webhook     ┌───────────────────┐
│  Partner IT  │◄───────────────────►│  Partner Adapter  │
│  (POS, ERP)  │                     │  (per-partner)    │
└──────────────┘                     └────────┬──────────┘
                                              │
                                    ┌─────────▼─────────┐
                                    │   Booking Platform │
                                    └───────────────────┘
```

- **Adapter Pattern**: Each partner's API differences are abstracted behind a common interface
- **Async Sync**: Kafka topics for schedule/inventory updates → eventual consistency
- **Partner Code**: Each theatre has a unique `partnerCode` for identity mapping
- **Localization**: Movie titles and metadata can be stored per locale

---

## 8. Payment Gateway Integration

### Recommended Flow
```
1. Customer selects seats → POST /bookings
2. Seats marked BLOCKED (temporary hold, 10 min TTL)
3. Create payment order with gateway (Razorpay / Stripe / Adyen)
4. Redirect customer to payment page
5. Payment webhook → confirm or release
6. On SUCCESS: seats → BOOKED, booking → CONFIRMED
7. On FAILURE/TIMEOUT: seats → AVAILABLE, booking → CANCELLED
```

### Key Principles
- **Idempotency**: Payment callbacks may arrive multiple times; use idempotency key
- **Separation**: Payment is a separate bounded context; booking service calls payment service
- **Reconciliation**: Nightly job reconciles bookings vs. payment records
- **PCI-DSS**: Platform never stores card data; delegate to gateway's hosted checkout

---

## 9. Platform Monetization

| Revenue Stream | Description |
|---------------|-------------|
| **Convenience fee** | Per-ticket fee charged to customers (₹20–50) |
| **Commission** | Percentage of ticket price from theatre partners (5–15%) |
| **Subscription** | Monthly/annual onboarding fee for theatres |
| **Promoted listings** | Theatres pay for premium placement in search results |
| **Advertising** | Banner ads, cross-sell (food, parking, merchandise) |
| **Data insights** | Anonymized demand/trend analytics sold to studios & theatres |

---

## 10. Hosting, Sizing & Cloud Strategy

### Recommended: AWS (adaptable to Azure / GCP)

| Component | AWS Service | Sizing (initial) |
|-----------|-----------|-------------------|
| Compute | EKS (Kubernetes) | 3 nodes, c5.xlarge, multi-AZ |
| Database | RDS PostgreSQL | db.r5.large, Multi-AZ, 100 GB |
| Cache | ElastiCache Redis | cache.r5.large, 2 replicas |
| Object Storage | S3 | Movie posters, assets |
| CDN | CloudFront | Edge caching for static content |
| Message Queue | Amazon MSK (Kafka) | 3 brokers |
| Secrets | Secrets Manager | API keys, DB credentials |
| DNS | Route 53 | Latency-based routing |
| Monitoring | CloudWatch + Grafana | Dashboards, alerts |
| Logging | ELK on EKS or CloudWatch Logs | Centralized logs |

### Multi-Region Strategy (future)
- Active-active in two regions for 99.99% SLA
- Global database (Aurora Global / CockroachDB) for cross-region replication
- CDN for latency optimization

---

## 11. Release Management

### Environments
```
dev → QA → staging → production
```

### Deployment Strategy
| Strategy | Use Case |
|----------|---------|
| **Rolling update** | Default for routine releases |
| **Blue/Green** | Major releases; instant rollback |
| **Canary** | Risky changes; gradual rollout (5% → 25% → 100%) |

### CI/CD Pipeline
```
Code Push → Build (Maven) → Unit Tests → SonarQube →
  Docker Build → Push to ECR → Deploy to Staging →
    Integration Tests → Manual Approval → Production Deploy
```

### Internationalization
- Config per geography: timezone, currency, locale
- Feature flags (LaunchDarkly) for geo-specific features
- Database-level locale support for movie metadata

---

## 12. Monitoring & Observability

### Three Pillars

| Pillar | Tool | Details |
|--------|------|---------|
| **Metrics** | Micrometer + Prometheus + Grafana | API latency, error rate, JVM metrics |
| **Logs** | SLF4J + ELK / CloudWatch | Structured JSON logs, correlation IDs |
| **Traces** | OpenTelemetry + Jaeger | Distributed tracing across services |

### Key Alerts
| Alert | Threshold |
|-------|-----------|
| API error rate | > 1% for 5 min |
| Booking failures | > 5 in 1 min |
| Payment webhook delay | > 2 min |
| DB connection pool exhaustion | > 80% |
| Pod restart | Any unexpected restart |

---

## 13. KPIs

### Business KPIs
| KPI | Target |
|-----|--------|
| Bookings per minute | Track growth trend |
| Search-to-book conversion | > 15% |
| Cancellation rate | < 10% |
| Theatre onboarding time | < 2 business days |
| Revenue per booking | Track and optimize |

### Technical KPIs
| KPI | Target |
|-----|--------|
| API uptime | 99.99% |
| Show search p95 latency | < 200 ms |
| Booking p95 latency | < 500 ms |
| Payment success rate | > 98% |
| Mean time to recovery (MTTR) | < 15 min |

---

## 14. AI Enhancements (Future)

| Feature | Description |
|---------|-------------|
| **Personalized recommendations** | Collaborative filtering based on booking history |
| **Dynamic pricing** | Demand-based price adjustment using ML models |
| **Fraud detection** | Anomaly detection on booking patterns |
| **Chatbot** | NLP-powered customer support and booking assistant |
| **Demand forecasting** | Predict occupancy for theatres to optimize show scheduling |
| **Sentiment analysis** | Analyze reviews to surface quality signals |

---

## 15. Compliance

| Area | Requirement |
|------|-------------|
| **PCI-DSS** | No card data stored; use gateway's hosted checkout |
| **GDPR / Privacy** | Consent management; data export/deletion APIs; anonymization |
| **Data Retention** | Booking records retained for 7 years (financial); personal data purged on request |
| **Audit Logging** | Immutable audit trail for all booking and payment events |
| **Accessibility** | WCAG 2.1 AA for any future UI |

---

## 16. High-Level Project Plan

| Phase | Duration | Deliverables |
|-------|----------|-------------|
| **Phase 1: Foundation** | 2 weeks | Domain model, DB schema, core APIs (browse + book), seed data |
| **Phase 2: Partner Onboarding** | 2 weeks | B2B admin APIs, partner adapter framework, auth (JWT) |
| **Phase 3: Pricing & Offers** | 1 week | Discount engine, pricing API, offer configuration |
| **Phase 4: Payment Integration** | 2 weeks | Payment gateway integration, seat reservation flow, webhooks |
| **Phase 5: Hardening** | 2 weeks | Error handling, validation, rate limiting, security review |
| **Phase 6: Observability** | 1 week | Metrics, logging, tracing, dashboards, alerts |
| **Phase 7: Deployment** | 1 week | Docker, K8s manifests, CI/CD pipeline, staging env |
| **Phase 8: Launch** | 1 week | Load testing, UAT, documentation, go-live |

**Total estimated effort: ~12 weeks for MVP with one city**

### Team Sizing (suggested)
| Role | Count |
|------|-------|
| Backend Engineer | 2–3 |
| Frontend Engineer | 1–2 |
| DevOps / SRE | 1 |
| QA | 1 |
| Product / BA | 1 |

---

## Trade-offs & Future Work

| Current Simplification | Production Evolution |
|----------------------|---------------------|
| H2 in-memory DB | PostgreSQL with replicas |
| No authentication | JWT + OAuth 2.0 with role-based access |
| Synchronous booking | Async reservation + payment confirmation |
| Single service | Microservices: catalog, booking, payment, notification |
| No caching | Redis for show listings, seat availability |
| No message queue | Kafka for partner sync, notifications, event sourcing |
| Seat count per show | Full seat map with row/column coordinates |

