# CONTINUE AND COMPLETE THE FLOWABLE BANKING COLLECTION WORKFLOW

## ROLE

You are a **Senior Java / Spring Boot / Flowable Workflow Architect**.

You are working inside an **existing Spring Boot 3 microservice project** that already contains a first implementation of a banking debt-collection workflow using **Flowable**.

The previous implementation created only the initial **BPMN process**.

Your job is now to **continue the existing implementation and make the workflow fully executable and demonstrable end-to-end**.

DO NOT throw away the existing implementation.

First inspect the entire repository and understand what Claude already created.

Then complete everything that is missing.

---

# 1. FIRST: ANALYZE THE EXISTING PROJECT

Before modifying anything:

* Inspect the complete project structure.
* Inspect `pom.xml`.
* Inspect all Flowable dependencies.
* Inspect Spring Boot configuration.
* Inspect existing BPMN files.
* Inspect existing DMN files, if any.
* Inspect CMMN files, if any.
* Inspect Java entities.
* Inspect controllers.
* Inspect services.
* Inspect repositories.
* Inspect configuration.
* Inspect application.yml/application.properties.
* Inspect existing REST APIs.
* Inspect existing database configuration.
* Inspect existing Docker configuration.
* Inspect existing frontend/API contracts if available.

Find exactly what was already implemented.

Create an internal implementation plan based on the existing architecture.

DO NOT duplicate existing functionality.

DO NOT rename existing APIs or classes unnecessarily.

DO NOT break existing functionality.

---

# 2. TARGET ARCHITECTURE

The final architecture must look conceptually like this:

```text
                  BANKING / CREDIT SYSTEM
                           |
                           v
                 IRREGULAR PORTFOLIO
                           |
                           v
                 BATCH / REST INGESTION
                           |
                           v
                QUALIFICATION SERVICE
                           |
                           v
                  FLOWABLE PROCESS
                           |
              +------------+-------------+
              |                          |
              v                          v
           DMN ENGINE                BUSINESS DATA
              |                          |
              v                          |
       SEGMENTATION/RULES                |
              |                          |
              +------------+-------------+
                           |
                           v
                    PRIORITIZATION
                           |
                           v
                    ROUTING / ACL
                           |
                           v
                  FLOWABLE USER TASK
                           |
                           v
                  COLLECTION AGENT
                           |
             +-------------+-------------+
             |             |             |
             v             v             v
          PROMISE      PAYMENT PLAN    ESCALATE
             |             |             |
             v             v             v
          MONITOR       MONITOR       LEGAL
             |             |             |
             +-------------+-------------+
                           |
                           v
                        CLOSE
```

The implementation must support this architecture.

---

# 3. FLOWABLE COMPONENTS TO IMPLEMENT

The project must use the appropriate Flowable components:

### BPMN

Use BPMN for process orchestration.

### DMN

Use DMN for business decisions/rules.

### CMMN

Use CMMN where case-management behavior is more appropriate, especially for complex collection/legal cases.

### User Tasks

Use User Tasks for collection-agent work.

### Service Tasks

Use Service Tasks for communication with Spring Boot services.

### Exclusive Gateways

Use gateways for decision routing.

### Timer Events

Use timers for payment promises, reminders, follow-ups and escalations.

### Boundary Events

Use boundary timers where appropriate.

### Message Events

Use message events when external business events need to resume a workflow.

### Async processing

Use Flowable async execution where appropriate.

---

# 4. COMPLETE BPMN PROCESS

Create/complete an executable BPMN process.

Process name:

```text
Banking Debt Collection Process
```

Process key:

```text
bankingDebtCollectionProcess
```

The process should contain at least:

