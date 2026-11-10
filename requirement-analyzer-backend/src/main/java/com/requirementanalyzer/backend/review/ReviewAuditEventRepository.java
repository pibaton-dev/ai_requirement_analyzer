package com.requirementanalyzer.backend.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewAuditEventRepository extends JpaRepository<ReviewAuditEvent, UUID> {
    List<ReviewAuditEvent> findAllByAnalysisVersion_IdOrderByCreatedAtDesc(UUID analysisVersionId);
}
