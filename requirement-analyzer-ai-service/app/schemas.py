from __future__ import annotations

from typing import Literal
from uuid import UUID

from pydantic import BaseModel, Field, model_validator


class RequirementItem(BaseModel):
    type: Literal["FUNCTIONAL", "NON_FUNCTIONAL", "BUSINESS", "TECHNICAL"]
    title: str = Field(min_length=1, max_length=200)
    description: str = Field(min_length=1, max_length=4000)
    priority: Literal["LOW", "MEDIUM", "HIGH"]
    confidence: float = Field(ge=0.0, le=1.0)


class AnalysisIssue(BaseModel):
    severity: Literal["LOW", "MEDIUM", "HIGH"]
    message: str = Field(min_length=1, max_length=1000)


class AnalysisRequest(BaseModel):
    projectId: UUID
    projectName: str = Field(min_length=1, max_length=200)
    text: str | None = None
    documentIds: list[UUID] = Field(default_factory=list)

    @model_validator(mode="after")
    def validate_input(self) -> "AnalysisRequest":
        combined_text = (self.text or "").strip()
        if not combined_text and not self.documentIds:
            raise ValueError("Either text or at least one documentId is required.")
        return self


class AnalysisResponse(BaseModel):
    projectId: UUID
    projectName: str
    correlationId: str
    requirements: list[RequirementItem]
    issues: list[AnalysisIssue]
    summary: str
