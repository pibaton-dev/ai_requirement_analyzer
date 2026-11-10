package com.requirementanalyzer.backend.document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DocumentDtos {

    private DocumentDtos() {
    }

    public record DocumentResponse(
            UUID id,
            UUID projectId,
            UUID ownerId,
            String fileName,
            String contentType,
            String fileExtension,
            long fileSize,
            DocumentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }
}
