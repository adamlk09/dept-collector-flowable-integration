# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

This file is scoped to **workflow-service**. The monorepo-wide guide lives at `../../CLAUDE.md` (parent BOM, shared-kernel, tenant model, infrastructure, security toggle) — read it for anything not specific to this service.

## What this service is

The BPMN side of the Flowable split (the DMN side is `segmentation-service`). It runs the **debt-collection process engine** and exposes a REST API to start processes, drive human tasks, and inspect runtime/historic state. Port **8094**, dedicated database **`workflow_service`**, base package **`com.mercure.recouvrement.workflow`**.

Note the package split: this service's code is under `com.mercure.recouvrement.workflow`, but shared-kernel classes it depends on (e.g. `TenantContext`) are under `com.deptcollector.shared`. Component scanning reaches them because the Spring Boot app + the kernel's auto-config bridge them; don't "fix" the differing roots.

## Commands

Run from the **backend root** (`../../`), not this directory — this is a child Maven module.

```bash
# Run only this service (rebuilds shared-kernel first via -am)
mvn -pl services/workflow-service -am spring-boot:run

# Test only this service
mvn -pl services/workflow-service test

# Smoke-check a running instance (X-Tenant-Id is mandatory on every call)
curl -H 'X-Tenant-Id: tenant-demo' http://localhost:8094/api/v1/_service
curl -X POST http://localhost:8094/api/v1/workflows/processes \
  -H 'X-Tenant-Id: tenant-demo' -H 'Content-Type: application/json' \
  -d '{"debtId":"DEBT-1001"}'
```

Infrastructure (Postgres/Keycloak/Redis/Kafka) comes from `docker compose up -d` at the backend root.

## The BPMN process (`resources/processes/debt-collection-process.bpmn20.xml`)

Process key **`debtCollectionProcess`**. Flowable auto-deploys this file on startup; there is no manual deploy step.

```
start → validateDebt → segmentDebt → selectStrategy → agentReview (user task)
                                                          │
                                          ┌───────────────┴───────────────┐
                                       flow5                          slaTimer (boundary, PT72H)
                                          │                               │
                                         end                         endEscalated
```

- The three service tasks are wired to Spring beans by **bean name** via `flowable:delegateExpression="${...}"`. Bean names are set explicitly in `@Component("...")` — the BPMN expression and the annotation value must stay in sync.
- `agentReview` is a user task assigned to `${tenantId}`, with a **72-hour SLA boundary timer** (`cancelActivity="true"`) that routes to `endEscalated` on breach. This is why `flowable.async-executor-activate=true` and `FlowableConfig.setAsyncExecutorActivate(true)` matter — timers won't fire without the async executor.

### Delegates (`delegate/`) — currently stubbed

All four `JavaDelegate`s are Spring beans using SLF4J (never `System.out`). The segmentation/validation logic is intentionally placeholder, marked `TODO (M5)`:
- `ValidateDebtDelegate` — will call debt-service via REST; for now sets `debtValid` from a blank check.
- `SegmentDebtDelegate` — will call segmentation-service's DMN; for now hard-codes `segment=STANDARD`. Also sets `segmentExplication` (the matched-rule explanation required by the acceptance criteria).
- `SelectStrategyDelegate` — maps `segment` → `strategy` (`CRITICAL→LEGAL_ACTION`, `HIGH→INTENSIVE_CONTACT`, `MEDIUM→STANDARD_CONTACT`, else `FRIENDLY_REMINDER`).

When implementing the real REST calls, keep the inter-service chain (debt → DMN segmentation → BPMN) and propagate tenant/correlation headers — see `../../CLAUDE.md`.

## Service-layer invariants (`application/CollectionWorkflowService.java`)

- **Tenancy is enforced on every operation**: each method calls `TenantContext.getRequiredTenantId()`, processes are started with `.tenantId(tenantId)`, and every runtime/history/task/job query is filtered by that tenant. Any new method must do the same — never query Flowable across tenants.
- **Idempotent start**: `startProcess` first queries for a running instance with the same `processDefinitionKey + businessKey(=debtId) + tenantId`; if found it returns it with `created=false` (HTTP 200) instead of starting a duplicate (HTTP 201). Preserve this contract.
- `debtId` is the BPMN **business key**; it's also injected as a process variable alongside `tenantId`.

## REST API (`api/CollectionWorkflowController.java`, base `/api/v1/workflows`)

Beyond `POST /processes`, the controller exposes operational endpoints that map directly to Flowable services: active tasks + complete, process history, live variables, incidents (failed jobs with exceptions), dead-letter jobs + retry, and timers + manual trigger. The timer-trigger endpoint (`moveTimerToExecutableJob` + `executeJob`) is the mechanism used to observe the SLA transition without waiting 72h.

## Persistence & schema ownership

Two distinct table sets in the one database — keep the boundary:
- **Flowable engine tables** are created/managed by the engine itself (`flowable.database-schema-update=true`). Never write Flyway migrations for them.
- **Business tables** are owned by Flyway only (`resources/db/migration/`). Currently just `V1__baseline.sql` (the `outbox_event` table for the Outbox pattern). `ddl-auto` is `validate` — Hibernate creates nothing; add tables via new versioned migrations.

Do not switch to H2 or `ddl-auto: update`; both break the validate-against-Flyway contract.

## Tests

`WorkflowApplicationTest` is a `@SpringBootTest` context-load check under the `dev` profile. There is **no H2 and no Testcontainers** wired up, so this test needs a reachable PostgreSQL (`docker compose up -d`) and the Flowable schema. If you add tests that drive the process engine, either reuse a running Postgres or introduce Testcontainers deliberately — don't add H2 as a shortcut, it diverges from the Postgres-only production config.
