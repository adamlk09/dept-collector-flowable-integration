# Collect frontend

Base Vue 3 du POC de recouvrement.

## Stack

- Vue 3 + TypeScript
- Vite
- Vue Router
- Pinia
- Flowable BPMN/DMN représenté côté interface

## Démarrage

```bash
npm install
npm run dev
```

## Connexion au backend

Les données simulées sont isolées dans `src/services/api.ts`. Remplacer les méthodes par les appels HTTP correspondants sans modifier les vues.

Le profil connecté est isolé dans `src/stores/session.ts`. En production, il doit être alimenté par les claims IAM/OIDC du JWT (`sub`, rôle, tenant et habilitations), jamais par une valeur codée dans les composants.

Variables recommandées :

```bash
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

Contrats frontend attendus :

- `GET /cases`
- `GET /cases/{id}`
- `GET /cases/{id}/decision`
- `GET /cases/{id}/tasks`
- `GET /agents`
- `POST /cases/{id}/actions`
- `POST /cases/{id}/decision`
- `POST /assignments/execute`
- `POST /tasks/{id}/complete`

La version actuelle simule ces opérations dans `localStorage` afin de permettre le développement frontend sans attendre le backend. La couche `src/services/api.ts` constitue l’unique point à remplacer lors du branchement aux services réels.
