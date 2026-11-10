CREATE TABLE analysis_jobs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE RESTRICT,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    text TEXT,
    document_ids VARCHAR(2048),
    status VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
    idempotency_key VARCHAR(255),
    correlation_id VARCHAR(64),
    summary VARCHAR(4000),
    error_message VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT analysis_jobs_status_check CHECK (status IN ('QUEUED', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX analysis_jobs_project_status_idx ON analysis_jobs(project_id, status, created_at DESC, id DESC);
CREATE INDEX analysis_jobs_owner_status_idx ON analysis_jobs(owner_id, status, created_at DESC, id DESC);
