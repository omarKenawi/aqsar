package com.example.aqsar.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class RateLimitServiceTest {


    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private RateLimitService rateLimitService;

    @Test
    void givenFirstRequest_whenIsAllowed_thenAllowAndSetTTL() {

        // Given
        String ip = "1.2.3.4";

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rate_limit:" + ip)).thenReturn(1L);

        // When
        boolean result = rateLimitService.isAllowed(ip);

        // Then
        assertTrue(result);

        verify(redisTemplate).expire(
                eq("rate_limit:" + ip),
                any(Duration.class)
        );
    }

    @Test
    void givenRequestsWithinLimit_whenIsAllowed_thenAllow() {

        String ip = "1.2.3.4";

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rate_limit:" + ip)).thenReturn(3L);

        boolean result = rateLimitService.isAllowed(ip);

        assertTrue(result);

        verify(redisTemplate, never()).expire(any(), any());
    }

    @Test
    void givenRequestsExceedLimit_whenIsAllowed_thenDeny() {

        String ip = "1.2.3.4";

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rate_limit:" + ip)).thenReturn(6L);

        boolean result = rateLimitService.isAllowed(ip);

        assertFalse(result);
    }

    @Test
    void givenRedisFailure_whenIsAllowed_thenAllowRequest() {

        String ip = "1.2.3.4";

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString()))
                .thenThrow(new RuntimeException());

        boolean result = rateLimitService.isAllowed(ip);

        assertTrue(result);
    }

}
