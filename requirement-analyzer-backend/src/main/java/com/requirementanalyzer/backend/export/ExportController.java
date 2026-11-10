package com.requirementanalyzer.backend.export;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/projects/{projectId}/export/json")
    public ResponseEntity<ExportDtos.ProjectExportResponse> exportProject(@AuthenticationPrincipal UUID ownerId,
                                                                          @PathVariable UUID projectId) {
        return ResponseEntity.ok(exportService.exportLatestProjectVersion(ownerId, projectId));
    }

    @GetMapping("/projects/{projectId}/analysis-versions/{versionId}/export/json")
    public ResponseEntity<ExportDtos.ProjectExportResponse> exportVersion(@AuthenticationPrincipal UUID ownerId,
                                                                        @PathVariable UUID projectId,
                                                                        @PathVariable UUID versionId) {
        return ResponseEntity.ok(exportService.exportProjectVersion(ownerId, projectId, versionId));
    }
}
