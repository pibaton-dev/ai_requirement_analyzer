package com.requirementanalyzer.backend.analysis;

import com.requirementanalyzer.backend.auth.UserAccount;
import com.requirementanalyzer.backend.auth.UserAccountRepository;
import com.requirementanalyzer.backend.common.DomainException;
import com.requirementanalyzer.backend.project.Project;
import com.requirementanalyzer.backend.project.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
public class AnalysisJobService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AnalysisJobRepository analysisJobRepository;
    private final ProjectRepository projectRepository;
    private final UserAccountRepository userRepository;
    private final AnalysisJobProcessor analysisJobProcessor;

    public AnalysisJobService(AnalysisJobRepository analysisJobRepository,
                             ProjectRepository projectRepository,
                             UserAccountRepository userRepository,
                             AnalysisJobProcessor analysisJobProcessor) {
        this.analysisJobRepository = analysisJobRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.analysisJobProcessor = analysisJobProcessor;
    }

    @Transactional
    public AnalysisJobDtos.AnalysisJobResponse submit(UUID ownerId, UUID projectId, AnalysisJobDtos.AnalysisRequest request) {
        Project project = projectRepository.findByIdAndOwner_IdAndDeletedAtIsNull(projectId, ownerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project was not found."));

        String normalizedText = request == null || request.text() == null ? "" : request.text().trim();
        List<UUID> documentIds = request == null || request.documentIds() == null ? List.of() : request.documentIds();
        if (normalizedText.isEmpty() && (documentIds == null || documentIds.isEmpty())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Either text or at least one documentId is required.");
        }

        String idempotencyKey = request == null || request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                ? null : request.idempotencyKey().trim();
        if (idempotencyKey != null) {
            var existingJob = analysisJobRepository.findByProject_IdAndIdempotencyKeyAndDeletedAtIsNull(projectId, idempotencyKey);
            if (existingJob.isPresent()) {
                return toResponse(existingJob.get());
            }
        }

        UserAccount owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found."));

        String documentIdsCsv = documentIds.isEmpty() ? null : String.join(",", documentIds.stream().map(UUID::toString).toList());
        AnalysisJob job = new AnalysisJob(project, owner, normalizedText, documentIdsCsv, idempotencyKey);
        AnalysisJob saved = analysisJobRepository.save(job);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    analysisJobProcessor.process(saved.getId());
                }
            });
        } else {
            analysisJobProcessor.process(saved.getId());
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AnalysisJobDtos.AnalysisJobResponse get(UUID jobId, UUID ownerId) {
        return toResponse(analysisJobRepository.findByIdAndProject_Owner_IdAndDeletedAtIsNull(jobId, ownerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "ANALYSIS_JOB_NOT_FOUND", "Analysis job was not found.")));
    }

    @Transactional(readOnly = true)
    public AnalysisJobDtos.PageResponse<AnalysisJobDtos.AnalysisJobResponse> list(UUID ownerId, UUID projectId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "Page must be non-negative and size between 1 and 100.");
        }
        projectRepository.findByIdAndOwner_IdAndDeletedAtIsNull(projectId, ownerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project was not found."));

        Page<AnalysisJob> jobs = analysisJobRepository.findAllByProject_IdAndDeletedAtIsNull(
                projectId, PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new AnalysisJobDtos.PageResponse<>(
                jobs.getContent().stream().map(this::toResponse).toList(),
                jobs.getNumber(), jobs.getSize(), jobs.getTotalElements(), jobs.getTotalPages());
    }

    private AnalysisJobDtos.AnalysisJobResponse toResponse(AnalysisJob job) {
        List<UUID> parsedDocumentIds = job.getDocumentIds() == null || job.getDocumentIds().isBlank()
                ? List.of()
                : java.util.Arrays.stream(job.getDocumentIds().split(","))
                    .map(String::trim)
                    .filter(candidate -> !candidate.isEmpty())
                    .map(UUID::fromString)
                    .toList();

        return new AnalysisJobDtos.AnalysisJobResponse(
                job.getId(),
                job.getProject().getId(),
                job.getOwner().getId(),
                job.getText(),
                parsedDocumentIds,
                job.getStatus(),
                job.getCorrelationId(),
                job.getSummary(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getStartedAt(),
                job.getCompletedAt());
    }
}
