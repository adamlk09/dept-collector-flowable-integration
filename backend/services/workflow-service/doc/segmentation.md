# Collection Qualification Engine

## Owner

Adam (Workflow & Flowable)

---

## OBJECTIF

Construire le moteur de qualification des dossiers de recouvrement.

Le moteur doit analyser le dossier avant de démarrer un workflow Flowable.

Le résultat attendu est une **qualification métier** qui permettra de lancer le bon processus BPMN.

---

## EPIC 1 — Vérification des informations du client

### Story 1.1

En tant qu'agent, je veux vérifier que les informations du client sont complètes, afin de pouvoir commencer une procédure de recouvrement.

#### Tasks

- Vérifier Nom / Prénom
- Vérifier CIN / ICE
- Vérifier Téléphone principal
- Vérifier Téléphone secondaire
- Vérifier Email
- Vérifier Adresse
- Vérifier Ville
- Vérifier Employeur
- Vérifier Personne à contacter

#### Résultat

```
CLIENT_INFORMATION_COMPLETE
CLIENT_INFORMATION_INCOMPLETE
```

---

## EPIC 2 — Vérification du contrat

### Story 2.1

L'agent vérifie le contrat.

#### Tasks

- Vérifier le numéro du contrat
- Vérifier le produit
- Vérifier le statut du contrat
- Vérifier la date de début
- Vérifier la date d'échéance
- Vérifier si le contrat est actif
- Vérifier si le contrat est suspendu
- Vérifier si le contrat est résilié

#### Résultat

```
ACTIVE_CONTRACT
SUSPENDED_CONTRACT
TERMINATED_CONTRACT
```

---

## EPIC 3 — Vérification de la dette

### Story 3.1

L'agent analyse la dette.

#### Tasks

- Calculer le montant restant
- Vérifier le montant initial
- Vérifier les intérêts
- Vérifier les pénalités
- Vérifier les frais
- Vérifier les échéances impayées
- Vérifier le dernier paiement
- Calculer les jours de retard

#### Résultat

```
OPEN_DEBT
PAID_DEBT
PARTIAL_PAYMENT
```

---

## EPIC 4 — Historique des paiements

### Story 4.1

Consulter l'historique du client.

#### Tasks

- Lister les paiements
- Identifier le dernier paiement
- Identifier les paiements partiels
- Identifier les paiements rejetés
- Identifier les paiements en attente

#### Résultat

```
GOOD_PAYMENT_HISTORY
IRREGULAR_PAYMENT_HISTORY
NO_PAYMENT
```

---

## EPIC 5 — Historique des actions de recouvrement

### Story 5.1

Analyser les actions déjà réalisées.

#### Tasks

- SMS envoyés
- Emails envoyés
- Appels effectués
- Courriers envoyés
- Visites effectuées
- Notifications envoyées
- Dossiers clôturés
- Dossiers réouverts

#### Résultat

```
FIRST_CONTACT
FOLLOW_UP
MULTIPLE_FOLLOW_UPS
```

---

## EPIC 6 — Promesses de paiement

### Story 6.1

Contrôler les promesses.

#### Tasks

- Vérifier s'il existe une promesse
- Vérifier la date
- Vérifier le montant promis
- Vérifier le respect de la promesse
- Vérifier les promesses rompues

#### Résultat

```
PROMISE_ACTIVE
PROMISE_KEPT
PROMISE_BROKEN
```

---

## EPIC 7 — Qualification métier

Toutes les informations précédentes sont maintenant disponibles. Le moteur doit qualifier le dossier.

### Cas 1 — Informations incomplètes

↓ Créer une tâche : `Compléter les informations client`

### Cas 2 — Dette déjà payée

↓ Clôturer le dossier.

### Cas 3 — Premier impayé

↓ Envoyer SMS → Envoyer Email

### Cas 4 — Deuxième relance

↓ Créer une tâche d'appel.

### Cas 5 — Promesse non respectée

↓ Créer une relance renforcée.

### Cas 6 — Client injoignable

↓ Créer une mission de recherche d'informations.

### Cas 7 — Retard important avec plusieurs relances sans succès

↓ Transférer le dossier vers le service pré-juridique.

### Cas 8 — Conditions juridiques réunies

↓ Lancer le workflow juridique.

---

## EPIC 8 — Démarrage du Workflow Flowable

Selon la qualification obtenue :

| Qualification | Workflow démarré |
| --- | --- |
| `CLIENT_INFORMATION_INCOMPLETE` | Update Customer Information Process |
| `FIRST_CONTACT` | Reminder Process |
| `FOLLOW_UP` | Phone Collection Process |
| `PROMISE_BROKEN` | Escalation Process |
| `PRE_LEGAL` | Pre Legal Collection Process |
| `LEGAL` | Legal Collection Process |

---

## Definition of Done

Le moteur est considéré terminé lorsque :

- Toutes les vérifications métier sont automatisées.
- La qualification du dossier est correcte.
- Le bon workflow BPMN est démarré automatiquement.
- Les décisions sont historisées.
- Les APIs sont testées.
- Les règles sont facilement modifiables via Flowable DMN.