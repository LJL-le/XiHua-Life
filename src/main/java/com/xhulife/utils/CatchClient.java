package com.xhulife.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 缓存访问工具：
 * 支持空值缓存、逻辑过期、同步冷启动和异步缓存重建。
 */
@Component
@Slf4j
public class CatchClient {

    private static final int CORE_REBUILD_THREADS = 2;   // 缓存重建核心线程数
    private static final int MAX_REBUILD_THREADS = 4;    // 缓存重建最大线程数
    private static final int REBUILD_QUEUE_CAPACITY = 100; // 等待执行的重建任务上限

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ExecutorService cacheRebuildExecutor;

    public CatchClient(StringRedisTemplate stringRedisTemplate, RedissonClient redissonClient) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
        // 使用有界线程池，避免缓存同时过期时无限创建线程或堆积任务。
        this.cacheRebuildExecutor = new ThreadPoolExecutor(
                CORE_REBUILD_THREADS,
                MAX_REBUILD_THREADS,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(REBUILD_QUEUE_CAPACITY),
                new CacheThreadFactory(),
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 写入带逻辑过期时间的缓存。
     *
     * <p>逻辑过期时间决定何时触发异步重建；物理过期时间决定 Redis 何时真正删除 Key。</p>
     */
    public void setWithLogicalExpire(
            String key,
            Object value,
            Long logicalTime,
            TimeUnit logicalUnit,
            Long physicalTime,
            TimeUnit physicalUnit
    ) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        // RedisData 中保存业务数据和逻辑过期时间。
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(logicalUnit.toSeconds(logicalTime)));
        stringRedisTemplate.opsForValue().set(
                key,
                JSONUtil.toJsonStr(redisData),
                physicalTime,
                physicalUnit
        );
    }

    /**
     * 使用逻辑过期策略查询缓存。
     *
     * <p>处理规则：</p>
     * <ol>
     *     <li>命中空字符串：表示数据库中不存在该数据，直接返回 null；</li>
     *     <li>Redis 中没有 Key：使用 Redisson 锁同步查询数据库并完成冷启动；</li>
     *     <li>缓存未逻辑过期：直接返回缓存数据；</li>
     *     <li>缓存已逻辑过期：立即返回旧数据，同时提交异步重建任务。</li>
     * </ol>
     */
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix,
            String lockPrefix,
            ID id,
            Class<R> type,
            Long logicalTime,
            TimeUnit logicalUnit,
            Long physicalTime,
            TimeUnit physicalUnit,
            Long nullTime,
            TimeUnit nullUnit,
            Function<ID, R> dbFallback
    ) {
        String cacheKey = keyPrefix + id;
        String lockKey = lockPrefix + id;
        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);

        // 空字符串是为不存在的数据设置的占位值，用于防止缓存穿透。
        if (cachedJson != null && cachedJson.isEmpty()) {
            return null;
        }
        // Key 完全不存在时没有旧数据可返回，因此必须同步完成首次缓存构建。
        if (cachedJson == null) {
            return loadSynchronously(
                    cacheKey, lockKey, id, type,
                    logicalTime, logicalUnit, physicalTime, physicalUnit,
                    nullTime, nullUnit, dbFallback
            );
        }

        RedisData cachedData = JSONUtil.toBean(cachedJson, RedisData.class);
        R cachedValue = convert(cachedData.getData(), type);
        LocalDateTime expireTime = cachedData.getExpireTime();
        if (expireTime != null && expireTime.isAfter(LocalDateTime.now())) {
            return cachedValue;
        }

        // 已逻辑过期：不阻塞当前请求，先返回旧值，由后台线程更新缓存。
        submitRebuild(
                cacheKey, lockKey, id, type,
                logicalTime, logicalUnit, physicalTime, physicalUnit,
                nullTime, nullUnit, dbFallback
        );
        return cachedValue;
    }

    /**
     * Redis 无缓存时同步加载数据，保证首次请求也能获得查询结果。
     */
    private <R, ID> R loadSynchronously(
            String cacheKey,
            String lockKey,
            ID id,
            Class<R> type,
            Long logicalTime,
            TimeUnit logicalUnit,
            Long physicalTime,
            TimeUnit physicalUnit,
            Long nullTime,
            TimeUnit nullUnit,
            Function<ID, R> dbFallback
    ) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            // 最多等待 1 秒；不指定租期，由 Redisson 看门狗在任务执行期间自动续期。
            locked = lock.tryLock(1L, TimeUnit.SECONDS);
            if (!locked) {
                // 等锁期间其他线程可能已经完成缓存构建，因此再读取一次。
                String retryJson = stringRedisTemplate.opsForValue().get(cacheKey);
                if (retryJson == null) {
                    throw new IllegalStateException("缓存正在初始化，请稍后重试");
                }
                return retryJson.isEmpty() ? null : parseValue(retryJson, type);
            }

            // 双重检查：获得锁之前，其他实例可能已经完成缓存初始化。
            String latestJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (latestJson != null) {
                return latestJson.isEmpty() ? null : parseValue(latestJson, type);
            }

            R loadedValue = dbFallback.apply(id);
            if (loadedValue == null) {
                // 数据库中也不存在时缓存空值，短时间内阻止无效请求持续访问数据库。
                stringRedisTemplate.opsForValue().set(cacheKey, "", nullTime, nullUnit);
                return null;
            }
            setWithLogicalExpire(
                    cacheKey, loadedValue,
                    logicalTime, logicalUnit,
                    physicalTime, physicalUnit
            );
            return loadedValue;
        } catch (InterruptedException e) {
            // 恢复中断标记，避免上层无法感知线程已被中断。
            Thread.currentThread().interrupt();
            throw new IllegalStateException("缓存初始化被中断", e);
        } finally {
            // 只允许锁的持有线程解锁，避免误释放其他线程刚获得的锁。
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 提交异步缓存重建任务。线程池中的任务会再次竞争分布式锁，
     * 保证多线程、多实例环境下同一个 Key 只有一个线程访问数据库重建缓存。
     */
    private <R, ID> void submitRebuild(
            String cacheKey,
            String lockKey,
            ID id,
            Class<R> type,
            Long logicalTime,
            TimeUnit logicalUnit,
            Long physicalTime,
            TimeUnit physicalUnit,
            Long nullTime,
            TimeUnit nullUnit,
            Function<ID, R> dbFallback
    ) {
        cacheRebuildExecutor.execute(() -> {
            RLock lock = redissonClient.getLock(lockKey);
            boolean locked = lock.tryLock();
            if (!locked) {
                // 已有其他线程负责重建，本任务直接结束。
                return;
            }
            try {
                // 双重检查：排队或竞争锁期间，其他线程可能已经写入了新缓存。
                String latestJson = stringRedisTemplate.opsForValue().get(cacheKey);
                if (StrUtil.isNotBlank(latestJson)) {
                    RedisData latestData = JSONUtil.toBean(latestJson, RedisData.class);
                    LocalDateTime latestExpireTime = latestData.getExpireTime();
                    if (latestExpireTime != null && latestExpireTime.isAfter(LocalDateTime.now())) {
                        return;
                    }
                } else if (latestJson != null) {
                    return;
                }

                R loadedValue = dbFallback.apply(id);
                if (loadedValue == null) {
                    stringRedisTemplate.opsForValue().set(cacheKey, "", nullTime, nullUnit);
                    return;
                }
                setWithLogicalExpire(
                        cacheKey, loadedValue,
                        logicalTime, logicalUnit,
                        physicalTime, physicalUnit
                );
            } catch (Exception e) {
                log.error("Failed to rebuild cache key {}", cacheKey, e);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        });
    }

    private <R> R parseValue(String cachedJson, Class<R> type) {
        RedisData redisData = JSONUtil.toBean(cachedJson, RedisData.class);
        return convert(redisData.getData(), type);
    }

    /**
     * RedisData.data 反序列化后通常是 JSONObject，这里统一转换成调用方需要的实体类型。
     */
    private <R> R convert(Object value, Class<R> type) {
        return value == null ? null : JSONUtil.toBean(JSONUtil.toJsonStr(value), type);
    }

    @PreDestroy
    public void shutdown() {
        // Spring 容器关闭时停止接收新的缓存重建任务。
        cacheRebuildExecutor.shutdown();
    }

    private static class CacheThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "cache-rebuild-" + sequence.incrementAndGet());
            // 守护线程不会阻止 JVM 正常退出。
            thread.setDaemon(true);
            return thread;
        }
    }
}
