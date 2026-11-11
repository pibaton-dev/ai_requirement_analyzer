from __future__ import annotations

from typing import Literal
from uuid import UUID

from pydantic import BaseModel, Field, model_validator

MAX_DOCUMENTS_PER_ANALYSIS = 10
MAX_DOCUMENT_BASE64_LENGTH = 14_000_000


class RequirementItem(BaseModel):
    type: Literal["FUNCTIONAL", "NON_FUNCTIONAL", "BUSINESS", "TECHNICAL"]
    title: str = Field(min_length=1, max_length=200)
    description: str = Field(min_length=1, max_length=4000)
    priority: Literal["LOW", "MEDIUM", "HIGH"]
    confidence: float = Field(ge=0.0, le=1.0)


class AnalysisIssue(BaseModel):
    severity: Literal["LOW", "MEDIUM", "HIGH"]
    message: str = Field(min_length=1, max_length=1000)


class AnalysisDocument(BaseModel):
    documentId: UUID
    fileName: str = Field(min_length=1, max_length=255)
    contentType: str = Field(min_length=1, max_length=128)
    contentBase64: str = Field(min_length=1, max_length=MAX_DOCUMENT_BASE64_LENGTH)


class AnalysisRequest(BaseModel):
    projectId: UUID
    projectName: str = Field(min_length=1, max_length=200)
    text: str | None = None
    documentIds: list[UUID] = Field(default_factory=list)
    documents: list[AnalysisDocument] = Field(default_factory=list, max_length=MAX_DOCUMENTS_PER_ANALYSIS)

    @model_validator(mode="after")
    def validate_input(self) -> "AnalysisRequest":
        combined_text = (self.text or "").strip()
        if len(set(self.documentIds)) != len(self.documentIds):
            raise ValueError("Duplicate document identifiers are not allowed.")
        supplied_document_ids = [document.documentId for document in self.documents]
        if (
            len(supplied_document_ids) != len(self.documentIds)
            or len(set(supplied_document_ids)) != len(supplied_document_ids)
            or set(self.documentIds) != set(supplied_document_ids)
        ):
            raise ValueError("Each selected document must include its authorized content.")
        if not combined_text and not self.documents:
            raise ValueError("Either text or at least one documentId is required.")
        return self


class AnalysisResponse(BaseModel):
    projectId: UUID
    projectName: str
    correlationId: str
    requirements: list[RequirementItem]
    issues: list[AnalysisIssue]
    summary: str
