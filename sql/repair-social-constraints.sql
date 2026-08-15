-- Run after removing any existing duplicate follow rows.
-- Back up the database before applying schema changes.

ALTER TABLE tb_follow
    ADD UNIQUE KEY uk_follow_user (user_id, follow_user_id);

UPDATE tb_user_info SET fans = 0 WHERE fans IS NULL OR fans < 0;
UPDATE tb_user_info SET followee = 0 WHERE followee IS NULL OR followee < 0;

ALTER TABLE tb_user_info
    MODIFY fans INT NOT NULL DEFAULT 0,
    MODIFY followee INT NOT NULL DEFAULT 0;

UPDATE tb_blog SET liked = 0 WHERE liked IS NULL OR liked < 0;
UPDATE tb_blog SET comments = 0 WHERE comments IS NULL OR comments < 0;

ALTER TABLE tb_blog
    MODIFY liked INT NOT NULL DEFAULT 0,
    MODIFY comments INT NOT NULL DEFAULT 0;
