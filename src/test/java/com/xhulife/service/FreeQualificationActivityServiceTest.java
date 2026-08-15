package com.xhulife.service;

import com.xhulife.dto.Result;
import com.xhulife.entity.FreeQualificationActivity;
import com.xhulife.mapper.FreeQualificationActivityMapper;
import com.xhulife.repository.FreeQualificationRedisRepository;
import com.xhulife.service.impl.FreeQualificationActivityServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreeQualificationActivityServiceTest {

    @Mock
    private FreeQualificationActivityMapper activityMapper;
    @Mock
    private FreeQualificationRedisRepository redisRepository;
    @InjectMocks
    private FreeQualificationActivityServiceImpl activityService;
    @Captor
    private ArgumentCaptor<FreeQualificationActivity> activityCaptor;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createActivityInitializesQuotaAndRedisReservationAfterCommit() {
        ReflectionTestUtils.setField(activityService, "baseMapper", activityMapper);
        when(redisRepository.initializeIfMissing(any(FreeQualificationActivity.class))).thenReturn(true);
        doAnswer(invocation -> {
            FreeQualificationActivity activity = invocation.getArgument(0);
            activity.setId(18L);
            return 1;
        }).when(activityMapper).insert(any(FreeQualificationActivity.class));
        TransactionSynchronizationManager.initSynchronization();

        Result result = activityService.createActivity(activeActivity(5));

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(18L);
        verify(activityMapper).insert(activityCaptor.capture());
        assertThat(activityCaptor.getValue().getRemainingQuota()).isEqualTo(5);
        assertThat(activityCaptor.getValue().getStatus()).isEqualTo(1);
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        verify(redisRepository).initializeIfMissing(activityCaptor.getValue());
    }

    private FreeQualificationActivity activeActivity(int totalQuota) {
        FreeQualificationActivity activity = new FreeQualificationActivity();
        activity.setShopId(1L);
        activity.setTitle("Campus coffee free order");
        activity.setTotalQuota(totalQuota);
        activity.setBeginTime(LocalDateTime.now().minusMinutes(1));
        activity.setEndTime(LocalDateTime.now().plusHours(1));
        return activity;
    }
}
