package com.xhulife.service;

import com.xhulife.entity.FreeQualificationActivity;
import com.xhulife.entity.FreeQualificationRecord;
import com.xhulife.repository.FreeQualificationRedisRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static com.xhulife.utils.RedisConstants.FREE_QUALIFICATION_STOCK_KEY;

@Slf4j
@Component
public class QualificationMaintenanceJob {
    @Resource private IFreeQualificationRecordService recordService;
    @Resource private IFreeQualificationActivityService activityService;
    @Resource private FreeQualificationRedisRepository redisRepository;
    @Resource private StringRedisTemplate redis;

    @Scheduled(fixedDelay = 60000)
    public void expireAndReconcile() {
        LocalDateTime now = LocalDateTime.now();
        recordService.update().set("status", 2).eq("status", 0)
                .inSql("activity_id", "SELECT id FROM tb_free_qualification_activity WHERE end_time < NOW()").update();
        List<FreeQualificationActivity> active = activityService.query().eq("status", 1).ge("end_time", now).list();
        for (FreeQualificationActivity activity : active) {
            redisRepository.ensureInitialized(activity.getId());
            String stock = redis.opsForValue().get(FREE_QUALIFICATION_STOCK_KEY + activity.getId());
            if (stock != null && Integer.parseInt(stock) != activity.getRemainingQuota()) {
                log.warn("Qualification quota differs temporarily. activityId={}, redis={}, mysql={}",
                        activity.getId(), stock, activity.getRemainingQuota());
            }
        }
    }
}
