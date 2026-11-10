package com.requirementanalyzer.backend.analysis;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1")
public class AnalysisJobController {

    private final AnalysisJobService analysisJobService;

    public AnalysisJobController(AnalysisJobService analysisJobService) {
        this.analysisJobService = analysisJobService;
    }

    @PostMapping("/projects/{projectId}/analyses")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AnalysisJobDtos.AnalysisJobResponse submit(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID projectId,
            @Valid @RequestBody AnalysisJobDtos.AnalysisRequest request
    ) {
        return analysisJobService.submit(ownerId, projectId, request);
    }

    @GetMapping("/projects/{projectId}/analyses")
    public AnalysisJobDtos.PageResponse<AnalysisJobDtos.AnalysisJobResponse> list(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return analysisJobService.list(ownerId, projectId, page, size);
    }

    @GetMapping("/analyses/{analysisId}")
    public AnalysisJobDtos.AnalysisJobResponse get(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID analysisId
    ) {
        return analysisJobService.get(analysisId, ownerId);
    }
}
