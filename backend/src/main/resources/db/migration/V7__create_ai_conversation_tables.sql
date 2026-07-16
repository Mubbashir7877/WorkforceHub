CREATE TABLE ai_conversations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    user_email  VARCHAR(255) NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    CONSTRAINT fk_aic_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_aic_user_id ON ai_conversations (user_id);

CREATE TABLE ai_messages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT       NOT NULL,
    role            VARCHAR(10)  NOT NULL,
    content         TEXT         NOT NULL,
    grounded        BOOLEAN,
    model_name      VARCHAR(100),
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_aim_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_aim_conversation_id ON ai_messages (conversation_id);

CREATE TABLE ai_response_sources (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id        BIGINT        NOT NULL,
    document_id       BIGINT        NOT NULL,
    document_title    VARCHAR(255),
    page_number       INT,
    chunk_index       INT,
    excerpt           VARCHAR(1000),
    similarity_score  DOUBLE,
    CONSTRAINT fk_airs_message  FOREIGN KEY (message_id)  REFERENCES ai_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_airs_document FOREIGN KEY (document_id) REFERENCES hr_policy_documents(id)
);

CREATE INDEX idx_airs_message_id ON ai_response_sources (message_id);
