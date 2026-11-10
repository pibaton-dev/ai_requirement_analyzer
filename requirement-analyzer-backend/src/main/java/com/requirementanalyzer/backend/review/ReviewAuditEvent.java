package com.requirementanalyzer.backend.review;

import com.requirementanalyzer.backend.auth.UserAccount;
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
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_audit_events")
public class ReviewAuditEvent {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_version_id", nullable = false, updatable = false)
    private AnalysisVersion analysisVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false, updatable = false)
    private UserAccount actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewAction action;

    @Column(name = "target_type", length = 64)
    private String targetType;

    @Column(name = "target_key", length = 128)
    private String targetKey;

    @Column(name = "previous_status", length = 64)
    private String previousStatus;

    @Column(name = "new_status", length = 64)
    private String newStatus;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReviewAuditEvent() {
    }

    public ReviewAuditEvent(AnalysisVersion analysisVersion, UserAccount actor, ReviewAction action,
                           String targetType, String targetKey, String previousStatus, String newStatus,
                           String details) {
        this.analysisVersion = analysisVersion;
        this.actor = actor;
        this.action = action;
        this.targetType = targetType;
        this.targetKey = targetKey;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.details = details;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public AnalysisVersion getAnalysisVersion() {
        return analysisVersion;
    }

    public UserAccount getActor() {
        return actor;
    }

    public ReviewAction getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetKey() {
        return targetKey;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
