package com.requirementanalyzer.backend.analysis;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AnalysisJobDtos {

    private AnalysisJobDtos() {
    }

    public record AnalysisRequest(
            String text,
            List<UUID> documentIds,
            String idempotencyKey
    ) {
    }

    public record AnalysisJobResponse(
            UUID id,
            UUID projectId,
            UUID ownerId,
            String text,
            List<UUID> documentIds,
            AnalysisJobStatus status,
            String correlationId,
            String summary,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt,
            Instant startedAt,
            Instant completedAt
    ) {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }
}
