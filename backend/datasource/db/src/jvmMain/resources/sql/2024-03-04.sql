CREATE TABLE user_received_mails
(
    user_received_mail_id int auto_increment                   not null,
    user_id               int                                  not null,
    created_datetime      datetime default current_timestamp() not null,
    raw_text              text                                 not null
);

ALTER TABLE user_password_extend_data
    MODIFY COLUMN update_datetime
        datetime default current_timestamp() not null
        ON UPDATE current_timestamp();

CREATE TABLE api_tokens
(
    api_token_id     int auto_increment primary key       not null,
    user_id          int                                  not null,
    token_hash       varchar(512)                         not null,
    salt             varchar(32)                          not null,
    iteration_count  int                                  not null,
    algorithm        varchar(255)                         not null,
    key_length       int                                  not null,
    permissions      TEXT                                 not null,
    created_datetime datetime default current_timestamp() not null,
    update_datetime  datetime default current_timestamp() not null ON UPDATE current_timestamp(),
    constraint token
        unique (token_hash),
    index user_id (user_id)
);

DROP TABLE api_tokens