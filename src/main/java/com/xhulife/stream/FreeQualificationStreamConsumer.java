package com.xhulife.stream;

import com.xhulife.service.FreeQualificationRecordWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.xhulife.utils.RedisConstants.FREE_QUALIFICATION_STREAM_KEY;

@Component
public class FreeQualificationStreamConsumer {

    private static final String GROUP = "g1";
    private static final String CONSUMER = "free-qualification-" + UUID.randomUUID().toString().substring(0, 8);

    private static final Logger log = LoggerFactory.getLogger(FreeQualificationStreamConsumer.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private FreeQualificationRecordWriter recordWriter;

    @PostConstruct
    public void start() {
        ensureGroup();
        executor.submit(this::consume);
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
    }

    private boolean ensureGroup() {
        RecordId bootstrapId = null;
        try {
            bootstrapId = stringRedisTemplate.opsForStream()
                    .add(FREE_QUALIFICATION_STREAM_KEY, Collections.singletonMap("bootstrap", "1"));
            stringRedisTemplate.opsForStream().createGroup(
                    FREE_QUALIFICATION_STREAM_KEY,
                    ReadOffset.from("0"),
                    GROUP
            );
            log.info("Created free qualification stream consumer group. group={}", GROUP);
            return true;
        } catch (Exception e) {
            if (containsMessage(e, "BUSYGROUP")) {
                return true;
            }
            log.warn("Failed to ensure free qualification stream consumer group. group={}", GROUP, e);
            return false;
        } finally {
            if (bootstrapId != null) {
                try {
                    stringRedisTemplate.opsForStream().delete(FREE_QUALIFICATION_STREAM_KEY, bootstrapId);
                } catch (Exception e) {
                    log.debug("Failed to delete stream bootstrap record. recordId={}", bootstrapId, e);
                }
            }
        }
    }

    private void consume() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                        Consumer.from(GROUP, CONSUMER),
                        StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                        StreamOffset.create(FREE_QUALIFICATION_STREAM_KEY, ReadOffset.lastConsumed())
                );
                if (records == null || records.isEmpty()) {
                    handlePending();
                    continue;
                }
                for (MapRecord<String, Object, Object> record : records) {
                    handle(record);
                }
            } catch (Exception e) {
                if (containsMessage(e, "NOGROUP")) {
                    ensureGroup();
                    continue;
                }
                log.warn("Failed to read new free qualification stream messages; trying pending messages.", e);
                handlePending();
            }
        }
    }

    private void handlePending() {
        try {
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                    Consumer.from(GROUP, CONSUMER),
                    StreamReadOptions.empty().count(1),
                    StreamOffset.create(FREE_QUALIFICATION_STREAM_KEY, ReadOffset.from("0"))
            );
            if (records == null || records.isEmpty()) {
                return;
            }
            for (MapRecord<String, Object, Object> record : records) {
                handle(record);
            }
        } catch (Exception e) {
            if (containsMessage(e, "NOGROUP")) {
                ensureGroup();
                return;
            }
            log.warn("Failed to read pending free qualification stream messages; next poll will retry.", e);
        }
    }

    private void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> values = record.getValue();
        Long recordId = Long.valueOf(String.valueOf(values.get("recordId")));
        Long userId = Long.valueOf(String.valueOf(values.get("userId")));
        Long activityId = Long.valueOf(String.valueOf(values.get("activityId")));
        try {
            recordWriter.write(recordId, userId, activityId);
            stringRedisTemplate.opsForStream().acknowledge(FREE_QUALIFICATION_STREAM_KEY, GROUP, record.getId());
        } catch (Exception e) {
            log.error("Failed to persist qualification record; message remains pending. messageId={}, recordId={}, userId={}, activityId={}",
                    record.getId(), recordId, userId, activityId, e);
        }
    }

    private boolean containsMessage(Throwable error, String expected) {
        Throwable current = error;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(expected)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
