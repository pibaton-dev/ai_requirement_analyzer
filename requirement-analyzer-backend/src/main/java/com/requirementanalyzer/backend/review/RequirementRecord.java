package com.requirementanalyzer.backend.review;

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
@Table(name = "requirement_records")
public class RequirementRecord {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_version_id", nullable = false, updatable = false)
    private AnalysisVersion analysisVersion;

    @Column(name = "requirement_id", nullable = false, length = 64)
    private String requirementId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RequirementPriority priority = RequirementPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RequirementStatus status = RequirementStatus.PENDING_REVIEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private RequirementSourceType sourceType = RequirementSourceType.AI_GENERATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RequirementRecord() {
    }

    public RequirementRecord(AnalysisVersion analysisVersion, String requirementId, String title,
                            String description, RequirementPriority priority, RequirementStatus status,
                            RequirementSourceType sourceType) {
        this.analysisVersion = analysisVersion;
        this.requirementId = requirementId;
        this.title = title == null || title.isBlank() ? "Untitled requirement" : title.trim();
        this.description = normalizeOptional(description);
        this.priority = priority == null ? RequirementPriority.MEDIUM : priority;
        this.status = status == null ? RequirementStatus.PENDING_REVIEW : status;
        this.sourceType = sourceType == null ? RequirementSourceType.AI_GENERATED : sourceType;
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

    public void update(String title, String description, RequirementPriority priority, RequirementStatus status) {
        this.title = title == null || title.isBlank() ? this.title : title.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.priority = priority == null ? this.priority : priority;
        this.status = status == null ? this.status : status;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() {
        return id;
    }

    public AnalysisVersion getAnalysisVersion() {
        return analysisVersion;
    }

    public String getRequirementId() {
        return requirementId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public RequirementPriority getPriority() {
        return priority;
    }

    public RequirementStatus getStatus() {
        return status;
    }

    public RequirementSourceType getSourceType() {
        return sourceType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
