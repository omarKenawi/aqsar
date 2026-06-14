package com.example.aqsar.service;

import com.example.aqsar.repository.ShortUrlRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClickSyncService {

    private final ShortUrlRepository repository;
    private final RedisTemplate<String, String> redisTemplate;

    public ClickSyncService(ShortUrlRepository repository,
                            RedisTemplate<String, String> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }
    @Transactional
    public void flushKey(String key) {

        String shortKey = key.replace("click:", "");
        String value = redisTemplate.opsForValue().get(key);

        if (value != null) {
            long clicks = Long.parseLong(value);

            repository.incrementClickCountBatch(shortKey, clicks);

            redisTemplate.delete(key);
        }
    }
}
