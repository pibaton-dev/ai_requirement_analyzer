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
@Table(name = "requirement_questions")
public class RequirementQuestion {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_version_id", nullable = false, updatable = false)
    private AnalysisVersion analysisVersion;

    @Column(name = "question_id", nullable = false, length = 64)
    private String questionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "related_requirement_id", length = 64)
    private String relatedRequirementId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(length = 255)
    private String impact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QuestionStatus status = QuestionStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RequirementQuestion() {
    }

    public RequirementQuestion(AnalysisVersion analysisVersion, String questionId, String question,
                              String relatedRequirementId, String reason, String impact, QuestionStatus status) {
        this.analysisVersion = analysisVersion;
        this.questionId = questionId;
        this.question = question == null || question.isBlank() ? "Untitled question" : question.trim();
        this.relatedRequirementId = relatedRequirementId;
        this.reason = normalizeOptional(reason);
        this.impact = normalizeOptional(impact);
        this.status = status == null ? QuestionStatus.OPEN : status;
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

    public void answer(String answer) {
        this.answer = answer == null || answer.isBlank() ? null : answer.trim();
        this.status = QuestionStatus.ANSWERED;
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

    public String getQuestionId() {
        return questionId;
    }

    public String getQuestion() {
        return question;
    }

    public String getRelatedRequirementId() {
        return relatedRequirementId;
    }

    public String getReason() {
        return reason;
    }

    public String getImpact() {
        return impact;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public String getAnswer() {
        return answer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
