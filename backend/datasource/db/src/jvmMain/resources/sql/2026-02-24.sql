ALTER TABLE money_usage_presets
    ADD COLUMN amount      INT          NULL,
    ADD COLUMN description VARCHAR(1000) NULL;
