package com.xhulife.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xhulife.entity.FreeQualificationActivity;
import com.xhulife.mapper.FreeQualificationActivityMapper;
import com.xhulife.repository.FreeQualificationRedisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class FreeQualificationRedisRecovery {

    private static final Logger log = LoggerFactory.getLogger(FreeQualificationRedisRecovery.class);

    @Resource
    private FreeQualificationActivityMapper activityMapper;
    @Resource
    private FreeQualificationRedisRepository redisRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpAfterStartup() {
        recoverMissingActivities();
    }

    @Scheduled(fixedDelay = 60_000L, initialDelay = 60_000L)
    public void recoverMissingActivities() {
        List<FreeQualificationActivity> activities = activityMapper.selectList(
                new QueryWrapper<FreeQualificationActivity>()
                        .eq("status", 1)
                        .ge("end_time", LocalDateTime.now())
        );
        int recovered = 0;
        for (FreeQualificationActivity activity : activities) {
            if (!redisRepository.isInitialized(activity.getId())
                    && redisRepository.initializeIfMissing(activity)) {
                recovered++;
            }
        }
        if (recovered > 0) {
            log.info("Recovered missing free qualification Redis data. count={}", recovered);
        }
    }
}
