package com.requirementanalyzer.backend.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequirementRecordRepository extends JpaRepository<RequirementRecord, UUID> {
    List<RequirementRecord> findAllByAnalysisVersion_IdOrderByCreatedAtAsc(UUID analysisVersionId);

    Optional<RequirementRecord> findByAnalysisVersion_IdAndRequirementId(UUID analysisVersionId, String requirementId);
}
