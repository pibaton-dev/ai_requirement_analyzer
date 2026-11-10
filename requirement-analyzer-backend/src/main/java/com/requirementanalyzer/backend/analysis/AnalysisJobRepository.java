package com.requirementanalyzer.backend.analysis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, UUID> {
    Optional<AnalysisJob> findByProject_IdAndIdempotencyKeyAndDeletedAtIsNull(UUID projectId, String idempotencyKey);

    Page<AnalysisJob> findAllByProject_IdAndDeletedAtIsNull(UUID projectId, Pageable pageable);

    Optional<AnalysisJob> findByIdAndProject_Owner_IdAndDeletedAtIsNull(UUID id, UUID ownerId);
}
