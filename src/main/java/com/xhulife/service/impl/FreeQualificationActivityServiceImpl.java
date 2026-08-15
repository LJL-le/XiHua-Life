package com.xhulife.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhulife.dto.Result;
import com.xhulife.entity.FreeQualificationActivity;
import com.xhulife.mapper.FreeQualificationActivityMapper;
import com.xhulife.repository.FreeQualificationRedisRepository;
import com.xhulife.service.IFreeQualificationActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class FreeQualificationActivityServiceImpl
        extends ServiceImpl<FreeQualificationActivityMapper, FreeQualificationActivity>
        implements IFreeQualificationActivityService {

    private static final Logger log = LoggerFactory.getLogger(FreeQualificationActivityServiceImpl.class);

    @Resource
    private FreeQualificationRedisRepository redisRepository;

    @Override
    @Transactional
    public Result createActivity(FreeQualificationActivity activity) {
        if (activity.getTotalQuota() == null || activity.getTotalQuota() < 1) {
            return Result.fail("免单名额必须大于 0");
        }
        if (activity.getBeginTime() == null || activity.getEndTime() == null
                || !activity.getEndTime().isAfter(activity.getBeginTime())) {
            return Result.fail("活动时间范围不合法");
        }
        activity.setRemainingQuota(activity.getTotalQuota());
        activity.setStatus(1);
        if (!save(activity)) {
            return Result.fail("创建免单资格活动失败");
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    initializeReservationSafely(activity);
                }
            });
        } else {
            initializeReservationSafely(activity);
        }
        return Result.ok(activity.getId());
    }

    @Override
    public Result queryActivitiesOfShop(Long shopId) {
        return Result.ok(query()
                .eq("shop_id", shopId)
                .eq("status", 1)
                .ge("end_time", LocalDateTime.now())
                .orderByAsc("begin_time")
                .list());
    }

    private void initializeReservationSafely(FreeQualificationActivity activity) {
        if (!redisRepository.initializeIfMissing(activity)) {
            log.warn("Free qualification activity was saved but Redis initialization is pending retry. activityId={}",
                    activity.getId());
        }
    }
}
