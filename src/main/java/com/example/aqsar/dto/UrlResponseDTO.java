package com.example.aqsar.dto;

import java.time.LocalDateTime;

public record UrlResponseDTO(
        Long id,
        String originalUrl,
        String shortKey,
        String fullShortUrl,
        Long clickCount,
        LocalDateTime createdAt
) {}