package com.example.aqsar.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record UrlRequestDTO(
        @NotBlank(message = "URL is required")
        @URL(message = "Please enter a valid URL")
        String originalUrl
) {}