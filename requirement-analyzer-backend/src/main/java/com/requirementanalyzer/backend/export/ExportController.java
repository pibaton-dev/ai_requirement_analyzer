package com.requirementanalyzer.backend.export;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ExportController {

    private final ExportService exportService;
    private final ExportDocumentRenderer exportDocumentRenderer;

    public ExportController(ExportService exportService, ExportDocumentRenderer exportDocumentRenderer) {
        this.exportService = exportService;
        this.exportDocumentRenderer = exportDocumentRenderer;
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

    @GetMapping("/projects/{projectId}/export/pdf")
    public ResponseEntity<byte[]> exportProjectPdf(@AuthenticationPrincipal UUID ownerId,
                                                   @PathVariable UUID projectId) {
        ExportDtos.ProjectExportResponse export = exportService.exportLatestProjectVersion(ownerId, projectId);
        return asDownload(exportDocumentRenderer.toPdf(export), export, "pdf",
                MediaType.APPLICATION_PDF);
    }

    @GetMapping("/projects/{projectId}/analysis-versions/{versionId}/export/pdf")
    public ResponseEntity<byte[]> exportVersionPdf(@AuthenticationPrincipal UUID ownerId,
                                                   @PathVariable UUID projectId,
                                                   @PathVariable UUID versionId) {
        ExportDtos.ProjectExportResponse export = exportService.exportProjectVersion(ownerId, projectId, versionId);
        return asDownload(exportDocumentRenderer.toPdf(export), export, "pdf",
                MediaType.APPLICATION_PDF);
    }

    @GetMapping("/projects/{projectId}/export/xlsx")
    public ResponseEntity<byte[]> exportProjectXlsx(@AuthenticationPrincipal UUID ownerId,
                                                    @PathVariable UUID projectId) {
        ExportDtos.ProjectExportResponse export = exportService.exportLatestProjectVersion(ownerId, projectId);
        return asDownload(exportDocumentRenderer.toXlsx(export), export, "xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/projects/{projectId}/analysis-versions/{versionId}/export/xlsx")
    public ResponseEntity<byte[]> exportVersionXlsx(@AuthenticationPrincipal UUID ownerId,
                                                    @PathVariable UUID projectId,
                                                    @PathVariable UUID versionId) {
        ExportDtos.ProjectExportResponse export = exportService.exportProjectVersion(ownerId, projectId, versionId);
        return asDownload(exportDocumentRenderer.toXlsx(export), export, "xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    private ResponseEntity<byte[]> asDownload(byte[] content, ExportDtos.ProjectExportResponse export,
                                              String extension, MediaType mediaType) {
        String projectName = export.project().name().toLowerCase().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String version = export.version() == null ? "" : "-v" + export.version().versionNumber();
        String filename = (projectName.isBlank() ? "project" : projectName) + version + "." + extension;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build().toString())
                .body(content);
    }
}
