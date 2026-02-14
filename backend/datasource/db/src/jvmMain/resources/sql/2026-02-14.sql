CREATE TABLE user_images
(
    user_image_id    INT PRIMARY KEY AUTO_INCREMENT,
    user_id          INT                                 not null,
    image_hash       CHAR(64)                            not null,
    image_path       VARCHAR(1000)                       not null,
    created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP  not null,
    CONSTRAINT user_image_unique UNIQUE (user_id, image_hash)
);
CREATE INDEX user_image_user_id ON user_images (user_id);
