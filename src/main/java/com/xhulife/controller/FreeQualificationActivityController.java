package com.xhulife.controller;

import com.xhulife.dto.Result;
import com.xhulife.entity.FreeQualificationActivity;
import com.xhulife.service.IFreeQualificationActivityService;
import com.xhulife.service.IFreeQualificationRecordService;
import com.xhulife.utils.UserHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/free-qualification/activities")
public class FreeQualificationActivityController {

    @Resource
    private IFreeQualificationActivityService activityService;
    @Resource
    private IFreeQualificationRecordService recordService;

    @PostMapping
    public Result createActivity(@RequestBody FreeQualificationActivity activity) {
        if (UserHolder.getUser() == null || !"ADMIN".equals(UserHolder.getUser().getRole())) return Result.fail("仅管理员可创建活动");
        return activityService.createActivity(activity);
    }

    @GetMapping("/shop/{shopId}")
    public Result queryActivitiesOfShop(@PathVariable Long shopId) {
        return activityService.queryActivitiesOfShop(shopId);
    }

    @PostMapping("/{activityId}/claim")
    public Result claim(@PathVariable Long activityId) {
        return recordService.claim(activityId);
    }
}
