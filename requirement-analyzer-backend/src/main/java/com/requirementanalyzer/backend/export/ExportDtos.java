package com.requirementanalyzer.backend.export;

import com.requirementanalyzer.backend.project.ProjectDtos;
import com.requirementanalyzer.backend.review.ReviewDtos;

import java.time.Instant;
import java.util.List;

public final class ExportDtos {

    private ExportDtos() {
    }

    public record ProjectExportResponse(
            ProjectDtos.ProjectResponse project,
            ReviewDtos.AnalysisVersionResponse version,
            List<ReviewDtos.RequirementResponse> requirements,
            List<ReviewDtos.RequirementQuestionResponse> questions,
            List<ReviewDtos.ReviewAuditResponse> auditEvents,
            Instant exportedAt
    ) {
    }
}
