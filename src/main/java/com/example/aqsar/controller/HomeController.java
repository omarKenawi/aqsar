package com.example.aqsar.controller;

import com.example.aqsar.dto.UrlRequestDTO;
import com.example.aqsar.dto.UrlResponseDTO;
import com.example.aqsar.repository.ShortUrlRepository;
import com.example.aqsar.service.RateLimitService;
import com.example.aqsar.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    private final RateLimitService rateLimitService;

    public HomeController(UrlService urlService, RateLimitService rateLimitService) {
        this.urlService = urlService;
        this.rateLimitService = rateLimitService;
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
            RedirectAttributes redirectAttributes
            , HttpServletRequest request
    ) {

        if (bindingResult.hasErrors()) {
            return "home";
        }
        String ip = request.getRemoteAddr();
        System.out.println("IP=> " + ip);
        if (!rateLimitService.isAllowed(ip)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Too many requests. Please try again in a minute."
            );

            return "redirect:/";
        }

        UrlResponseDTO response = urlService.createShortUrl(urlRequestDTO);

        redirectAttributes.addFlashAttribute(
                "successmessage",
                "Short URL created successfully"
        );

        redirectAttributes.addFlashAttribute(
                "url",
                response
        );

        return "redirect:/";
    }

}
