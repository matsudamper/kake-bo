CREATE TABLE admin_passwords
(
    id               INT PRIMARY KEY AUTO_INCREMENT,
    password_hash    VARCHAR(512)                       NOT NULL,
    salt             BINARY(32)                         NOT NULL,
    algorithm        VARCHAR(255)                       NOT NULL,
    iteration_count  INT                                NOT NULL,
    key_length       INT                                NOT NULL,
    created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_datetime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);
