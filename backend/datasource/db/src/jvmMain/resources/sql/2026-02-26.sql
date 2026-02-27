CREATE TABLE recurring_usage_rules
(
    recurring_usage_rule_id     INT PRIMARY KEY AUTO_INCREMENT,
    user_id                     INT          not null,
    title                       VARCHAR(500) not null,
    description                 VARCHAR(1000) not null,
    amount                      INT          not null,
    money_usage_sub_category_id INT,
    next_usage_date             DATE         not null,
    interval_iso                VARCHAR(20)  not null,
    lead_time_iso               VARCHAR(20)  not null,
    created_datetime            DATETIME DEFAULT CURRENT_TIMESTAMP not null,
    update_datetime             DATETIME DEFAULT CURRENT_TIMESTAMP not null ON UPDATE CURRENT_TIMESTAMP,
    INDEX recurring_usage_rules_user_id (user_id),
    INDEX recurring_usage_rules_next_usage_date (next_usage_date)
);
