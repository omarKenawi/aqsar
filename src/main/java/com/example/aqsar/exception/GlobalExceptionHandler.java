package com.example.aqsar.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidUrlException.class)
    public String handleInvalidUrl(
            InvalidUrlException ex,
            RedirectAttributes redirectAttributes
    ) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                ex.getMessage()
        );

        return "redirect:/";
    }
}