CREATE TABLE time_clock_sessions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id     BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    clock_in_time   DATETIME(6)  NOT NULL,
    clock_out_time  DATETIME(6),
    status          VARCHAR(10)  NOT NULL DEFAULT 'OPEN',
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_tcs_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_tcs_user     FOREIGN KEY (user_id)     REFERENCES users(id)
);

CREATE INDEX idx_tcs_employee_status ON time_clock_sessions (employee_id, status);
CREATE INDEX idx_tcs_user_id         ON time_clock_sessions (user_id);
CREATE INDEX idx_tcs_clock_in_time   ON time_clock_sessions (clock_in_time);

CREATE TABLE time_clock_event_logs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id            VARCHAR(36)  NOT NULL,
    event_type          VARCHAR(30)  NOT NULL,
    employee_id         BIGINT,
    employee_email      VARCHAR(255),
    employee_full_name  VARCHAR(255),
    user_id             BIGINT,
    user_email          VARCHAR(255),
    session_id          BIGINT,
    event_time          DATETIME(6)  NOT NULL,
    consumed_at         DATETIME(6)  NOT NULL,
    source_topic        VARCHAR(255),
    message             TEXT,
    metadata_json       TEXT,
    CONSTRAINT uk_tcel_event_id UNIQUE (event_id)
);

CREATE INDEX idx_tcel_event_time  ON time_clock_event_logs (event_time);
CREATE INDEX idx_tcel_employee_id ON time_clock_event_logs (employee_id);
CREATE INDEX idx_tcel_user_email  ON time_clock_event_logs (user_email);
CREATE INDEX idx_tcel_event_type  ON time_clock_event_logs (event_type);
CREATE INDEX idx_tcel_session_id  ON time_clock_event_logs (session_id);
