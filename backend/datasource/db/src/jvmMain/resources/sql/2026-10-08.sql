-- 主キーに画像IDを含めるため、同じ利用に同じ画像が重複して紐付いている行は並び順が最小のものだけ残す
DELETE duplicated
FROM money_usage_images_relation duplicated
         JOIN money_usage_images_relation kept
              ON duplicated.user_id = kept.user_id
                  AND duplicated.money_usage_id = kept.money_usage_id
                  AND duplicated.user_image_id = kept.user_image_id
                  AND duplicated.image_order > kept.image_order;

ALTER TABLE money_usage_images_relation
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (user_id, money_usage_id, user_image_id),
    ADD INDEX user_money_usage_image_order (user_id, money_usage_id, image_order);
