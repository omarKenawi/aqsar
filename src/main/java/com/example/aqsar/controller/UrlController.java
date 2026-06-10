package com.example.aqsar.controller;


import com.example.aqsar.service.UrlService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/urls")
    public String getAllUrls(Model model) {
        model.addAttribute("urls", urlService.getAllUrls());
        model.addAttribute("urlService", urlService);
        return "urls";
    }
}
