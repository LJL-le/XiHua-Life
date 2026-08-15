package com.xhulife.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhulife.dto.Result;
import com.xhulife.entity.FreeQualificationRecord;

public interface IFreeQualificationRecordService extends IService<FreeQualificationRecord> {

    Result claim(Long activityId);

    Result queryMyRecords(Integer current);

    Result queryMyRecord(Long id);
}
