package com.example.aqsar.controller;

import com.example.aqsar.dto.UrlRequestDTO;
import com.example.aqsar.repository.ShortUrlRepository;
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

    final ShortUrlRepository shortUrlRepository;

    @Autowired
    public HomeController(ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("urlRequestDTO", new UrlRequestDTO(""));
        return "home";
    }

    @PostMapping("/shorten")
    public String shortenUrl(
             @ModelAttribute("urlRequestDTO") @Valid UrlRequestDTO urlRequestDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "home";
        }
        redirectAttributes.addFlashAttribute("successmessage","short URl creates successfully");
        String originalUrl = urlRequestDTO.originalUrl();
        return "redirect:/";
    }

}
