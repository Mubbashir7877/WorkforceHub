CREATE TABLE hr_policy_documents (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    title                 VARCHAR(255)  NOT NULL,
    description           VARCHAR(2000),
    category              VARCHAR(20)   NOT NULL,
    version               VARCHAR(50),
    effective_date        DATE,
    active                BOOLEAN       NOT NULL DEFAULT FALSE,
    processing_status     VARCHAR(12)   NOT NULL DEFAULT 'DRAFT',
    chunk_count           INT           NOT NULL DEFAULT 0,
    original_file_name    VARCHAR(255),
    content_type          VARCHAR(100),
    storage_type          VARCHAR(10),
    storage_location      VARCHAR(500),
    uploaded_by_user_id   BIGINT,
    uploaded_by_email     VARCHAR(255),
    created_at            DATETIME(6)   NOT NULL,
    updated_at            DATETIME(6)   NOT NULL,
    CONSTRAINT fk_hpd_uploaded_by FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_hpd_active            ON hr_policy_documents (active);
CREATE INDEX idx_hpd_category          ON hr_policy_documents (category);
CREATE INDEX idx_hpd_processing_status ON hr_policy_documents (processing_status);
