# Tutoriel présentation tech lead — 10 scénarios en un batch

Script chronométré, 7 étapes, chacune entre 30 secondes et 5 minutes (total ≈ 7-8 min). Tout se
pilote depuis Flowable Admin UI (http://localhost:8090/flowable-ui/admin/) — aucune commande
`curl` visible pendant la démo elle-même (le batch est déjà lancé, voir §0).

Jeu de données : `src/main/resources/demo-data/banking-cases.csv`, 10 dossiers fictifs couvrant
chaque branche du DMN (voir tableau §1). Process déployé : **Processus de recouvrement bancaire
(v8)** — diagramme et libellés en français.

---

## §0. Avant la démo (à faire hors présentation, ~2 min)

```bash
curl -X POST http://localhost:8094/api/v1/workflows/batch/irregular-portfolio/run \
  -H 'X-Tenant-Id: tenant-demo'
```

→ `{"status":"COMPLETED","jobExecutionId":"..."}`. Vérifiez dans Flowable Admin UI
(**Instances → filtre "Process definition" = Processus de recouvrement bancaire (v8)**) que les
10 `SCENARIO-xx` apparaissent, tous **Active**, avec une tâche `Traiter le dossier de
recouvrement` ouverte. Si le service vient d'être redémarré, attendez ~15s qu'il finisse son
démarrage avant de lancer le batch.

---

## Étape 1 — Accroche : un batch, dix dossiers, zéro ligne de code (≈ 30 s)

**À dire** : "On a un portefeuille de 10 dossiers irréguliers. Un seul appel a suffi à tous les
qualifier et les faire entrer dans le workflow — je vais vous montrer comment chacun a été traité
différemment selon ses propres données."

**À l'écran** : Flowable Admin UI → **Instances**, filtre déjà posé sur
"Processus de recouvrement bancaire (v8)" → les 10 lignes `SCENARIO-01` à `SCENARIO-10`, toutes
créées à la même seconde.

---

## Étape 2 — Le DMN a tout décidé tout seul (≈ 1 min)

**À dire** : "Aucune de ces qualifications n'est codée en Java — c'est une chaîne de 4 tables de
décision qui a tourné pour chaque dossier."

**À l'écran** :
1. Ouvrir `SCENARIO-05-LEGAL-ENTREPRISE` (120j de retard, 95 000 € d'encours) → onglet
   **Variables** → montrer `stagingSource = DMN`, `ifrsStage = STAGE_3`,
   `collectionPhase = LEGAL`, `priority = CRITICAL`, `recommendedAction = LEGAL_ESCALATION`.
2. Ouvrir `SCENARIO-01-COMMERCIAL` (15j, 3 000 €) en face-à-face → même moteur, résultat
   totalement différent : `collectionPhase = COMMERCIAL`, `priority = LOW`.
3. (Bonus si le temps le permet) `SCENARIO-07-RECLASSIFICATION-PRO` : un professionnel avec
   60 000 € d'encours est automatiquement reclassé `customerSegment = ENTREPRISE` — une 4ᵉ
   décision indépendante des 3 premières.

---

## Étape 3 — Traiter un dossier standard (≈ 1 min 30)

**À dire** : "Voici la seule étape humaine du process — un agent valide ou corrige la
qualification automatique."

**À l'écran** :
1. `SCENARIO-01-COMMERCIAL` → onglet **Tasks** → cliquer la tâche `Traiter le dossier de
   recouvrement` → **Actions → Edit task** → renseigner `Assignee` (ex. `agent.dupont`) →
   **Update task**.
2. **Actions → Complete task** → laisser passer sans variable additionnelle (l'agent confirme
   la qualification automatique).
3. **Show process diagram** → montrer le token qui est passé la porte d'escalade, la porte de
   stage, et s'est arrêté sur `Appliquer l'action de recouvrement`.

---

## Étape 4 — Le dossier contentieux ouvre un vrai case CMMN (≈ 1 min 30)

**À dire** : "Pour un dossier grave, le BPMN ne modélise plus la suite en étapes fixes — il ouvre
un plateau de tâches CMMN que l'équipe juridique traite dans l'ordre qu'elle veut."

**À l'écran** :
1. `SCENARIO-05-LEGAL-ENTREPRISE` → compléter `Traiter le dossier de recouvrement` (Actions →
   Complete task).
2. **Show process diagram** → le token part vers `Ouvrir un dossier contentieux (CMMN)` puis
   `Transfert juridique (Avocat / Huissier)`.
3. Menu **CMMN Engine → Instances** → ouvrir le case qui vient de naître → montrer les 7 tâches
   discrétionnaires (Enquêter sur le client, Demander des documents, Contacter le client,
   Approbation manager, Mise en demeure, Escalader au juridique, Clôturer le dossier) toutes
   disponibles en même temps, aucun ordre imposé.

---

## Étape 5 — Le moteur asynchrone tourne réellement (≈ 2 min)

**À dire** : "Les timers ne sont pas décoratifs — le moteur async les déclenche tout seul, en
tâche de fond, sans qu'on ait besoin d'intervenir. Je le calibre à 2 minutes pour la démo au lieu
de 2 jours en production, mais le mécanisme est identique."

**À l'écran** :
1. Ouvrir `SCENARIO-02-AMIABLE`, compléter sa tâche `Traiter le dossier de recouvrement`.
2. Process instance → **Jobs** → montrer le timer boundary `assignToAgentReminder` (si le job
   n'a pas encore consommé) ou l'event-gateway `waitPaymentDeadline` (timer `PT1M`).
3. Laisser l'écran ouvert, continuer à parler (étape 6 pendant ce temps) — revenir après ~1-2 min
   et rafraîchir : le timer a expiré tout seul, le process a avancé vers l'escalade ou la
   relance SLA, visible dans **Show process diagram** (le nœud suivant est surligné).

---

## Étape 6 — Bilan et audit trail (≈ 45 s)

**À dire** : "Tout ce qu'on vient de voir est historisé automatiquement — aucun code écrit pour
ça."

**À l'écran** : sur n'importe quel dossier terminé → **Show all subtasks / History** → dérouler
la timeline complète (chaque activité, son horodatage, sa durée).

---

## Étape 7 — Conclusion (≈ 30 s)

**À dire** : "Un seul batch, dix comportements différents, calculés par 4 décisions DMN,
orchestrés par un seul diagramme BPMN, avec un case CMMN pour les dossiers qui sortent du chemin
standard. Rien de tout ça n'est du code métier — c'est déclaratif, versionné, et modifiable sans
redéploiement Java. Des questions ?"

---

## Minutage récapitulatif

| Étape | Durée | Cumul |
|---|---|---|
| 1. Accroche | 30 s | 0:30 |
| 2. DMN | 1 min | 1:30 |
| 3. Dossier standard | 1 min 30 | 3:00 |
| 4. CMMN | 1 min 30 | 4:30 |
| 5. Timer live | 2 min | 6:30 |
| 6. Audit trail | 45 s | 7:15 |
| 7. Conclusion | 30 s | 7:45 |

Total ≈ **7 min 45**, chaque étape individuellement entre 30 s et 5 min.

Voir aussi `demo-script.md` (version longue, ligne de commande) et `speech.html` (présentation
équipe, sans manipulation live).
