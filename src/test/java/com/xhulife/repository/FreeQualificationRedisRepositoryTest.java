package com.xhulife.repository;

import com.xhulife.entity.FreeQualificationActivity;
import com.xhulife.entity.FreeQualificationRecord;
import com.xhulife.mapper.FreeQualificationActivityMapper;
import com.xhulife.mapper.FreeQualificationRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreeQualificationRedisRepositoryTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private FreeQualificationActivityMapper activityMapper;
    @Mock
    private FreeQualificationRecordMapper recordMapper;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;
    @InjectMocks
    private FreeQualificationRedisRepository repository;

    @Test
    void completeRedisStateIsNotOverwritten() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

        boolean initialized = repository.initializeIfMissing(activeActivity());

        assertThat(initialized).isTrue();
        verify(stringRedisTemplate, never()).opsForValue();
        verify(recordMapper, never()).selectList(any());
    }

    @Test
    void missingRedisStateRestoresStockAndClaimedUsers() {
        FreeQualificationActivity activity = activeActivity();
        FreeQualificationRecord record = new FreeQualificationRecord();
        record.setUserId(8L);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(recordMapper.selectList(any())).thenReturn(Collections.singletonList(record));

        boolean initialized = repository.initializeIfMissing(activity);

        assertThat(initialized).isTrue();
        verify(valueOperations).set("free:qualification:stock:3", "4");
        verify(setOperations).add("free:qualification:users:3", "__initialized__");
        verify(setOperations).add("free:qualification:users:3", "8");
        verify(stringRedisTemplate).expire(eq("free:qualification:activity:3"), anyLong(), eq(TimeUnit.SECONDS));
        verify(stringRedisTemplate).expire(eq("free:qualification:stock:3"), anyLong(), eq(TimeUnit.SECONDS));
        verify(stringRedisTemplate).expire(eq("free:qualification:users:3"), anyLong(), eq(TimeUnit.SECONDS));
        verify(lock).unlock();
    }

    @Test
    void repairingMissingUsersDoesNotResetLiveStock() {
        FreeQualificationActivity activity = activeActivity();
        when(stringRedisTemplate.hasKey(anyString()))
                .thenReturn(true, true, false)
                .thenReturn(true, true, false)
                .thenReturn(true, true, false);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(recordMapper.selectList(any())).thenReturn(Collections.emptyList());

        boolean initialized = repository.initializeIfMissing(activity);

        assertThat(initialized).isTrue();
        verify(stringRedisTemplate, never()).opsForValue();
        verify(stringRedisTemplate, never()).opsForHash();
        verify(setOperations).add("free:qualification:users:3", "__initialized__");
    }

    private FreeQualificationActivity activeActivity() {
        FreeQualificationActivity activity = new FreeQualificationActivity();
        activity.setId(3L);
        activity.setStatus(1);
        activity.setRemainingQuota(4);
        activity.setBeginTime(LocalDateTime.now().minusMinutes(1));
        activity.setEndTime(LocalDateTime.now().plusHours(1));
        return activity;
    }
}
