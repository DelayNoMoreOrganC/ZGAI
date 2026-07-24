-- ZGAI PostgreSQL foundation schema notes.
-- Spring Data JPA can create/update these tables automatically; this file documents
-- the intended durable schema for deployment and migration review.

ALTER TABLE "user"
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- 行政案件角色代码（如 ADMINISTRATIVE_COUNTERPART）超过旧版 20 字符限制。
ALTER TABLE party ALTER COLUMN party_role TYPE VARCHAR(50);

ALTER TABLE data_backup ADD COLUMN IF NOT EXISTS content_sha256 VARCHAR(64);
ALTER TABLE data_backup ADD COLUMN IF NOT EXISTS verification_status VARCHAR(20);
ALTER TABLE data_backup ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS zgai_data_migration_audit (
    migration_id VARCHAR(36) PRIMARY KEY,
    source_sha256 VARCHAR(64) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source_rows BIGINT NOT NULL,
    target_rows BIGINT NOT NULL,
    details_json TEXT NOT NULL
);

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
    case_id BIGINT,
    matched_client_ids VARCHAR(1000),
    matched_case_ids VARCHAR(1000),
    similar_names VARCHAR(1000),
    matched_related_subjects VARCHAR(2000),
    conflict_level VARCHAR(30),
    conclusion VARCHAR(500),
    remark VARCHAR(1000),
    review_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    review_decision VARCHAR(30),
    review_conclusion VARCHAR(1000),
    waiver_basis VARCHAR(2000),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    archived_document_id BIGINT,
    archived_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_conflict_check_subject ON conflict_check_record(subject_name);
CREATE INDEX IF NOT EXISTS idx_conflict_check_operator ON conflict_check_record(checked_by);
CREATE INDEX IF NOT EXISTS idx_conflict_check_level ON conflict_check_record(conflict_level);
CREATE INDEX IF NOT EXISTS idx_conflict_check_case ON conflict_check_record(case_id);

ALTER TABLE conflict_check_record ADD COLUMN IF NOT EXISTS review_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW';
ALTER TABLE conflict_check_record ADD COLUMN IF NOT EXISTS review_decision VARCHAR(30);
ALTER TABLE conflict_check_record ADD COLUMN IF NOT EXISTS review_conclusion VARCHAR(1000);
ALTER TABLE conflict_check_record ADD COLUMN IF NOT EXISTS waiver_basis VARCHAR(2000);
ALTER TABLE conflict_check_record ADD COLUMN IF NOT EXISTS reviewed_by BIGINT;
ALTER TABLE conflict_check_record ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE conflict_check_record ADD COLUMN IF NOT EXISTS case_id BIGINT;
ALTER TABLE conflict_check_record ADD COLUMN IF NOT EXISTS archived_document_id BIGINT;
ALTER TABLE conflict_check_record ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;
ALTER TABLE conflict_check_record ADD COLUMN IF NOT EXISTS matched_related_subjects VARCHAR(2000);

