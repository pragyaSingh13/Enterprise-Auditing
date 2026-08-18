package org.enterpriseauditing.enterpriseauditing.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration BLOCK_DURATION =
            Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public boolean isBlocked(String username) {

        String key = getKey(username);

        String attempts =
                redisTemplate.opsForValue().get(key);

        if (attempts == null) {
            return false;
        }

        return Integer.parseInt(attempts) >= MAX_ATTEMPTS;
    }

    public void recordFailedAttempt(String username) {

        String key = getKey(username);

        Long attempts =
                redisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1) {
            redisTemplate.expire(
                    key,
                    BLOCK_DURATION
            );
        }
    }

    public void resetAttempts(String username) {

        redisTemplate.delete(getKey(username));
    }

    private String getKey(String username) {
        return "login:attempts:" + username;
    }
}