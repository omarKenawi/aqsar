package com.example.aqsar.mapper;

import com.example.aqsar.dto.UrlResponseDTO;
import com.example.aqsar.entity.ShortUrl;

public class UrlMapper {

    public UrlResponseDTO toShortUrlDTO(ShortUrl entity) {
        return new UrlResponseDTO(
                entity.getId(),
                entity.getOriginalUrl(),
                entity.getShortKey(),
                entity.getClickCount(),
                entity.getCreatedAt()
        );
    }
}