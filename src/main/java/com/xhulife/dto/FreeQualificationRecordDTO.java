package com.xhulife.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FreeQualificationRecordDTO {
    private Long id;
    private Long activityId;
    private Long shopId;
    private String activityTitle;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime useTime;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private String statusName;
    private String redeemCode;
}
