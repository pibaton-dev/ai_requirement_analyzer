package com.requirementanalyzer.backend.document;

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
@Table(name = "documents")
public class Document {

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

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(name = "file_extension", nullable = false, length = 32)
    private String fileExtension;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "storage_key", nullable = false, unique = true, length = 512)
    private String storageKey;

    @Column(nullable = false, length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Document() {
    }

    public Document(Project project, UserAccount owner, String fileName, String contentType,
                    String fileExtension, long fileSize, String storageKey, String checksum) {
        this.project = project;
        this.owner = owner;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileExtension = fileExtension;
        this.fileSize = fileSize;
        this.storageKey = storageKey;
        this.checksum = checksum;
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

    public void softDelete(Instant now) {
        this.deletedAt = now;
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

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getChecksum() {
        return checksum;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
