package com.xhulife.service;

import com.xhulife.entity.FreeQualificationRecord;
import com.xhulife.mapper.FreeQualificationActivityMapper;
import com.xhulife.mapper.FreeQualificationRecordMapper;
import com.xhulife.service.impl.FreeQualificationRecordWriterImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreeQualificationRecordWriterTest {

    @Mock
    private FreeQualificationActivityMapper activityMapper;
    @Mock
    private FreeQualificationRecordMapper recordMapper;
    @Mock
    private org.redisson.api.RedissonClient redissonClient;
    @Mock
    private org.redisson.api.RLock lock;
    @InjectMocks
    private FreeQualificationRecordWriterImpl writer;
    @Captor
    private ArgumentCaptor<FreeQualificationRecord> recordCaptor;

    @Test
    void duplicateMessageDoesNotDecrementQuotaAgain() {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(recordMapper.selectCount(any())).thenReturn(1);

        writer.write(100L, 8L, 3L);

        verify(activityMapper, never()).decrementQuotaIfAvailable(3L);
        verify(recordMapper, never()).insert(any());
        verify(lock).unlock();
    }

    @Test
    void newMessageDecrementsQuotaAndSavesRecord() {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(recordMapper.selectCount(any())).thenReturn(0);
        when(activityMapper.decrementQuotaIfAvailable(3L)).thenReturn(1);

        writer.write(100L, 8L, 3L);

        verify(recordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getId()).isEqualTo(100L);
        assertThat(recordCaptor.getValue().getUserId()).isEqualTo(8L);
        assertThat(recordCaptor.getValue().getActivityId()).isEqualTo(3L);
        assertThat(recordCaptor.getValue().getStatus()).isEqualTo(0);
    }
}
