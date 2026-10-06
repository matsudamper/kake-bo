RENAME TABLE category_mail_filter_conditions TO category_mail_filter_matchers,
    category_mail_filter_condition_type TO category_mail_filter_matcher_type;

ALTER TABLE category_mail_filter_matcher_type
    CHANGE COLUMN category_mail_filter_condition_type_id category_mail_filter_matcher_type_id INT NOT NULL;

ALTER TABLE category_mail_filter_matchers
    CHANGE COLUMN category_mail_filter_condition_id category_mail_filter_matcher_id INT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN category_mail_filter_condition_type_id category_mail_filter_matcher_type_id INT NOT NULL,
    ADD COLUMN matcher_key VARCHAR(50) NULL AFTER category_mail_filter_id,
    RENAME INDEX user_category_mail_filter_condition_id TO user_category_mail_filter_matcher_id;

UPDATE category_mail_filter_matchers matchers
    JOIN (SELECT category_mail_filter_matcher_id,
                 ROW_NUMBER() OVER (
                     PARTITION BY category_mail_filter_id
                     ORDER BY category_mail_filter_matcher_id
                     ) AS key_number
          FROM category_mail_filter_matchers) numbered
    ON matchers.category_mail_filter_matcher_id = numbered.category_mail_filter_matcher_id
SET matchers.matcher_key     = CONCAT('id', numbered.key_number),
    matchers.update_datetime = matchers.update_datetime;

ALTER TABLE category_mail_filter_matchers
    MODIFY COLUMN matcher_key VARCHAR(50) NOT NULL,
    ADD CONSTRAINT category_mail_filter_matcher_key_unique UNIQUE (category_mail_filter_id, matcher_key);

ALTER TABLE category_mail_filters
    ADD COLUMN match_expression TEXT NULL AFTER category_mail_filter_condition_operator_type_id;
