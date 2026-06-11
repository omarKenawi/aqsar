package com.example.aqsar.controller;

import com.example.aqsar.dto.UrlRequestDTO;
import com.example.aqsar.dto.UrlResponseDTO;
import com.example.aqsar.repository.ShortUrlRepository;
import com.example.aqsar.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class HomeController {


    private final UrlService urlService;

    public HomeController(UrlService urlService) {
        this.urlService = urlService;
    }


    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("urlRequestDTO", new UrlRequestDTO(""));
        return "home";
    }

    @PostMapping("/shorten")
    public String shortenUrl(
            @Valid @ModelAttribute("urlRequestDTO") UrlRequestDTO urlRequestDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "home";
        }

        UrlResponseDTO response = urlService.createShortUrl(urlRequestDTO);

        redirectAttributes.addFlashAttribute(
                "successmessage",
                "Short URL created successfully"
        );

        redirectAttributes.addFlashAttribute(
                "shortUrl",
                response.shortKey()
        );

        return "redirect:/";
    }

}
