package com.example.aqsar.service;

import com.example.aqsar.dto.UrlRequestDTO;
import com.example.aqsar.dto.UrlResponseDTO;
import com.example.aqsar.entity.ShortUrl;
import com.example.aqsar.exception.InvalidUrlException;
import com.example.aqsar.mapper.UrlMapper;
import com.example.aqsar.repository.ShortUrlRepository;
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
    private static final Logger log = LoggerFactory.getLogger(UrlService.class);

    @Value("${app.base-url}")
    private String baseUrl;


    public UrlService(ShortUrlRepository repository, UrlMapper urlMapper, Hashids hashids, UrlCacheService urlCacheService) {
        this.repository = repository;
        this.urlMapper = urlMapper;
        this.hashids = hashids;
        this.urlCacheService = urlCacheService;
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
        if (!isUrlExist(urlRequestDTO)) {
            throw new InvalidUrlException("Url does not exist");
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

    public boolean isUrlExist(UrlRequestDTO urlRequestDTO) {

        String url = urlRequestDTO.originalUrl();

        try {

            URI uri = URI.create(url);

            // Allow only http/https
            String scheme = uri.getScheme();
            if (scheme == null ||
                    (!scheme.equalsIgnoreCase("http")
                            && !scheme.equalsIgnoreCase("https"))) {

                log.warn("Unsupported scheme: {}", url);
                return false;
            }

            String host = uri.getHost();

            if (host == null) {
                return false;
            }

            // Block localhost
            if (host.equalsIgnoreCase("localhost")) {
                log.warn("Blocked localhost URL: {}", url);
                return false;
            }

            // Resolve IP
            InetAddress address = InetAddress.getByName(host);

            // Block private/internal networks
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isSiteLocalAddress()) {

                log.warn("Blocked private IP URL: {}", url);
                return false;
            }

            HttpURLConnection connection =
                    (HttpURLConnection) uri.toURL().openConnection();

            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(true);

            int statusCode = connection.getResponseCode();

            log.debug("URL {} responded with status {}", url, statusCode);

            return statusCode >= 200 && statusCode < 400;

        } catch (Exception e) {

            log.warn("URL validation failed for {}", url, e);

            return false;
        }
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