```text
START
  |
  v
Receive Collection Case
  |
  v
Validate Customer / KYC / Contactability
  |
  v
DMN - Customer Segmentation
  |
  v
DMN - Collection Strategy
  |
  v
DMN - Priority Calculation
  |
  v
Assign Collection Strategy
  |
  v
Route to Appropriate Collector
  |
  v
Create Agent User Task
  |
  v
Agent Collection Action
  |
  v
Gateway
  |
  +--> PROMISE_OF_PAYMENT
  |
  +--> PAYMENT_PLAN
  |
  +--> CONTACT_CUSTOMER
  |
  +--> SEND_FORMAL_NOTICE
  |
  +--> ESCALATE_TO_SENIOR
  |
  +--> ESCALATE_TO_LEGAL
  |
  +--> CLOSE_CASE
```

Make the BPMN executable, not just visually correct.

---

# 5. PROCESS VARIABLES

Define a coherent process-variable model.

At minimum:

```text
caseId
customerId
customerName

customerType

daysOverdue
globalExposure

kycValid
phoneValid
addressValid
contactable

ifrsStage

collectionSegment
collectionPhase

priority
priorityScore

recommendedAction

collectorLevel
collectorGroup
collectorId

promiseAmount
promiseDate

paymentPlanAmount
paymentPlanInstallments

lastContactDate
nextActionDate

escalationReason

caseStatus
```

Use typed values consistently.

Avoid random string values where an enum or structured object is more appropriate.

---

# 6. DMN DECISION TABLES

Create actual executable `.dmn` files.

Do NOT merely document the rules.

Create DMN decisions that Flowable can execute.

## DMN 1 — Customer Segmentation

Decision:

```text
customerSegmentationDecision
```

Inputs:

```text
customerType
globalExposure
daysOverdue
```

Outputs:

```text
customerSegment
```

Example:

```text
PARTICULIER
PROFESSIONNEL
ENTREPRISE
```

---

# 7. DMN 2 — IFRS / COLLECTION STAGING

Decision:

```text
collectionStagingDecision
```

Inputs:

```text
daysOverdue
globalExposure
```

Outputs:

```text
ifrsStage
collectionPhase
```

Example demonstration rules:

```text
1-30 days
    -> STAGE_2
    -> COMMERCIAL

31-60 days + exposure < 15000
    -> STAGE_2
    -> AMIABLE

31-90 days + exposure >= 15000
    -> STAGE_2
    -> HIGH_EXPOSURE

61-90 days + exposure < 15000
    -> STAGE_2
    -> PRE_CONTENTIOUS

>90 days
    -> STAGE_3
    -> LEGAL
```

These are demonstration rules and must be clearly configurable.

---

# 8. DMN 3 — PRIORITY CALCULATION

Decision:

```text
collectionPriorityDecision
```

Inputs:

```text
daysOverdue
globalExposure
collectionPhase
```

Outputs:

```text
priority
priorityScore
```

Example:

```text
LOW
MEDIUM
HIGH
CRITICAL
```

Do not hard-code all this logic inside Java.

The purpose is to demonstrate that business rules are externalized into DMN.

---

# 9. DMN 4 — COLLECTION ACTION

Create:

```text
collectionActionDecision
```

Inputs:

```text
daysOverdue
globalExposure
priority
collectionPhase
```

Outputs:

```text
recommendedAction
collectorLevel
```

Example:

```text
SMS_EMAIL
COLLECTOR_LEVEL_1
```

```text
PHONE_CALL
COLLECTOR_LEVEL_1
```

```text
FORMAL_NOTICE
SENIOR_COLLECTOR
```

```text
LEGAL_ESCALATION
LEGAL_TEAM
```

---

# 10. DECISION SERVICE

Create a clean Spring Boot service responsible for interacting with Flowable DMN.

For example:

```java
CollectionDecisionService
```

Responsibilities:

* Execute DMN decisions.
* Convert process variables into DMN inputs.
* Return decision results.
* Validate decision outputs.
* Keep decision execution isolated from controllers.

Do not duplicate DMN business rules inside Java.

---

# 11. CMMN

Introduce CMMN for cases requiring flexible/manual handling.

Create a case model such as:

```text
bankingCollectionCase
```

Use it for complex situations such as:

```text
PRE_CONTENTIOUS
LEGAL
HIGH_EXPOSURE
CUSTOMER_DISPUTE
PAYMENT_PLAN_FAILURE
```

