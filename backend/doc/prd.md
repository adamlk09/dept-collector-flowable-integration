init # PRD — Plateforme Flowable BPMNDMN (Périmètre Adam, Référent Flowable)

Projet  POC SaaS multi-tenant de recouvrement de créances
Fenêtre  22 juin → 1er juillet 2026 (10 jours, taskforce 8 personnes)
Responsable  Adam — Backend Developer  Référent Flowable
Relecture technique  Oussama (Tech Lead)

 BPMN (Business Process Model and Notation)  standard de modélisation et d'exécution de processus métier (ici, le workflow de recouvrement).
 DMN (Decision Model and Notation)  standard de modélisation et d'exécution de règles de décision sous forme de tables (ici, la segmentation des dettes).

---

## 1. Résumé exécutif

Ce PRD couvre l'intégration et l'industrialisation du moteur Flowable dans le POC de recouvrement. L'enjeu central est d'exposer, via API REST, une chaîne DMN → BPMN exécutable de bout en bout  segmentation automatique d'une dette, démarrage d'un processus de recouvrement, création de tâches humaines et consultation de l'historique. L'intégration doit s'articuler proprement avec le Domain Core métier, PostgreSQL et le frontend Vue.js, sans manipulation manuelle de la base ni de la console Flowable. La livraison validée est attendue au J10 (0107) après validation technique d'Oussama.

---

## 2. Objectifs et métriques de succès

 Objectif  Métrique de succès (vérifiable) 
------
 Exécuter la segmentation DMN  ≥ 1 décision DMN exécutable via API, retournant segment + explication 
 Démarrer le workflow de recouvrement  ≥ 1 processus BPMN démarrable via API REST 
 Rendre les tâches accessibles  Tâche humaine créée, listable et complétable via API 
 Exposer l'historique  Historique d'au moins 1 instance consultable via API 
 Garantir la non-régression  100 % des tests d'intégration principaux (DMN + BPMN + API) passent 
 Assurer la reproductibilité  `docker compose up` démarre Flowable + PostgreSQL + déploie BPMNDMN automatiquement 
 Rendre les incidents visibles  Un incident simple (erreur de tâche) est exposé via API et accompagné d'un retry minimal 

Ces objectifs sont alignés sur le scénario vertical  dette → DMN → segmentation expliquée → affectation → BPMN → tâche agent → historique → affichage frontend.

---

## 3. Périmètre détaillé

### Inclus
- Installation et configuration de Flowable avec PostgreSQL.
- Activation des moteurs BPMN et DMN.
- Déploiement versionné des processus BPMN et des tables de décision DMN.
- Exécution de la segmentation via DMN et exposition du résultat avec explication.
- Démarrage d'un processus BPMN de recouvrement.
- Gestion minimale des variables de processus.
- Création, listing et complétion de tâches humaines Flowable.
- Consultation de l'historique du processus.
- Gestion minimale des incidents, retry, reprise et idempotence.
- Timers ou SLA simples.
- Endpoints REST consommables par le frontend et le backend métier.
- Tests d'intégration BPMN + DMN + API.
- Documentation d'installation et d'exécution.

### Explicitement hors périmètre
Workflows juridiques complets ; relances multicanales ; scoring prédictif ou IA ; orchestration multi-moteurs ; haute disponibilité et clustering Flowable ; monitoring avancé de production ; gestion complète des SLA métier ; migration de processus en production ; tests de charge ; sécurité production complète ; connecteurs externes (CRM, ERP, SMS, email).

---

## 4. User stories (J1 → J9)

 Jalon  User story 
------
 J1  En tant que système, je veux disposer de Flowable BPMN et DMN installés, afin de pouvoir exécuter des processus et décisions. 
 J2  En tant qu'administrateur métier, je veux que les moteurs persistent leur état dans PostgreSQL, afin de conserver processus et décisions de façon fiable. 
 J3  En tant qu'administrateur métier, je veux déployer une table DMN de segmentation versionnée, afin de classer automatiquement les dettes. 
 J4  En tant que système, je veux exécuter et simuler une décision DMN via API, afin d'obtenir un segment et son explication. 
 J5  En tant qu'agent de recouvrement, je veux qu'un processus BPMN de recouvrement démarre après segmentation, afin de prendre en charge la dette. 
 J6  En tant qu'administrateur métier, je veux des timers et SLA simples sur le processus, afin de cadrer les délais de traitement. 
 J7  En tant que système, je veux un retry, une reprise et de l'idempotence, afin de tolérer les erreurs sans doublons. 
 J8  En tant que système, je veux que Flowable s'intègre au Domain Core via événements et contrats API, afin d'exécuter le flux réel. 
 J9  En tant que système, je veux exécuter les tests d'intégration et corriger les régressions, afin de fiabiliser la chaîne complète. 

---

## 5. Exigences fonctionnelles détaillées par jour (J1 → J9)

