package com.requirementanalyzer.backend.document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/projects/{projectId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDtos.DocumentResponse upload(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID projectId,
            @RequestParam("file") MultipartFile file
    ) {
        return documentService.upload(ownerId, projectId, file);
    }

    @GetMapping("/projects/{projectId}/documents")
    public DocumentDtos.PageResponse<DocumentDtos.DocumentResponse> list(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return documentService.list(ownerId, projectId, page, size);
    }

    @GetMapping("/documents/{documentId}")
    public DocumentDtos.DocumentResponse get(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID documentId
    ) {
        return documentService.get(documentId, ownerId);
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> download(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID documentId
    ) {
        DocumentDtos.DocumentResponse document = documentService.get(documentId, ownerId);
        byte[] content = documentService.download(documentId, ownerId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.fileName(), java.nio.charset.StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(document.contentType()))
                .body(content);
    }

    @DeleteMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID documentId
    ) {
        documentService.delete(documentId, ownerId);
    }
}