CREATE TABLE IF NOT EXISTS client_subject_relation (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    source_client_id BIGINT NOT NULL,
    target_client_id BIGINT,
    target_subject_name VARCHAR(200) NOT NULL,
    target_credit_code VARCHAR(50),
    relation_type VARCHAR(40) NOT NULL,
    description VARCHAR(1000),
    created_by BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_client_relation_source ON client_subject_relation(source_client_id);
CREATE INDEX IF NOT EXISTS idx_client_relation_target ON client_subject_relation(target_client_id);
CREATE INDEX IF NOT EXISTS idx_client_relation_target_name ON client_subject_relation(target_subject_name);
CREATE INDEX IF NOT EXISTS idx_client_relation_deleted ON client_subject_relation(deleted);

CREATE TABLE IF NOT EXISTS conflict_waiver_attachment (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    conflict_check_record_id BIGINT NOT NULL,
    case_id BIGINT NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    content_sha256 VARCHAR(64) NOT NULL,
    uploaded_by BIGINT NOT NULL,
    archived_document_id BIGINT,
    archived_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_conflict_waiver_record
    ON conflict_waiver_attachment(conflict_check_record_id);
CREATE INDEX IF NOT EXISTS idx_conflict_waiver_case
    ON conflict_waiver_attachment(case_id);
CREATE INDEX IF NOT EXISTS idx_conflict_waiver_hash
    ON conflict_waiver_attachment(content_sha256);

ALTER TABLE case_document ADD COLUMN IF NOT EXISTS folder_id BIGINT;
ALTER TABLE case_document ADD COLUMN IF NOT EXISTS original_file_name VARCHAR(255);
ALTER TABLE case_document ADD COLUMN IF NOT EXISTS mime_type VARCHAR(100);
ALTER TABLE case_document ADD COLUMN IF NOT EXISTS version_no INTEGER DEFAULT 1;
ALTER TABLE case_document ADD COLUMN IF NOT EXISTS knowledge_eligible BOOLEAN DEFAULT FALSE;
ALTER TABLE case_document ADD COLUMN IF NOT EXISTS index_status VARCHAR(30) DEFAULT 'NOT_INDEXED';
ALTER TABLE case_document ADD COLUMN IF NOT EXISTS content_sha256 VARCHAR(64);

ALTER TABLE "case" ADD COLUMN IF NOT EXISTS consultant_unit_name VARCHAR(200);
ALTER TABLE "case" ADD COLUMN IF NOT EXISTS consultant_contact_name VARCHAR(100);
ALTER TABLE "case" ADD COLUMN IF NOT EXISTS consultant_contact_department VARCHAR(100);
ALTER TABLE "case" ADD COLUMN IF NOT EXISTS consultant_contact_title VARCHAR(100);
ALTER TABLE "case" ADD COLUMN IF NOT EXISTS consultant_contact_phone VARCHAR(30);
ALTER TABLE "case" ADD COLUMN IF NOT EXISTS consultant_contact_email VARCHAR(120);
ALTER TABLE "case" ADD COLUMN IF NOT EXISTS consultant_service_scope VARCHAR(2000);
ALTER TABLE "case" ADD COLUMN IF NOT EXISTS consultant_response_requirement VARCHAR(500);
ALTER TABLE "case" ADD COLUMN IF NOT EXISTS consultant_included_services VARCHAR(2000);
ALTER TABLE "case" ADD COLUMN IF NOT EXISTS consultant_excluded_services VARCHAR(2000);
ALTER TABLE "case" ADD COLUMN IF NOT EXISTS renewal_reminder_date DATE;

CREATE INDEX IF NOT EXISTS idx_case_document_hash ON case_document(case_id, content_sha256);

CREATE TABLE IF NOT EXISTS case_activity (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    case_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    occurred_at TIMESTAMP NOT NULL,
    source_type VARCHAR(40),
    source_id BIGINT,
    operator_id BIGINT NOT NULL,
    procedure_stage VARCHAR(80),
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_case_activity_case_time ON case_activity(case_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_case_activity_source ON case_activity(source_type, source_id);

CREATE TABLE IF NOT EXISTS ai_case_command (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    idempotency_key VARCHAR(80) NOT NULL,
    user_id BIGINT NOT NULL,
    case_id BIGINT,
    instruction TEXT NOT NULL,
    instruction_hash VARCHAR(64),
    actions_json TEXT,
    status VARCHAR(30) NOT NULL,
    risk_level VARCHAR(20),
    clarification VARCHAR(500),
    model_name VARCHAR(120),
    executed_at TIMESTAMP,
    CONSTRAINT uk_ai_command_user_key UNIQUE(user_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_ai_command_case ON ai_case_command(case_id);
CREATE INDEX IF NOT EXISTS idx_ai_command_status ON ai_case_command(status);
ALTER TABLE ai_case_command ADD COLUMN IF NOT EXISTS instruction_hash VARCHAR(64);
ALTER TABLE ai_case_command ADD COLUMN IF NOT EXISTS privacy_sanitized_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS ai_document_intake (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    original_file_name VARCHAR(255) NOT NULL,
    temp_path VARCHAR(1000) NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT NOT NULL,
    content_sha256 VARCHAR(64) NOT NULL,
    extracted_text TEXT,
    analysis_json TEXT,
    candidates_json TEXT,
    suggested_folder VARCHAR(200),
    suggested_document_type VARCHAR(50),
    status VARCHAR(30) NOT NULL,
    upload_by BIGINT NOT NULL,
    confirmed_case_id BIGINT,
    case_document_id BIGINT,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_document_intake_user ON ai_document_intake(upload_by, status);
CREATE INDEX IF NOT EXISTS idx_document_intake_hash ON ai_document_intake(content_sha256);
CREATE INDEX IF NOT EXISTS idx_document_intake_expiry ON ai_document_intake(expires_at, status);

CREATE TABLE IF NOT EXISTS calendar_reminder (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    calendar_id BIGINT NOT NULL,
    offset_minutes INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT uk_calendar_reminder_offset UNIQUE(calendar_id, offset_minutes)
);

CREATE INDEX IF NOT EXISTS idx_calendar_reminder_status ON calendar_reminder(status);

ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS knowledge_source VARCHAR(30) DEFAULT 'FIRM_KNOWLEDGE';
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS knowledge_eligible BOOLEAN DEFAULT TRUE;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS index_status VARCHAR(30) DEFAULT 'PENDING';
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS source_reference VARCHAR(500);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS issuing_authority VARCHAR(200);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS document_number VARCHAR(100);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS effective_date DATE;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS validity_status VARCHAR(20) DEFAULT 'UNKNOWN';
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS authorization_confirmed BOOLEAN DEFAULT FALSE;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS source_url VARCHAR(1000);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS source_relative_path VARCHAR(1000);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS content_sha256 VARCHAR(64);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS collected_at TIMESTAMP;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS review_status VARCHAR(30) DEFAULT 'APPROVED';
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS reviewed_by BIGINT;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS review_reason VARCHAR(1000);

CREATE UNIQUE INDEX IF NOT EXISTS uk_knowledge_article_sha
    ON knowledge_article(content_sha256) WHERE content_sha256 IS NOT NULL AND deleted = FALSE;

CREATE TABLE IF NOT EXISTS knowledge_import_batch (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    source_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by BIGINT NOT NULL,
    item_count INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS knowledge_import_item (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    batch_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    title VARCHAR(300),
    source_url VARCHAR(1000),
    source_relative_path VARCHAR(1000),
    original_file_name VARCHAR(500),
    source_absolute_path VARCHAR(1500),
    staged_path VARCHAR(1500),
    content_sha256 VARCHAR(64),
    issuing_authority VARCHAR(200),
    document_number VARCHAR(100),
    published_date DATE,
    effective_date DATE,
    validity_status VARCHAR(20) DEFAULT 'UNKNOWN',
    article_id BIGINT,
    error_message VARCHAR(1000),
    collected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_import_batch ON knowledge_import_item(batch_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_import_sha ON knowledge_import_item(content_sha256);

CREATE TABLE IF NOT EXISTS rag_evaluation_case (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    name VARCHAR(120) NOT NULL,
    question VARCHAR(1000) NOT NULL,
    expected_article_ids VARCHAR(2000) NOT NULL,
    forbidden_article_ids VARCHAR(2000),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS rag_evaluation_run (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    evaluation_case_id BIGINT NOT NULL,
    retrieved_article_ids VARCHAR(2000),
    search_method VARCHAR(30) NOT NULL,
    top3_hit BOOLEAN NOT NULL,
    forbidden_hit BOOLEAN NOT NULL,
    passed BOOLEAN NOT NULL,
    duration_ms BIGINT NOT NULL,
    run_by BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_rag_evaluation_run_case ON rag_evaluation_run(evaluation_case_id);
CREATE INDEX IF NOT EXISTS idx_rag_evaluation_run_created ON rag_evaluation_run(created_at);

ALTER TABLE ai_log ADD COLUMN IF NOT EXISTS provider_type VARCHAR(50);
ALTER TABLE ai_log ADD COLUMN IF NOT EXISTS input_summary VARCHAR(500);
ALTER TABLE ai_log ADD COLUMN IF NOT EXISTS input_hash VARCHAR(64);
ALTER TABLE ai_log ADD COLUMN IF NOT EXISTS output_hash VARCHAR(64);
ALTER TABLE ai_log ADD COLUMN IF NOT EXISTS estimated_cost_micros BIGINT;
ALTER TABLE ai_log ADD COLUMN IF NOT EXISTS privacy_sanitized_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS legacy_material_search_record (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    keyword VARCHAR(200),
    query_params VARCHAR(2000),
    searched_by BIGINT,
    source_case_id BIGINT,
    result_count INTEGER DEFAULT 0,
    archive_path_configured BOOLEAN DEFAULT FALSE
);

ALTER TABLE legacy_material_search_record ADD COLUMN IF NOT EXISTS source_case_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_legacy_search_user ON legacy_material_search_record(searched_by);
CREATE INDEX IF NOT EXISTS idx_legacy_search_keyword ON legacy_material_search_record(keyword);
CREATE INDEX IF NOT EXISTS idx_legacy_search_created ON legacy_material_search_record(created_at);

CREATE TABLE IF NOT EXISTS legacy_material_search_result (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    search_record_id BIGINT NOT NULL,
    source_case_id BIGINT NOT NULL,
    relative_path VARCHAR(1200) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    last_modified_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_legacy_result_record ON legacy_material_search_result(search_record_id);
CREATE INDEX IF NOT EXISTS idx_legacy_result_case ON legacy_material_search_result(source_case_id);

CREATE TABLE IF NOT EXISTS archive_job (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    case_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    template_version VARCHAR(30) NOT NULL DEFAULT 'CIVIL_V1',
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    created_by BIGINT NOT NULL,
    submitted_by BIGINT,
    reviewed_by BIGINT,
    submitted_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    completed_at TIMESTAMP,
    progress INTEGER DEFAULT 0,
    current_stage VARCHAR(100),
    error_message VARCHAR(1000),
    review_reason VARCHAR(1000),
    exception_reason VARCHAR(1000),
    correction_reason VARCHAR(1000),
    model_provider VARCHAR(30) DEFAULT 'LM_STUDIO',
    model_name VARCHAR(100),
    prompt_version VARCHAR(30) DEFAULT 'ARCHIVE_CIVIL_V1'
);
CREATE INDEX IF NOT EXISTS idx_archive_job_case ON archive_job(case_id);
CREATE INDEX IF NOT EXISTS idx_archive_job_status ON archive_job(status);
ALTER TABLE archive_job ADD COLUMN IF NOT EXISTS correction_reason VARCHAR(1000);

CREATE TABLE IF NOT EXISTS archive_document_item (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    job_id BIGINT NOT NULL,
    case_document_id BIGINT NOT NULL,
    original_file_name VARCHAR(500) NOT NULL,
    catalog_seq INTEGER,
    catalog_name VARCHAR(200),
    document_type VARCHAR(80),
    included BOOLEAN NOT NULL DEFAULT TRUE,
    source_page_count INTEGER DEFAULT 0,
    output_start_page INTEGER,
    output_end_page INTEGER,
    content_sha256 VARCHAR(64),
    confidence DOUBLE PRECISION,
    classification_reason VARCHAR(1000),
    sort_order INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_archive_item_job ON archive_document_item(job_id);
CREATE INDEX IF NOT EXISTS idx_archive_item_document ON archive_document_item(case_document_id);

CREATE TABLE IF NOT EXISTS archive_field_snapshot (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    job_id BIGINT NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    field_value TEXT,
    source_document_id BIGINT,
    source_page INTEGER,
    confidence DOUBLE PRECISION,
    extraction_reason VARCHAR(1000),
    confirmed_by BIGINT,
    confirmed_at TIMESTAMP,
    CONSTRAINT uk_archive_field_key UNIQUE(job_id, field_key)
);
CREATE INDEX IF NOT EXISTS idx_archive_field_job ON archive_field_snapshot(job_id);

CREATE TABLE IF NOT EXISTS archive_output (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    job_id BIGINT NOT NULL,
    case_id BIGINT NOT NULL,
    version_no INTEGER NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1500) NOT NULL,
    content_sha256 VARCHAR(64) NOT NULL,
    manifest_file_path VARCHAR(1500),
    manifest_sha256 VARCHAR(64),
    page_count INTEGER,
    source_page_count INTEGER,
    gap_pages INTEGER DEFAULT 0,
    duplicate_pages INTEGER DEFAULT 0,
    template_version VARCHAR(30),
    created_by BIGINT NOT NULL,
    CONSTRAINT uk_archive_output_version UNIQUE(case_id, version_no)
);
CREATE INDEX IF NOT EXISTS idx_archive_output_job ON archive_output(job_id);
CREATE INDEX IF NOT EXISTS idx_archive_output_case ON archive_output(case_id);
ALTER TABLE archive_output ADD COLUMN IF NOT EXISTS manifest_file_path VARCHAR(1500);
ALTER TABLE archive_output ADD COLUMN IF NOT EXISTS manifest_sha256 VARCHAR(64);

CREATE TABLE IF NOT EXISTS archive_audit_log (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    job_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    detail VARCHAR(2000)
);
CREATE INDEX IF NOT EXISTS idx_archive_audit_job ON archive_audit_log(job_id);

CREATE TABLE IF NOT EXISTS approval_attachment (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    approval_id BIGINT NOT NULL,
    case_document_id BIGINT,
    original_file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(150),
    content_sha256 VARCHAR(64),
    source_type VARCHAR(30) NOT NULL,
    seal_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    uploaded_by BIGINT NOT NULL,
    decided_by BIGINT,
    decided_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_approval_attachment_approval ON approval_attachment(approval_id);
CREATE INDEX IF NOT EXISTS idx_approval_attachment_case_document ON approval_attachment(case_document_id);

CREATE TABLE IF NOT EXISTS case_closure_request (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    case_id BIGINT NOT NULL,
    approval_id BIGINT NOT NULL UNIQUE,
    applicant_id BIGINT NOT NULL,
    review_todo_id BIGINT,
    closure_type VARCHAR(40) NOT NULL,
    case_outcome VARCHAR(1000) NOT NULL,
    closure_summary VARCHAR(5000) NOT NULL,
    fee_status VARCHAR(40) NOT NULL,
    client_delivery_status VARCHAR(40) NOT NULL,
    client_delivery_notes VARCHAR(1000),
    documents_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL,
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    review_notes VARCHAR(2000)
);
CREATE INDEX IF NOT EXISTS idx_case_closure_case ON case_closure_request(case_id);
CREATE INDEX IF NOT EXISTS idx_case_closure_status ON case_closure_request(status);

CREATE TABLE IF NOT EXISTS case_closure_document (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    closure_request_id BIGINT NOT NULL,
    case_document_id BIGINT NOT NULL,
    CONSTRAINT uk_case_closure_document UNIQUE(closure_request_id, case_document_id)
);
CREATE INDEX IF NOT EXISTS idx_case_closure_document_request ON case_closure_document(closure_request_id);

-- Structured filing, seal and closure reviews can exceed the legacy VARCHAR(255) limit.
ALTER TABLE approval ALTER COLUMN content TYPE TEXT;

CREATE TABLE IF NOT EXISTS law_firm_letter (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    case_id BIGINT NOT NULL,
    recipient VARCHAR(300) NOT NULL,
    client_name VARCHAR(500) NOT NULL,
    lawyer_names VARCHAR(1000) NOT NULL,
    opposing_party VARCHAR(1000) NOT NULL,
    case_reason VARCHAR(500) NOT NULL,
    letter_type_code VARCHAR(10) NOT NULL,
    lawyer_contacts VARCHAR(1000) NOT NULL,
    closing_text VARCHAR(100) NOT NULL DEFAULT '特此函告！',
    issue_date DATE NOT NULL,
    serial_no INTEGER,
    letter_number VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    approval_id BIGINT,
    final_document_id BIGINT,
    draft_sha256 VARCHAR(64),
    final_sha256 VARCHAR(64),
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    submitted_at TIMESTAMP,
    approved_at TIMESTAMP,
    rejected_reason VARCHAR(2000),
    lock_version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_law_firm_letter_case ON law_firm_letter(case_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_law_firm_letter_approval ON law_firm_letter(approval_id) WHERE approval_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_law_firm_letter_status ON law_firm_letter(status);

CREATE TABLE IF NOT EXISTS law_firm_letter_sequence (
    id BIGSERIAL PRIMARY KEY,
    letter_year INTEGER NOT NULL,
    letter_type_code VARCHAR(10) NOT NULL,
    last_serial INTEGER NOT NULL,
    initialized_by BIGINT NOT NULL,
    initialized_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_law_firm_letter_sequence UNIQUE(letter_year, letter_type_code)
);
