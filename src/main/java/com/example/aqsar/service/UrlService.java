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
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UrlService {

    private final ShortUrlRepository repository;
    private final UrlMapper urlMapper;
    private final Hashids hashids;
    private static final Logger log = (Logger) LoggerFactory.getLogger(UrlService.class);

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
        Optional<ShortUrl> shortUrlOptional = repository.findByShortKey(shortKey);
        if (shortUrlOptional.isEmpty())
            return Optional.empty();
        ShortUrl shortUrl = shortUrlOptional.get();
        shortUrl.setClickCount(shortUrl.getClickCount()+1);
        return shortUrlOptional.map(urlMapper::toShortUrlDTO);
    }
}