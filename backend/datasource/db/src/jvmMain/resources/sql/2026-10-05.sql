CREATE TABLE user_mail_ai_parse_results
(
    user_mail_id    INT                                NOT NULL PRIMARY KEY,
    user_id         INT                                NOT NULL,
    status          VARCHAR(16)                        NOT NULL,
    result_json     LONGTEXT,
    error_message   VARCHAR(1000),
    update_datetime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP
);