### J1 — Installer Flowable BPMN et DMN (2206)
- Objectif  moteurs BPMN et DMN opérationnels en local.
- Étapes  récupérer l'imagele runtime Flowable ; activer les moteurs BPMN et DMN ; démarrer un conteneur minimal.
- Livrable  Flowable démarre et répond sur son endpoint d'état.
- Dépendances  Architecture et conventions (Oussama, J1).
- Critère d'acceptation (ON)  Flowable démarre et l'endpoint de santé répond `200`.

### J2 — Configurer les moteurs et PostgreSQL (2306)
- Objectif  persistance Flowable sur PostgreSQL.
- Étapes  configurer la datasource ; valider la création des schémas Flowable ; vérifier le redémarrage sans perte d'état.
- Livrable  Flowable connecté à PostgreSQL avec schémas créés automatiquement.
- Dépendances  PostgreSQL et migrations initiales (Hamza, J2).
- Critère d'acceptation (ON)  après redémarrage, les données de processus persistent en base.

### J3 — Déployer la table DMN de segmentation (2406)
- Objectif  table DMN versionnée et déployée.
- Étapes  définir les entrées DMN à partir du modèle ClientContratDette ; déployer la table ; vérifier le versioning.
- Livrable  table DMN déployée, identifiable par version.
- Dépendances  Modèle métier + contrats API (Abdeladim, J3).
- Critère d'acceptation (ON)  la table DMN est déployée et consultable par sa cléversion via API.

### J4 — Exposer l'exécution et la simulation des décisions (2506)
- Objectif  décision DMN exécutable et simulable via API.
- Étapes  exposer un endpoint d'exécution DMN ; retourner segment + explication des règles déclenchées ; permettre une simulation sans effet de bord.
- Livrable  endpoint REST d'exécutionsimulation DMN.
- Dépendances  Service d'affectation (Ayoub, J4) ; dataset de démo (Hamza, J4).
- Critère d'acceptation (ON)  un appel API renvoie un segment et la liste des règles ayant conduit au résultat.

### J5 — Déployer le workflow BPMN de recouvrement (2606)
- Objectif  processus BPMN démarrable.
- Étapes  déployer le BPMN versionné ; démarrer une instance via API en injectant les variables ; déclencher l'affectation après segmentation.
- Livrable  processus BPMN déployé et démarrable.
- Dépendances  Frontend connecté (Mouad, J5).
- Critère d'acceptation (ON)  un appel API démarre une instance et retourne son identifiant.

### J6 — Configurer timers, incidents et SLA minimal (2706)
- Objectif  délais et incidents simples gérés.
- Étapes  ajouter un timerSLA sur une tâche ; exposer l'état d'incident ; déclencher une échéance.
- Livrable  processus avec timerSLA et incidents visibles.
- Critère d'acceptation (ON)  un timer échu modifie l'état de l'instance, observable via API.

### J7 — Sécuriser retry, reprise et idempotence (2806)
- Objectif  tolérance aux erreurs sans doublons.
- Étapes  configurer un retry sur erreur ; permettre la reprise d'une instance bloquée ; garantir l'idempotence des appels de démarrage.
- Livrable  mécanismes de retryrepriseidempotence opérationnels.
- Critère d'acceptation (ON)  un appel de démarrage répété avec la même clé ne crée pas de doublon ; une tâche en erreur peut être rejouée.

### J8 — Intégrer Flowable avec le Domain Core et les événements (2906)
- Objectif  flux réel branché sur le métier.
- Étapes  consommerproduire les événements du Domain Core ; brancher les contrats API métier ; valider la cohérence des variables.
- Livrable  chaîne réelle dette → DMN → BPMN intégrée.
- Dépendances  Domain Core (Abdeladim).
- Critère d'acceptation (ON)  une dette réelle traverse DMN puis BPMN sans données factices.

### J9 — Tests d'intégration et correction des régressions (3006)
- Objectif  chaîne fiabilisée.
- Étapes  exécuter la matrice DMN (nominallimiteerreur) ; tester chaque transition BPMN ; corriger les régressions.
- Livrable  suite de tests d'intégration verte.
- Dépendances  Support tests Flowable (Stagiaire 2, J2→J9).
- Critère d'acceptation (ON)  les tests d'intégration principaux passent et les régressions identifiées sont corrigées.

---

## 6. Exigences fonctionnelles Flowable

 #  Exigence  Détail vérifiable 
---------
 F1  Déploiement BPMN versionné  Chaque déploiement crée une version identifiable ; la dernière version est utilisée par défaut. 
 F2  Déploiement DMN versionné  Chaque table DMN est déployée avec une version traçable. 
 F3  Exécution d'une décision DMN  Un endpoint exécute la décision à partir des entrées métier et retourne le segment. 
 F4  Explication de la segmentation  Le résultat inclut les règleslignes DMN déclenchées justifiant le segment. 
 F5  Démarrage d'un processus BPMN  Un endpoint démarre une instance avec variables et renvoie son identifiant. 
 F6  Variables de processus  Lectureécriture minimale des variables d'instance via API. 
 F7  Tâches humaines  Création, listing et complétion d'une tâche via API. 
 F8  Historique  Endpoint exposant l'historique d'une instance (activités, décisions, tâches). 
 F9  Incidents  Un incident simple est exposé avec son état et son message d'erreur. 
 F10  Retry et reprise  Une tâcheinstance en erreur peut être relancée ou reprise via API. 
 F11  Timers  SLA simples  Un timer ou SLA déclenche une transition ou un état à échéance. 
 F12  API REST frontend  Tous les éléments ci-dessus sont consommables par le frontend Vue.js via REST. 

