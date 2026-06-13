package com.example.aqsar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class AqsarApplication {

	public static void main(String[] args) {
		SpringApplication.run(AqsarApplication.class, args);
	}

}
