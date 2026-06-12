package com.example.aqsar.controller;


import com.example.aqsar.dto.UrlResponseDTO;
import com.example.aqsar.service.UrlService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

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
    @GetMapping("/{shortKey}")
    String redirectToOriginalUrl(@PathVariable String shortKey){
        Optional<UrlResponseDTO> shortUrlResponseDTOOptional= urlService.accessShortKey(shortKey);
        if (shortUrlResponseDTOOptional.isEmpty()){
            throw new RuntimeException("invalid short key: "+shortKey);
        }
      UrlResponseDTO urlResponseDTO=  shortUrlResponseDTOOptional.get();

        return "redirect:"+urlResponseDTO.originalUrl();

    }
}
