CREATE TABLE user_setting
(
    user_id                  INT NOT NULL PRIMARY KEY,
    timezone_offset_minutes  INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_user_setting_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);
