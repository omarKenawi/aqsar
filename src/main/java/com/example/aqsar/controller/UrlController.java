package com.example.aqsar.controller;


import com.example.aqsar.dto.UrlResponseDTO;
import com.example.aqsar.exception.ShortUrlNotFoundException;
import com.example.aqsar.service.UrlService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @Value("${app.page-size}")
    private int pageSize;

    @GetMapping("/urls")
    public String getAllUrls(
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        Page<UrlResponseDTO> urlsPage = urlService.getAllUrls(page, pageSize);
        model.addAttribute("urls", urlsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", urlsPage.getTotalPages());
        return "urls";
    }

    @GetMapping("/{shortKey}")
    String redirectToOriginalUrl(@PathVariable String shortKey) {
        Optional<UrlResponseDTO> shortUrlResponseDTOOptional = urlService.accessShortKey(shortKey);
        if (shortUrlResponseDTOOptional.isEmpty()) {
            throw new ShortUrlNotFoundException("invalid short key: " + shortKey);
        }
        UrlResponseDTO urlResponseDTO = shortUrlResponseDTOOptional.get();

        return "redirect:" + urlResponseDTO.originalUrl();

    }
}
