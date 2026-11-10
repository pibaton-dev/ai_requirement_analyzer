# Requirement Analyzer AI Service

Python 3.12+, FastAPI, Pydantic, and LangGraph service. The service is currently a health-checked scaffold; analysis routes and parsers are implemented in a later milestone.

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