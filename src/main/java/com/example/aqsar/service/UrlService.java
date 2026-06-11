package com.example.aqsar.service;

import com.example.aqsar.dto.UrlRequestDTO;
import com.example.aqsar.dto.UrlResponseDTO;
import com.example.aqsar.entity.ShortUrl;
import com.example.aqsar.mapper.UrlMapper;
import com.example.aqsar.repository.ShortUrlRepository;
import jakarta.transaction.Transactional;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UrlService {

    private final ShortUrlRepository repository;
    private final UrlMapper urlMapper;
    private final Hashids hashids;

    @Value("${app.base-url}")
    private String baseUrl;


    public UrlService(ShortUrlRepository repository, UrlMapper urlMapper, Hashids hashids) {
        this.repository = repository;
        this.urlMapper = urlMapper;
        this.hashids = hashids;
    }

    public List<UrlResponseDTO> getAllUrls() {
        return repository.findAll().stream().map(urlMapper::toShortUrlDTO).toList();
    }

    public String buildShortUrl(String shortKey) {
        return baseUrl + "/" + shortKey;
    }

    @Transactional
    public UrlResponseDTO createShortUrl(UrlRequestDTO urlRequestDTO) {
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl(urlRequestDTO.originalUrl());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setClickCount(0L);
        entity.setShortKey(UUID.randomUUID()
                .toString().substring(0, 7));
        ShortUrl saved = repository.save(entity);
        String shortKey = hashids.encode(saved.getId());
        saved.setShortKey(shortKey);
        return new UrlResponseDTO(
                saved.getId(),
                saved.getOriginalUrl(),
                shortKey,
                saved.getClickCount(),
                saved.getCreatedAt()
        );
    }
}