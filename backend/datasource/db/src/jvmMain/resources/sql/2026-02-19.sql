CREATE TABLE money_usage_presets
(
    money_usage_preset_id       INT PRIMARY KEY AUTO_INCREMENT,
    user_id                     INT          NOT NULL,
    name                        VARCHAR(500) NOT NULL,
    money_usage_sub_category_id INT,
    order_number                INT          NOT NULL DEFAULT 0,
    created_datetime            DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_datetime             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX user_id (user_id),
    INDEX user_preset (user_id, money_usage_preset_id)
);
