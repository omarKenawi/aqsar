package com.example.aqsar.repository;

import com.example.aqsar.entity.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortKey(String shortKey);
    @Modifying
    @Query("""
    UPDATE ShortUrl s
    SET s.clickCount = s.clickCount + :clicks
    WHERE s.shortKey = :shortKey
""")
    void incrementClickCountBatch(String shortKey, long clicks);


}