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

## The BPMN processes (`resources/processes/`)

Two deployment files, auto-deployed on startup (no manual deploy step):
- `debt-collection-process.bpmn20.xml` — the orchestrator, process key **`debtCollectionProcess`**.
- `collection-subprocesses.bpmn20.xml` — the six EPIC-8 sub-processes, each its own process definition.

```
start → validateDebt → segmentDebt → reviewQualification → qualificationGateway ─┬─ LEGAL                  → callLegal      → end
                                       (human task, agent    (routes on            ├─ PRE_LEGAL              → callPreLegal   → end
                                        candidateGroup=       ${qualification})     ├─ PROMISE_BROKEN         → callEscalation → end
                                        collectionAgents)                          ├─ FOLLOW_UP              → callPhone      → end
                                                                                    ├─ CLIENT_INFO_INCOMPLETE → callUpdateInfo → end
                                                                                    ├─ DEBT_CLOSED            → closeFile      → end  (inline)
                                                                                    ├─ INFO_RESEARCH          → infoResearch   → end  (inline)
                                                                                    └─ default (FIRST_CONTACT)→ callReminder   → end
```

- `validateDebt` and `segmentDebt` are wired to Spring beans by **bean name** via `flowable:delegateExpression="${...}"`; the names are set explicitly in `@Component("...")` — BPMN expression and annotation value must stay in sync.
- `reviewQualification` is the process's one wait state: a `userTask` with `flowable:candidateGroups="collectionAgents"`. `qualification`/`qualificationReason`/`segment` are already process variables by the time it's created (set by `segmentDebt`), so a UI can render them on the task with no extra wiring. Completing the task with a `qualification` variable in the payload overrides the routed decision; completing it with none passes the automatic qualification through unchanged.
- `qualificationGateway` is an exclusive gateway routing on the `qualification` variable. `flowDefault` (FIRST_CONTACT and any unmatched value) → `callReminder`.
- The six call activities (`callLegal`, `callPreLegal`, `callEscalation`, `callPhone`, `callUpdateInfo`, `callReminder`) invoke the matching sub-process in `collection-subprocesses.bpmn20.xml` with `inheritVariables="true"`, and map `strategy`/`status`/`workflow` back out so the parent history reflects the outcome. Two non-workflow outcomes (`closeFile`, `infoResearch`) run inline instead of as call activities.
- The processes have no timers. The async executor is still enabled (`FlowableConfig` / `flowable.async-executor-activate=true`) and the controller exposes timer/job endpoints (see below) as operational tooling for when timed steps are added.

### Delegates (`delegate/`)

- `ValidateDebtDelegate` — Spring bean (`@Component`), SLF4J logging. Validation logic still a placeholder.
- `SegmentDebtDelegate` (`@Component("segmentDebtDelegate")`) — the qualification engine (see `doc/segmentation.md`). It (1) resolves the EPIC 1–6 facts through `facts/CollectionFactsResolver` (per-EPIC providers, currently stubbed to echo the request), then (2) qualifies them by calling segmentation-service's DMN via `client/SegmentationClient`, **falling back to a local engine** (`qualifyLocally`, EPIC-7 cases in priority order) if the DMN call fails. Sets `qualification`, `qualificationReason`, the routed `segment` (`HIGH_RISK`/`LOW_RISK`), the resolved fact variables, and `qualificationSource` (`DMN`/`LOCAL`). Never throws on DMN failure — keep that contract.
- `ApplyStrategyDelegate` — used via **`flowable:class`** (not a Spring bean) so Flowable creates a fresh instance per execution and field injection is safe. Each sub-process / inline task injects its own `strategy`/`status`/`workflow` literals, keeping per-qualification behavior declared in the BPMN rather than duplicated in Java.
- `SelectStrategyDelegate` (`@Component("selectStrategyDelegate")`) is currently **dead code** — not wired into either BPMN file (superseded by `ApplyStrategyDelegate`). Don't assume it runs; confirm against the BPMN before relying on it or extending it.

The inter-service chain (BPMN → DMN segmentation) and tenant/correlation header propagation are described in `../../CLAUDE.md`; `SegmentationClient` already implements both.

## Service-layer invariants (`application/CollectionWorkflowService.java`)

- **Tenancy, but not via Flowable's native tenant column**: the BPMN/DMN auto-deploy to the default (tenantless) deployment, so process instances/tasks are never tagged with a native Flowable tenant — doing so makes Flowable try to resolve a definition "for tenant X", which doesn't exist, and throw. Instead `tenantId` is carried as a **process variable**, and every per-tenant query filters on it (`processVariableValueEquals("tenantId", tenantId)` for task queries; `HistoricProcessInstanceQuery` for the dedup check). A single `processInstanceId`/`taskId` lookup doesn't re-filter by tenant — the boundary is that it was only ever handed back to the tenant that started it. Any new query must follow the same pattern — never call `.tenantId(...)` / `.taskTenantId(...)` / `.processInstanceTenantId(...)` here.
- **Idempotent start**: `startProcess` first queries **history** (not runtime — the process has a wait state now, but a retry could still land after the instance moved further/completed) for an instance with the same `processDefinitionKey + businessKey(=debtId)`; if found it returns it with `created=false` (HTTP 200) instead of starting a duplicate (HTTP 201). Preserve this contract.
- `debtId` is the BPMN **business key**; it's also injected as a process variable alongside `tenantId`.

