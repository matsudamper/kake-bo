CREATE TABLE user_ai_settings
(
    user_id                  INT NOT NULL PRIMARY KEY,
    encrypted_gemini_api_key VARCHAR(1000)
);
