package com.xhulife.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long actorId;
    private String type;
    private String content;
    private String targetType;
    private Long targetId;
    private Boolean isRead;
    private String dedupeKey;
    private LocalDateTime createTime;
}
