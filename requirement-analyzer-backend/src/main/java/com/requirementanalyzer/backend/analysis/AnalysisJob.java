package com.requirementanalyzer.backend.analysis;

import com.requirementanalyzer.backend.auth.UserAccount;
import com.requirementanalyzer.backend.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_jobs")
public class AnalysisJob {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private UserAccount owner;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "document_ids", length = 2048)
    private String documentIds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AnalysisJobStatus status = AnalysisJobStatus.QUEUED;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "summary", length = 4000)
    private String summary;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected AnalysisJob() {
    }

    public AnalysisJob(Project project, UserAccount owner, String text, String documentIds, String idempotencyKey) {
        this.project = project;
        this.owner = owner;
        this.text = text == null || text.isBlank() ? null : text.trim();
        this.documentIds = documentIds;
        this.idempotencyKey = idempotencyKey;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void markProcessing(String correlationId) {
        this.status = AnalysisJobStatus.PROCESSING;
        this.startedAt = Instant.now();
        this.correlationId = correlationId;
    }

    public void markSucceeded(String summary, String correlationId) {
        this.status = AnalysisJobStatus.SUCCEEDED;
        this.summary = summary;
        this.correlationId = correlationId;
        this.completedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markFailed(String message, String correlationId) {
        this.status = AnalysisJobStatus.FAILED;
        this.errorMessage = message;
        this.correlationId = correlationId;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public String getText() {
        return text;
    }

    public String getDocumentIds() {
        return documentIds;
    }

    public AnalysisJobStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getSummary() {
        return summary;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
