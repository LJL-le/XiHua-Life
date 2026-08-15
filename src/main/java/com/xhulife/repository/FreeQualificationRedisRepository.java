package com.xhulife.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xhulife.entity.FreeQualificationActivity;
import com.xhulife.entity.FreeQualificationRecord;
import com.xhulife.mapper.FreeQualificationActivityMapper;
import com.xhulife.mapper.FreeQualificationRecordMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.xhulife.utils.RedisConstants.FREE_QUALIFICATION_ACTIVITY_KEY;
import static com.xhulife.utils.RedisConstants.FREE_QUALIFICATION_STOCK_KEY;
import static com.xhulife.utils.RedisConstants.FREE_QUALIFICATION_USERS_INITIALIZED_MEMBER;
import static com.xhulife.utils.RedisConstants.FREE_QUALIFICATION_USERS_KEY;
import static com.xhulife.utils.RedisConstants.LOCK_FREE_QUALIFICATION_INIT_KEY;

@Repository
public class FreeQualificationRedisRepository {

    private static final Logger log = LoggerFactory.getLogger(FreeQualificationRedisRepository.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Duration RETENTION_AFTER_END = Duration.ofDays(1);

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private FreeQualificationActivityMapper activityMapper;
    @Resource
    private FreeQualificationRecordMapper recordMapper;
    @Resource
    private RedissonClient redissonClient;

    public boolean isInitialized(Long activityId) {
        Boolean activityExists = stringRedisTemplate.hasKey(FREE_QUALIFICATION_ACTIVITY_KEY + activityId);
        Boolean stockExists = stringRedisTemplate.hasKey(FREE_QUALIFICATION_STOCK_KEY + activityId);
        Boolean usersExists = stringRedisTemplate.hasKey(FREE_QUALIFICATION_USERS_KEY + activityId);
        return Boolean.TRUE.equals(activityExists)
                && Boolean.TRUE.equals(stockExists)
                && Boolean.TRUE.equals(usersExists);
    }

    public boolean ensureInitialized(Long activityId) {
        if (isInitialized(activityId)) {
            return true;
        }
        FreeQualificationActivity activity = activityMapper.selectById(activityId);
        if (!isActive(activity)) {
            return false;
        }
        return initializeIfMissing(activity);
    }

    public boolean initializeIfMissing(FreeQualificationActivity activity) {
        if (!isActive(activity)) {
            return false;
        }
        if (isInitialized(activity.getId())) {
            return true;
        }

        RLock lock = redissonClient.getLock(LOCK_FREE_QUALIFICATION_INIT_KEY + activity.getId());
        boolean locked = false;
        try {
            lock.lock();
            locked = true;
            if (isInitialized(activity.getId())) {
                return true;
            }
            repairMissingKeys(activity);
            return true;
        } catch (RuntimeException e) {
            log.error("Failed to initialize free qualification in Redis. activityId={}", activity.getId(), e);
            return false;
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    private void repairMissingKeys(FreeQualificationActivity activity) {
        String activityKey = FREE_QUALIFICATION_ACTIVITY_KEY + activity.getId();
        String stockKey = FREE_QUALIFICATION_STOCK_KEY + activity.getId();
        String usersKey = FREE_QUALIFICATION_USERS_KEY + activity.getId();

        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(activityKey))) {
            stringRedisTemplate.opsForHash().put(activityKey, "status", String.valueOf(activity.getStatus()));
            stringRedisTemplate.opsForHash().put(activityKey, "beginAt", String.valueOf(toEpochMillis(activity.getBeginTime())));
            stringRedisTemplate.opsForHash().put(activityKey, "endAt", String.valueOf(toEpochMillis(activity.getEndTime())));
        }
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(activity.getRemainingQuota()));
        }
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(usersKey))) {
            stringRedisTemplate.opsForSet().add(usersKey, FREE_QUALIFICATION_USERS_INITIALIZED_MEMBER);
            List<FreeQualificationRecord> records = recordMapper.selectList(
                    new QueryWrapper<FreeQualificationRecord>()
                            .select("user_id")
                            .eq("activity_id", activity.getId())
            );
            List<String> userIds = records.stream()
                    .map(record -> String.valueOf(record.getUserId()))
                    .collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                stringRedisTemplate.opsForSet().add(usersKey, userIds.toArray(new String[0]));
            }
        }

        long ttlSeconds = Duration.between(
                LocalDateTime.now(BUSINESS_ZONE),
                activity.getEndTime().plus(RETENTION_AFTER_END)
        ).getSeconds();
        ttlSeconds = Math.max(ttlSeconds, 60L);
        stringRedisTemplate.expire(activityKey, ttlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.expire(stockKey, ttlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.expire(usersKey, ttlSeconds, TimeUnit.SECONDS);
    }

    private boolean isActive(FreeQualificationActivity activity) {
        return activity != null
                && Integer.valueOf(1).equals(activity.getStatus())
                && activity.getEndTime() != null
                && !activity.getEndTime().isBefore(LocalDateTime.now(BUSINESS_ZONE));
    }

    private long toEpochMillis(LocalDateTime time) {
        return time.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
    }
}
