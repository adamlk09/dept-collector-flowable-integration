# Roadmap par Milestones — Collection Qualification Engine

> Dérivé de [`task-segmentation.md`](./task-segmentation.md) et de [`doc/segmentation.md`](./doc/segmentation.md).
> Découpage en **milestones** livrables, du moteur comportemental minimal jusqu'au moteur
> complet piloté par DMN avec un workflow BPMN dédié par qualification.

## Owner

Adam (Workflow & Flowable) — branche `ft-setup-flowable`.

---

## Vue d'ensemble des milestones

| Milestone | Objectif | Résultat livrable |
| --- | --- | --- |
| **M1** | Moteur comportemental (sans scoring) | Qualification EPIC 7 dans le `workflow-service` |
| **M2** | Sortie API & historisation | Décision + explication exposées et tracées |
| **M3** | Externalisation des règles en DMN | Règles éditables dans `segmentation-service` |
| **M4** | Automatisation des vérifications EPIC 1–6 | Dimensions calculées, plus saisies à la main |
| **M5** | Workflows BPMN dédiés (EPIC 8) | Un process par qualification |
| **M6** | Qualité, tests & observabilité | Couverture des 8 cas, build vert, métriques |

---

## Milestone 1 — Moteur comportemental (cible immédiate)

> **Aucun scoring numérique.** La décision repose uniquement sur les catégories EPIC 1–6.

### M1.1 — Modèle d'entrée comportemental
- [ ] `StartCollectionProcessRequest` : retirer `score`, ajouter les dimensions catégorielles
  `clientInfoStatus`, `contractStatus`, `debtStatus`, `paymentHistory`,
  `collectionStage`, `promiseStatus`, `reachable`.
- [ ] Champs optionnels + valeurs par défaut sûres (rétrocompatibilité).

### M1.2 — Propagation des variables
- [ ] `CollectionWorkflowService` : injecter les variables comportementales dans
  `startCollectionProcess` **et** `startProcess` (idempotence préservée).
- [ ] Retirer `score` des variables et de la validation amont.

### M1.3 — Moteur de qualification (`SegmentDebtDelegate`)
- [ ] Évaluer les 8 cas de l'EPIC 7 **dans l'ordre de priorité** (premier match gagne).
- [ ] Produire `qualification`, `qualificationReason` (explication) et `segment` dérivé.
- [ ] Lecture défensive des variables (fallback catégoriel).

### M1.4 — Routing BPMN
- [ ] `riskGateway` branche sur `segment` (issu de la qualification), plus sur `score`.
- [ ] Conserver la forme du process et les deux délégués de stratégie.

**DoD M1** : les 8 cas EPIC 7 produisent la bonne qualification + explication ; build vert.

---

## Milestone 2 — Sortie API & historisation

### M2.1 — Réponse enrichie
- [ ] `StartCollectionProcessResponse` : retirer `score`, ajouter `qualification`
  et `qualificationReason`, lus depuis l'historique.

### M2.2 — Décisions tracées
- [ ] Vérifier l'historisation des variables de décision (history API).
- [ ] Exposer la décision via les endpoints existants (`/variables`, `/history`).

**DoD M2** : chaque dossier qualifié est consultable avec sa décision et son explication.

---

## Milestone 3 — Externalisation des règles en DMN

> La DMN appartient à `segmentation-service` (cf. `../../CLAUDE.md`), pas au `workflow-service`.

### M3.1 — Table de décision DMN
- [ ] Modéliser les règles EPIC 7 en table de décision (`segmentation-service/resources/dmn/`).
- [ ] Entrées = dimensions EPIC 1–6 ; sorties = `qualification` + `qualificationReason`.

### M3.2 — Appel inter-service
- [ ] `SegmentDebtDelegate` appelle la DMN via `segmentation-service` (REST),
  propage `X-Tenant-Id` / `X-Correlation-Id`.
- [ ] Fallback / gestion d'erreur si le service DMN est indisponible.

**DoD M3** : les règles sont modifiables sans recompiler le `workflow-service`.

---

## Milestone 4 — Automatisation des vérifications EPIC 1–6

> Aujourd'hui les dimensions sont fournies en entrée ; ici elles sont calculées.

- [ ] EPIC 1 — vérification complétude client (debt/customer-service).
- [ ] EPIC 2 — statut du contrat.
- [ ] EPIC 3 — analyse de la dette (montant, retard, échéances).
- [ ] EPIC 4 — historique des paiements.
- [ ] EPIC 5 — historique des actions de recouvrement.
- [ ] EPIC 6 — promesses de paiement.

**DoD M4** : le moteur dérive lui-même les catégories à partir des services métier.

---

## Milestone 5 — Workflows BPMN dédiés (EPIC 8)

Un process BPMN par qualification, selon la table EPIC 8 :

| Qualification | Workflow |
| --- | --- |
| `CLIENT_INFORMATION_INCOMPLETE` | Update Customer Information Process |
| `FIRST_CONTACT` | Reminder Process |
| `FOLLOW_UP` | Phone Collection Process |
| `PROMISE_BROKEN` | Escalation Process |
| `PRE_LEGAL` | Pre Legal Collection Process |
| `LEGAL` | Legal Collection Process |

- [ ] Démarrage dynamique du bon process selon `qualification`.
- [ ] Auto-déploiement de tous les BPMN au démarrage (aucune étape manuelle).

**DoD M5** : la qualification déclenche automatiquement le bon workflow.

---

## Milestone 6 — Qualité, tests & observabilité

- [ ] Tests unitaires du moteur : un cas par règle EPIC 7.
- [ ] Tests d'intégration : start → qualification → workflow → historique.
- [ ] Smoke-tests `curl` documentés pour les 8 cas.
- [ ] Logs/metrics sur la qualification (sans `System.out`).

**DoD M6** : couverture des 8 cas, build vert, décisions observables.

---

## Hors scope (toutes milestones confondues)

- Refonte du modèle de tenant / sécurité (géré par le shared-kernel).
- Migration vers H2 ou `ddl-auto: update` (interdit, cf. `CLAUDE.md`).
