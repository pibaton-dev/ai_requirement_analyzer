from fastapi import FastAPI
from pydantic import BaseModel


class HealthResponse(BaseModel):
    status: str
    service: str


app = FastAPI(
    title="AI Requirement Analyzer Service",
    version="0.1.0",
    docs_url="/docs",
    redoc_url=None,
)


@app.get("/health", response_model=HealthResponse, tags=["health"])
def health() -> HealthResponse:
    return HealthResponse(status="UP", service="requirement-analyzer-ai-service")