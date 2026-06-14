package com.example.aqsar.service;

import com.example.aqsar.repository.ShortUrlRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ClickSyncService {

    private final ShortUrlRepository repository;

    public ClickSyncService(ShortUrlRepository repository) {
        this.repository = repository;
    }
    @Transactional
    public void flushKey(Object shortKeyObj, Object valueObj) {

        String shortKey = shortKeyObj.toString();
        long clicks = Long.parseLong(valueObj.toString());

        repository.incrementClickCountBatch(shortKey, clicks);
    }
}
