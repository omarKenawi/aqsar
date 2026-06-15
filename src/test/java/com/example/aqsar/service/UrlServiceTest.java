package com.example.aqsar.service;

import com.example.aqsar.dto.UrlRequestDTO;
import com.example.aqsar.dto.UrlResponseDTO;
import com.example.aqsar.entity.ShortUrl;
import com.example.aqsar.exception.InvalidUrlException;
import com.example.aqsar.mapper.UrlMapper;
import com.example.aqsar.repository.ShortUrlRepository;
import org.hashids.Hashids;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UrlServiceTest {
    @Mock
    private ShortUrlRepository repository;

    @Mock
    private UrlMapper urlMapper;

    @Mock
    private Hashids hashids;

    @Mock
    private UrlCacheService urlCacheService;

    @InjectMocks
    private UrlService urlService;

    private static final String SHORT_KEY = "abc123";
    private static final String ORIGINAL_URL = "https://google.com";

    @Test
    @DisplayName("Should return cached URL when cache hit occurs")
    void givenCacheHit_whenAccessShortKey_thenReturnCachedResult() {

        // Given
        when(urlCacheService.get(SHORT_KEY)).thenReturn(ORIGINAL_URL);

        ShortUrl entity = new ShortUrl();
        entity.setShortKey(SHORT_KEY);
        entity.setOriginalUrl(ORIGINAL_URL);

        UrlResponseDTO dto = new UrlResponseDTO(
                1L,
                ORIGINAL_URL,
                SHORT_KEY,
                ORIGINAL_URL,
                0L,
                null
        );

        when(urlMapper.toShortUrlDTO(any())).thenReturn(dto);

        // When
        Optional<UrlResponseDTO> result = urlService.accessShortKey(SHORT_KEY);

        // Then
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(ORIGINAL_URL, result.get().originalUrl());

        verify(urlCacheService).refreshTtl(SHORT_KEY);
        verify(urlCacheService).incrementClick(SHORT_KEY);
    }

    @Test
    @DisplayName("Should fetch from DB when cache miss occurs")
    void givenCacheMiss_whenAccessShortKey_thenFetchFromDatabase() {

        // Given
        when(urlCacheService.get(SHORT_KEY)).thenReturn(null);

        ShortUrl entity = new ShortUrl();
        entity.setShortKey(SHORT_KEY);
        entity.setOriginalUrl(ORIGINAL_URL);

        when(repository.findByShortKey(SHORT_KEY))
                .thenReturn(Optional.of(entity));

        UrlResponseDTO dto = new UrlResponseDTO(
                1L,
                ORIGINAL_URL,
                SHORT_KEY,
                ORIGINAL_URL,
                0L,
                null
        );

        when(urlMapper.toShortUrlDTO(any())).thenReturn(dto);

        // When
        Optional<UrlResponseDTO> result = urlService.accessShortKey(SHORT_KEY);

        // Then
        Assertions.assertTrue(result.isPresent());

        verify(repository).findByShortKey(SHORT_KEY);
        verify(urlCacheService).save(SHORT_KEY, ORIGINAL_URL);
    }

    @Test
    @DisplayName("Should throw exception when URL is invalid")
    void givenInvalidUrl_whenCreateShortUrl_thenThrowException() {

        // Given
        UrlRequestDTO request = new UrlRequestDTO("http://invalid-url");

        // simulate invalid URL
        UrlService spyService = Mockito.spy(urlService);
        doReturn(false).when(spyService).isUrlExist(request);

        // Then
        Assertions.assertThrows(
                InvalidUrlException.class,
                () -> spyService.createShortUrl(request)
        );
    }

    @Test
    @DisplayName("Should return empty when URL is not found in cache and database")
    void givenCacheMissAndDatabaseMiss_whenAccessShortKey_thenReturnEmpty() {

        // Given
        String shortKey = "abc123";

        when(urlCacheService.get(shortKey))
                .thenReturn(null);

        when(repository.findByShortKey(shortKey))
                .thenReturn(Optional.empty());

        // When
        Optional<UrlResponseDTO> result =
                urlService.accessShortKey(shortKey);

        // Then
        Assertions.assertTrue(result.isEmpty());

        verify(repository).findByShortKey(shortKey);

        verify(urlCacheService, never())
                .save(anyString(), anyString());

        verify(urlCacheService, never())
                .incrementClick(anyString());
    }

    @Test
    @DisplayName("Should create short URL successfully when URL is valid")
    void givenValidUrl_whenCreateShortUrl_thenReturnResponseDto() {

        // Given
        UrlRequestDTO request =
                new UrlRequestDTO("https://google.com");

        UrlService spyService = Mockito.spy(urlService);

        doReturn(true)
                .when(spyService)
                .isUrlExist(request);

        ShortUrl savedEntity = new ShortUrl();
        savedEntity.setId(1L);
        savedEntity.setOriginalUrl(request.originalUrl());

        when(repository.save(any(ShortUrl.class)))
                .thenReturn(savedEntity);

        when(hashids.encode(1L))
                .thenReturn("abc123");

        // When
        UrlResponseDTO result =
                spyService.createShortUrl(request);

        // Then
        Assertions.assertNotNull(result);

        Assertions.assertEquals(
                "https://google.com",
                result.originalUrl()
        );

        Assertions.assertEquals(
                "abc123",
                result.shortKey()
        );

        verify(repository).save(any(ShortUrl.class));
    }

    @Test
    @DisplayName("Should return paginated URLs")
    void givenValidPageRequest_whenGetAllUrls_thenReturnPageOfDtos() {

        // Given
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setId(1L);
        shortUrl.setOriginalUrl("https://google.com");

        UrlResponseDTO dto = new UrlResponseDTO(
                1L,
                "https://google.com",
                "abc123",
                "http://localhost:8080/abc123",
                0L,
                null
        );

        Page<ShortUrl> entityPage = new PageImpl<>(
                List.of(shortUrl)
        );

        when(repository.findAll(any(Pageable.class)))
                .thenReturn(entityPage);

        when(urlMapper.toShortUrlDTO(shortUrl))
                .thenReturn(dto);

        // When
        Page<UrlResponseDTO> result =
                urlService.getAllUrls(1, 10);

        // Then
        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals(
                "https://google.com",
                result.getContent().get(0).originalUrl()
        );

        verify(repository).findAll(any(Pageable.class));
    }
    @Test
    @DisplayName("Should convert page number from one based to zero based indexing")
    void givenPageOne_whenGetAllUrls_thenUseZeroBasedPageIndex() {

        // Given
        Page<ShortUrl> page =
                new PageImpl<>(Collections.emptyList());

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        when(repository.findAll(any(Pageable.class)))
                .thenReturn(page);

        // When
        urlService.getAllUrls(1, 10);

        // Then
        verify(repository).findAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        Assertions.assertEquals(0, pageable.getPageNumber());
        Assertions.assertEquals(10, pageable.getPageSize());
    }
    @Test
    @DisplayName("Should use zero when page number is less than one")
    void givenNegativePageNumber_whenGetAllUrls_thenUseZero() {

        // Given
        Page<ShortUrl> page =
                new PageImpl<>(Collections.emptyList());

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        when(repository.findAll(any(Pageable.class)))
                .thenReturn(page);

        // When
        urlService.getAllUrls(-5, 10);

        // Then
        verify(repository).findAll(pageableCaptor.capture());

        Assertions.assertEquals(
                0,
                pageableCaptor.getValue().getPageNumber()
        );
    }
}
