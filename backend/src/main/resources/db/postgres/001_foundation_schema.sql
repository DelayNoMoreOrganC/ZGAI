-- ZGAI PostgreSQL foundation schema notes.
-- Spring Data JPA can create/update these tables automatically; this file documents
-- the intended durable schema for deployment and migration review.

CREATE TABLE IF NOT EXISTS document_folder (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    case_id BIGINT,
    parent_id BIGINT,
    folder_code VARCHAR(50) NOT NULL,
    folder_name VARCHAR(100) NOT NULL,
    folder_path VARCHAR(300) NOT NULL,
    folder_type VARCHAR(20) NOT NULL DEFAULT 'TEMPLATE',
    sort_order INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    system_default BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_document_folder_case ON document_folder(case_id);
CREATE INDEX IF NOT EXISTS idx_document_folder_type ON document_folder(folder_type);
CREATE INDEX IF NOT EXISTS idx_document_folder_active ON document_folder(active);

CREATE TABLE IF NOT EXISTS conflict_check_record (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subject_name VARCHAR(100) NOT NULL,
    client_type VARCHAR(30),
    client_relationship VARCHAR(30),
    client_role VARCHAR(30),
    id_card VARCHAR(20),
    credit_code VARCHAR(50),
    checked_by BIGINT,
    matched_client_ids VARCHAR(1000),
    matched_case_ids VARCHAR(1000),
    similar_names VARCHAR(1000),
    conflict_level VARCHAR(30),
    conclusion VARCHAR(500),
    remark VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS idx_conflict_check_subject ON conflict_check_record(subject_name);
CREATE INDEX IF NOT EXISTS idx_conflict_check_operator ON conflict_check_record(checked_by);
CREATE INDEX IF NOT EXISTS idx_conflict_check_level ON conflict_check_record(conflict_level);

ALTER TABLE case_document ADD COLUMN IF NOT EXISTS folder_id BIGINT;
ALTER TABLE case_document ADD COLUMN IF NOT EXISTS original_file_name VARCHAR(255);
ALTER TABLE case_document ADD COLUMN IF NOT EXISTS mime_type VARCHAR(100);
ALTER TABLE case_document ADD COLUMN IF NOT EXISTS version_no INTEGER DEFAULT 1;
ALTER TABLE case_document ADD COLUMN IF NOT EXISTS knowledge_eligible BOOLEAN DEFAULT FALSE;
ALTER TABLE case_document ADD COLUMN IF NOT EXISTS index_status VARCHAR(30) DEFAULT 'NOT_INDEXED';

ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS knowledge_source VARCHAR(30) DEFAULT 'FIRM_KNOWLEDGE';
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS knowledge_eligible BOOLEAN DEFAULT TRUE;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS index_status VARCHAR(30) DEFAULT 'PENDING';
