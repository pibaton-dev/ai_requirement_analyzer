package com.requirementanalyzer.backend.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReviewDtos {

    public record AnalysisVersionResponse(
            UUID id,
            UUID projectId,
            UUID analysisJobId,
            int versionNumber,
            String title,
            String summary,
            AnalysisVersionStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record RequirementRequest(
            @NotBlank String requirementId,
            @NotBlank String title,
            String description,
            @NotNull RequirementPriority priority,
            @NotNull RequirementStatus status
    ) {
    }

    public record RequirementUpdateRequest(
            String description,
            RequirementPriority priority,
            RequirementStatus status
    ) {
    }

    public record RequirementResponse(
            UUID id,
            String requirementId,
            String title,
            String description,
            RequirementPriority priority,
            RequirementStatus status,
            RequirementSourceType sourceType,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record RequirementQuestionRequest(
            @NotBlank String questionId,
            @NotBlank String question,
            String relatedRequirementId,
            String reason,
            String impact,
            @NotNull QuestionStatus status
    ) {
    }

    public record QuestionAnswerRequest(
            @NotBlank String answer
    ) {
    }

    public record RequirementQuestionResponse(
            UUID id,
            String questionId,
            String question,
            String relatedRequirementId,
            String reason,
            String impact,
            QuestionStatus status,
            String answer,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ReviewAuditResponse(
            UUID id,
            String action,
            String targetType,
            String targetKey,
            String previousStatus,
            String newStatus,
            String details,
            Instant createdAt
    ) {
    }

    public record ReviewSummaryResponse(
            AnalysisVersionResponse version,
            List<RequirementResponse> requirements,
            List<RequirementQuestionResponse> questions,
            List<ReviewAuditResponse> auditEvents
    ) {
    }
}
