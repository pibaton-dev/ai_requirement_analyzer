from __future__ import annotations

import re
from uuid import uuid4

from .schemas import AnalysisIssue, AnalysisRequest, AnalysisResponse, RequirementItem


class AnalysisProvider:
    def analyze(self, request: AnalysisRequest, extracted_documents: list[str]) -> AnalysisResponse:
        raise NotImplementedError


class MockAnalysisProvider(AnalysisProvider):
    def analyze(self, request: AnalysisRequest, extracted_documents: list[str]) -> AnalysisResponse:
        text = "\n".join(part for part in [request.text or "", *extracted_documents] if part.strip()).strip()
        summary = text[:4000]
        first_statement = next((part.strip() for part in re.split(r"[\n.!?]+", text) if part.strip()), "")
        requirement = RequirementItem(
            type="FUNCTIONAL",
            title=first_statement[:200],
            description=first_statement[:4000],
            priority="MEDIUM",
            confidence=0.65,
        )

        issues = [
            AnalysisIssue(
                severity="MEDIUM",
                message="Mock analysis is a deterministic demonstration and does not replace a configured language model.",
            )
        ]

        return AnalysisResponse(
            projectId=request.projectId,
            projectName=request.projectName,
            correlationId=str(uuid4()),
            requirements=[requirement],
            issues=issues,
            summary=summary,
        )
