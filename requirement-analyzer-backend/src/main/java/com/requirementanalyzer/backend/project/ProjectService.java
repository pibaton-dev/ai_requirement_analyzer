package com.requirementanalyzer.backend.project;

import com.requirementanalyzer.backend.auth.UserAccount;
import com.requirementanalyzer.backend.auth.UserAccountRepository;
import com.requirementanalyzer.backend.common.DomainException;
import com.requirementanalyzer.backend.project.ProjectDtos.PageResponse;
import com.requirementanalyzer.backend.project.ProjectDtos.ProjectRequest;
import com.requirementanalyzer.backend.project.ProjectDtos.ProjectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProjectService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProjectRepository projectRepository;
    private final UserAccountRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserAccountRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectResponse create(UUID ownerId, ProjectRequest request) {
        UserAccount owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found."));
        return toResponse(projectRepository.save(new Project(
                request.name(), request.description(), request.domain(), owner)));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(UUID ownerId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "Page must be non-negative and size must be between 1 and 100.");
        }
        Sort sort = Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));
        Page<Project> result = projectRepository.findAllByOwner_IdAndDeletedAtIsNull(
                ownerId, PageRequest.of(page, size, sort));
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId, UUID ownerId) {
        return toResponse(findOwnedProject(projectId, ownerId));
    }

    @Transactional
    public ProjectResponse update(UUID projectId, UUID ownerId, ProjectRequest request) {
        Project project = findOwnedProject(projectId, ownerId);
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new DomainException(HttpStatus.CONFLICT, "PROJECT_ARCHIVED", "Archived projects cannot be edited.");
        }
        project.update(request.name(), request.description(), request.domain());
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse archive(UUID projectId, UUID ownerId) {
        Project project = findOwnedProject(projectId, ownerId);
        project.archive();
        return toResponse(project);
    }

    @Transactional
    public void delete(UUID projectId, UUID ownerId) {
        findOwnedProject(projectId, ownerId).softDelete(Instant.now());
    }

    private Project findOwnedProject(UUID projectId, UUID ownerId) {
        return projectRepository.findByIdAndOwner_IdAndDeletedAtIsNull(projectId, ownerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project was not found."));
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(), project.getName(), project.getDescription(), project.getDomain(),
                project.getOwner().getId(), project.getStatus(), project.getCreatedAt(), project.getUpdatedAt());
    }
}