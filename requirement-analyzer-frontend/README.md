# Requirement Analyzer Frontend

React, TypeScript, Vite, Tailwind CSS, and Vitest application.

## Local development

From this directory, install dependencies and start Vite:

```bash
npm ci
npm run dev
```

The frontend uses `/api/v1` as its API base and proxies API requests to `http://localhost:8080` in development. Run the backend and PostgreSQL through the repository's Docker Compose setup.

The current application includes account sign-in/registration, an owner-scoped project dashboard, document upload and removal (PDF, DOCX, TXT, and XLSX, up to 10 MB), analysis submission using project text and/or selected documents, job status/history, human review workflows, and JSON, PDF, and XLSX exports for a review version.

## Checks

```bash
npm run typecheck
npm test
npm run build
```