# flowable-ui

> **⚠️ Not currently used.** The running setup uses the official `flowable/flowable-ui:6.8.0`
> image declared in `../compose.yaml`, on its **own** `flowable_ui` database, with the Admin app
> pointed at workflow-service's REST API (`/process-api`). This folder is a custom-build
> alternative whose `src/` was never written, and its shared-DB design breaks once the 7.2 engine
> touches the schema (see [Version skew](#version-skew-62-vs-72)). Kept for reference only.

Workflow **designer** for the existing `workflow-service`. It bundles the four Flowable UI
applications (Modeler, Task, Admin, IDM) into a single Spring Boot app and points them at the
**same PostgreSQL engine database** that `workflow-service` uses (`workflow_service`).

Because the database is shared, a BPMN process deployed from the Modeler lands directly in the
engine's deployment tables, so `workflow-service` can run it with **no synchronisation code**:

```java
runtimeService.startProcessInstanceByKey("debtCollectionProcess");
```

This project is **not** a workflow engine. It has no business code, no `RuntimeService` REST APIs,
and no domain services. The engine lives in `workflow-service`.

---

## ⚠️ Read this first — two hard constraints

1. **Flowable 7.x has no UI apps.** The Modeler / Task / Admin / IDM applications were removed
   after Flowable **6.x**; they are not published for 7.x. This project therefore uses the
   **6.8.1** UI starters. As a consequence it is a **Spring Boot 2.7 / Java 17** app — it does
   **not** run on Java 21, unlike the rest of the monorepo. That is why it carries its own parent
   and is built independently (it is not a module of `../pom.xml`).

2. **Version skew on the shared schema.** `workflow-service` runs Flowable **7.2**; this UI runs
   Flowable **6.8**. Both managing the *same* engine schema is not officially supported — see
   [Version skew](#version-skew-62-vs-72) below before running in anything other than dev.

---

## What you get

| App | Path | Purpose |
|---|---|---|
| Modeler | `/flowable-modeler` | Create/edit **BPMN** workflows, **DMN** decision tables and **Forms**; deploy them |
| Task | `/flowable-task` | Start processes, view running instances, work on human tasks |
| Admin | `/flowable-admin` | Inspect deployments, process instances, jobs |
| IDM | `/flowable-idm` | Manage users & groups; login provider for the other apps |

Default URL (local): `http://localhost:8090/flowable-modeler` — login `admin` / `admin`.

---

## Project structure

```
flowable-ui/
├── pom.xml                         # Spring Boot 2.7 parent, 4 Flowable 6.8.1 UI starters, Java 17
├── Dockerfile                      # multi-stage build (Maven 17 → JRE 17)
├── docker-compose.yml              # postgres + flowable-ui (no workflow-service)
├── README.md
└── src/main/
    ├── java/com/mercure/recouvrement/flowableui/
    │   └── FlowableUiApplication.java   # plain @SpringBootApplication entry point
    ├── resources/
    │   └── application.yml              # env-driven DB / IDM / security / port config
    └── docker/
        └── init-db.sql                  # creates the shared `workflow_service` DB
```

---

## Dependencies — what each one is for

| Dependency | Why it is here |
|---|---|
| `flowable-spring-boot-starter-ui-modeler` | The **designer**: BPMN/DMN/Form editors + the "Deploy" action |
| `flowable-spring-boot-starter-ui-task` | Task list app **and the embedded process engine** that writes deployments to the shared DB and reads runtime/history |
| `flowable-spring-boot-starter-ui-admin` | Monitoring UI for deployments, instances and jobs |
| `flowable-spring-boot-starter-ui-idm` | Identity store (users/groups) + form login / SSO for the apps |
| `spring-boot-starter-actuator` | `/actuator/health`, `info`, `metrics` |
| `postgresql` (runtime) | JDBC driver for the shared engine database |

Spring Security is **not** configured by hand — the Flowable IDM starter provides the default
form-login security. (Keycloak/OIDC can be wired in later via the standard Flowable OAuth2
properties without code changes.)

---

## How the UI talks to workflow-service

There is **no direct call** between the two apps. They communicate **through the shared database**:

```
┌────────────────────┐   deploy (RepositoryService)    ┌──────────────────────────┐
│   flowable-ui      │ ──────────────────────────────► │  PostgreSQL              │
│   (Modeler 6.8)    │   ACT_RE_DEPLOYMENT              │  workflow_service        │
│                    │   ACT_RE_PROCDEF                 │  (ACT_* engine tables)   │
└────────────────────┘   ACT_GE_BYTEARRAY              └──────────────────────────┘
                                                                    ▲
                                  startProcessInstanceByKey(...)    │
                            ┌──────────────────────────┐            │
                            │  workflow-service        │ ───────────┘
                            │  (Flowable 7.2 engine)   │
                            └──────────────────────────┘
```

1. A business user designs a process in the **Modeler** and clicks **Deploy**.
2. The Modeler hands the model to the embedded engine's `RepositoryService`, which inserts the
   deployment into `ACT_RE_DEPLOYMENT` / `ACT_RE_PROCDEF` and the BPMN XML into `ACT_GE_BYTEARRAY`.
3. Those rows live in the **same** `workflow_service` database `workflow-service` reads, so its
   engine resolves the new `processDefinitionKey` on the next `startProcessInstanceByKey(...)` —
   no export, no import, no sync job.

Because runtime/history tables are shared too, processes started by `workflow-service` are visible
in the UI's **Task** and **Admin** apps, and vice-versa.

> The DMN tables you design are deployed the same way; `workflow-service`'s `segmentation-service`
> chain (see `../CLAUDE.md`) can evaluate them once they are in the shared engine.

---

## Version skew (6.2 vs 7.2)

The single non-trivial risk. Both engines run schema management (`flowable.database-schema-update`)
against the same `ACT_*` tables, but each Flowable major stamps its own version in
`ACT_GE_PROPERTY (schema.version)`. A 6.8 engine booting against a schema upgraded to 7.2 (or the
reverse) can fail the boot-time version check.

The design/IDM tables (`ACT_DE_*`, `ACT_ID_*`, `ACT_APP_*`) are **exclusive to this UI** —
`workflow-service` never touches them, so they never conflict. The conflict surface is only the
shared engine tables (`ACT_RE_*`, `ACT_RU_*`, `ACT_HI_*`, `ACT_GE_*`).

Recommended handling:

- **Dev / demo:** start the UI **first** on an empty DB so 6.8 creates the full schema, then start
  `workflow-service` (`flowable.database-schema-update: true`) and let 7.2 upgrade the engine
  tables. Good enough to design → deploy → execute.
- **Production:** unify the Flowable major version across both apps (the only fully supported
  option). Since 7.x has no UI, the realistic production patterns are (a) keep the designer on a
  separate schema and promote BPMN/DMN to `workflow-service` as a deploy step, or (b) keep both
  engines on the same 6.8 line. Decide this before going live.

If you hit `FlowableWrongDbException` / a schema-version error on startup, this is the cause.

---

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8090` | Port serving all four UI apps |
| `DB_URL` | `jdbc:postgresql://localhost:5432/workflow_service` | Shared engine DB JDBC URL |
| `DB_USER` | `collector` | DB user |
| `DB_PASSWORD` | `collector` | DB password |
| `IDM_ADMIN_USER` | `admin` | Bootstrapped IDM admin login |
| `IDM_ADMIN_PASSWORD` | `admin` | Bootstrapped IDM admin password |
| `ENGINE_REST_ADDRESS` | `http://localhost` | Address the Admin app uses to reach the engine REST |
| `ENGINE_REST_PORT` | `8090` | Port for the Admin app's engine REST target |
| `LOG_LEVEL_FLOWABLE` | `INFO` | Log level for `org.flowable` |

---

## Run it

### Option A — Docker Compose (self-contained: Postgres + UI)

```bash
docker compose up --build
```

Then open `http://localhost:8090/flowable-modeler` (admin / admin).

### Option B — point at the backend's existing Postgres

If the backend stack (`../compose.yaml`) is already running and owns `workflow_service`, don't start
this compose's Postgres — run only the UI against the existing DB:

```bash
DB_URL=jdbc:postgresql://localhost:5432/workflow_service \
DB_USER=collector DB_PASSWORD=collector \
mvn spring-boot:run
```

### Option C — local JVM (needs Java 17 + a reachable Postgres)

```bash
mvn clean package
DB_URL=jdbc:postgresql://localhost:5432/workflow_service \
java -jar target/flowable-ui.jar
```

> Build/run with **JDK 17**. The 6.8.x UI stack will not start on Java 21.

---

## Smoke test the share

1. In the **Modeler**, create a process with id `debtCollectionProcess` and **Deploy** it.
2. From `workflow-service`, start it:
   ```bash
   curl -X POST http://localhost:8094/api/v1/workflows/processes \
     -H 'X-Tenant-Id: tenant-demo' -H 'Content-Type: application/json' \
     -d '{"debtId":"DEBT-1001"}'
   ```
3. The instance should appear in the UI's **Admin** / **Task** apps — same database, no sync.
