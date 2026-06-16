package com.example.aqsar.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final int LIMIT = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    public RateLimitService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String ip) {

        String key = "rate_limit:" + ip;

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            System.out.println("count: "+count);

            if (count == 1) {
                redisTemplate.expire(key, WINDOW);
            }

            return count <= LIMIT;

        } catch (Exception e) {
            // fail open → system doesn't break if Redis is down
            return true;
        }
    }
}