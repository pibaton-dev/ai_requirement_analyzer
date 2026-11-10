package com.requirementanalyzer.backend.review;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1")
public class ReviewDomainController {

    private final ReviewDomainService reviewDomainService;

    public ReviewDomainController(ReviewDomainService reviewDomainService) {
        this.reviewDomainService = reviewDomainService;
    }

    @PostMapping("/projects/{projectId}/analyses/{analysisJobId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDtos.AnalysisVersionResponse createVersion(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID projectId,
            @PathVariable UUID analysisJobId
    ) {
        return reviewDomainService.createVersion(ownerId, projectId, analysisJobId);
    }

    @GetMapping("/projects/{projectId}/analysis-versions/{versionId}/review")
    public ReviewDtos.ReviewSummaryResponse getReview(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID projectId,
            @PathVariable UUID versionId
    ) {
        return reviewDomainService.getReview(ownerId, projectId, versionId);
    }

    @PostMapping("/projects/{projectId}/analysis-versions/{versionId}/requirements")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDtos.RequirementResponse addRequirement(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID projectId,
            @PathVariable UUID versionId,
            @Valid @RequestBody ReviewDtos.RequirementRequest request
    ) {
        return reviewDomainService.addRequirement(ownerId, projectId, versionId, request);
    }

    @PutMapping("/projects/{projectId}/analysis-versions/{versionId}/requirements/{requirementId}")
    public ReviewDtos.RequirementResponse updateRequirement(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID projectId,
            @PathVariable UUID versionId,
            @PathVariable String requirementId,
            @Valid @RequestBody ReviewDtos.RequirementUpdateRequest request
    ) {
        return reviewDomainService.updateRequirement(ownerId, projectId, versionId, requirementId, request);
    }

    @PostMapping("/projects/{projectId}/analysis-versions/{versionId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDtos.RequirementQuestionResponse createQuestion(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID projectId,
            @PathVariable UUID versionId,
            @Valid @RequestBody ReviewDtos.RequirementQuestionRequest request
    ) {
        return reviewDomainService.createQuestion(ownerId, projectId, versionId, request);
    }

    @PostMapping("/projects/{projectId}/analysis-versions/{versionId}/questions/{questionId}/answer")
    public ReviewDtos.RequirementQuestionResponse answerQuestion(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID projectId,
            @PathVariable UUID versionId,
            @PathVariable UUID questionId,
            @Valid @RequestBody ReviewDtos.QuestionAnswerRequest request
    ) {
        return reviewDomainService.answerQuestion(ownerId, projectId, versionId, questionId, request);
    }
}
