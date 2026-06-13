package com.example.aqsar.mapper;

import com.example.aqsar.dto.UrlResponseDTO;
import com.example.aqsar.entity.ShortUrl;
import com.example.aqsar.util.UrlBuilder;
import org.springframework.stereotype.Component;

@Component
public class UrlMapper {
    private final UrlBuilder urlBuilder;

    public UrlMapper(UrlBuilder urlBuilder) {
        this.urlBuilder = urlBuilder;
    }


    public UrlResponseDTO toShortUrlDTO(ShortUrl entity) {
        return new UrlResponseDTO(
                entity.getId(),
                entity.getOriginalUrl(),
                entity.getShortKey(),
                urlBuilder.build(entity.getShortKey()),
                entity.getClickCount(),
                entity.getCreatedAt()
        );
    }
}