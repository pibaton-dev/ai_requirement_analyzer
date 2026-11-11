package com.requirementanalyzer.backend.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class AnalysisJobProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisJobProcessor.class);
    private static final String FAILURE_MESSAGE = "Analysis could not be completed. Please try again.";

    private final AnalysisJobRepository analysisJobRepository;
    private final AiAnalysisClient aiAnalysisClient;

    public AnalysisJobProcessor(AnalysisJobRepository analysisJobRepository, AiAnalysisClient aiAnalysisClient) {
        this.analysisJobRepository = analysisJobRepository;
        this.aiAnalysisClient = aiAnalysisClient;
    }

    @Async
    @Transactional
    public void process(UUID jobId) {
        AnalysisJob job = analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis job not found: " + jobId));

        String correlationId = java.util.UUID.randomUUID().toString();
        job.markProcessing(correlationId);
        analysisJobRepository.save(job);

        try {
            AiAnalysisClient.AiAnalysisResponse response = aiAnalysisClient.submitAnalysis(job);
            job.markSucceeded(response.summary(), response.correlationId() == null ? correlationId : response.correlationId());
            analysisJobRepository.save(job);
        } catch (Exception exception) {
            LOGGER.error("Analysis failed for job {} with correlation {} ({}).",
                    jobId, correlationId, exception.getClass().getSimpleName());
            job.markFailed(FAILURE_MESSAGE, correlationId);
            analysisJobRepository.save(job);
        }
    }
}
