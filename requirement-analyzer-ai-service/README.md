# Requirement Analyzer AI Service

Python 3.12+, FastAPI, Pydantic, PyMuPDF, python-docx, and openpyxl service. Spring remains the owner of business records and sends selected, owner-authorized documents to this internal service for parsing.

## Service contract

The service exposes a typed request/response model for project analysis:

- `POST /api/v1/analyze`
- Request body includes `projectId`, `projectName`, optional `text`, `documentIds`, and base64 document content for each selected document
- Response includes `projectId`, `projectName`, `correlationId`, `requirements`, `issues`, and `summary`
- Validation rejects empty payloads with a `422` error
- Supported document types are PDF, DOCX, UTF-8 TXT, and XLSX. Parsing enforces per-file, page, archive-expansion, worksheet, cell, and extracted-text limits.
- The default provider is deterministic mock output based on extracted input; it is for local demonstration and is not an LLM integration.

## Local development

```bash
python3.12 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Health: `/health`. OpenAPI docs: `/docs`. Internal analysis requests require `AI_SERVICE_API_KEY`; no LLM key is needed for the mock provider or tests.

## Tests

```bash
.venv/bin/python -m pytest
```