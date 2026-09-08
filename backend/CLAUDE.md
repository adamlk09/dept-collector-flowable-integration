# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build and test all modules
mvn test

# Run a single service (example: workflow-service)
mvn -pl services/workflow-service -am spring-boot:run

# Run tests for a single module
mvn -pl services/workflow-service test

# Start infrastructure (PostgreSQL, Keycloak, Redis, Kafka)
docker compose up -d

# Reset databases (destroy and recreate)
docker compose down -v && docker compose up -d

# Verify a running service
curl -H 'X-Tenant-Id: tenant-demo' http://localhost:8094/api/v1/_service
curl http://localhost:8094/actuator/health
```

## Architecture

Maven multi-module project. Module layout:

```
pom.xml                  ← parent BOM (Java 21, Spring Boot 3.5.12, Flowable 7.2)
shared-kernel/           ← cross-cutting library (no Spring Boot app)
api-gateway/             ← Spring Cloud Gateway on port 8080
services/<name>/         ← 25 autonomous microservices, ports 8081+
```

**Port assignment:** services follow declaration order in the root `pom.xml`, starting at 8081. The gateway is 8080. `segmentation-service` is 8092, `assignment-service` is 8093, `workflow-service` is 8094 (check each `application.yml` for `SERVER_PORT`).

### shared-kernel

Not a Spring Boot app — it is a plain library compiled into every service. Key classes:
- `TenantContext` — ThreadLocal storing the mandatory `X-Tenant-Id` per request; `getRequiredTenantId()` throws if missing.
- `TenantContextFilter` — populates `TenantContext` from the `X-Tenant-Id` header.
- `CorrelationFilter` — propagates `X-Correlation-Id` into MDC (`correlationId`).
- `ApiExceptionHandler` — Problem Details error format (RFC 9457).
- `SecurityConfiguration` — JWT validation toggle via `app.security.enabled` (default `false` for local dev).
- `DomainEvent` — base class for domain events (Outbox pattern).

### Flowable engines

Flowable is intentionally split across two services only — never add Flowable dependencies to other services:

| Service | Flowable artifact | Purpose |
|---|---|---|
| `segmentation-service` | `flowable-spring-boot-starter-dmn` | DMN decision tables for debt segmentation |
| `workflow-service` | `flowable-spring-boot-starter-process` | BPMN process engine for recouvrement workflow |

BPMN process files go under `services/workflow-service/src/main/resources/processes/`.  
DMN decision tables go under `services/segmentation-service/src/main/resources/dmn/`.  
Flowable auto-deploys resources found in these directories on startup.

The two engines are chained over REST: `workflow-service`'s `segmentDebt` step calls segmentation-service's DMN at `POST /api/v1/segmentation/execute` (via `SegmentationClient`, base URL `app.segmentation-service.url`, default `http://localhost:8092`), propagating `X-Tenant-Id`/`X-Correlation-Id`. The call has a local fallback so the BPMN keeps running if the DMN service is down.

### Per-service conventions

Each service owns:
- One dedicated PostgreSQL database (`<service_name>` in `init-databases.sql`)
- Flyway migrations under `src/main/resources/db/migration/` (versioned `V1__baseline.sql`, etc.)
- `application.yml` with `app.bounded-context` and `app.capabilities` metadata fields
- `ServiceInfoController` from shared-kernel exposed at `GET /api/v1/_service`

### Infrastructure (compose.yaml)

| Service | Port | Credentials |
|---|---|---|
| PostgreSQL 17 | 5432 | `collector`/`collector` |
| Keycloak 26 | 8180 | `admin`/`admin` |
| Redis 7.4 | 6379 | — |
| Kafka 3.9 (KRaft) | 9092 | — |

### Security

JWT validation is **disabled by default** (`SECURITY_ENABLED=false`). To enable:

```bash
SECURITY_ENABLED=true \
OIDC_JWK_SET_URI=http://localhost:8180/realms/dept-collector/protocol/openid-connect/certs \
mvn -pl services/<name> -am spring-boot:run
```

All requests must carry `X-Tenant-Id`; the kernel filter enforces this regardless of JWT state. No repository may accept data without a tenant context.

## Flowable integration (Adam's scope — ft-setup-flowable)

The full chain to implement: **debt input → DMN segmentation → BPMN process start → human task → history**.

Key acceptance criteria:
- DMN execution returns both the segment and the matched rule(s) (explication).
- BPMN process start returns the process instance ID.
- Idempotent start: repeated calls with the same business key must not create duplicates.
- Timer/SLA transition must be observable via the history API.
- `docker compose up` + service start must auto-deploy all BPMN/DMN artifacts without manual steps.
