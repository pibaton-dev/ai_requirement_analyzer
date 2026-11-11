package com.requirementanalyzer.backend.export;

import com.requirementanalyzer.backend.project.ProjectDtos;
import com.requirementanalyzer.backend.project.ProjectService;
import com.requirementanalyzer.backend.review.AnalysisVersionRepository;
import com.requirementanalyzer.backend.review.ReviewDomainService;
import com.requirementanalyzer.backend.review.ReviewDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ExportService {

    private final ProjectService projectService;
    private final AnalysisVersionRepository analysisVersionRepository;
    private final ReviewDomainService reviewDomainService;

    public ExportService(ProjectService projectService,
                        AnalysisVersionRepository analysisVersionRepository,
                        ReviewDomainService reviewDomainService) {
        this.projectService = projectService;
        this.analysisVersionRepository = analysisVersionRepository;
        this.reviewDomainService = reviewDomainService;
    }

    @Transactional(readOnly = true)
    public ExportDtos.ProjectExportResponse exportProjectVersion(UUID ownerId, UUID projectId, UUID versionId) {
        ProjectDtos.ProjectResponse project = projectService.get(projectId, ownerId);
        ReviewDtos.ReviewSummaryResponse review = reviewDomainService.getReview(ownerId, projectId, versionId);
        return new ExportDtos.ProjectExportResponse(
                project,
                review.version(),
                review.requirements(),
                review.questions(),
                review.auditEvents(),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public ExportDtos.ProjectExportResponse exportLatestProjectVersion(UUID ownerId, UUID projectId) {
        ProjectDtos.ProjectResponse project = projectService.get(projectId, ownerId);
        var latestVersion = analysisVersionRepository.findTopByProject_IdAndProject_Owner_IdOrderByVersionNumberDesc(projectId, ownerId);

        if (latestVersion.isEmpty()) {
            return new ExportDtos.ProjectExportResponse(project, null, null, null, null, Instant.now());
        }

        ReviewDtos.ReviewSummaryResponse review = reviewDomainService.getReview(ownerId, projectId, latestVersion.get().getId());
        return new ExportDtos.ProjectExportResponse(
                project,
                review.version(),
                review.requirements(),
                review.questions(),
                review.auditEvents(),
                Instant.now()
        );
    }

}
