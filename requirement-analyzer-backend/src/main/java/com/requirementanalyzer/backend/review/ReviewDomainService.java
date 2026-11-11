package com.requirementanalyzer.backend.review;

import com.requirementanalyzer.backend.analysis.AnalysisJob;
import com.requirementanalyzer.backend.analysis.AnalysisJobRepository;
import com.requirementanalyzer.backend.analysis.AiAnalysisClient;
import com.requirementanalyzer.backend.auth.UserAccount;
import com.requirementanalyzer.backend.auth.UserAccountRepository;
import com.requirementanalyzer.backend.common.DomainException;
import com.requirementanalyzer.backend.project.Project;
import com.requirementanalyzer.backend.project.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReviewDomainService {

    private final ProjectRepository projectRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final AnalysisVersionRepository analysisVersionRepository;
    private final RequirementRecordRepository requirementRecordRepository;
    private final RequirementQuestionRepository requirementQuestionRepository;
    private final ReviewAuditEventRepository reviewAuditEventRepository;
    private final UserAccountRepository userAccountRepository;

    public ReviewDomainService(
            ProjectRepository projectRepository,
            AnalysisJobRepository analysisJobRepository,
            AnalysisVersionRepository analysisVersionRepository,
            RequirementRecordRepository requirementRecordRepository,
            RequirementQuestionRepository requirementQuestionRepository,
            ReviewAuditEventRepository reviewAuditEventRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.projectRepository = projectRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.analysisVersionRepository = analysisVersionRepository;
        this.requirementRecordRepository = requirementRecordRepository;
        this.requirementQuestionRepository = requirementQuestionRepository;
        this.reviewAuditEventRepository = reviewAuditEventRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public ReviewDtos.AnalysisVersionResponse createVersion(UUID ownerId, UUID projectId, UUID analysisJobId) {
        Project project = findOwnedProject(projectId, ownerId);
        AnalysisJob analysisJob = analysisJobRepository.findByIdAndProject_Owner_IdAndDeletedAtIsNull(analysisJobId, ownerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "ANALYSIS_JOB_NOT_FOUND", "Analysis job was not found."));

        if (!analysisJob.getProject().getId().equals(projectId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "ANALYSIS_JOB_NOT_FOUND", "Analysis job was not found.");
        }
        var existingVersion = analysisVersionRepository.findByAnalysisJob_Id(analysisJobId);
        if (existingVersion.isPresent()) {
            return toResponse(existingVersion.get());
        }

        return toResponse(createVersionRecord(ownerId, project, analysisJob));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistAnalysisResult(UUID ownerId, UUID projectId, UUID analysisJobId,
                                     AiAnalysisClient.AiAnalysisResponse response) {
        if (!projectId.equals(response.projectId())) {
            throw new IllegalStateException("AI response project does not match the analysis job.");
        }
        ReviewDtos.AnalysisVersionResponse savedVersion = createVersion(ownerId, projectId, analysisJobId);
        AnalysisVersion version = analysisVersionRepository.findByIdAndProject_IdAndProject_Owner_Id(
                        savedVersion.id(), projectId, ownerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "ANALYSIS_VERSION_NOT_FOUND",
                        "Analysis version was not found."));
        for (int index = 0; index < response.requirements().size(); index++) {
            AiAnalysisClient.Requirement generated = response.requirements().get(index);
            String requirementId = "REQ-%03d".formatted(index + 1);
            if (requirementRecordRepository.findByAnalysisVersion_IdAndRequirementId(version.getId(), requirementId).isPresent()) {
                continue;
            }
            RequirementRecord requirement = new RequirementRecord(
                    version,
                    requirementId,
                    generated.title(),
                    generated.description(),
                    RequirementPriority.valueOf(generated.priority()),
                    RequirementStatus.PENDING_REVIEW,
                    RequirementSourceType.AI_GENERATED
            );
            requirementRecordRepository.save(requirement);
            recordAudit(version, ownerId, ReviewAction.CREATED, "requirement", requirementId,
                    null, RequirementStatus.PENDING_REVIEW.name(), "AI-generated requirement added for human review.");
        }
    }

    private AnalysisVersion createVersionRecord(UUID ownerId, Project project, AnalysisJob analysisJob) {
        int versionNumber = (int) (analysisVersionRepository.countByProject_Id(project.getId()) + 1L);
        AnalysisVersion version = new AnalysisVersion(project, analysisJob, versionNumber,
                "Version " + versionNumber,
                analysisJob.getSummary() == null ? "Initial analysis review" : analysisJob.getSummary());
        AnalysisVersion saved = analysisVersionRepository.save(version);
        recordAudit(saved, ownerId, ReviewAction.CREATED, "analysis_version", saved.getId().toString(), null,
                saved.getStatus().name(), "Created review version for the analysis job.");
        return saved;
    }

    @Transactional(readOnly = true)
    public ReviewDtos.ReviewSummaryResponse getReview(UUID ownerId, UUID projectId, UUID versionId) {
        AnalysisVersion version = findOwnedVersion(projectId, ownerId, versionId);
        return new ReviewDtos.ReviewSummaryResponse(
                toResponse(version),
                requirementRecordRepository.findAllByAnalysisVersion_IdOrderByCreatedAtAsc(version.getId()).stream()
                        .map(this::toResponse).toList(),
                requirementQuestionRepository.findAllByAnalysisVersion_IdOrderByCreatedAtAsc(version.getId()).stream()
                        .map(this::toQuestionResponse).toList(),
                reviewAuditEventRepository.findAllByAnalysisVersion_IdOrderByCreatedAtDesc(version.getId()).stream()
                        .map(this::toAuditResponse).toList()
        );
    }

    @Transactional
    public ReviewDtos.RequirementResponse addRequirement(UUID ownerId, UUID projectId, UUID versionId,
                                                        ReviewDtos.RequirementRequest request) {
        AnalysisVersion version = findOwnedVersion(projectId, ownerId, versionId);
        if (requirementRecordRepository.findByAnalysisVersion_IdAndRequirementId(versionId, request.requirementId()).isPresent()) {
            throw new DomainException(HttpStatus.CONFLICT, "REQUIREMENT_ALREADY_EXISTS",
                    "A requirement with this identifier already exists for this version.");
        }

        RequirementRecord requirement = new RequirementRecord(
                version,
                request.requirementId(),
                request.title(),
                request.description(),
                request.priority(),
                request.status(),
                RequirementSourceType.MANUAL
        );
        RequirementRecord saved = requirementRecordRepository.save(requirement);
        recordAudit(version, ownerId, ReviewAction.MANUAL_ADDED, "requirement", saved.getRequirementId(),
                null, saved.getStatus().name(), "Requirement added to review.");
        return toResponse(saved);
    }

    @Transactional
    public ReviewDtos.RequirementResponse updateRequirement(UUID ownerId, UUID projectId, UUID versionId, String requirementId,
                                                          ReviewDtos.RequirementUpdateRequest request) {
        AnalysisVersion version = findOwnedVersion(projectId, ownerId, versionId);
        RequirementRecord requirement = requirementRecordRepository.findByAnalysisVersion_IdAndRequirementId(versionId, requirementId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "REQUIREMENT_NOT_FOUND", "Requirement was not found."));

        String previousStatus = requirement.getStatus().name();
        requirement.update(requirement.getTitle(), request.description(), request.priority(), request.status());
        RequirementRecord saved = requirementRecordRepository.save(requirement);
        ReviewAction action = switch (saved.getStatus()) {
            case APPROVED -> ReviewAction.APPROVED;
            case REJECTED -> ReviewAction.REJECTED;
            default -> ReviewAction.UPDATED;
        };
        recordAudit(version, ownerId, action, "requirement", saved.getRequirementId(), previousStatus,
                saved.getStatus().name(), "Requirement updated in review.");
        return toResponse(saved);
    }

    @Transactional
    public ReviewDtos.RequirementQuestionResponse createQuestion(UUID ownerId, UUID projectId, UUID versionId,
                                                                 ReviewDtos.RequirementQuestionRequest request) {
        AnalysisVersion version = findOwnedVersion(projectId, ownerId, versionId);
        if (requirementQuestionRepository.findByAnalysisVersion_IdAndQuestionId(versionId, request.questionId()).isPresent()) {
            throw new DomainException(HttpStatus.CONFLICT, "QUESTION_ALREADY_EXISTS",
                    "A question with this identifier already exists for this version.");
        }

        RequirementQuestion question = new RequirementQuestion(
                version,
                request.questionId(),
                request.question(),
                request.relatedRequirementId(),
                request.reason(),
                request.impact(),
                request.status()
        );
        RequirementQuestion saved = requirementQuestionRepository.save(question);
        recordAudit(version, ownerId, ReviewAction.CREATED, "question", saved.getQuestionId(),
                null, saved.getStatus().name(), "Question added for review.");
        return toQuestionResponse(saved);
    }

    @Transactional
    public ReviewDtos.RequirementQuestionResponse answerQuestion(UUID ownerId, UUID projectId, UUID versionId, UUID questionId,
                                                                 ReviewDtos.QuestionAnswerRequest request) {
        AnalysisVersion version = findOwnedVersion(projectId, ownerId, versionId);
        RequirementQuestion question = requirementQuestionRepository.findById(questionId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", "Question was not found."));
        if (!question.getAnalysisVersion().getId().equals(versionId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", "Question was not found.");
        }

        String previousStatus = question.getStatus().name();
        question.answer(request.answer());
        RequirementQuestion saved = requirementQuestionRepository.save(question);
        recordAudit(version, ownerId, ReviewAction.ANSWERED, "question", saved.getQuestionId(), previousStatus,
                saved.getStatus().name(), "Question answered by the user.");
        return toQuestionResponse(saved);
    }

    private Project findOwnedProject(UUID projectId, UUID ownerId) {
        return projectRepository.findByIdAndOwner_IdAndDeletedAtIsNull(projectId, ownerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project was not found."));
    }

    private AnalysisVersion findOwnedVersion(UUID projectId, UUID ownerId, UUID versionId) {
        return analysisVersionRepository.findByIdAndProject_IdAndProject_Owner_Id(versionId, projectId, ownerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "ANALYSIS_VERSION_NOT_FOUND",
                        "Analysis version was not found."));
    }

    private void recordAudit(AnalysisVersion version, UUID actorId, ReviewAction action,
                             String targetType, String targetKey, String previousStatus, String newStatus,
                             String details) {
        UserAccount actor = userAccountRepository.findById(actorId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found."));
        reviewAuditEventRepository.save(new ReviewAuditEvent(version, actor, action, targetType, targetKey,
                previousStatus, newStatus, details));
    }

    private ReviewDtos.AnalysisVersionResponse toResponse(AnalysisVersion version) {
        return new ReviewDtos.AnalysisVersionResponse(
                version.getId(),
                version.getProject().getId(),
                version.getAnalysisJob().getId(),
                version.getVersionNumber(),
                version.getTitle(),
                version.getSummary(),
                version.getStatus(),
                version.getCreatedAt(),
                version.getUpdatedAt()
        );
    }

    private ReviewDtos.RequirementResponse toResponse(RequirementRecord requirement) {
        return new ReviewDtos.RequirementResponse(
                requirement.getId(),
                requirement.getRequirementId(),
                requirement.getTitle(),
                requirement.getDescription(),
                requirement.getPriority(),
                requirement.getStatus(),
                requirement.getSourceType(),
                requirement.getCreatedAt(),
                requirement.getUpdatedAt()
        );
    }

    private ReviewDtos.RequirementQuestionResponse toQuestionResponse(RequirementQuestion question) {
        return new ReviewDtos.RequirementQuestionResponse(
                question.getId(),
                question.getQuestionId(),
                question.getQuestion(),
                question.getRelatedRequirementId(),
                question.getReason(),
                question.getImpact(),
                question.getStatus(),
                question.getAnswer(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }

    private ReviewDtos.ReviewAuditResponse toAuditResponse(ReviewAuditEvent event) {
        return new ReviewDtos.ReviewAuditResponse(
                event.getId(),
                event.getAction().name(),
                event.getTargetType(),
                event.getTargetKey(),
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getDetails(),
                event.getCreatedAt()
        );
    }
}
