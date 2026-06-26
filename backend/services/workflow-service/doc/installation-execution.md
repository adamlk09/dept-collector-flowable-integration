# Workflow-Service — Guide d'installation et d'exécution

Documentation d'installation et d'exécution du **workflow-service**, le moteur de processus
BPMN (Flowable) de la chaîne de recouvrement : **debt input → segmentation → décision → stratégie**.

---

## Sommaire

1. [Présentation](#1-présentation)
2. [Prérequis](#2-prérequis)
3. [Préparation de la base de données](#3-préparation-de-la-base-de-données)
4. [Configuration](#4-configuration)
5. [Compilation (build)](#5-compilation-build)
6. [Exécution des tests](#6-exécution-des-tests)
7. [Démarrage du service](#7-démarrage-du-service)
8. [Vérification](#8-vérification)
9. [Utilisation de l'API](#9-utilisation-de-lapi)
10. [Documentation interactive (Swagger UI)](#10-documentation-interactive-swagger-ui)
11. [Dépannage](#11-dépannage)
12. [Annexe — Variables d'environnement](#12-annexe--variables-denvironnement)

---

## 1. Présentation

Le `workflow-service` reçoit une demande de recouvrement de dette, exécute un **processus BPMN**
qui valide la dette, la **segmente** par risque, puis **décide** de la stratégie de collecte.

```
POST /api/v1/workflows/collection/start  { debtId, customerId, score }
        │
        ▼
   Validate Debt → Segment Debt → ◇ Gateway (score ≥ 70 ?) → Select Strategy → End
                                       │                │
                                  PRIORITY          STANDARD
```

| Élément | Valeur |
|---|---|
| Port HTTP | `8094` |
| Base de données | PostgreSQL (`workflow_db` en profil `dev`) |
| Clé du processus BPMN | `debtCollectionProcess` |
| Moteur | Flowable 7.2 (starter *process*) |

---

## 2. Prérequis

| Outil | Version | Remarque |
|---|---|---|
| **JDK** | **21** | `JAVA_HOME` doit pointer vers un JDK 21 |
| **Maven** | — | Inutile d'installer Maven : le wrapper `mvnw.cmd` télécharge Maven 3.9.x automatiquement |
| **PostgreSQL** | 17 | En local (port `5432`) ou via Docker |
| **Docker** | (optionnel) | Pour démarrer l'infra via `compose.yaml` |

Vérifier Java :

```bash
java -version        # doit afficher 21.x
```

> Toutes les commandes Maven se lancent **depuis la racine `backend/`** avec `.\mvnw.cmd`
> (le service est un module enfant du POM parent).

---

## 3. Préparation de la base de données

Le service attend une base PostgreSQL et l'utilisateur `collector` / `collector`.

### Option A — PostgreSQL local

Créer la base attendue par le profil `dev` :

```sql
-- psql -U postgres
CREATE USER collector WITH PASSWORD 'collector';
CREATE DATABASE workflow_db OWNER collector;
```

> ⚠️ Le **nom de la base** doit correspondre à la configuration :
> - profil `dev` → `workflow_db`
> - configuration par défaut (`application.yml`) → `workflow_service`

### Option B — Infrastructure Docker

Depuis `backend/` :

```bash
docker compose up -d        # PostgreSQL, Keycloak, Redis, Kafka
```

Le script `infra/postgres/init-databases.sql` crée les bases par service.

### Ce qui est créé automatiquement

| Tables | Créées par |
|---|---|
| Tables du moteur Flowable (`ACT_*`) | Flowable (`flowable.database-schema-update: true`) |
| Tables métier (`outbox_event`) | Flyway (`src/main/resources/db/migration/`) |

Aucune création manuelle de table n'est nécessaire.

---

## 4. Configuration

Deux fichiers de configuration :

| Fichier | Base par défaut | Usage |
|---|---|---|
| `application.yml` | `workflow_service` | Configuration de référence (ne pas modifier) |
| `application-dev.yml` | `workflow_db` | Profil **dev** — développement local |

Toutes les valeurs sont surchargeables par variables d'environnement (voir [annexe](#12-annexe--variables-denvironnement)) :

```yaml
spring.datasource.url:      ${DB_URL:jdbc:postgresql://localhost:5432/workflow_db}
spring.datasource.username: ${DB_USER:collector}
spring.datasource.password: ${DB_PASSWORD:collector}
server.port:                ${SERVER_PORT:8094}
app.security.enabled:       ${SECURITY_ENABLED:false}
```

Paramètres à **ne pas changer** (contrat du projet) :

```yaml
spring.jpa.hibernate.ddl-auto: validate   # Hibernate ne crée rien
spring.flyway.enabled:         true        # migrations métier via Flyway
flowable.database-schema-update: true      # tables Flowable gérées par le moteur
flowable.history-level:        audit       # historique complet (lecture du résultat)
```

---

## 5. Compilation (build)

Depuis `backend/` :

```bash
# Compiler le service et ses dépendances (shared-kernel)
.\mvnw.cmd -pl services/workflow-service -am compile
```

`-pl services/workflow-service` cible le module · `-am` (« also make ») construit aussi `shared-kernel`.

---

## 6. Exécution des tests

```bash
# Build propre + tests (recommandé : clean évite les ressources BPMN obsolètes)
.\mvnw.cmd -pl services/workflow-service -am clean test
```

Résultat attendu :

```
[INFO] BUILD SUCCESS
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

> Le test `WorkflowApplicationTest` démarre le contexte Spring complet : il **nécessite
> une base PostgreSQL accessible** (pas de H2). Démarrer PostgreSQL avant.

---

## 7. Démarrage du service

Depuis `backend/`, avec le profil `dev` :

```bash
.\mvnw.cmd -pl services/workflow-service spring-boot:run "-Dspring-boot.run.profiles=dev"
```

> ⚠️ **Ne pas ajouter `-am` avec `spring-boot:run`** : cela inclut le POM parent (sans classe
> `main`) et provoque l'erreur *« Unable to find a suitable main class »*. Si `shared-kernel`
> n'est pas encore dans le dépôt local, l'installer une fois :
> `.\mvnw.cmd -pl services/workflow-service -am -DskipTests install`

Démarrage réussi quand le log affiche :

```
Tomcat started on port 8094 (http)
Started WorkflowApplication in ~6 seconds
```

---

## 8. Vérification

```bash
# Santé du service (pas de header requis sur /actuator)
curl http://localhost:8094/actuator/health
# → {"status":"UP"}

# Endpoint descriptif (nécessite X-Tenant-Id)
curl -H "X-Tenant-Id: tenant-demo" http://localhost:8094/api/v1/_service
```

---

## 9. Utilisation de l'API

**Toute requête métier exige l'en-tête `X-Tenant-Id`** (multi-tenant). Sans lui → `400 Missing X-Tenant-Id header`.

### Cas « haut risque » (score ≥ 70)

```bash
curl -X POST http://localhost:8094/api/v1/workflows/collection/start \
  -H "X-Tenant-Id: tenant-demo" \
  -H "Content-Type: application/json" \
  -d '{"debtId":"DEBT-1001","customerId":"CUST-1001","score":85}'
```

Réponse (`HTTP 201`) :

```json
{
  "processInstanceId": "97612c03-...",
  "debtId": "DEBT-1001", "customerId": "CUST-1001", "score": 85,
  "segment": "HIGH_RISK", "strategy": "PRIORITY_COLLECTION", "status": "COMPLETED"
}
```

### Cas « risque standard » (score < 70)

```bash
curl -X POST http://localhost:8094/api/v1/workflows/collection/start \
  -H "X-Tenant-Id: tenant-demo" \
  -H "Content-Type: application/json" \
  -d '{"debtId":"DEBT-2002","customerId":"CUST-2002","score":40}'
```

Réponse : `segment = LOW_RISK`, `strategy = STANDARD_COLLECTION`, `status = COMPLETED`.

### Cas invalide (validation)

```bash
curl -X POST http://localhost:8094/api/v1/workflows/collection/start \
  -H "X-Tenant-Id: tenant-demo" -H "Content-Type: application/json" \
  -d '{"debtId":"","customerId":"","score":150}'
```

Réponse : `HTTP 400` (format Problem Details RFC 9457) listant les champs invalides
(`debtId`/`customerId` non vides, `score` entre 0 et 100).

> **Sous PowerShell**, utiliser `curl.exe` et échapper les guillemets, ou `Invoke-RestMethod` :
> ```powershell
> curl.exe -X POST "http://localhost:8094/api/v1/workflows/collection/start" `
>   -H "X-Tenant-Id: tenant-demo" -H "Content-Type: application/json" `
>   -d '{\"debtId\":\"DEBT-1001\",\"customerId\":\"CUST-1001\",\"score\":85}'
> ```

---

## 10. Documentation interactive (Swagger UI)

Une fois le service démarré :

| Ressource | URL |
|---|---|
| **Swagger UI** | http://localhost:8094/swagger-ui/index.html |
| Spécification OpenAPI (JSON) | http://localhost:8094/v3/api-docs |

Pour tester un endpoint :

1. Cliquer sur **Authorize**.
2. Renseigner `X-Tenant-Id` = `tenant-demo` puis **Authorize**.
3. Ouvrir `POST /api/v1/workflows/collection/start` → **Try it out** → exécuter.

> Les chemins `/swagger-ui/**` et `/v3/api-docs/**` sont exemptés du filtre tenant
> (comme `/actuator`), ils se chargent donc sans en-tête.

---

## 11. Dépannage

| Symptôme | Cause | Solution |
|---|---|---|
| `FATAL: database "workflow_db" does not exist` | Base non créée / mauvais nom | Créer la base (§3) ou ajuster `DB_URL` |
| `400 Missing X-Tenant-Id header` | En-tête tenant absent | Ajouter `-H "X-Tenant-Id: tenant-demo"` |
| `FlowableObjectNotFoundException ... tenantId '...' not found` | Processus déployé sans tenant mais démarré avec tenant | Déjà corrigé (démarrage *tenantless*) ; faire un `clean` si ressource obsolète |
| `cvc-complex-type ... outgoing` au déploiement BPMN | Fichier BPMN obsolète dans `target/classes` | `.\mvnw.cmd ... clean test` |
| `Unable to find a suitable main class` | `spring-boot:run` lancé avec `-am` | Retirer `-am` (§7) |
| `Port 8094 already in use` | Instance déjà démarrée | Arrêter l'autre instance ou changer `SERVER_PORT` |
| Tests en échec sur la connexion DB | PostgreSQL non démarré | Démarrer PostgreSQL / `docker compose up -d` |

---

## 12. Annexe — Variables d'environnement

| Variable | Défaut (profil dev) | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/workflow_db` | URL JDBC PostgreSQL |
| `DB_USER` | `collector` | Utilisateur base |
| `DB_PASSWORD` | `collector` | Mot de passe base |
| `SERVER_PORT` | `8094` | Port HTTP |
| `SECURITY_ENABLED` | `false` | Active la validation JWT (OAuth2) |
| `OIDC_JWK_SET_URI` | `http://localhost:8180/realms/dept-collector/protocol/openid-connect/certs` | JWKS Keycloak (si sécurité activée) |

### Démarrage avec sécurité activée (optionnel)

```bash
SECURITY_ENABLED=true \
OIDC_JWK_SET_URI=http://localhost:8180/realms/dept-collector/protocol/openid-connect/certs \
.\mvnw.cmd -pl services/workflow-service spring-boot:run
```

Avec la sécurité activée, les requêtes (hors `/actuator/health` et `/actuator/info`) exigent
un JWT valide ; l'en-tête `X-Tenant-Id` reste obligatoire dans tous les cas.
