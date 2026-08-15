package com.xhulife.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tb_free_qualification_record")
public class FreeQualificationRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private Long userId;
    private Long activityId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime useTime;
    private String redeemCode;
    private LocalDateTime updateTime;
}
