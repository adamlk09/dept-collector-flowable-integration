# Dept Collector backend

Socle multi-modules des microservices de la plateforme de recouvrement.

## Stack

- Java 21
- Spring Boot 3.5.12
- Maven multi-modules
- PostgreSQL et Flyway
- OAuth2 Resource Server / OIDC
- Flowable 7.2 pour BPMN et DMN
- Actuator, health probes et Problem Details
- Contexte tenant et identifiant de corrélation propagés par en-têtes

## Modules

Le projet contient une passerelle, un noyau partagé et 25 microservices autonomes :

| Domaine | Services |
|---|---|
| Plateforme | tenant, billing, tenant-config, feature-flag, audit, configuration, reference-data |
| Cœur métier | customer, debt, contract, cheque, guarantee, organization, case, payment, legal |
| Décision | segmentation DMN, assignment, scoring |
| Orchestration | workflow BPMN, notification |
| Support | agency, document, kpi, data-contract |

Chaque service possède :

- son application Spring Boot et son port ;
- sa base PostgreSQL dédiée ;
- ses migrations Flyway ;
- un socle Outbox transactionnel ;
- Actuator et des probes de santé ;
- la validation JWT activable ;
- le contexte tenant obligatoire via `X-Tenant-Id` ;
- la corrélation via `X-Correlation-Id`.

## Compiler et tester

```bash
mvn test
```

## Démarrer l’infrastructure locale

```bash
docker compose up -d
```

Le premier démarrage crée une base dédiée par microservice. Pour recréer les bases, supprimer explicitement le volume `postgres-data`.

## Démarrer un service

```bash
mvn -pl services/case-service -am spring-boot:run
```

Tester le socle :

```bash
curl -H 'X-Tenant-Id: tenant-demo' http://localhost:8091/api/v1/_service
curl http://localhost:8091/actuator/health
```

Les ports commencent à `8081` dans l’ordre déclaré dans le `pom.xml`. La passerelle utilise `8080`.

## Sécurité

La sécurité est désactivée uniquement pour faciliter le bootstrap local. Pour activer la validation JWT :

```bash
SECURITY_ENABLED=true \
OIDC_JWK_SET_URI=http://localhost:8180/realms/dept-collector/protocol/openid-connect/certs \
mvn -pl services/case-service -am spring-boot:run
```

Le tenant doit à terme être dérivé d’un claim JWT signé puis comparé à `X-Tenant-Id` par la passerelle. Aucun repository métier ne doit accepter un accès sans `tenant_id`.

## Flowable

- `segmentation-service` embarque le moteur Flowable DMN.
- `workflow-service` embarque le moteur Flowable Process/BPMN.
- L’installation et les paramètres sont centralisés dans ces deux services, sans dupliquer les moteurs dans les services métier.

## Prochaine étape de développement

Les modules exposent actuellement le socle technique et un endpoint descriptif. Les équipes peuvent ajouter les agrégats, repositories, commandes, événements et contrats OpenAPI dans leur bounded context sans modifier les autres services.
