package com.example.aqsar.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class UrlCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "url:";
    private static final Duration TTL = Duration.ofHours(1);

    public UrlCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // get from cache
    public String get(String shortKey) {
        return redisTemplate.opsForValue().get(PREFIX + shortKey);
    }

    // save with TTL
    public void save(String shortKey, String originalUrl) {
        redisTemplate.opsForValue()
                .set(PREFIX + shortKey, originalUrl, TTL);
    }

    // refresh TTL (sliding behavior)
    public void refreshTtl(String shortKey) {
        redisTemplate.expire(PREFIX + shortKey, TTL);
    }
}