The CMMN case should support discretionary/manual actions where the exact sequence cannot always be predetermined.

Examples:

```text
Investigate customer
Request additional documents
Contact customer
Request manager approval
Send formal notice
Escalate to legal
Close case
```

Do not force CMMN into parts that are naturally deterministic BPMN processes.

Use BPMN for orchestration and CMMN for case management.

---

# 12. BATCH PROCESSING

Create a Spring Batch process that simulates/exposes the ingestion of an irregular banking portfolio.

Example:

```text
IrregularPortfolioBatchJob
```

Flow:

```text
Database / CSV / API
       |
       v
Read irregular customers
       |
       v
Validate records
       |
       v
Calculate basic attributes
       |
       v
Create collection cases
       |
       v
Start Flowable process instance
```

The batch must be restartable and fault tolerant.

Use Spring Batch best practices.

Create:

```text
Job
Step
ItemReader
ItemProcessor
ItemWriter
```

where appropriate.

Do NOT create one giant batch class.

---

# 13. COLLECTION CASE ENTITY

If the project does not already have one, create a proper domain model.

Example:

```text
CollectionCase
```

Suggested fields:

```text
id
caseReference
customerId
customerType
daysOverdue
globalExposure
ifrsStage
collectionSegment
priority
priorityScore
status
assignedCollector
createdAt
updatedAt
```

Use the existing project's persistence conventions.

Do not introduce another database technology unnecessarily.

---

# 14. FLOWABLE PROCESS INSTANCE MANAGEMENT

Implement a service:

```text
CollectionProcessService
```

Responsibilities:

```text
startProcess()
getProcessInstance()
getProcessStatus()
getProcessVariables()
cancelProcess()
suspendProcess()
resumeProcess()
```

When starting a process, pass the required business variables.

Example:

```text
POST /api/collection/cases/{caseId}/start
```

The endpoint should start the corresponding Flowable process instance.

---

# 15. FLOWABLE USER TASK MANAGEMENT

Implement task APIs.

Required operations:

```text
GET /api/collection/tasks
GET /api/collection/tasks/{taskId}
POST /api/collection/tasks/{taskId}/claim
POST /api/collection/tasks/{taskId}/complete
POST /api/collection/tasks/{taskId}/delegate
```

Support filters:

```text
assignee
candidateGroup
priority
status
caseId
customerId
```

Example:

```text
GET /api/collection/tasks?candidateGroup=SENIOR_COLLECTOR
```

Use Flowable's TaskService instead of creating a parallel task engine.

---

# 16. AGENT TO-DO LIST

Expose an API suitable for Angular.

Example:

```text
GET /api/collection/my-tasks
```

Response should contain useful information such as:

```json
{
  "taskId": "...",
  "taskName": "Handle Collection Case",
  "caseId": "...",
  "customerId": "...",
  "priority": "HIGH",
  "priorityScore": 85,
  "collectionPhase": "HIGH_EXPOSURE",
  "recommendedAction": "FORMAL_NOTICE",
  "dueDate": "...",
  "createdAt": "..."
}
```

The frontend should not need to understand Flowable internals.

---

# 17. TASK COMPLETION ACTIONS

Do not create one generic "complete" endpoint only.

Support business actions.

For example:

```text
POST /api/collection/tasks/{taskId}/actions/promise-payment

POST /api/collection/tasks/{taskId}/actions/payment-plan

POST /api/collection/tasks/{taskId}/actions/contact-customer

POST /api/collection/tasks/{taskId}/actions/formal-notice

POST /api/collection/tasks/{taskId}/actions/escalate

POST /api/collection/tasks/{taskId}/actions/close
```

Each action should:

1. Validate the action.
2. Update business data.
3. Set Flowable process variables.
4. Complete the current Flowable task.
5. Allow the BPMN process to continue.

---

# 18. EMAIL INTEGRATION

This is IMPORTANT.

The process must be able to send emails.

Integrate Spring Boot Mail.

Use:

```text
spring-boot-starter-mail
```

Create:

