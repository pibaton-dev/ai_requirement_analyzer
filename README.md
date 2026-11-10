# ai_requirement_analyzer

The initial SRS, architecture, API proposal, data model, security boundaries, and milestone plan are in [docs/FOUNDATION_DESIGN.md](docs/FOUNDATION_DESIGN.md).

## Milestone 1: Local Platform

The repository is organized into three independently buildable codebases:

- `requirement-analyzer-frontend/`: React, TypeScript, Vite, Tailwind CSS.
- `requirement-analyzer-backend/`: Java 21, Spring Boot, Maven.
- `requirement-analyzer-ai-service/`: Python 3.12+, FastAPI, Pydantic, LangGraph.

The application features are intentionally not implemented yet; they are scheduled by milestone in the [foundation design](docs/FOUNDATION_DESIGN.md).

### Run locally

Requirements: Docker with Compose. Copy `.env.example` to `.env`, then run:

```bash
docker compose up --build
```

Open the frontend at <http://localhost:5173>. Backend health is at <http://localhost:8080/api/v1/health>, AI service health at <http://localhost:8000/health>, and PostgreSQL is available on localhost port 5432 for local development. The example database password is for local development only.

Stop the containers with `docker compose down`. Add `-v` only when you intend to delete the local database and uploaded-file volumes.

### Verify individual codebases

```bash
cd requirement-analyzer-frontend && npm ci && npm run typecheck && npm test && npm run build
cd requirement-analyzer-backend && mvn test
cd requirement-analyzer-ai-service && python -m venv .venv && .venv/bin/python -m pip install -r requirements.txt && .venv/bin/python -m pytest
```

CI runs the same codebase-specific checks. No production secrets or real LLM API key are needed for this platform milestone.