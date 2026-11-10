package com.requirementanalyzer.backend.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record ProjectRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 5000) String description,
            @Size(max = 120) String domain
    ) {
    }

    public record ProjectResponse(
            UUID id,
            String name,
            String description,
            String domain,
            UUID ownerId,
            ProjectStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }
}