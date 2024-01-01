CREATE TABLE web_auth_authenticator
(
    id                           INT PRIMARY KEY AUTO_INCREMENT,
    user_id                      INT    NOT NULL,
    name                         TEXT   NOT NULL,
    attestation_statement        TEXT   NOT NULL,
    attestation_statement_format TEXT   NOT NULL,
    attested_credential_data     TEXT   NOT NULL,
    authenticator_extensions     TEXT,
    counter                      BIGINT NOT NULL,
    created_at                   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at                   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT user_id_and_name UNIQUE (user_id, name)
);

ALTER TABLE user_sessions
    ADD COLUMN latest_accessed_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

UPDATE user_sessions
set latest_accessed_at = DATE_SUB(expire_datetime, INTERVAL 7 DAY)
where 1;

ALTER TABLE user_sessions ADD COLUMN name varchar(36) NOT NULL DEFAULT (UUID());
ALTER TABLE user_sessions ADD CONSTRAINT user_id_and_name UNIQUE (user_id, name);
