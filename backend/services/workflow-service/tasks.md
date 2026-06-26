# Workflow-Service — Flowable Decision Workflow (Smoke Test)

**Goal:** Prove the end-to-end Flowable chain in `workflow-service` — REST start → BPMN process → service-task delegates → exclusive gateway decision (`score >= 70`) → final strategy returned from history, on PostgreSQL.

**Scope guardrails:** No user tasks, timers, Kafka, email, external HTTP, or async jobs in this iteration. Preserve the base package `com.mercure.recouvrement.workflow`, existing configuration, and `ddl-auto: validate` / Flyway / Flowable schema settings.

> This document defines **milestones only**. Tasks for each milestone will be detailed in a later pass.

---

## Milestones

### M0 — Discovery & Baseline Inspection
**Objective:** Understand the existing service before touching code.
**Scope:** Read `pom.xml`, all Java sources, `FlowableConfig`, the BPMN file, `application.yml`, and existing Flyway migrations. Catalog current process key, endpoint paths, delegate bean names, DTO fields, and the conditional-security pattern.
**Exit criteria:** A confirmed inventory of what exists vs. what the mission requires, and an explicit list of conflicts/divergences to resolve (e.g. current async/human-task design vs. the synchronous smoke-test target).

### M1 — Dependencies & Configuration Validation
**Objective:** Confirm the build and runtime baseline supports the workflow without changes.
**Scope:** Verify Flowable process starter, Spring Web, Validation, PostgreSQL driver, and Spring Boot Test are present and compatible via the parent BOM. Confirm `application.yml` invariants are intact.
**Exit criteria:** No missing or duplicate dependencies; configuration untouched except any unavoidable minor correction, documented.

### M2 — BPMN Process Model
**Objective:** Model the decision workflow in `debt-collection-process.bpmn20.xml`.
**Scope:** Start → Validate → Segment → Exclusive Gateway (`score >= 70` / `score < 70`) → Priority/Standard strategy tasks → End. Stable process key `debtCollectionProcess`, `isExecutable="true"`, unique IDs, valid sequence flows, delegate expressions wired by bean name.
**Exit criteria:** BPMN is valid XML, deploys on startup, and the gateway routes purely on `score`.

### M3 — Delegate Implementation
**Objective:** Implement the four service-task delegates as Spring beans.
**Scope:** `validateDebtDelegate`, `segmentDebtDelegate`, `selectPriorityStrategyDelegate`, `selectStandardStrategyDelegate` — reading/writing process variables (`debtId`, `customerId`, `score`, `segment`, `strategy`, `status`), SLF4J logging, no `System.out`. Decide and document the strategy-delegate approach (preferred: two dedicated delegates alongside the existing one).
**Exit criteria:** Each delegate has a unique, explicit bean name matching the BPMN, sets the correct variables, and validation failures raise a clear exception.

### M4 — API & DTO Layer
**Objective:** Expose `POST /api/v1/workflows/collection/start` with proper contracts.
**Scope:** Request DTO (`debtId`, `customerId`, `score`) with validation (`@NotBlank`, score 0–100); dedicated `StartCollectionProcessResponse`; no Flowable internals leaked through the controller.
**Exit criteria:** Endpoint accepts the documented payload, rejects invalid input cleanly, and returns the response DTO shape.

### M5 — Service Orchestration
**Objective:** Drive the process and assemble the result in `CollectionWorkflowService`.
**Scope:** Inject `RuntimeService` + `HistoryService`; start by process key with variables; after synchronous completion read `segment`, `strategy`, `status` from history. No segment/strategy logic in the service or controller.
**Exit criteria:** Service returns the correct historic variables for both paths; a missing historic variable produces an explicit, meaningful error.

### M6 — Security Alignment
**Objective:** Make the endpoint usable locally without weakening global security.
**Scope:** Honor the existing conditional-security pattern (`app.security.enabled`), ensure it works with `SECURITY_ENABLED=false`, and provide a test profile/convention.
**Exit criteria:** Endpoint reachable locally with security disabled; no blanket `permitAll()` added.

### M7 — Testing
**Objective:** Cover the decision logic and validation.
**Scope:** High-risk path (score 85 → HIGH_RISK / PRIORITY_COLLECTION / COMPLETED), low-risk path (score 40 → LOW_RISK / STANDARD_COLLECTION / COMPLETED), and a validation-failure case. Reuse existing PostgreSQL/Testcontainers setup if present; otherwise document the test approach without large infra changes.
**Exit criteria:** New tests pass, existing tests still pass, and the test data/DB strategy is documented.

### M8 — Verification & Sign-off
**Objective:** Prove the Definition of Done.
**Scope:** Full compile, BPMN deployment check, and the two `curl` smoke tests returning the expected segment/strategy/status. Compile the final report (files created/modified, process key, delegate bean names, endpoint + curl, test results, assumptions/blockers).
**Exit criteria:** Every Definition-of-Done item satisfied; no unrelated files broken.
