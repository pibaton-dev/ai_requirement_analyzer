# ai_requirement_analyzer

The initial SRS, architecture, API proposal, data model, security boundaries, and milestone plan are in [docs/FOUNDATION_DESIGN.md](docs/FOUNDATION_DESIGN.md).

## Project status

The repository is organized into three independently buildable codebases:

- `requirement-analyzer-frontend/`: React, TypeScript, Vite, Tailwind CSS.
- `requirement-analyzer-backend/`: Java 21, Spring Boot, Maven.
- `requirement-analyzer-ai-service/`: Python 3.12+, FastAPI, Pydantic, LangGraph.

Milestones 1 and 2 are complete; Milestones 0 and 3–8 remain partial. Uploaded PDF, DOCX, TXT, and XLSX files are now parsed by the AI service, and analysis drafts are saved into the review workspace. Analysis currently uses a deterministic mock provider that generates one draft requirement; it is not a configured LLM or the full requirement-analysis workflow. See the [milestone comparison and remaining gaps](docs/FOUNDATION_DESIGN.md#12-milestones) and [deployment notes](docs/DEPLOYMENT.md).

The largest remaining gaps are real provider/LangGraph integration; generation and persistence of stories, acceptance criteria, business rules, validation issues, questions, prioritization, and source references; retry/cancellation; full review and result views; and end-to-end/security/production hardening.

### Run locally

Requirements: Docker with Compose. Copy `.env.example` to `.env`, then run:

```bash
docker compose up --build
```

Open the frontend at <http://localhost:5173>. Backend health is at <http://localhost:8080/api/v1/health>, AI service health at <http://localhost:8000/health>, and PostgreSQL is available on localhost port 5432 for local development. The example database password is for local development only.

Stop the containers with `docker compose down`. Add `-v` only when you intend to delete the local database and uploaded-file volumes.

### Verify individual codebases

```bash
npm --prefix requirement-analyzer-frontend ci
npm --prefix requirement-analyzer-frontend run typecheck
npm --prefix requirement-analyzer-frontend test
npm --prefix requirement-analyzer-frontend run build
mvn -f requirement-analyzer-backend/pom.xml verify
python3 -m venv requirement-analyzer-ai-service/.venv
requirement-analyzer-ai-service/.venv/bin/python -m pip install -r requirement-analyzer-ai-service/requirements.txt
(cd requirement-analyzer-ai-service && .venv/bin/python -m pytest)
```

CI runs the same codebase-specific checks. No production secrets or real LLM API key are needed for local verification.