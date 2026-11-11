package com.requirementanalyzer.backend.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Page<Document> findAllByProject_IdAndDeletedAtIsNull(UUID projectId, Pageable pageable);

    Optional<Document> findByIdAndProject_Owner_IdAndDeletedAtIsNull(UUID documentId, UUID ownerId);

    Optional<Document> findByIdAndProject_IdAndProject_Owner_IdAndDeletedAtIsNull(
            UUID documentId, UUID projectId, UUID ownerId);

    boolean existsByProject_IdAndChecksumAndDeletedAtIsNull(UUID projectId, String checksum);
}