---

## 7. Exigences non fonctionnelles

 Catégorie  Exigence 
------
 Reproductibilité  `docker compose up` démarre l'ensemble et déploie BPMNDMN automatiquement, sans étape manuelle. 
 Versioning  BPMN et DMN sont versionnés et la version utilisée est traçable. 
 Observabilité minimale  État des instances, incidents et historique consultables via API. 
 Gestion d'erreurs  Erreurs exploitables  code, message et contexte exposés au consommateur. 
 Traçabilité  Toute décision et tout processus sont rattachables à une instance et à une version. 
 Compatibilité  Fonctionne avec la version PostgreSQL fournie par Hamza ; schémas créés automatiquement. 
 Performance perçue  Exécution DMN et démarrage BPMN avec latence acceptable pour une démo (réponse sub-seconde sur dataset de démo). 
 Documentation  Procédure d'installation et d'exécution disponible et suivie de bout en bout par un tiers. 

---

## 8. Dépendances et points de blocage

 Dépendance  Responsable  Date limite  Impact si indisponible  Solution temporaire 
---------------
 Architecture & conventions techniques  Oussama  J1  Intégration Flowable non validée  Démarrer sur conventions par défaut, geler après revue 
 PostgreSQL & migrations initiales  Hamza  J2  Pas de persistance Flowable  Base PostgreSQL locale temporaire dédiée à Flowable 
 Modèle métier ClientContratDette  Abdeladim  J3  Entrées DMN et variables BPMN incertaines  Contrat d'entrée provisoire figé, à remplacer 
 Contrats API métier  Abdeladim  J3  Démarrage des processus bloqué  Stubsmocks d'API métier 
 Service d'affectation  Ayoub  J4  Pas d'affectation post-segmentation  Affectation simulée (mock) renvoyant un agent fixe 
 Dataset de démonstration  Hamza  J4  Tests DMNBPMN limités  Fixtures locales minimales 
 Frontend connecté  Mouad  J5  Affichage non démontrable  Validation via appels API directs (curlPostman) 
 Support tests Flowable  Stagiaire 2  J2→J9  Couverture de tests réduite  Adam priorise les tests critiques en binôme 

---

## 9. Risques et mitigations

 Risque  Impact  Probabilité  Mitigation 
------------
 Décalage variables Flowable ↔ modèle métier  Élevé  Élevé  Geler les contrats d'entrée avant J3 ; tester les types DMNBPMN 
 Intégration tardive avec le backend métier  Élevé  Moyen  Exposer les contrats API dès J3 ; brancher le flux réel avant J5 
 Workflow BPMN bloqué sur une tâchetransition  Élevé  Moyen  Fixtures BPMN et test de chaque transition 
 Décision DMN ambiguë ou non déterministe  Élevé  Moyen  Matrice de tests nominallimiteerreur ; politique de hit fixée 
 Configuration FlowablePostgreSQL incorrecte  Élevé  Moyen  Valider version, schémas et démarrage dès J1-J2 
 Incidents Flowable non visibles  Moyen  Moyen  Exposer état, erreurs, historique et retry minimal 
 Dépendance excessive à Adam  Moyen  Moyen  Documentation courte, binômage Stagiaire 2, revue Oussama 
 POC confondu avec une plateforme de production  Moyen  Élevé  Backlog post-POC explicite et maintenu 

---

## 10. Definition of Done — J10 (0107)

Le jalon J10 est validé uniquement si toutes les conditions ci-dessous sont remplies 

 #  Condition  Validé (ON) 
---------
 1  Flowable démarre dans l'environnement local  
 2  BPMN et DMN sont déployés automatiquement  
 3  Une décision DMN retourne un segment et une explication  
 4  Un processus BPMN est démarrable via API  
 5  Une tâche humaine est créée et complétable  
 6  L'historique est consultable  
 7  Les incidents simples sont visibles  
 8  Les tests d'intégration principaux passent  
 9  La documentation d'installation est disponible  
 10  Oussama valide techniquement la release  

---

## 11. Hors périmètre  Backlog post-POC

Éléments reportés après le POC  workflows juridiques complets ; relances multicanales (SMS, email, courrier) ; scoring prédictif  IA ; orchestration multi-moteurs BPM ; haute disponibilité et clustering Flowable ; monitoring avancé de production ; gestion complète des SLA métier ; migration de processus en production ; tests de charge complets ; sécurité production complète ; connecteurs externes CRM  ERP  SMS  email.