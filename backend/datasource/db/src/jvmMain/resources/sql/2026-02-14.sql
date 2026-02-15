CREATE TABLE user_images
(
    user_image_id    INT PRIMARY KEY AUTO_INCREMENT,
    user_id          INT                                not null,
    display_id       CHAR(36)                           not null,
    image_path       VARCHAR(1000)                      not null,
    content_type     VARCHAR(255)                       not null,
    created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP not null,
    uploaded         boolean                            not null,
    CONSTRAINT user_image_display_id_unique UNIQUE (display_id),
    CONSTRAINT image_path_unique UNIQUE (image_path),
    INDEX user_image_user_id (user_id)
);

CREATE TABLE money_usage_images_relation
(
    user_id        INT not null,
    money_usage_id INT not null,
    user_image_id  INT not null,
    image_order    INT not null,
    PRIMARY KEY (user_id, money_usage_id, image_order),
    INDEX user_money_usage_id (user_id, money_usage_id),
    INDEX user_image_id (user_id, user_image_id)
);
