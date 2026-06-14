package com.example.aqsar.job;

import com.example.aqsar.service.ClickSyncService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ClickSyncJob {
    private final RedisTemplate<String, String> redisTemplate;
    private final ClickSyncService clickSyncService;

    public ClickSyncJob(RedisTemplate<String, String> redisTemplate, ClickSyncService clickSyncService) {
        this.redisTemplate = redisTemplate;
        this.clickSyncService = clickSyncService;
    }

    @Scheduled(fixedRate = 30000)
    public void syncClicks() {

        Set<String> keys = redisTemplate.keys("click:*");

        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            clickSyncService.flushKey(key);
        }
    }


}
