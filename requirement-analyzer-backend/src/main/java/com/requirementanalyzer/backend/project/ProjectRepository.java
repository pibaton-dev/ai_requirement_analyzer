package com.requirementanalyzer.backend.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Page<Project> findAllByOwner_IdAndDeletedAtIsNull(UUID ownerId, Pageable pageable);

    Optional<Project> findByIdAndOwner_IdAndDeletedAtIsNull(UUID id, UUID ownerId);
}