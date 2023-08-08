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
    user_id         INT      not null,
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

CREATE TABLE user_imap_settings
(
    user_id  INT not null PRIMARY KEY,
    host     VARCHAR(500),
    port     INT,
    use_name VARCHAR(500),
    password VARCHAR(500)
);

CREATE TABLE user_mails
(
    user_mail_id     INT                                not null PRIMARY KEY AUTO_INCREMENT,
    user_id          INT                                not null,
    plain            TEXT,
    html             TEXT,
    from_mail        VARCHAR(500)                       not null,
    subject          VARCHAR(500)                       not null,
    datetime         DATETIME                           not null,
    created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP not null
);
CREATE INDEX user_id ON user_mails (user_id);
CREATE INDEX user_index ON user_mails (user_mail_id, user_id);

CREATE TABLE money_usage_categories
(
    money_usage_category_id INT                                not null PRIMARY KEY AUTO_INCREMENT,
    user_id                 INT                                not null,
    name                    VARCHAR(500)                       not null,
    created_datetime        DATETIME DEFAULT CURRENT_TIMESTAMP not null,
    update_datetime         DATETIME DEFAULT CURRENT_TIMESTAMP not null ON UPDATE CURRENT_TIMESTAMP,
    INDEX user_id (user_id),
    INDEX user_index (money_usage_category_id, user_id)
);

CREATE TABLE money_usage_sub_categories
(
    money_usage_sub_category_id INT                                not null PRIMARY KEY AUTO_INCREMENT,
    user_id                     INT                                not null,
    money_usage_category_id     INT                                not null,
    name                        VARCHAR(500)                       not null,
    created_datetime            DATETIME DEFAULT CURRENT_TIMESTAMP not null,
    update_datetime             DATETIME DEFAULT CURRENT_TIMESTAMP not null ON UPDATE CURRENT_TIMESTAMP,
    INDEX user_id (user_id),
    INDEX user_index (money_usage_sub_category_id, user_id)
);

CREATE TABLE money_usages
(
    money_usage_id              INT                                not null PRIMARY KEY AUTO_INCREMENT,
    user_id                     INT                                not null,
    title                       VARCHAR(500)                       not null,
    description                 VARCHAR(1000)                      not null,
    amount                      int                                not null,
    money_usage_sub_category_id INT,
    datetime                    DATETIME                           not null,
    created_datetime            DATETIME DEFAULT CURRENT_TIMESTAMP not null,
    update_datetime             DATETIME DEFAULT CURRENT_TIMESTAMP not null ON UPDATE CURRENT_TIMESTAMP,
    INDEX user_id (user_id),
    INDEX user_index (money_usage_id, user_id)
);

CREATE TABLE money_usages_mails_relation
(
    user_id          INT                                not null,
    money_usage_id   INT                                not null,
    user_mail_id     INT                                not null,
    PRIMARY KEY (money_usage_id, user_mail_id),
    index user_id (user_id),
    index money_usage_id (user_id, money_usage_id),
    index user_mail_id (user_id, user_mail_id),
    created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP not null
);

CREATE TABLE category_mail_filters
(
    category_mail_filter_id                         INT                                not null PRIMARY KEY AUTO_INCREMENT,
    user_id                                         INT                                not null,
    title                                           VARCHAR(500)                       not null,
    money_usage_sub_category_id                     INT,
    category_mail_filter_condition_operator_type_id INT                                not null,
    created_datetime                                DATETIME DEFAULT CURRENT_TIMESTAMP not null,
    update_datetime                                 DATETIME DEFAULT CURRENT_TIMESTAMP not null ON UPDATE CURRENT_TIMESTAMP,
    order_number                                    INT                                not null,
    index user_category_mail_filter_id (user_id, category_mail_filter_id, order_number)
);

CREATE TABLE category_mail_filter_condition_operator_type
(
    category_mail_filter_condition_operator_type_id INT         not null PRIMARY KEY,
    operator_name                                   VARCHAR(10) not null
);
INSERT INTO category_mail_filter_condition_operator_type
    (category_mail_filter_condition_operator_type_id, operator_name)
VALUES (0, 'OR'),
       (1, 'AND');

CREATE TABLE category_mail_filter_conditions
(
    category_mail_filter_condition_id       INT                                not null PRIMARY KEY AUTO_INCREMENT,
    category_mail_filter_id                 INT                                not null,
    user_id                                 INT                                not null,
    text                                    VARCHAR(500)                       not null,
    category_mail_filter_datasource_type_id INT                                not null,
    category_mail_filter_condition_type_id  INT                                not null,
    created_datetime                        DATETIME DEFAULT CURRENT_TIMESTAMP not null,
    update_datetime                         DATETIME DEFAULT CURRENT_TIMESTAMP not null ON UPDATE CURRENT_TIMESTAMP,
    index user_category_mail_filter_condition_id (user_id, category_mail_filter_condition_id),
    index category_mail_filter_id (user_id, category_mail_filter_id)
);

CREATE TABLE category_mail_filter_datasource_type
(
    category_mail_filter_datasource_type_id INT         not null PRIMARY KEY,
    name                                    VARCHAR(50) not null,
    order_number                            INT         not null
);
INSERT INTO category_mail_filter_datasource_type
    (category_mail_filter_datasource_type_id, order_number, name)
VALUES (0, 0, 'メールタイトル'),
       (1, 1, 'メールFrom'),
       (2, 2, 'メールHTML'),
       (5, 3, 'メールテキスト'),
       (3, 4, 'タイトル'),
       (4, 5, 'サービス名');

CREATE TABLE category_mail_filter_condition_type
(
    category_mail_filter_condition_type_id INT         not null PRIMARY KEY,
    name                                   VARCHAR(50) not null,
    order_number                           INT         not null
);
INSERT INTO category_mail_filter_condition_type
    (category_mail_filter_condition_type_id, order_number, name)
VALUES (0, 0, '含む'),
       (1, 1, '含まない'),
       (2, 2, '一致する'),
       (3, 3, '一致しない');
