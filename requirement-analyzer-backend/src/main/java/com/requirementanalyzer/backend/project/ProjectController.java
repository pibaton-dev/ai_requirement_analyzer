package com.requirementanalyzer.backend.project;

import com.requirementanalyzer.backend.project.ProjectDtos.PageResponse;
import com.requirementanalyzer.backend.project.ProjectDtos.ProjectRequest;
import com.requirementanalyzer.backend.project.ProjectDtos.ProjectResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(
            @AuthenticationPrincipal UUID ownerId,
            @Valid @RequestBody ProjectRequest request
    ) {
        return projectService.create(ownerId, request);
    }

    @GetMapping
    public PageResponse<ProjectResponse> list(
            @AuthenticationPrincipal UUID ownerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return projectService.list(ownerId, page, size);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID projectId) {
        return projectService.get(projectId, ownerId);
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectRequest request
    ) {
        return projectService.update(projectId, ownerId, request);
    }

    @PostMapping("/{projectId}/archive")
    public ProjectResponse archive(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID projectId) {
        return projectService.archive(projectId, ownerId);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID projectId) {
        projectService.delete(projectId, ownerId);
    }
}