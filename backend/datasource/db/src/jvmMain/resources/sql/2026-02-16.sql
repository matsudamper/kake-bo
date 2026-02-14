CREATE TABLE money_usage_images
(
    user_id        INT      not null,
    money_usage_id INT      not null,
    image_hash     CHAR(64) not null,
    image_order    INT      not null,
    PRIMARY KEY (user_id, money_usage_id, image_order),
    INDEX user_money_usage_id (user_id, money_usage_id),
    INDEX user_image_hash (user_id, image_hash)
);
