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
    public void flushKey(Object shortKeyObj, Object valueObj) {

        String shortKey = shortKeyObj.toString();
        long clicks = Long.parseLong(valueObj.toString());

        repository.incrementClickCountBatch(shortKey, clicks);

        redisTemplate.opsForHash()
                .delete("url_clicks", shortKey);
    }
}
