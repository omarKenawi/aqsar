package com.example.aqsar.config;

import org.hashids.Hashids;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HashidsConfig {

    @Bean
    public Hashids hashids() {
        return new Hashids("deliver-more-than-expected", 7);
    }
}