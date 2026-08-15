package com.xhulife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xhulife.entity.FreeQualificationRecord;
import com.xhulife.mapper.FreeQualificationActivityMapper;
import com.xhulife.mapper.FreeQualificationRecordMapper;
import com.xhulife.service.FreeQualificationRecordWriter;
import com.xhulife.service.IMessageService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.UUID;

import static com.xhulife.utils.RedisConstants.LOCK_FREE_QUALIFICATION_KEY;

@Service
public class FreeQualificationRecordWriterImpl implements FreeQualificationRecordWriter {

    @Resource
    private FreeQualificationActivityMapper activityMapper;
    @Resource
    private FreeQualificationRecordMapper recordMapper;
    @Resource
    private RedissonClient redissonClient;
    @Resource private IMessageService messageService;

    @Override
    @Transactional
    public void write(Long recordId, Long userId, Long activityId) {
        RLock lock = redissonClient.getLock(LOCK_FREE_QUALIFICATION_KEY + activityId + ":" + userId);
        lock.lock();
        try {
            Integer existing = recordMapper.selectCount(new QueryWrapper<FreeQualificationRecord>()
                    .eq("user_id", userId)
                    .eq("activity_id", activityId));
            if (existing != null && existing > 0) {
                return;
            }
            if (activityMapper.decrementQuotaIfAvailable(activityId) != 1) {
                throw new IllegalStateException("Qualification quota is inconsistent");
            }
            FreeQualificationRecord record = new FreeQualificationRecord();
            record.setId(recordId);
            record.setUserId(userId);
            record.setActivityId(activityId);
            record.setStatus(0);
            record.setRedeemCode(UUID.randomUUID().toString().replace("-", ""));
            recordMapper.insert(record);
            if (messageService != null) messageService.notify(userId, null, "QUALIFICATION", "你已获得免单资格", "QUALIFICATION", recordId,
                    "QUALIFICATION:" + recordId);
        } finally {
            lock.unlock();
        }
    }
}
