package com.requirementanalyzer.backend.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class AiAnalysisClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AiAnalysisClient(@Value("${ai-service.base-url}") String baseUrl,
                            @Value("${ai-service.api-key}") String apiKey,
                            ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Service-Token", apiKey)
                .build();
        this.objectMapper = objectMapper;
    }

    public AiAnalysisResponse submitAnalysis(AnalysisJob job) {
        AiAnalysisRequest request = new AiAnalysisRequest(
                job.getProject().getId(),
                job.getProject().getName(),
                job.getText(),
                parseDocumentIds(job.getDocumentIds())
        );
        byte[] requestBody;
        try {
            requestBody = objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Analysis request could not be encoded.", exception);
        }

        var response = restClient.post()
                .uri("/api/v1/analyze")
                .header("X-Correlation-ID", job.getCorrelationId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                    throw new IllegalStateException("AI service returned status " + httpResponse.getStatusCode());
                })
                .body(AiAnalysisResponse.class);

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

    public record AiAnalysisRequest(
            UUID projectId,
            String projectName,
            String text,
            List<UUID> documentIds
    ) {
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
