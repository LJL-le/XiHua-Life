package com.xhulife.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhulife.dto.Result;
import com.xhulife.entity.FreeQualificationActivity;

public interface IFreeQualificationActivityService extends IService<FreeQualificationActivity> {

    Result createActivity(FreeQualificationActivity activity);

    Result queryActivitiesOfShop(Long shopId);
}
