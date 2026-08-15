SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS tb_user (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, phone VARCHAR(20) NOT NULL UNIQUE, password VARCHAR(100),
 nick_name VARCHAR(64) NOT NULL, icon VARCHAR(255) DEFAULT '', role VARCHAR(16) NOT NULL DEFAULT 'USER',
 create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tb_user_info (
 user_id BIGINT PRIMARY KEY, city VARCHAR(64), introduce VARCHAR(255), fans INT NOT NULL DEFAULT 0,
 followee INT NOT NULL DEFAULT 0, gender TINYINT(1), birthday DATE, credits INT NOT NULL DEFAULT 0,
 level TINYINT(1) NOT NULL DEFAULT 0, create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tb_shop_type (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(64) NOT NULL, icon VARCHAR(255), sort INT DEFAULT 0,
 create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tb_shop (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(128) NOT NULL, type_id BIGINT NOT NULL, images TEXT,
 area VARCHAR(64), address VARCHAR(255), x DOUBLE, y DOUBLE, avg_price BIGINT DEFAULT 0, sold INT DEFAULT 0,
 comments INT NOT NULL DEFAULT 0, score INT NOT NULL DEFAULT 0, open_hours VARCHAR(64),
 create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY idx_shop_type(type_id), KEY idx_shop_name(name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tb_blog (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, shop_id BIGINT, user_id BIGINT NOT NULL, title VARCHAR(255) NOT NULL,
 images TEXT, content TEXT, liked INT NOT NULL DEFAULT 0, comments INT NOT NULL DEFAULT 0, status INT NOT NULL DEFAULT 0,
 create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY idx_blog_user(user_id), KEY idx_blog_hot(status,liked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tb_blog_comments (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, blog_id BIGINT NOT NULL, parent_id BIGINT DEFAULT 0,
 answer_id BIGINT DEFAULT 0, content VARCHAR(1000) NOT NULL, liked INT NOT NULL DEFAULT 0, status TINYINT NOT NULL DEFAULT 0,
 create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY idx_comment_blog(blog_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tb_follow (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, follow_user_id BIGINT NOT NULL,
 create_time DATETIME DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uk_follow_user(user_id,follow_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tb_shop_review (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, shop_id BIGINT NOT NULL, user_id BIGINT NOT NULL, score INT NOT NULL,
 taste_score INT NOT NULL, environment_score INT NOT NULL, service_score INT NOT NULL, content VARCHAR(1000) NOT NULL,
 images TEXT, status INT NOT NULL DEFAULT 0, create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uk_review_shop_user(shop_id,user_id), KEY idx_review_shop(shop_id,status,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tb_message (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, actor_id BIGINT, type VARCHAR(32) NOT NULL,
 content VARCHAR(255) NOT NULL, target_type VARCHAR(32), target_id BIGINT, is_read TINYINT(1) NOT NULL DEFAULT 0,
 dedupe_key VARCHAR(128), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_message_dedupe(dedupe_key), KEY idx_message_user(user_id,is_read,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tb_free_qualification_activity (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, shop_id BIGINT NOT NULL, title VARCHAR(255) NOT NULL, sub_title VARCHAR(255), rules VARCHAR(1024),
 total_quota INT NOT NULL, remaining_quota INT NOT NULL, begin_time DATETIME NOT NULL, end_time DATETIME NOT NULL,
 status INT NOT NULL DEFAULT 1, create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, KEY idx_activity_shop(shop_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tb_free_qualification_record (
 id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, activity_id BIGINT NOT NULL, status INT NOT NULL DEFAULT 0,
 redeem_code VARCHAR(64) NOT NULL UNIQUE, create_time DATETIME DEFAULT CURRENT_TIMESTAMP, use_time DATETIME,
 update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uk_record_user_activity(user_id,activity_id), KEY idx_record_activity(activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO tb_user(id,phone,password,nick_name,role) VALUES
(1,'13800000000','xhulife-demo-salt@266cb0916d91e224ac1095ede1e251a7','演示管理员','ADMIN'),
(2,'13900000000','xhulife-demo-salt@6eb025e9565d37109fc19da8a3ab3d06','演示用户','USER');
INSERT IGNORE INTO tb_user_info(user_id,city,introduce) VALUES (1,'成都','系统管理员'),(2,'成都','热爱校园生活');
INSERT IGNORE INTO tb_shop_type(id,name,icon,sort) VALUES (1,'美食','types/xhul-food.png',1),(2,'休闲娱乐','types/xhul-fun.png',2),(3,'运动健身','types/xhul-sport.png',3);
INSERT IGNORE INTO tb_shop(id,name,type_id,images,area,address,x,y,avg_price,sold,comments,score,open_hours) VALUES
(1,'西华校园餐厅',1,'/imgs/blogs/blog1.jpg','红光校区','西华大学校内',103.958,30.782,2500,128,1,50,'08:00-22:00');
INSERT IGNORE INTO tb_blog(id,shop_id,user_id,title,images,content,liked,comments,status) VALUES
(1,1,2,'校园餐厅体验','/imgs/blogs/blog1.jpg','价格实惠，适合同学聚餐。',0,0,0);
INSERT IGNORE INTO tb_shop_review(id,shop_id,user_id,score,taste_score,environment_score,service_score,content,status) VALUES
(1,1,2,5,5,5,5,'味道很好，服务也很热情。',0);
INSERT IGNORE INTO tb_free_qualification_activity(id,shop_id,title,sub_title,rules,total_quota,remaining_quota,begin_time,end_time,status) VALUES
(1,1,'校园店铺限量免单','每人限领一份','领取后请在活动结束前到店核销',20,20,DATE_SUB(NOW(),INTERVAL 10 MINUTE),DATE_ADD(NOW(),INTERVAL 7 DAY),1);