```text
EmailService
```

For example:

```java
sendCollectionReminder(...)
sendPaymentPromiseConfirmation(...)
sendFormalNoticeNotification(...)
sendEscalationNotification(...)
```

DO NOT send email directly from controllers.

Use a dedicated service.

---

# 19. FLOWABLE EMAIL SERVICE TASK

Connect the BPMN process to the email service.

For example:

```text
Flowable
   |
   v
Service Task
   |
   v
CollectionNotificationService
   |
   v
EmailService
   |
   v
SMTP
```

The workflow should be able to send:

### Reminder

```text
Subject:
Payment Reminder - Collection Case {caseId}
```

### High exposure alert

```text
Subject:
HIGH PRIORITY Collection Case {caseId}
```

### Formal notice notification

```text
Subject:
Formal Notice - Collection Case {caseId}
```

### Escalation

```text
Subject:
Collection Case Escalated to Legal
```

Use templates instead of hard-coded HTML inside Java classes.

Create email templates.

If real SMTP credentials are not available, provide a development configuration using a local/test SMTP server and make the system fail gracefully.

NEVER commit real credentials.

---

# 20. AUTOMATIC EMAIL VS USER ACTION

Distinguish:

```text
AUTOMATIC EMAIL
```

from:

```text
AGENT-TRIGGERED EMAIL
```

For example:

```text
Case created
    ↓
Automatic email
    ↓
Agent task
```

and:

```text
Agent clicks "Formal Notice"
    ↓
Business action
    ↓
Email
    ↓
Workflow continues
```

---

# 21. TIMER / FOLLOW-UP MECHANISM

Implement realistic timers.

Example:

```text
Promise of payment
       |
       v
Wait 7 days
       |
       v
Check payment
       |
       +---- PAID ----> Close
       |
       +---- NOT PAID -> Reminder
```

Another:

```text
Agent task
       |
       v
Due date approaching
       |
       v
Reminder
       |
       v
Escalation if overdue
```

Use BPMN timer events where appropriate.

Do not implement timers using `Thread.sleep()`.

---

# 22. PAYMENT EVENT

Support an external payment event.

Example:

```text
POST /api/collection/cases/{caseId}/payment
```

Input:

```json
{
  "amount": 2500,
  "paymentDate": "2026-09-01"
}
```

The API should:

1. Register the payment.
2. Update the collection case.
3. Correlate a Flowable message/event where applicable.
4. Continue the waiting workflow.

Use Flowable message correlation mechanisms rather than polling unnecessarily.

---

# 23. ESCALATION

Implement automatic escalation.

Examples:

```text
Agent Level 1
      |
      | high exposure
      v
Senior Collector
      |
      | unresolved
      v
Pre-contentious
      |
      | unresolved
      v
Legal
```

The escalation must update:

```text
collectorGroup
collectorLevel
priority
collectionPhase
```

and create the appropriate Flowable task.

---

# 24. REST API

Create clean REST controllers.

Suggested structure:

```text
/api/collection/cases
/api/collection/tasks
/api/collection/processes
/api/collection/decisions
/api/collection/payments
/api/collection/notifications
```

Minimum APIs:

```text
POST   /api/collection/cases
GET    /api/collection/cases
GET    /api/collection/cases/{id}

POST   /api/collection/cases/{id}/start

GET    /api/collection/processes/{processInstanceId}

GET    /api/collection/tasks
GET    /api/collection/tasks/{taskId}

POST   /api/collection/tasks/{taskId}/claim
POST   /api/collection/tasks/{taskId}/complete

POST   /api/collection/tasks/{taskId}/actions/promise-payment
POST   /api/collection/tasks/{taskId}/actions/payment-plan
POST   /api/collection/tasks/{taskId}/actions/contact-customer
POST   /api/collection/tasks/{taskId}/actions/formal-notice
POST   /api/collection/tasks/{taskId}/actions/escalate
POST   /api/collection/tasks/{taskId}/actions/close

POST   /api/collection/cases/{id}/payment

GET    /api/collection/cases/{id}/history
```

