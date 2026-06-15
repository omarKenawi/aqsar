package com.example.aqsar.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

public class UrlCacheServiceTest {
    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    UrlCacheService urlCacheService;
    @Test
    @DisplayName("Should return cached URL when key exists")
    void givenExistingKey_whenGet_thenReturnValue() {

        // Given
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOps);

        when(valueOps.get("url:abc123"))
                .thenReturn("https://google.com");

        // When
        String result = urlCacheService.get("abc123");

        // Then
        Assertions.assertEquals(
                "https://google.com",
                result
        );
    }
    @Test
    @DisplayName("Should return null when Redis throws exception")
    void givenRedisFailure_whenGet_thenReturnNull() {

        // Given
        when(redisTemplate.opsForValue())
                .thenThrow(new RuntimeException());

        // When
        String result = urlCacheService.get("abc123");

        // Then
        Assertions.assertNull(result);
    }
    @Test
    @DisplayName("Should save URL with TTL")
    void givenUrl_whenSave_thenStoreInRedis() {

        // Given
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOps);

        // When
        urlCacheService.save(
                "abc123",
                "https://google.com"
        );

        // Then
        verify(valueOps).set(
                eq("url:abc123"),
                eq("https://google.com"),
                any(Duration.class)
        );
    }

    @Test
    @DisplayName("Should refresh cache TTL")
    void givenExistingKey_whenRefreshTtl_thenExpireAgain() {

        // When
        urlCacheService.refreshTtl("abc123");

        // Then
        verify(redisTemplate)
                .expire(
                        eq("url:abc123"),
                        any(Duration.class)
                );
    }
    @Test
    @DisplayName("Should increment click counter")
    void givenShortKey_whenIncrementClick_thenIncrementCounter() {

        // Given
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);

        when(redisTemplate.opsForHash())
                .thenReturn(hashOps);

        // When
        urlCacheService.incrementClick("abc123");

        // Then
        verify(hashOps).increment(
                eq("url_clicks:active"),
                eq("abc123"),
                eq(1L)
        );
    }

    @Test
    @DisplayName("Should ignore Redis failure when incrementing click count")
    void givenRedisFailure_whenIncrementClick_thenDoNothing() {

        // Given
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);

        when(redisTemplate.opsForHash())
                .thenReturn(hashOps);

        doThrow(new RuntimeException())
                .when(hashOps)
                .increment(anyString(), anyString(), anyLong());

        // When / Then
        Assertions.assertDoesNotThrow(
                () -> urlCacheService.incrementClick("abc123")
        );
    }
}
