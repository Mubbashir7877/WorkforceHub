CREATE TABLE roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE users (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    account_locked BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL,
    employee_id    BIGINT       NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_employee_id UNIQUE (employee_id),
    CONSTRAINT fk_users_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);

CREATE INDEX idx_users_email ON users (email);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    token_hash VARCHAR(64)  NOT NULL,
    expires_at DATETIME     NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME     NOT NULL,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- Idempotent by virtue of being a one-time Flyway migration; safe to re-run a fresh database only.
INSERT INTO roles (name) VALUES ('EMPLOYEE'), ('MANAGER'), ('HR_ADMIN'), ('SYSTEM_ADMIN');