Use DTOs.

Do not expose JPA entities directly from controllers.

---

# 25. FLOWABLE HISTORY

Expose workflow history.

For a case, I should be able to see:

```text
Process started
       ↓
KYC validation
       ↓
DMN segmentation
       ↓
Priority calculated
       ↓
Task assigned
       ↓
Agent claimed task
       ↓
Customer contacted
       ↓
Promise created
       ↓
Payment received
       ↓
Case closed
```

Create:

```text
GET /api/collection/cases/{id}/history
```

Use Flowable history APIs.

---

# 26. AUDIT TRAIL

Every important action must be traceable.

Record:

```text
caseId
user
action
timestamp
oldStatus
newStatus
comment
```

Do not rely only on application logs.

If the project already has an audit mechanism, integrate with it.

---

# 27. SWAGGER / OPENAPI

Document all APIs.

For every endpoint provide:

* Description
* Request DTO
* Response DTO
* HTTP status
* Example request
* Example response

The project should expose Swagger/OpenAPI correctly.

---

# 28. ERROR HANDLING

Implement proper error handling.

Handle:

```text
Case not found
Task not found
Process not found
Task already completed
Task not assigned
Invalid action
Invalid payment
Invalid process variables
DMN decision failure
Email failure
Flowable engine failure
```

Do not return stack traces to API clients.

Use appropriate HTTP status codes.

---

# 29. CONFIGURATION

Externalize configuration.

Example:

```yaml
flowable:
  database-schema-update: true
  history-level: audit
  async-executor-activate: true

spring:
  mail:
    host: ${MAIL_HOST:localhost}
    port: ${MAIL_PORT:1025}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
```

Do not hard-code secrets.

Use environment variables.

---

# 30. DATABASE

Use the existing project's database.

Do not create an unnecessary second database.

Separate:

```text
Business data
```

from:

```text
Flowable runtime/history data
```

where the existing architecture supports this.

Do not manually create Flowable tables if Flowable is already configured to manage them.

---

# 31. DEMO DATA

Create a realistic dataset for presentation.

At least:

```text
CASE-001
1-30 days
low exposure
LOW

CASE-002
45 days
8,000
MEDIUM

CASE-003
67 days
18,500
HIGH

CASE-004
120 days
25,000
CRITICAL
```

Starting these cases should demonstrate different DMN paths.

---

# 32. DEMO SCRIPT

Create a simple demo scenario.

### Scenario 1

```text
CASE-001
21 days
3,000
```

Expected:

```text
Commercial
Low priority
Automatic SMS/email
```

### Scenario 2

```text
CASE-002
45 days
8,000
```

Expected:

```text
Amicable
Medium priority
Level 1 collector
```

### Scenario 3

```text
CASE-003
67 days
18,500
```

Expected:

```text
High exposure
High priority
Senior collector
Formal notice
```

### Scenario 4

```text
CASE-004
120 days
25,000
```

Expected:

```text
Stage 3
Legal
Critical
Legal team
```

---

# 33. FLOWABLE MODELER FILES

The repository must contain the executable artifacts.

Expected examples:

```text
src/main/resources/processes/
    banking-debt-collection.bpmn20.xml

src/main/resources/dmn/
    customer-segmentation.dmn
    collection-staging.dmn
    collection-priority.dmn
    collection-action.dmn

src/main/resources/cmmn/
    banking-collection-case.cmmn.xml

src/main/resources/templates/
    collection-reminder.html
    formal-notice.html
    escalation.html
    payment-confirmation.html
```

Adapt paths to the existing project.

Do not create duplicate files if equivalents already exist.

---

# 34. FLOWABLE DEPLOYMENT

Make sure the BPMN, DMN and CMMN resources are automatically deployed by Flowable when Spring Boot starts.

Verify:

```text
Process Definitions
DMN Decisions
CMMN Case Definitions
```

are actually deployed.

Do not assume that putting files in resources is enough.

Verify deployment programmatically.

---

# 35. AUTOMATED TESTS

Create integration tests.

Tests must verify:

