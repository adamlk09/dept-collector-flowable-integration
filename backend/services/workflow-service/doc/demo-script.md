# Banking Recovery — demo script

Full pipeline: **case creation → Flowable process → DMN decisions → agent To-Do List → email →
payment/action → escalation → completion**, all backed by the synthetic dataset in
`src/main/resources/demo-data/banking-cases.csv` (15 fictitious cases, no real customer data).

## 0. Start everything

```bash
docker compose up -d          # Postgres, Keycloak, Redis, Kafka, Flowable UI, MailHog
mvn -pl services/segmentation-service -am spring-boot:run   # port 8092
mvn -pl services/workflow-service -am spring-boot:run       # port 8094
mvn -pl services/case-service -am spring-boot:run            # port 8091
mvn -pl services/notification-service -am spring-boot:run    # port 8095
```

MailHog UI (every email the demo sends, no real inbox needed): http://localhost:8025
Flowable Admin UI (live process/CMMN diagrams): http://localhost:8090/flowable-ui
Swagger UI (workflow-service): http://localhost:8094/swagger-ui/index.html

## 1. Batch-ingest the synthetic portfolio (doc §12)

```bash
curl -X POST http://localhost:8094/api/v1/workflows/batch/irregular-portfolio/run \
  -H 'X-Tenant-Id: tenant-demo'
```

Ingests all 15 CSV rows through `IrregularPortfolioBatchJob` → validates → starts one
`bankingRecoveryProcess` per row (idempotent by `debtId`) → each staged case is persisted in
case-service and triggers an automatic email (reminder or high-exposure alert, visible in MailHog).

## 2. The four canonical scenarios (doc §32)

| Case | Overdue / Exposure | Expected |
|---|---|---|
| `CASE-001` | 21j / 3 000 € | Commercial, LOW, automatic SMS/email |
| `CASE-002` | 45j / 8 000 € | Amiable, MEDIUM, Level-1 collector |
| `CASE-003` | 67j / 18 500 € | High exposure, HIGH, senior collector, formal notice |
| `CASE-004` | 120j / 25 000 € | Stage 3, CRITICAL/MAXIMUM, legal team + CMMN case |

Inspect any one directly:

```bash
curl http://localhost:8094/api/v1/workflows/tasks -H 'X-Tenant-Id: tenant-demo'
curl http://localhost:8091/api/v1/cases -H 'X-Tenant-Id: tenant-demo'
```

## 3. Drive a case through its full lifecycle (CASE-003 walkthrough)

```bash
TASK_ID=<taskId for CASE-003 from step 2>

# Agent claims it
curl -X POST http://localhost:8094/api/v1/workflows/tasks/$TASK_ID/assign \
  -H 'X-Tenant-Id: tenant-demo' -H 'Content-Type: application/json' \
  -d '{"assignee":"agent.senior.laila"}'

# Agent records a payment promise (business action — doc §17)
curl -X POST http://localhost:8094/api/v1/workflows/tasks/$TASK_ID/actions/promise-payment \
  -H 'X-Tenant-Id: tenant-demo' -H 'Content-Type: application/json' \
  -d '{"amount":18500,"promiseDate":"2026-09-08"}'

# 7-day timer now racing an external payment message — check it
curl http://localhost:8094/api/v1/workflows/processes/<processInstanceId>/timers -H 'X-Tenant-Id: tenant-demo'

# Customer pays — message correlation resumes the process immediately (doc §22)
curl -X POST http://localhost:8094/api/v1/workflows/cases/CASE-003/payment \
  -H 'X-Tenant-Id: tenant-demo' -H 'Content-Type: application/json' \
  -d '{"amount":18500,"paymentDate":"2026-09-02"}'
```

Confirm in Flowable Admin UI: open the process instance → "Show process diagram" — the
message branch is highlighted straight into `Close Case (Payment Received)`, and a payment
confirmation email appears in MailHog.

## 4. Escalation ladder (doc §23)

```bash
curl -X POST http://localhost:8094/api/v1/workflows/tasks/$TASK_ID/actions/escalate \
  -H 'X-Tenant-Id: tenant-demo' -H 'Content-Type: application/json' \
  -d '{"reason":"Client unresponsive after 3 calls"}'
```

The case moves to the `collectionManagers` queue, `collectorLevel`/`priority` bump to
`SENIOR_COLLECTOR`/`HIGH`, and an escalation email is sent — then routes normally through the
DMN outcome once the senior agent completes it.

## 5. Full history / audit for a case

```bash
curl http://localhost:8094/api/v1/workflows/processes/<processInstanceId>/history -H 'X-Tenant-Id: tenant-demo'
curl http://localhost:8095/api/v1/notifications/email -H 'X-Tenant-Id: tenant-demo'
# Audit trail (outbox_event table, doc §26) — CaseStatusChanged / TaskActionPerformed
```

## 6. What to show live in Flowable Admin UI

1. **Definitions**: `bankingRecoveryProcess` + `bankingCollectionCase` (CMMN) deployed alongside the
   untouched `debtCollectionProcess`.
2. **Process diagram** of an in-flight instance — boundary timer (clock icon on the human task),
   the event-based gateway racing timer vs. message, the CMMN case link on the Stage-3 branch.
3. **CMMN Engine tab**: the discretionary case with its 7 human tasks, closed by completing
   "Close Case" (which auto-terminates every other open task via the exit sentry).