## REST API (`api/CollectionWorkflowController.java`, base `/api/v1/workflows`)

Two start endpoints:
- `POST /collection/start` (`startCollectionProcess`) — **synchronous**. The process runs in the calling thread up to its one wait state, the `reviewQualification` user task, then returns. `qualification`/`qualificationReason`/`segment` are already set (by `segmentDebt`) and returned; `strategy`/`status` come back `null` until an agent completes the task and the process runs on to a call activity or end event. Always 201. (Not idempotent — use it to kick off the demo/full-chain path, then drive the task through the endpoints below.)
- `POST /processes` (`startProcess`) — idempotent start (see invariants above); 201 on create, 200 if a running instance already exists. Also stops at `reviewQualification`.

Driving the human task: `GET /tasks` (active tasks for the tenant), `POST /tasks/{taskId}/assign` (`{"assignee":"..."}`, `TaskService.setAssignee`), `POST /tasks/{taskId}/complete` (optional `{"variables":{...}}` — include a `qualification` override here if the agent disagrees with the automatic routing). Completing the task resumes the process through `qualificationGateway` to its call activity/inline task and on to the end event.

The remaining endpoints map directly to Flowable services: process history, live variables (read from history — the instance may have already completed), incidents (failed jobs with exceptions), dead-letter jobs + retry, and timers + manual trigger. The timer-trigger endpoint (`moveTimerToExecutableJob` + `executeJob`) lets you fire a timer without waiting for its due date — kept for when timed/SLA steps are added (the current processes deploy none).

## Second process: `bankingRecoveryProcess` (`resources/processes/banking-recovery-process.bpmn20.xml`)

Independent of `debtCollectionProcess` above — deployed alongside it, never touches it. Models the bank-specific recovery flow end to end, exercising every Flowable construct in this service (see `precess-banking-recovery.md` for the original brief; `doc/demo-script.md` for a full walkthrough):

```
extractIrregularAccount → checkReachability → stageRecovery → assignToAgent (userTask,
  candidateGroups="${recoveryTeam}", boundary timer P2D → sendReminderNotice)
  → escalationGateway (${escalationRequested}) ─┬─ escalateToSenior (userTask, bumps
  │                                                collectorLevel/priority via a TaskListener)
  └─ default ──────────────────────────────────┴─→ stageGateway (${stage})
       STAGE3_LITIGATION → startLegalCase (opens bankingCollectionCase CMMN) → applyLegalTransfer → end
       default            → applyRecoveryAction → eventGatewayWait ─┬─ timer P7D → paymentGateway → closeCase/escalateCase → end
                                                                     └─ message "paymentReceivedMessage" → closeCase → end
```

