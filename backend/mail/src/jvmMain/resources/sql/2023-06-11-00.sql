CREATE TABLE users
(
    user_id          INT PRIMARY KEY AUTO_INCREMENT,
    user_name        VARCHAR(255)                       not null,
    created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP not null,
    CONSTRAINT user_name_unique UNIQUE (user_name)
);
CREATE INDEX user_name ON users (user_name);

CREATE TABLE user_passwords
(
    user_id          INT PRIMARY KEY,
    password_hash    VARCHAR(512)                       not null,
    created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP not null,
    update_datetime  DATETIME DEFAULT CURRENT_TIMESTAMP not null
);

CREATE TABLE user_sessions
(
    session_id      VARCHAR(255) PRIMARY KEY,
    user_id         INT not null ,
    created_date    DATETIME not null DEFAULT CURRENT_TIMESTAMP,
    expire_datetime DATETIME not null
);
CREATE INDEX user_id ON user_sessions (user_id);

CREATE TABLE admin_sessions
(
    session_id      VARCHAR(255) PRIMARY KEY,
    created_date    DATETIME not null DEFAULT CURRENT_TIMESTAMP,
    expire_datetime DATETIME not null
);

CREATE TABLE user_password_extend_data
(
    user_id          INT                                not null PRIMARY KEY,
    salt             BINARY(32)                         not null,
    iteration_count  INT                                not null,
    algorithm        VARCHAR(255)                       not null,
    key_length       INT                                not null,
    created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP not null,
    update_datetime  DATETIME DEFAULT CURRENT_TIMESTAMP not null
);