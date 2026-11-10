CREATE TABLE documents (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE RESTRICT,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    file_extension VARCHAR(32) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(512) NOT NULL UNIQUE,
    checksum VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'UPLOADED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT documents_status_check CHECK (status IN ('UPLOADED', 'PROCESSING', 'READY', 'FAILED'))
);

CREATE INDEX documents_project_created_idx ON documents(project_id, created_at DESC, id DESC);
CREATE INDEX documents_owner_deleted_idx ON documents(owner_id, deleted_at);
