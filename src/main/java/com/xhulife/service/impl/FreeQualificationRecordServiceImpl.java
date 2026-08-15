package com.xhulife.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhulife.dto.Result;
import com.xhulife.dto.UserDTO;
import com.xhulife.dto.FreeQualificationRecordDTO;
import com.xhulife.entity.FreeQualificationActivity;
import com.xhulife.repository.FreeQualificationRedisRepository;
import com.xhulife.service.IFreeQualificationActivityService;
import com.xhulife.utils.SystemConstants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xhulife.entity.FreeQualificationRecord;
import com.xhulife.mapper.FreeQualificationRecordMapper;
import com.xhulife.service.IFreeQualificationRecordService;
import com.xhulife.utils.RedisIdWorker;
import com.xhulife.utils.UserHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.xhulife.utils.RedisConstants.FREE_QUALIFICATION_ACTIVITY_KEY;
import static com.xhulife.utils.RedisConstants.FREE_QUALIFICATION_STOCK_KEY;
import static com.xhulife.utils.RedisConstants.FREE_QUALIFICATION_STREAM_KEY;
import static com.xhulife.utils.RedisConstants.FREE_QUALIFICATION_USERS_KEY;

@Service
public class FreeQualificationRecordServiceImpl
        extends ServiceImpl<FreeQualificationRecordMapper, FreeQualificationRecord>
        implements IFreeQualificationRecordService {

    private static final DefaultRedisScript<Long> CLAIM_SCRIPT = new DefaultRedisScript<>();

    static {
        CLAIM_SCRIPT.setLocation(new ClassPathResource("free_qualification_claim.lua"));
        CLAIM_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private IFreeQualificationActivityService activityService;
    @Resource
    private FreeQualificationRedisRepository redisRepository;

    @Override
    public Result claim(Long activityId) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        if (!redisRepository.ensureInitialized(activityId)) {
            return Result.fail(messageOf(3L));
        }
        long recordId = redisIdWorker.nextId("free-qualification-record");
        Long result = executeClaim(activityId, user.getId(), recordId);
        if (Long.valueOf(0L).equals(result)) {
            return Result.ok(recordId);
        }
        if (Long.valueOf(3L).equals(result)
                && !redisRepository.isInitialized(activityId)
                && redisRepository.ensureInitialized(activityId)) {
            result = executeClaim(activityId, user.getId(), recordId);
            if (Long.valueOf(0L).equals(result)) {
                return Result.ok(recordId);
            }
        }
        return Result.fail(messageOf(result));
    }

    private Long executeClaim(Long activityId, Long userId, long recordId) {
        return stringRedisTemplate.execute(
                CLAIM_SCRIPT,
                Arrays.asList(
                        FREE_QUALIFICATION_ACTIVITY_KEY + activityId,
                        FREE_QUALIFICATION_STOCK_KEY + activityId,
                        FREE_QUALIFICATION_USERS_KEY + activityId,
                        FREE_QUALIFICATION_STREAM_KEY
                ),
                String.valueOf(userId),
                String.valueOf(recordId),
                String.valueOf(activityId)
        );
    }

    @Override
    public Result queryMyRecords(Integer current) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        long pageNo = current == null || current < 1 ? 1 : current;
        Page<FreeQualificationRecord> page = query()
                .eq("user_id", user.getId())
                .orderByDesc("create_time")
                .page(new Page<>(pageNo, SystemConstants.DEFAULT_PAGE_SIZE));
        List<FreeQualificationRecordDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return Result.ok(records, page.getTotal());
    }

    @Override
    public Result queryMyRecord(Long id) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        FreeQualificationRecord record = query()
                .eq("id", id)
                .eq("user_id", user.getId())
                .one();
        return record == null ? Result.fail("免单资格记录不存在") : Result.ok(toDTO(record));
    }

    private FreeQualificationRecordDTO toDTO(FreeQualificationRecord record) {
        FreeQualificationRecordDTO dto = new FreeQualificationRecordDTO();
        dto.setId(record.getId());
        dto.setActivityId(record.getActivityId());
        dto.setStatus(record.getStatus());
        dto.setCreateTime(record.getCreateTime());
        dto.setUseTime(record.getUseTime());
        dto.setRedeemCode(record.getRedeemCode());
        FreeQualificationActivity activity = activityService.getById(record.getActivityId());
        if (activity != null) {
            dto.setShopId(activity.getShopId());
            dto.setActivityTitle(activity.getTitle());
            dto.setBeginTime(activity.getBeginTime());
            dto.setEndTime(activity.getEndTime());
            if (Integer.valueOf(0).equals(record.getStatus())
                    && activity.getEndTime() != null
                    && activity.getEndTime().isBefore(java.time.LocalDateTime.now())) {
                dto.setStatus(2);
            }
        }
        switch (dto.getStatus() == null ? 0 : dto.getStatus()) {
            case 1: dto.setStatusName("USED"); break;
            case 2: dto.setStatusName("EXPIRED"); break;
            case 3: dto.setStatusName("CANCELLED"); break;
            default: dto.setStatusName("UNUSED");
        }
        return dto;
    }

    private String messageOf(Long result) {
        if (result == null) {
            return "抢免单资格失败，请稍后重试";
        }
        switch (result.intValue()) {
            case 1:
                return "活动尚未开始";
            case 2:
                return "活动已结束";
            case 3:
                return "活动不可用";
            case 4:
                return "免单名额已抢完";
            case 5:
                return "您已获得该活动的免单资格";
            default:
                return "抢免单资格失败，请稍后重试";
        }
    }
}
