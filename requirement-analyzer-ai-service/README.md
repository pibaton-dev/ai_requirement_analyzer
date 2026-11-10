# Requirement Analyzer AI Service

Python 3.12+, FastAPI, Pydantic, and LangGraph service. The service includes a health endpoint and a typed analysis contract for structured requirement extraction. The async job orchestration, parser stack, and full LangGraph workflow are planned for the next milestone.

## Service contract

The service exposes a typed request/response model for project analysis:

- `POST /api/v1/analyze`
- Request body includes `projectId`, `projectName`, optional `text`, and optional `documentIds`
- Response includes `projectId`, `projectName`, `correlationId`, `requirements`, `issues`, and `summary`
- Validation rejects empty payloads with a `422` error

## Local development

```bash
python3.12 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Health: `/health`. OpenAPI docs: `/docs`. The future provider adapter will read provider configuration from environment variables; no API key is needed for the current scaffold or tests.

## Tests

```bash
.venv/bin/python -m pytest
```