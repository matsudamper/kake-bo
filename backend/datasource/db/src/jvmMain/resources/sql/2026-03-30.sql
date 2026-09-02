CREATE TABLE user_timezone_setting
(
    user_id                  INT NOT NULL PRIMARY KEY,
    timezone_offset_minutes  INT NOT NULL DEFAULT 0
);
