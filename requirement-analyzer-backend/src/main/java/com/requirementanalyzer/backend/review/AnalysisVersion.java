package com.requirementanalyzer.backend.review;

import com.requirementanalyzer.backend.analysis.AnalysisJob;
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
@Table(name = "analysis_versions")
public class AnalysisVersion {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_job_id", nullable = false, updatable = false)
    private AnalysisJob analysisJob;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "content_json", columnDefinition = "TEXT")
    private String contentJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AnalysisVersionStatus status = AnalysisVersionStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AnalysisVersion() {
    }

    public AnalysisVersion(Project project, AnalysisJob analysisJob, int versionNumber, String title, String summary) {
        this.project = project;
        this.analysisJob = analysisJob;
        this.versionNumber = versionNumber;
        this.title = title == null || title.isBlank() ? "Version " + versionNumber : title.trim();
        this.summary = normalizeOptional(summary);
        this.contentJson = "{}";
        this.status = AnalysisVersionStatus.ACTIVE;
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

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public AnalysisJob getAnalysisJob() {
        return analysisJob;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getContentJson() {
        return contentJson;
    }

    public AnalysisVersionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
