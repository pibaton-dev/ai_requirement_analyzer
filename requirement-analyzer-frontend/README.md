# Requirement Analyzer Frontend

React, TypeScript, Vite, Tailwind CSS, and Vitest application.

## Local development

From this directory, install dependencies and start Vite:

```bash
npm ci
npm run dev
```

The frontend uses `/api/v1` as its API base and proxies API requests to `http://localhost:8080` in development. Run the backend and PostgreSQL through the repository's Docker Compose setup. The current screen is a platform health view; account and project workflows are scheduled for later frontend milestones.

## Checks

```bash
npm run typecheck
npm test
npm run build
```