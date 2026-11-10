from __future__ import annotations

from uuid import uuid4

from fastapi import FastAPI, Request
from pydantic import BaseModel

from .providers import MockAnalysisProvider
from .schemas import AnalysisRequest, AnalysisResponse


class HealthResponse(BaseModel):
    status: str
    service: str


app = FastAPI(
    title="AI Requirement Analyzer Service",
    version="0.1.0",
    docs_url="/docs",
    redoc_url=None,
)

provider = MockAnalysisProvider()


@app.middleware("http")
async def add_correlation_id(request: Request, call_next):
    correlation_id = request.headers.get("X-Correlation-ID") or str(uuid4())
    response = await call_next(request)
    response.headers["X-Correlation-ID"] = correlation_id
    return response


@app.get("/health", response_model=HealthResponse, tags=["health"])
def health() -> HealthResponse:
    return HealthResponse(status="UP", service="requirement-analyzer-ai-service")


@app.post("/api/v1/analyze", response_model=AnalysisResponse, tags=["analysis"])
def analyze(request: AnalysisRequest) -> AnalysisResponse:
    response = provider.analyze(request)
    response.correlationId = request.projectId.__str__() + "-" + response.correlationId
    return response