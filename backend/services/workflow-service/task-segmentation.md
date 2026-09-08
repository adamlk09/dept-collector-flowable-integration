# Task Breakdown — Collection Qualification Engine

> Plan de travail dérivé de [`segmentation.md`](./segmentation.md).
> Scope : remplacer la segmentation binaire (`score >= 70`) du `workflow-service` par un
> moteur de qualification métier piloté par les règles de l'EPIC 7.

## Owner

Adam (Workflow & Flowable) — branche `ft-setup-flowable`.

---

## État actuel (point de départ)

| Élément | Aujourd'hui |
| --- | --- |
| Entrée API | `debtId`, `customerId`, `score` (0–100) |
| Segmentation | `SegmentDebtDelegate` : `score >= 70 ? HIGH_RISK : LOW_RISK` |
| Routing BPMN | `riskGateway` branche sur `${score >= 70}` |
| Stratégies | `selectPriorityStrategyDelegate` / `selectStandardStrategyDelegate` |
| Sortie | `segment`, `strategy`, `status` |

**Limite** : une seule dimension (`score`), deux segments, aucune explication de la décision.

---

## Cible (EPIC 7 → EPIC 8)

Le moteur doit produire une **qualification métier** parmi :

| Qualification | Cas EPIC 7 | Workflow cible (EPIC 8) |
| --- | --- | --- |
| `CLIENT_INFORMATION_INCOMPLETE` | Cas 1 | Update Customer Information |
| `DEBT_CLOSED` | Cas 2 | Clôture du dossier |
| `FIRST_CONTACT` | Cas 3 | Reminder Process |
| `FOLLOW_UP` | Cas 4 | Phone Collection Process |
| `PROMISE_BROKEN` | Cas 5 | Escalation Process |
| `INFO_RESEARCH` | Cas 6 | Mission de recherche d'informations |
| `PRE_LEGAL` | Cas 7 | Pre Legal Collection Process |
| `LEGAL` | Cas 8 | Legal Collection Process |

---

## Découpage des tâches

### T1 — Enrichir le modèle d'entrée (DTO + validation)
- [ ] Ajouter à `StartCollectionProcessRequest` les champs **optionnels** :
  `clientInfoComplete`, `debtStatus`, `reachable`, `promiseStatus`, `daysOverdue`, `contactAttempts`.
- [ ] Valeurs par défaut sûres → rétrocompatibilité avec les appelants à 3 champs.
- [ ] Conserver `score` requis (`@Min(0) @Max(100)`).

### T2 — Propager les variables vers le process
- [ ] `CollectionWorkflowService` : injecter les nouvelles variables dans
  `startCollectionProcess` **et** `startProcess` (sans casser l'idempotence).

### T3 — Moteur de qualification (`SegmentDebtDelegate`)
- [ ] Évaluer les cas EPIC 7 **dans l'ordre de priorité** (premier match gagne).
- [ ] Produire `qualification`, `qualificationReason` (explication de la règle),
  et `segment` (HIGH_RISK/LOW_RISK) dérivé.
- [ ] Lecture défensive des variables (fallback si absentes).
- [ ] Seuils en constantes (futur : externalisation DMN — cf. note ci-dessous).

### T4 — Routing BPMN
- [ ] `riskGateway` branche sur `${segment == 'HIGH_RISK'}` / `${segment == 'LOW_RISK'}`
  au lieu du `score` brut, pour que la qualification pilote le process.
- [ ] Conserver la forme du process et les deux délégués de stratégie.

### T5 — Sortie API
- [ ] Ajouter `qualification` et `qualificationReason` à `StartCollectionProcessResponse`,
  lus depuis l'historique (`requireHistoricVariable`).

### T6 — Tests & vérification
- [ ] Vérifier la compilation (`mvn -pl services/workflow-service -am test`).
- [ ] Smoke-test : un appel par cas EPIC 7 → bonne qualification + explication.
- [ ] Confirmer l'historisation des décisions (history API).

---

## Hors scope (volontairement)

Pour rester incrémental, ce lot **ne fait pas** :
- Les 8 workflows BPMN distincts de l'EPIC 8 (un seul process conservé, routing 2 voies).
- L'externalisation des règles en **Flowable DMN** — la DMN appartient à `segmentation-service`
  (cf. `../../CLAUDE.md`), pas au `workflow-service`. À traiter dans un lot dédié.
- Les vérifications automatisées des EPIC 1–6 (les dimensions sont fournies en entrée pour l'instant).

---

## Definition of Done (ce lot)

- [ ] La qualification couvre les 8 cas de l'EPIC 7.
- [ ] Chaque décision porte une explication (`qualificationReason`).
- [ ] Rétrocompatibilité des appelants existants préservée.
- [ ] Décisions historisées et observables via l'API.
- [ ] Build vert.