- `stageRecovery` (`StageRecoveryDelegate`) calls segmentation-service's 4-decision DMN chain (`SegmentationClient.stageRecovery`, see segmentation-service's `CollectionDecisionService`), falls back to an equivalent local engine — never throws. Sets both the legacy vocabulary (`stage`/`priority`/`action`, values unchanged since Milestone 1 so the gateway conditions never needed to change) and the additive Milestone-2 vocabulary (`ifrsStage`/`collectionPhase`/`priorityScore`/`recommendedAction`/`collectorLevel`/`customerSegment`). Also persists a case snapshot in case-service and sends the automatic reminder/high-exposure-alert email — both best-effort, via `CaseServiceClient`/`NotificationClient`.
- **Message correlation** (`paymentReceivedMessage`): `POST /api/v1/workflows/cases/{caseId}/payment` → `CollectionWorkflowService.correlatePayment` finds the waiting execution via `messageEventSubscriptionName` (Flowable 7.2's `RuntimeService` has no fluent correlation builder) filtered by the `debtId` variable, then `messageEventReceived`. Sends the payment-confirmation email.
- **CMMN**: `resources/cases/bankingCollectionCase.cmmn.xml` — 7 discretionary human tasks (investigate, request documents, manager approval, formal notice, escalate to legal, contact customer, close), any order. `StartLegalCaseDelegate` starts it via `CmmnRuntimeService` (Flowable has no native BPMN→CMMN call element). Completing "Close Case" auto-terminates the whole case via its `exitCriterion`/`sentry`. CMMN engine is already on the classpath transitively via `flowable-spring-boot-starter-rest` — no extra Maven dependency needed; auto-deploy location is the Flowable default `classpath*:/cases/`.
- **Business actions** (`CollectionWorkflowController`, `/tasks/{taskId}/actions/*`): `promise-payment`, `payment-plan`, `contact-customer`, `formal-notice`, `escalate`, `close` — thin wrappers that set variables and call the existing generic `completeTask`/`formalNoticeAction`/`escalateAction`. `formal-notice`/`escalate` additionally send an agent-triggered email.
- **Escalation ladder**: completing `assignToAgent` with `escalationRequested=true` (the `escalate` action does this) routes to `escalateToSenior` (queue `collectionManagers`) instead of straight to `stageGateway`; a task listener on its `create` event bumps `collectorLevel`/`collectorGroup`/`priority`/`collectionPhase` before the agent even sees it, then flows back into the same `stageGateway` as the normal path.
- **Gotcha**: exclusive-gateway conditions on a variable that was never set (e.g. `${escalationRequested == true}` when nothing ever assigned it) throw `PropertyNotFoundException`, not a null-safe false — `BankingRecoveryWorkflowService.buildVariables` defaults `escalationRequested`/`paymentReceived` to `false` up front so any business action that doesn't touch them still evaluates cleanly.
- **DI/diagram**: unlike `debtCollectionProcess`, this file *does* carry a `bpmndi:BPMNDiagram` section, so "Show process diagram" renders in Flowable Admin UI. Keep it in sync when editing the process — Flowable doesn't validate visual sanity, only that referenced element ids exist.

### Spring Batch ingestion (`batch/`)

`IrregularPortfolioBatchJob` (`IrregularPortfolioBatchJobConfig` + `BankingCaseRow`/`IrregularPortfolioItemProcessor`/`IrregularPortfolioItemWriter`) reads `resources/demo-data/banking-cases.csv`, validates each row (skip-on-error, `faultTolerant()`), and calls the existing idempotent `BankingRecoveryWorkflowService.startProcess` per row. Triggered manually via `POST /api/v1/workflows/batch/irregular-portfolio/run` (not a cron — this is a demo/ops trigger). Spring Batch's own `JobRepository` tables are auto-created via `spring.batch.jdbc.initialize-schema: always` in `application.yml` — separate mechanism from Flyway (business tables) and from Flowable's own schema management; don't try to unify them.

### Audit (`audit/OutboxEventWriter.java`)

Writes into the existing (`shared-kernel`-defined) `outbox_event` table — `CaseStatusChanged` from `StageRecoveryDelegate`, `TaskActionPerformed` from `CollectionWorkflowService.completeTask`. Best-effort (catches and logs, never throws) — audit is observability, not a transactional guarantee here. No downstream consumer wired up yet (audit-service is a stretch-goal follow-up); the table alone already makes every case/task change queryable.

### Cross-service clients (`client/`)

`CaseServiceClient` (→ case-service :8091, `POST /api/v1/cases`) and `NotificationClient` (→ notification-service :8095, `POST /api/v1/notifications/email`) — both best-effort/never-throw, same pattern as `SegmentationClient`. Base URLs configurable via `app.case-service.url` / `app.notification-service.url`.

## Persistence & schema ownership

Two distinct table sets in the one database — keep the boundary:
- **Flowable engine tables** are created/managed by the engine itself (`flowable.database-schema-update=true`). Never write Flyway migrations for them.
- **Business tables** are owned by Flyway only (`resources/db/migration/`). Currently just `V1__baseline.sql` (the `outbox_event` table for the Outbox pattern). `ddl-auto` is `validate` — Hibernate creates nothing; add tables via new versioned migrations.
- **Spring Batch tables** (`batch_job_*`, `batch_step_*`) are neither — auto-created by `spring.batch.jdbc.initialize-schema: always`, a third, independent mechanism.

Do not switch to H2 or `ddl-auto: update`; both break the validate-against-Flyway contract.

**Known environment quirk on this machine**: a native Windows PostgreSQL service also listens on port 5432 alongside the project's Docker container, and `localhost` resolution can hit either. If a service fails to start with `database "..._service" does not exist` despite `docker exec <postgres-container> psql -c '\l'` showing it, check the native instance too (`psql -h 127.0.0.1 -U collector -d postgres -c '\l'`) — that's usually the one actually being hit.

## Tests

`WorkflowApplicationTest` is a `@SpringBootTest` context-load check; `BankingRecoveryProcessIntegrationTest` drives `bankingRecoveryProcess` through all four DMN scenarios, task claim/complete, a business action, the escalation ladder, and CMMN case creation. Both run under the `dev` profile. There is **no H2 and no Testcontainers** wired up, so tests need a reachable PostgreSQL (`docker compose up -d`) and the Flowable schema — and the app must NOT already be running on port 8094 (stop it first, `@SpringBootTest` binds the same port). If you add tests that drive the process engine, either reuse a running Postgres or introduce Testcontainers deliberately — don't add H2 as a shortcut, it diverges from the Postgres-only production config.