### BPMN

```text
process starts
process variables are created
service tasks execute
user task is created
gateway routing works
process completes
```

### DMN

Test multiple decision inputs.

For example:

```text
21 / 3000
45 / 8000
67 / 18500
120 / 25000
```

### Tasks

Test:

```text
claim
complete
escalate
```

### Email

Test that email service is invoked.

Do not require a real external SMTP server for unit tests.

### Payment

Test:

```text
promise
payment
message correlation
workflow continuation
```

---

# 36. IMPORTANT ARCHITECTURAL RULES

Do NOT:

* Put business rules directly inside controllers.
* Put DMN logic inside Java if it belongs in DMN.
* Use `Thread.sleep()`.
* Hard-code SMTP credentials.
* Hard-code collector IDs.
* Duplicate Flowable task tables in your own database.
* Expose Flowable internal entities directly through REST.
* Create random REST endpoints without a clear domain structure.
* Rewrite the project unnecessarily.
* Remove the existing BPMN implementation.
* Change existing API signatures unless absolutely necessary.

DO:

* Reuse existing services.
* Reuse existing entities.
* Reuse existing configuration.
* Follow the existing package structure.
* Keep Flowable orchestration separate from domain logic.
* Use DTOs.
* Use transactions where appropriate.
* Add validation.
* Add integration tests.
* Keep configuration externalized.
* Make the workflow executable.

---

# 37. FINAL ACCEPTANCE CRITERIA

Do not consider the task complete until all of the following work:

```text
[ ] Existing project analyzed
[ ] Existing BPMN preserved/improved
[ ] Executable BPMN
[ ] DMN segmentation
[ ] DMN staging
[ ] DMN priority
[ ] DMN collection action
[ ] Decision service
[ ] CMMN case model
[ ] Spring Batch ingestion
[ ] Collection case domain model
[ ] Flowable process instance management
[ ] Flowable task management
[ ] Agent To-Do API
[ ] Claim task
[ ] Complete task
[ ] Promise of payment
[ ] Payment plan
[ ] Customer contact
[ ] Formal notice
[ ] Escalation
[ ] Legal workflow
[ ] Payment API
[ ] Flowable message correlation
[ ] Timer events
[ ] Automatic email
[ ] Agent-triggered email
[ ] Email templates
[ ] Audit trail
[ ] Flowable history
[ ] REST APIs
[ ] DTOs
[ ] Validation
[ ] Exception handling
[ ] Swagger/OpenAPI
[ ] Demo data
[ ] Integration tests
[ ] Documentation
```

---

# 38. VERY IMPORTANT: VERIFY THE COMPLETE SYSTEM

After implementation:

1. Start the Spring Boot application.
2. Verify Flowable tables are created/available.
3. Verify BPMN deployment.
4. Verify DMN deployment.
5. Verify CMMN deployment.
6. Create a collection case.
7. Start the process.
8. Verify process instance.
9. Verify DMN results.
10. Verify collector assignment.
11. Verify User Task creation.
12. Claim the task.
13. Execute an action.
14. Verify email generation/sending.
15. Verify process continuation.
16. Simulate payment.
17. Verify message correlation.
18. Verify timers if testable.
19. Verify escalation.
20. Verify final process completion.
21. Verify history.
22. Verify REST APIs.
23. Run all tests.

If something fails, FIX IT rather than simply documenting the failure.

---

# 39. DELIVERABLE

At the end, provide a concise implementation report:

```text
Implemented:
- BPMN:
- DMN:
- CMMN:
- Spring Batch:
- REST APIs:
- Flowable integration:
- Email:
- Timers:
- Payment events:
- Escalation:
- Audit:
- Tests:

Files created:
...

Files modified:
...

How to start:
...

How to test:
...

Demo scenario:
...
```

The goal is not to create a theoretical example.

The goal is to transform the current Spring Boot + Flowable project into a **fully executable banking debt-collection workflow that can be demonstrated from case creation → Flowable process → DMN decisions → agent To-Do List → email → payment/action → escalation → completion**.
