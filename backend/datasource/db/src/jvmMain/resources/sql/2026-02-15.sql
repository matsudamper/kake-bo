ALTER TABLE money_usages
    ADD COLUMN image_hash CHAR(64) NULL AFTER amount;
