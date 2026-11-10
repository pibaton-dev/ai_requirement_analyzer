package com.requirementanalyzer.backend.document;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Component
public class LocalDocumentStorage implements DocumentStorage {

    private final Path rootDirectory;

    public LocalDocumentStorage(@Value("${app.document-storage.path:${java.io.tmpdir}/requirement-analyzer-documents}") String rootDirectory) {
        this.rootDirectory = Paths.get(rootDirectory);
    }

    @PostConstruct
    void initialize() throws IOException {
        Files.createDirectories(rootDirectory);
    }

    @Override
    public String store(UUID projectId, String originalFileName, byte[] content) throws IOException {
        String safeFileName = sanitizeFileName(originalFileName);
        Path projectDirectory = rootDirectory.resolve(projectId.toString());
        Files.createDirectories(projectDirectory);

        String uniqueFileName = UUID.randomUUID() + "-" + safeFileName;
        Path filePath = projectDirectory.resolve(uniqueFileName).normalize();
        Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        return projectId + "/" + uniqueFileName;
    }

    @Override
    public byte[] load(String storageKey) throws IOException {
        Path filePath = rootDirectory.resolve(storageKey).normalize();
        return Files.readAllBytes(filePath);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Path filePath = rootDirectory.resolve(storageKey).normalize();
        Files.deleteIfExists(filePath);
    }

    private String sanitizeFileName(String originalFileName) {
        String safeBase = originalFileName == null || originalFileName.isBlank()
                ? "document"
                : originalFileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return safeBase.length() > 200 ? safeBase.substring(0, 200) : safeBase;
    }
}
