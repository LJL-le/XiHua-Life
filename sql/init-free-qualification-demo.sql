-- Non-destructive development initialization.
-- Initializes only the free-qualification tables and demo activity.

CREATE TABLE IF NOT EXISTS tb_free_qualification_activity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    shop_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    sub_title VARCHAR(255) DEFAULT NULL,
    rules VARCHAR(1024) DEFAULT NULL,
    total_quota INT NOT NULL,
    remaining_quota INT NOT NULL,
    begin_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_shop_status (shop_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tb_free_qualification_record (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    use_time DATETIME DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_activity (user_id, activity_id),
    KEY idx_activity (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO tb_free_qualification_activity (
    shop_id, title, sub_title, rules, total_quota, remaining_quota,
    begin_time, end_time, status
)
SELECT 1, '校园店铺限量免单', '每人限领一份',
       '领取成功后请在活动结束前到店使用',
       20, 20, DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 7 DAY), 1
WHERE NOT EXISTS (
    SELECT 1 FROM tb_free_qualification_activity
    WHERE shop_id = 1 AND status = 1 AND end_time >= NOW()
);
