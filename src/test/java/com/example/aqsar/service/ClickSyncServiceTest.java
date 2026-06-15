package com.example.aqsar.service;

import com.example.aqsar.repository.ShortUrlRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)

public class ClickSyncServiceTest {

    @Mock
    ShortUrlRepository repository;

    @InjectMocks
    ClickSyncService clickSyncService;
    @Test
    @DisplayName("Should update DB with correct click count")
    void givenValidInputs_whenFlushKey_thenUpdateDatabase() {

        // Given

        // When
        clickSyncService.flushKey("abc123", "10");

        // Then
        verify(repository)
                .incrementClickCountBatch("abc123", 10L);
    }

    @Test
    @DisplayName("Should throw NumberFormatException for invalid value")
    void givenInvalidNumber_whenFlushKey_thenThrowException() {

        // Then
        Assertions.assertThrows(
                NumberFormatException.class,
                () -> clickSyncService.flushKey("key1", "abc")
        );
    }

    @Test
    @DisplayName("Should throw NullPointerException when inputs are null")
    void givenNullInputs_whenFlushKey_thenThrowException() {

        // Then
        Assertions.assertThrows(
                NullPointerException.class,
                () -> clickSyncService.flushKey(null, null)
        );
    }
}
