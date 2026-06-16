package com.example.aqsar.service;

import com.example.aqsar.dto.UrlRequestDTO;
import com.example.aqsar.dto.UrlResponseDTO;
import com.example.aqsar.entity.ShortUrl;
import com.example.aqsar.exception.InvalidUrlException;
import com.example.aqsar.mapper.UrlMapper;
import com.example.aqsar.repository.ShortUrlRepository;
import com.example.aqsar.validator.UrlValidator;
import jakarta.transaction.Transactional;
import org.hashids.Hashids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UrlService {

    private final ShortUrlRepository repository;
    private final UrlMapper urlMapper;
    private final Hashids hashids;
    private final UrlCacheService urlCacheService;
    private final UrlValidator urlValidator;
    private static final Logger log = LoggerFactory.getLogger(UrlService.class);

    @Value("${app.base-url}")
    private String baseUrl;


    public UrlService(ShortUrlRepository repository, UrlMapper urlMapper, Hashids hashids, UrlCacheService urlCacheService,UrlValidator urlValidator) {
        this.repository = repository;
        this.urlMapper = urlMapper;
        this.hashids = hashids;
        this.urlCacheService = urlCacheService;
        this.urlValidator = urlValidator;

    }

    public Page<UrlResponseDTO> getAllUrls(int pageNo, int pageSize) {
        pageNo = Math.max(0, pageNo - 1);
        Pageable pageable = PageRequest.of(
                pageNo,
                pageSize,
                Sort.by("id")
        );
        return repository
                .findAll(pageable)
                .map(urlMapper::toShortUrlDTO);
    }

    public String buildShortUrl(String shortKey) {
        return baseUrl + "/" + shortKey;
    }

    @Transactional
    public UrlResponseDTO createShortUrl(UrlRequestDTO urlRequestDTO) {
        if (!urlValidator.isValid(urlRequestDTO.originalUrl())) {
            throw new InvalidUrlException("Invalid URL");
        }
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
                buildShortUrl(shortKey),
                saved.getClickCount(),
                saved.getCreatedAt()
        );
    }


    @Transactional
    public Optional<UrlResponseDTO> accessShortKey(String shortKey) {
        long start = System.nanoTime();


        String cachedUrl = urlCacheService.get(shortKey);

        ShortUrl entity;

        if (cachedUrl != null) {

            // sliding TTL
            urlCacheService.refreshTtl(shortKey);

            System.out.println("HIT CACHE");

            entity = new ShortUrl();
            entity.setShortKey(shortKey);
            entity.setOriginalUrl(cachedUrl);

        } else {

            System.out.println("HIT DB");

            entity = repository.findByShortKey(shortKey)
                    .orElse(null);

            if (entity == null) {
                return Optional.empty();
            }

            // save in cache
            urlCacheService.save(shortKey, entity.getOriginalUrl());
        }

        // increment click
        urlCacheService.incrementClick(shortKey);

        long end = System.nanoTime();
        System.out.println("Time: " + (end - start)/1_000_000 + " ms");
        return Optional.of(urlMapper.toShortUrlDTO(entity));
    }


}