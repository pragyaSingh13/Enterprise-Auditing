package org.enterpriseauditing.enterpriseauditing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    public String acquireLock(
            String lockKey,
            Duration lockDuration) {

        String lockValue = UUID.randomUUID().toString();

        Boolean acquired =
                redisTemplate.opsForValue().setIfAbsent(
                        lockKey,
                        lockValue,
                        lockDuration
                );

        if (Boolean.TRUE.equals(acquired)) {
            return lockValue;
        }

        return null;
    }

    public String acquireLockWithRetry(
            String lockKey,
            Duration lockDuration,
            int maxAttempts,
            long retryDelayMillis) {

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            String lockValue =
                    acquireLock(lockKey, lockDuration);

            if (lockValue != null) {
                return lockValue;
            }

            System.out.println(
                    "Redis lock busy. Retry "
                            + attempt
                            + "/"
                            + maxAttempts
            );

            try {
                Thread.sleep(retryDelayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new RuntimeException(
                        "Interrupted while waiting for Redis lock",
                        e
                );
            }
        }

        return null;
    }

    public void releaseLock(
            String lockKey,
            String lockValue) {

        String currentValue =
                redisTemplate.opsForValue().get(lockKey);

        if (lockValue != null &&
                lockValue.equals(currentValue)) {

            redisTemplate.delete(lockKey);
        }
    }
}