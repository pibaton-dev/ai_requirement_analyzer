from __future__ import annotations

import hmac
import logging
import os
import re
from uuid import uuid4

from fastapi import FastAPI, Header, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from pydantic import BaseModel, ValidationError
from starlette.responses import JSONResponse

from .providers import MockAnalysisProvider
from .schemas import AnalysisRequest, AnalysisResponse


class HealthResponse(BaseModel):
    status: str
    service: str


class CorrelationIdMiddleware:
    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        headers = dict(scope.get("headers", []))
        candidate = headers.get(b"x-correlation-id", b"").decode("latin-1")
        correlation_id = candidate if re.fullmatch(r"[A-Za-z0-9._:-]{1,64}", candidate) else str(uuid4())
        scope.setdefault("state", {})["correlation_id"] = correlation_id
        received_body_bytes = 0

        async def receive_with_metrics():
            nonlocal received_body_bytes
            message = await receive()
            if message["type"] == "http.request":
                received_body_bytes += len(message.get("body", b""))
            return message

        async def send_with_correlation(message):
            if message["type"] == "http.response.start":
                response_headers = list(message.get("headers", []))
                response_headers.append((b"x-correlation-id", correlation_id.encode("ascii")))
                message["headers"] = response_headers
            await send(message)

        await self.app(scope, receive_with_metrics, send_with_correlation)
        if scope.get("path") == "/api/v1/analyze":
            logger.info("analysis request completed correlation_id=%s body_bytes=%s",
                        correlation_id, received_body_bytes)


app = FastAPI(
    title="AI Requirement Analyzer Service",
    version="0.1.0",
    docs_url="/docs",
    redoc_url=None,
)

provider = MockAnalysisProvider()
logger = logging.getLogger(__name__)

app.add_middleware(CorrelationIdMiddleware)


@app.exception_handler(RequestValidationError)
async def handle_request_validation_error(request: Request, exception: RequestValidationError):
    logger.warning(
        "request validation failed path=%s content_type=%s content_length=%s transfer_encoding=%s errors=%s",
        request.url.path,
        request.headers.get("content-type"),
        request.headers.get("content-length"),
        request.headers.get("transfer-encoding"),
        [{"location": error["loc"], "type": error["type"]} for error in exception.errors()],
    )
    return JSONResponse(status_code=422, content={"detail": "The analysis request is invalid."})


@app.get("/health", response_model=HealthResponse, tags=["health"])
def health() -> HealthResponse:
    return HealthResponse(status="UP", service="requirement-analyzer-ai-service")


@app.post(
    "/api/v1/analyze",
    response_model=AnalysisResponse,
    tags=["analysis"],
    openapi_extra={
        "requestBody": {
            "required": True,
            "content": {"application/json": {"schema": AnalysisRequest.model_json_schema()}},
        },
    },
)
async def analyze(
    http_request: Request,
    service_token: str | None = Header(default=None, alias="X-Service-Token"),
) -> AnalysisResponse:
    expected_token = os.getenv("AI_SERVICE_API_KEY")
    if not expected_token:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI service authentication is not configured.",
        )
    if service_token is None or not hmac.compare_digest(service_token, expected_token):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid service credentials.")

    try:
        request = AnalysisRequest.model_validate_json(await http_request.body())
    except (ValidationError, ValueError) as exception:
        raise HTTPException(status_code=422, detail="The analysis request is invalid.") from exception

    response = provider.analyze(request)
    response.correlationId = http_request.state.correlation_id
    return response