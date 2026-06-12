package com.example.aqsar.exception;

public class ShortUrlNotFoundException extends RuntimeException {

    public ShortUrlNotFoundException(String message) {
        super(message);
    }
}
