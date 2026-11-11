package com.requirementanalyzer.backend.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisVersionRepository extends JpaRepository<AnalysisVersion, UUID> {
    long countByProject_Id(UUID projectId);

    Optional<AnalysisVersion> findByAnalysisJob_Id(UUID analysisJobId);

    Optional<AnalysisVersion> findByIdAndProject_IdAndProject_Owner_Id(UUID versionId, UUID projectId, UUID ownerId);

    Optional<AnalysisVersion> findTopByProject_IdAndProject_Owner_IdOrderByVersionNumberDesc(UUID projectId, UUID ownerId);
}
