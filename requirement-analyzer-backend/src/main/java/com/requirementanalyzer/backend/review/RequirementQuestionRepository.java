package com.requirementanalyzer.backend.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequirementQuestionRepository extends JpaRepository<RequirementQuestion, UUID> {
    List<RequirementQuestion> findAllByAnalysisVersion_IdOrderByCreatedAtAsc(UUID analysisVersionId);

    Optional<RequirementQuestion> findByAnalysisVersion_IdAndQuestionId(UUID analysisVersionId, String questionId);
}
