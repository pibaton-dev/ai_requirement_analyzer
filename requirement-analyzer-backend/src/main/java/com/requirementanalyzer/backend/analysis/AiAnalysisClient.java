package com.requirementanalyzer.backend.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.requirementanalyzer.backend.document.Document;
import com.requirementanalyzer.backend.document.DocumentRepository;
import com.requirementanalyzer.backend.document.DocumentStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
public class AiAnalysisClient {

    private static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;

    public AiAnalysisClient(@Value("${ai-service.base-url}") String baseUrl,
                            @Value("${ai-service.api-key}") String apiKey,
                            ObjectMapper objectMapper,
                            DocumentRepository documentRepository,
                            DocumentStorage documentStorage) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.objectMapper = objectMapper;
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
    }

    public AiAnalysisResponse submitAnalysis(AnalysisJob job) {
        List<UUID> documentIds = parseDocumentIds(job.getDocumentIds());
        AiAnalysisRequest request = new AiAnalysisRequest(
                job.getProject().getId(),
                job.getProject().getName(),
                job.getText(),
                documentIds,
                loadDocuments(job, documentIds)
        );
        byte[] requestBody;
        try {
            requestBody = objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Analysis request could not be encoded.", exception);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/analyze"))
                .timeout(REQUEST_TIMEOUT)
                .header("X-Service-Token", apiKey)
                .header("X-Correlation-ID", job.getCorrelationId())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();
        HttpResponse<byte[]> httpResponse;
        try {
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI service request was interrupted.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("AI service request could not be completed.", exception);
        }
        if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
            throw new IllegalStateException("AI service returned status " + httpResponse.statusCode());
        }
        AiAnalysisResponse response;
        try {
            response = objectMapper.readValue(httpResponse.body(), AiAnalysisResponse.class);
        } catch (IOException exception) {
            throw new IllegalStateException("AI service returned an invalid response.", exception);
        }
        if (response == null) {
            throw new IllegalStateException("AI service returned an empty response.");
        }
        return response;
    }

    private List<UUID> parseDocumentIds(String rawDocumentIds) {
        if (rawDocumentIds == null || rawDocumentIds.isBlank()) {
            return List.of();
        }
        String[] values = rawDocumentIds.split(",");
        return java.util.Arrays.stream(values)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(UUID::fromString)
                .toList();
    }

    private List<DocumentInput> loadDocuments(AnalysisJob job, List<UUID> documentIds) {
        List<DocumentInput> documents = new ArrayList<>(documentIds.size());
        long totalBytes = 0;
        for (UUID id : documentIds) {
            Document document = documentRepository
                    .findByIdAndProject_IdAndProject_Owner_IdAndDeletedAtIsNull(
                            id, job.getProject().getId(), job.getOwner().getId())
                    .orElseThrow(() -> new IllegalStateException("A selected project document is no longer available."));
            try {
                byte[] content = documentStorage.load(document.getStorageKey());
                totalBytes += content.length;
                if (content.length != document.getFileSize() || totalBytes > MAX_DOCUMENT_BYTES) {
                    throw new IllegalStateException("Selected document content does not match the validated analysis limits.");
                }
                documents.add(new DocumentInput(
                        id,
                        document.getFileName(),
                        document.getContentType(),
                        Base64.getEncoder().encodeToString(content)
                ));
            } catch (IOException exception) {
                throw new IllegalStateException("A selected project document could not be read for analysis.", exception);
            }
        }
        return List.copyOf(documents);
    }

    public record AiAnalysisRequest(
            UUID projectId,
            String projectName,
            String text,
            List<UUID> documentIds,
            List<DocumentInput> documents
    ) {
    }

    public record DocumentInput(UUID documentId, String fileName, String contentType, String contentBase64) {
    }

    public record AiAnalysisResponse(
            UUID projectId,
            String projectName,
            String correlationId,
            List<Requirement> requirements,
            List<Issue> issues,
            String summary
    ) {
    }

    public record Requirement(
            String type,
            String title,
            String description,
            String priority,
            double confidence
    ) {
    }

    public record Issue(
            String severity,
            String message
    ) {
    }
}
