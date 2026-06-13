package com.example.aqsar.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UrlBuilder {

    @Value("${app.base-url}")
    private String baseUrl;

    public String build(String shortKey) {
        return baseUrl + "/" + shortKey;
    }
}