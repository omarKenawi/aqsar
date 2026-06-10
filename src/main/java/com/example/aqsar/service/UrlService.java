package com.example.aqsar.service;

import com.example.aqsar.entity.ShortUrl;
import com.example.aqsar.repository.ShortUrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UrlService {

    private final ShortUrlRepository repository;
    @Value("${app.base-url}")
    private String baseUrl;

    public UrlService(ShortUrlRepository repository) {
        this.repository = repository;
    }

    public List<ShortUrl> getAllUrls() {
        return repository.findAll();
    }
    public String buildShortUrl(String shortKey) {
        return baseUrl + "/" + shortKey;
    }
}