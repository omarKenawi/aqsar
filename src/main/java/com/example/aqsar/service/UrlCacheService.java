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
        try {
            return redisTemplate.opsForValue().get(PREFIX + shortKey);
        } catch (Exception e) {
            return null;
        }

    }

    // save with TTL
    public void save(String shortKey, String originalUrl) {
        try {
            redisTemplate.opsForValue()
                    .set(PREFIX + shortKey, originalUrl, TTL);
        } catch (Exception e) {
            //ignore
        }

    }

    // refresh TTL (sliding behavior)
    public void refreshTtl(String shortKey) {
        try {
            redisTemplate.expire(PREFIX + shortKey, TTL);
        } catch (Exception e) {
            //ignore
        }

    }

    public void incrementClick(String shortKey) {
        try {
            redisTemplate.opsForValue()
                    .increment("click:" + shortKey);
        } catch (Exception e) {
            // ignore
        }
    }

}