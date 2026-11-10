from __future__ import annotations

from uuid import uuid4

from .schemas import AnalysisIssue, AnalysisRequest, AnalysisResponse, RequirementItem


class AnalysisProvider:
    def analyze(self, request: AnalysisRequest) -> AnalysisResponse:
        raise NotImplementedError


class MockAnalysisProvider(AnalysisProvider):
    def analyze(self, request: AnalysisRequest) -> AnalysisResponse:
        text = (request.text or "").strip()
        summary = text or "Project contains uploaded source documents for analysis."

        requirement = RequirementItem(
            type="FUNCTIONAL",
            title="User authentication and access control",
            description=(
                "The system shall allow users to sign in securely and restrict access to project data "
                "based on ownership and authorization."
            ),
            priority="HIGH",
            confidence=0.92,
        )

        issues = [
            AnalysisIssue(
                severity="MEDIUM",
                message="Verify whether the project requires role-based access beyond project ownership.",
            )
        ]
        if not request.documentIds:
            issues.append(
                AnalysisIssue(
                    severity="LOW",
                    message="No supporting documents were supplied; the analysis is based on project text only.",
                )
            )

        return AnalysisResponse(
            projectId=request.projectId,
            projectName=request.projectName,
            correlationId=str(uuid4()),
            requirements=[requirement],
            issues=issues,
            summary=summary,
        )
