# Tasks — POC Flowable BPMN/DMN (Périmètre Adam)

> Plan de jalons (milestones) dérivé du PRD `doc/prd.md`.
> Fenêtre : **22 juin → 1er juillet 2026** (10 jours). Responsable : **Adam**. Relecture : **Oussama**.
> Scénario cible : `dette → DMN → segmentation expliquée → affectation → BPMN → tâche agent → historique → frontend`.
>
> ⚠️ Ce document liste **uniquement les milestones**. Le découpage en tâches sera ajouté dans un second temps.

---

## Vue d'ensemble des milestones

| # | Milestone | Jours PRD | Exigences F | DoD couverts |
|---|---|---|---|---|
| M1 | Socle Flowable & persistance PostgreSQL | J1 → J2 | — | 1 |
| M2 | Segmentation DMN exécutable & expliquée | J3 → J4 | F2, F3, F4 | 2, 3 |
| M3 | Workflow BPMN de recouvrement | J5 | F1, F5, F6, F7, F8 | 4, 5, 6 |
| M4 | Résilience : timers, incidents, retry & idempotence | J6 → J7 | F9, F10, F11 | 7 |
| M5 | Intégration au Domain Core (flux réel) | J8 | F12 | — |
| M6 | Tests d'intégration & stabilisation | J9 | (toutes) | 8 |
| M7 | Release, documentation & validation | J10 | — | 9, 10 |

---

## M1 — Socle Flowable & persistance PostgreSQL
**Jours :** J1 → J2 (22–23/06) · **Services :** `workflow-service` (Process), `segmentation-service` (DMN)

- **Objectif :** moteurs BPMN et DMN opérationnels en local et persistant leur état dans PostgreSQL, démarrables par `docker compose up` sans étape manuelle.
- **Périmètre :** activation des moteurs Flowable Process/DMN ; configuration des datasources ; création automatique des schémas ; vérification de la persistance après redémarrage.
- **Critères de sortie :**
  - Flowable démarre et l'endpoint de santé répond `200`.
  - Après redémarrage, les données de processus persistent en base.
- **Dépendances :** Architecture & conventions (Oussama, J1) ; PostgreSQL & migrations (Hamza, J2).
- **Mitigation :** base PostgreSQL locale temporaire dédiée si la cible Hamza n'est pas prête.

---

## M2 — Segmentation DMN exécutable & expliquée
**Jours :** J3 → J4 (24–25/06) · **Service :** `segmentation-service`

- **Objectif :** table DMN de segmentation déployée, versionnée et exécutable via API REST, retournant **segment + explication** des règles déclenchées.
- **Périmètre :** définition des entrées DMN depuis le modèle Client/Contrat/Dette ; déploiement versionné ; endpoint d'exécution **et** de simulation (sans effet de bord).
- **Critères de sortie :**
  - La table DMN est déployée et consultable par clé/version via API.
  - Un appel API renvoie un segment et la liste des règles ayant conduit au résultat.
- **Dépendances :** modèle métier & contrats API (Abdeladim, J3) ; service d'affectation (Ayoub, J4) ; dataset de démo (Hamza, J4).
- **Mitigation :** contrat d'entrée provisoire figé + affectation mockée si les dépendances tardent.

---

## M3 — Workflow BPMN de recouvrement
**Jour :** J5 (26/06) · **Service :** `workflow-service`

- **Objectif :** processus BPMN de recouvrement déployé, versionné et démarrable via API, avec tâches humaines et historique consultables.
- **Périmètre :** déploiement BPMN versionné ; démarrage d'instance avec injection de variables ; déclenchement de l'affectation post-segmentation ; création/listing/complétion de tâches ; lecture/écriture des variables d'instance ; endpoint d'historique.
- **Critères de sortie :**
  - Un appel API démarre une instance et retourne son identifiant.
  - Une tâche humaine est créée, listable et complétable via API.
  - L'historique d'une instance est consultable via API.
- **Dépendances :** frontend connecté (Mouad, J5).
- **Mitigation :** validation via appels API directs (curl/Postman) si le frontend n'est pas prêt.

---

## M4 — Résilience : timers, incidents, retry & idempotence
**Jours :** J6 → J7 (27–28/06) · **Service :** `workflow-service`

- **Objectif :** délais (timers/SLA simples) et erreurs gérés de façon observable et tolérante, sans doublons.
- **Périmètre :** timer/SLA sur une tâche ; exposition de l'état d'incident et du message d'erreur ; retry sur erreur ; reprise d'une instance bloquée ; idempotence des appels de démarrage (clé métier).
- **Critères de sortie :**
  - Un timer échu modifie l'état de l'instance, observable via API.
  - Un appel de démarrage répété avec la même clé ne crée pas de doublon.
  - Une tâche/instance en erreur peut être rejouée ou reprise via API.
- **Risques traités :** incidents Flowable non visibles ; workflow bloqué sur une transition.

---

## M5 — Intégration au Domain Core (flux réel)
**Jour :** J8 (29/06) · **Services :** `workflow-service`, `segmentation-service` + Domain Core

- **Objectif :** brancher la chaîne réelle `dette → DMN → BPMN` sur le métier via événements et contrats API, sans données factices.
- **Périmètre :** consommation/production des événements du Domain Core ; câblage des contrats API métier ; validation de la cohérence des types/variables entre Flowable et le modèle métier ; consommation REST par le frontend Vue.js.
- **Critères de sortie :**
  - Une dette réelle traverse DMN puis BPMN sans données factices.
  - Les éléments DMN/BPMN sont consommables par le frontend via REST.
- **Dépendances :** Domain Core (Abdeladim).
- **Risques traités :** décalage variables Flowable ↔ modèle métier ; intégration tardive du backend.

---

## M6 — Tests d'intégration & stabilisation
**Jour :** J9 (30/06) · **Périmètre :** DMN + BPMN + API

- **Objectif :** fiabiliser la chaîne complète par une suite de tests d'intégration verte.
- **Périmètre :** matrice DMN nominal/limite/erreur ; test de chaque transition BPMN ; correction des régressions identifiées.
- **Critères de sortie :**
  - Les tests d'intégration principaux (DMN + BPMN + API) passent à 100 %.
  - Les régressions identifiées sont corrigées.
- **Dépendances :** support tests Flowable (Stagiaire 2, J2→J9).
- **Risques traités :** décision DMN ambiguë/non déterministe (politique de hit figée).

---

## M7 — Release, documentation & validation
**Jour :** J10 (01/07) · **Périmètre :** livraison

- **Objectif :** release validée techniquement, reproductible et documentée — Definition of Done atteinte.
- **Périmètre :** vérification de bout en bout des 10 conditions du DoD ; documentation d'installation et d'exécution suivie par un tiers ; revue technique d'Oussama.
- **Critères de sortie (DoD J10) :**
  - Les 10 conditions du PRD §10 sont validées.
  - La documentation d'installation est disponible et suivie de bout en bout par un tiers.
  - Oussama valide techniquement la release.
- **Risques traités :** dépendance excessive à Adam (doc courte + binômage) ; POC confondu avec une plateforme de production (backlog post-POC maintenu).

---

## Hors périmètre (rappel — backlog post-POC)

Workflows juridiques complets · relances multicanales (SMS/email/courrier) · scoring prédictif / IA · orchestration multi-moteurs · HA & clustering Flowable · monitoring avancé · gestion complète des SLA métier · migration de processus en production · tests de charge · sécurité production complète · connecteurs externes (CRM/ERP/SMS/email).
