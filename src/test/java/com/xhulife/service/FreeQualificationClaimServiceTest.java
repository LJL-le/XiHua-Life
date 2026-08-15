package com.xhulife.service;

import com.xhulife.dto.Result;
import com.xhulife.dto.UserDTO;
import com.xhulife.dto.FreeQualificationRecordDTO;
import com.xhulife.entity.FreeQualificationActivity;
import com.xhulife.entity.FreeQualificationRecord;
import com.xhulife.repository.FreeQualificationRedisRepository;
import com.xhulife.service.IFreeQualificationActivityService;
import com.xhulife.service.impl.FreeQualificationRecordServiceImpl;
import com.xhulife.utils.RedisIdWorker;
import com.xhulife.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreeQualificationClaimServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private RedisIdWorker redisIdWorker;
    @Mock
    private FreeQualificationRedisRepository redisRepository;
    @Mock
    private IFreeQualificationActivityService activityService;
    @InjectMocks
    private FreeQualificationRecordServiceImpl recordService;

    @AfterEach
    void clearUser() {
        UserHolder.removeUser();
    }

    @Test
    void claimReturnsRecordIdWhenLuaSucceeds() {
        login(8L);
        when(redisRepository.ensureInitialized(3L)).thenReturn(true);
        when(redisIdWorker.nextId("free-qualification-record")).thenReturn(101L);
        when(stringRedisTemplate.execute(
                org.mockito.ArgumentMatchers.<DefaultRedisScript<Long>>any(),
                org.mockito.ArgumentMatchers.<java.util.List<String>>any(),
                anyString(), anyString(), anyString()))
                .thenReturn(0L);

        Result result = recordService.claim(3L);

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(101L);
    }

    @Test
    void claimRejectsDuplicateQualification() {
        login(8L);
        when(redisRepository.ensureInitialized(3L)).thenReturn(true);
        when(redisIdWorker.nextId("free-qualification-record")).thenReturn(101L);
        when(stringRedisTemplate.execute(
                org.mockito.ArgumentMatchers.<DefaultRedisScript<Long>>any(),
                org.mockito.ArgumentMatchers.<java.util.List<String>>any(),
                anyString(), anyString(), anyString()))
                .thenReturn(5L);

        Result result = recordService.claim(3L);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorMsg()).isEqualTo("您已获得该活动的免单资格");
    }

    @Test
    void claimRejectsActivityThatCannotBeRecovered() {
        login(8L);
        when(redisRepository.ensureInitialized(3L)).thenReturn(false);

        Result result = recordService.claim(3L);

        assertThat(result.getSuccess()).isFalse();
    }

    @Test
    void unusedRecordIsReportedAsExpiredWithoutChangingStoredStatus() {
        FreeQualificationRecord record = new FreeQualificationRecord();
        record.setId(101L);
        record.setActivityId(3L);
        record.setStatus(0);
        FreeQualificationActivity activity = new FreeQualificationActivity();
        activity.setId(3L);
        activity.setEndTime(LocalDateTime.now().minusMinutes(1));
        when(activityService.getById(3L)).thenReturn(activity);

        FreeQualificationRecordDTO dto = ReflectionTestUtils.invokeMethod(recordService, "toDTO", record);

        assertThat(dto.getStatus()).isEqualTo(2);
        assertThat(record.getStatus()).isEqualTo(0);
    }

    private void login(Long userId) {
        UserDTO user = new UserDTO();
        user.setId(userId);
        UserHolder.saveUser(user);
    }
}
