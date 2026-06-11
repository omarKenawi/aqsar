package com.example.aqsar.dto;

import java.time.LocalDateTime;

public record UrlResponseDTO(
        Long id,
        String originalUrl,
        String shortUrl,
        Long clickCount,
        LocalDateTime createdAt
) {}