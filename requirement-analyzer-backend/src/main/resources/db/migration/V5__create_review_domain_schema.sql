CREATE TABLE analysis_versions (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE RESTRICT,
    analysis_job_id UUID NOT NULL REFERENCES analysis_jobs(id) ON DELETE RESTRICT,
    version_number INTEGER NOT NULL,
    title VARCHAR(160) NOT NULL,
    summary TEXT,
    content_json TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT analysis_versions_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'SUPERSEDED', 'ARCHIVED'))
);

CREATE TABLE requirement_records (
    id UUID PRIMARY KEY,
    analysis_version_id UUID NOT NULL REFERENCES analysis_versions(id) ON DELETE CASCADE,
    requirement_id VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    source_type VARCHAR(32) NOT NULL DEFAULT 'AI_GENERATED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT requirement_records_priority_check CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT requirement_records_status_check CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'MANUAL')),
    CONSTRAINT requirement_records_source_type_check CHECK (source_type IN ('AI_GENERATED', 'MANUAL', 'USER_EDITED')),
    CONSTRAINT requirement_records_unique UNIQUE (analysis_version_id, requirement_id)
);

CREATE TABLE requirement_questions (
    id UUID PRIMARY KEY,
    analysis_version_id UUID NOT NULL REFERENCES analysis_versions(id) ON DELETE CASCADE,
    question_id VARCHAR(64) NOT NULL,
    question TEXT NOT NULL,
    related_requirement_id VARCHAR(64),
    reason TEXT,
    impact VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    answer TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT requirement_questions_status_check CHECK (status IN ('OPEN', 'ANSWERED')),
    CONSTRAINT requirement_questions_unique UNIQUE (analysis_version_id, question_id)
);

CREATE TABLE review_audit_events (
    id UUID PRIMARY KEY,
    analysis_version_id UUID NOT NULL REFERENCES analysis_versions(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action VARCHAR(32) NOT NULL,
    target_type VARCHAR(64),
    target_key VARCHAR(128),
    previous_status VARCHAR(64),
    new_status VARCHAR(64),
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT review_audit_events_action_check CHECK (action IN ('CREATED', 'UPDATED', 'APPROVED', 'REJECTED', 'MANUAL_ADDED', 'ANSWERED'))
);

CREATE INDEX analysis_versions_project_idx ON analysis_versions(project_id, version_number DESC, id DESC);
CREATE INDEX requirement_records_version_idx ON requirement_records(analysis_version_id, status, priority, created_at ASC);
CREATE INDEX requirement_questions_version_idx ON requirement_questions(analysis_version_id, status, created_at ASC);
CREATE INDEX review_audit_events_version_idx ON review_audit_events(analysis_version_id, created_at DESC);
