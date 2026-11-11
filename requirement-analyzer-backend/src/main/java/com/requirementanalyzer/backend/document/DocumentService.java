package com.requirementanalyzer.backend.document;

import com.requirementanalyzer.backend.common.DomainException;
import com.requirementanalyzer.backend.project.Project;
import com.requirementanalyzer.backend.project.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class DocumentService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final long MAX_EXPANDED_ARCHIVE_BYTES = 50L * 1024 * 1024;
    private static final int MAX_ARCHIVE_ENTRIES = 2_000;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt", "xlsx");
    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final DocumentStorage documentStorage;

    public DocumentService(DocumentRepository documentRepository, ProjectRepository projectRepository,
                          DocumentStorage documentStorage) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.documentStorage = documentStorage;
    }

    @Transactional
    public DocumentDtos.DocumentResponse upload(UUID ownerId, UUID projectId, MultipartFile file) {
        Project project = projectRepository.findByIdAndOwner_IdAndDeletedAtIsNull(projectId, ownerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND",
                        "Project was not found."));

        if (file == null || file.isEmpty()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "A document file is required.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new DomainException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                    "Document size must be 10MB or smaller.");
        }

        String fileName = normalizeFileName(file.getOriginalFilename());
        String extension = extensionOf(fileName);
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType().toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension) || !contentTypeMatchesExtension(extension, contentType)) {
            throw new DomainException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_DOCUMENT_TYPE",
                    "Only PDF, DOCX, TXT, and XLSX files are supported.");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "FILE_READ_FAILED", "The document could not be read.");
        }
        if (!contentMatchesExtension(extension, content)) {
            throw new DomainException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_DOCUMENT_CONTENT",
                    "The document content does not match its declared file type.");
        }

        String checksum = checksumOf(content);
        if (documentRepository.existsByProject_IdAndChecksumAndDeletedAtIsNull(projectId, checksum)) {
            throw new DomainException(HttpStatus.CONFLICT, "DOCUMENT_DUPLICATE",
                    "A duplicate document already exists in this project.");
        }

        String storageKey;
        try {
            storageKey = documentStorage.store(projectId, fileName, content);
        } catch (IOException exception) {
            throw new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_WRITE_FAILED",
                    "The document could not be stored securely.");
        }

        Document document = new Document(project, project.getOwner(), fileName, contentType,
                extension, file.getSize(), storageKey, checksum);
        return toResponse(documentRepository.save(document));
    }

    @Transactional(readOnly = true)
    public DocumentDtos.PageResponse<DocumentDtos.DocumentResponse> list(UUID ownerId, UUID projectId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_PAGE",
                    "Page must be non-negative and size must be between 1 and 100.");
        }
        projectRepository.findByIdAndOwner_IdAndDeletedAtIsNull(projectId, ownerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND",
                        "Project was not found."));

        Page<Document> result = documentRepository.findAllByProject_IdAndDeletedAtIsNull(
                projectId, PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        return new DocumentDtos.PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public DocumentDtos.DocumentResponse get(UUID documentId, UUID ownerId) {
        Document document = findOwnedDocument(documentId, ownerId);
        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public byte[] download(UUID documentId, UUID ownerId) {
        Document document = findOwnedDocument(documentId, ownerId);
        try {
            return documentStorage.load(document.getStorageKey());
        } catch (IOException exception) {
            throw new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_READ_FAILED",
                    "The document could not be read from storage.");
        }
    }

    @Transactional
    public void delete(UUID documentId, UUID ownerId) {
        Document document = findOwnedDocument(documentId, ownerId);
        document.softDelete(Instant.now());
        try {
            documentStorage.delete(document.getStorageKey());
        } catch (IOException ignored) {
            // keep soft deletion as the source of truth even if storage cleanup fails
        }
    }

    private Document findOwnedDocument(UUID documentId, UUID ownerId) {
        return documentRepository.findByIdAndProject_Owner_IdAndDeletedAtIsNull(documentId, ownerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND",
                        "Document was not found."));
    }

    private DocumentDtos.DocumentResponse toResponse(Document document) {
        return new DocumentDtos.DocumentResponse(
                document.getId(),
                document.getProject().getId(),
                document.getOwner().getId(),
                document.getFileName(),
                document.getContentType(),
                document.getFileExtension(),
                document.getFileSize(),
                document.getStatus(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }

    private String normalizeFileName(String originalFileName) {
        String candidate = originalFileName == null || originalFileName.isBlank() ? "document" : originalFileName;
        String normalized = candidate.replaceAll("[\\\\/]+", "_");
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }

    private boolean contentTypeMatchesExtension(String extension, String contentType) {
        return switch (extension) {
            case "pdf" -> "application/pdf".equals(contentType);
            case "docx" -> DOCX_CONTENT_TYPE.equals(contentType);
            case "txt" -> "text/plain".equals(contentType);
            case "xlsx" -> XLSX_CONTENT_TYPE.equals(contentType);
            default -> false;
        };
    }

    private boolean contentMatchesExtension(String extension, byte[] content) {
        return switch (extension) {
            case "pdf" -> startsWith(content, "%PDF-".getBytes(StandardCharsets.US_ASCII));
            case "docx" -> isOfficeArchive(content, "word/document.xml");
            case "xlsx" -> isOfficeArchive(content, "xl/workbook.xml");
            case "txt" -> isUtf8Text(content);
            default -> false;
        };
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isUtf8Text(byte[] content) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
            return !text.isBlank() && text.indexOf('\0') < 0;
        } catch (CharacterCodingException exception) {
            return false;
        }
    }

    private boolean isOfficeArchive(byte[] content, String requiredDocumentEntry) {
        if (content.length < 4 || content[0] != 'P' || content[1] != 'K') {
            return false;
        }
        Set<String> entries = new HashSet<>();
        long expandedBytes = 0;
        int entryCount = 0;
        byte[] buffer = new byte[8_192];
        try (ZipInputStream archive = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ARCHIVE_ENTRIES) {
                    return false;
                }
                entries.add(entry.getName());
                int read;
                while ((read = archive.read(buffer)) != -1) {
                    expandedBytes += read;
                    if (expandedBytes > MAX_EXPANDED_ARCHIVE_BYTES) {
                        return false;
                    }
                }
            }
        } catch (IOException exception) {
            return false;
        }
        return entries.contains("[Content_Types].xml") && entries.contains(requiredDocumentEntry);
    }

    private String checksumOf(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder hex = new StringBuilder();
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
