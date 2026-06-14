package com.example.aqsar.job;

import com.example.aqsar.service.ClickSyncService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

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

        try {
            //  move active → buffer atomically
            redisTemplate.rename(
                    "url_clicks:active",
                    "url_clicks:buffer"
            );

            // ensure active exists for new writes
            redisTemplate.opsForHash()
                    .putIfAbsent("url_clicks:active", "init", "0");

        } catch (Exception e) {
            return;
        }

        // read buffer safely
        Map<Object, Object> clicks =
                redisTemplate.opsForHash()
                        .entries("url_clicks:buffer");

        if (clicks == null || clicks.isEmpty()) return;

        for (Map.Entry<Object, Object> entry : clicks.entrySet()) {
            clickSyncService.flushKey(entry.getKey(), entry.getValue());
        }
        redisTemplate.delete("url_clicks:buffer");
    }


}
