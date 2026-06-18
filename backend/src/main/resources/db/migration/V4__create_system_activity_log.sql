CREATE TABLE system_activity_logs (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id      VARCHAR(36)  NOT NULL,
    event_type    VARCHAR(50)  NOT NULL,
    entity_type   VARCHAR(50)  NOT NULL,
    entity_id     BIGINT,
    actor_user_id BIGINT,
    actor_email   VARCHAR(255),
    actor_roles   VARCHAR(255),
    message       TEXT,
    occurred_at   DATETIME(6)  NOT NULL,
    consumed_at   DATETIME(6)  NOT NULL,
    source_topic  VARCHAR(255),
    metadata_json TEXT,
    CONSTRAINT uk_sal_event_id UNIQUE (event_id)
);

CREATE INDEX idx_sal_occurred_at ON system_activity_logs (occurred_at);
CREATE INDEX idx_sal_event_type   ON system_activity_logs (event_type);
CREATE INDEX idx_sal_actor_email  ON system_activity_logs (actor_email);
CREATE INDEX idx_sal_entity       ON system_activity_logs (entity_type, entity_id);
