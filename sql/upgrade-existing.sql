-- Upgrade the legacy XHU Life schema without deleting existing business data.
ALTER TABLE tb_user ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER' AFTER icon;
ALTER TABLE tb_blog ADD COLUMN status INT NOT NULL DEFAULT 0 AFTER comments;
ALTER TABLE tb_free_qualification_record ADD COLUMN redeem_code VARCHAR(64) NULL AFTER status;
UPDATE tb_free_qualification_record
SET redeem_code = REPLACE(UUID(), '-', '')
WHERE redeem_code IS NULL OR redeem_code = '';
ALTER TABLE tb_free_qualification_record MODIFY redeem_code VARCHAR(64) NOT NULL;
ALTER TABLE tb_free_qualification_record ADD UNIQUE KEY uk_record_redeem_code (redeem_code);
ALTER TABLE tb_follow ADD UNIQUE KEY uk_follow_user (user_id, follow_user_id);

INSERT INTO tb_user(phone,password,nick_name,icon,role)
VALUES ('13800000000','xhulife-demo-salt@266cb0916d91e224ac1095ede1e251a7','演示管理员','','ADMIN')
ON DUPLICATE KEY UPDATE password=VALUES(password), role='ADMIN';
INSERT INTO tb_user(phone,password,nick_name,icon,role)
VALUES ('13900000000','xhulife-demo-salt@6eb025e9565d37109fc19da8a3ab3d06','演示用户','','USER')
ON DUPLICATE KEY UPDATE password=VALUES(password), role='USER';
INSERT IGNORE INTO tb_user_info(user_id,city,introduce,fans,followee,credits,level)
SELECT id,'成都','课程演示账号',0,0,0,0 FROM tb_user
WHERE phone IN ('13800000000','13900000000');
