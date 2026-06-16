# Aqsar — URL Shortener Service

> **أقصر** *(Arabic: "shorter")* — A production-oriented URL shortening service built with **Spring Boot + Thymeleaf**, featuring Redis caching, collision-free short keys, click tracking, and abuse protection.

![Java](https://img.shields.io/badge/Java-21-blue?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-6DB33F?style=flat-square&logo=springboot)
![Redis](https://img.shields.io/badge/Redis-Cache-DC382D?style=flat-square&logo=redis)
![MySQL](https://img.shields.io/badge/MySQL-Database-4479A1?style=flat-square&logo=mysql)

---

## 📌 What It Does

| You paste this | You get this |
|---|---|
| `https://example.com/a/very/long/path?with=params` | `http://localhost:8081/k3Xp9mQ` |

Anyone visiting `http://localhost:8081/k3Xp9mQ` is instantly redirected to the original URL. Every redirect is tracked, and the system is designed to handle high traffic without hammering the database.

---

## 🏗️ System Design



<img width="1536" height="1024" alt="6c893a83-7f0f-4ff2-822c-60974bfb0dd7" src="https://github.com/user-attachments/assets/6289084c-2681-4612-b72b-b0a0bae884d8" />

---

## ⚙️ Features

### 🔗 Collision-Free Short Keys with Hashids

Every URL shortened goes through a two-step process that mathematically guarantees no two URLs ever get the same key.
<img width="1350" height="600" alt="hashids-flow" src="https://github.com/user-attachments/assets/6f6bdc98-fc99-4bdb-98de-8398ee7d8424" />

**How it works in code:**

```java
// HashidsConfig.java
@Bean
public Hashids hashids() {
    return new Hashids("deliver-more-than-expected", 7);
}
```

```java
// UrlService.java — createShortUrl()
ShortUrl saved = repository.save(entity);       // → gets AUTO_INCREMENT id (e.g. 42)
String shortKey = hashids.encode(saved.getId()); // → encode(42) = "k3Xp9mQ" (always unique)
saved.setShortKey(shortKey);
```

Since MySQL's `AUTO_INCREMENT` guarantees each row a unique integer, and Hashids is a **bijective function** (one unique input → one unique output, always reversible), collisions are structurally impossible — not just unlikely.

---

### ⚡ Fast Redirection — Cache-Aside Pattern

Every redirect hits Redis first. A cache miss is the only time MySQL is touched.
<img width="1200" height="500" alt="redirect-flow" src="https://github.com/user-attachments/assets/4ce3a913-60e7-4f9f-8ac0-163a445d7369" />
- **Cache key:** `url:{shortKey}` with a **sliding TTL of 1 hour**
- Every cache hit calls `refreshTtl()` — frequently accessed links stay in memory indefinitely
- All Redis failures are caught silently; the system falls back to MySQL automatically

```java
// UrlCacheService.java
public String get(String shortKey) {
    try { return redisTemplate.opsForValue().get("url:" + shortKey); }
    catch (Exception e) { return null; } // graceful fallback to DB
}

public void refreshTtl(String shortKey) {
    redisTemplate.expire("url:" + shortKey, Duration.ofHours(1));
}
```

---

### 📊 Click Tracking — Atomic Rename Strategy

Clicks are never written to MySQL directly on the hot path. They accumulate in Redis and are flushed every 30 seconds using an atomic rename that guarantees **zero click loss**.

<img width="1200" height="500" alt="click-sync-flow" src="https://github.com/user-attachments/assets/d512b60e-6669-4ae7-9ec5-072cf8cf0510" />

```java
// ClickSyncJob.java — runs every 30 seconds
redisTemplate.rename("url_clicks:active", "url_clicks:buffer"); // atomic
redisTemplate.opsForHash().putIfAbsent("url_clicks:active", "init", "0"); // re-init immediately

Map<Object, Object> clicks = redisTemplate.opsForHash().entries("url_clicks:buffer");
for (Map.Entry<Object, Object> entry : clicks.entrySet()) {
    clickSyncService.flushKey(entry.getKey(), entry.getValue()); // batch UPDATE MySQL
}
redisTemplate.delete("url_clicks:buffer");
```

The RENAME is atomic at the Redis level — no click written between the rename and the re-init is ever lost.

---

### 📋 URLs Dashboard

The `/urls` page shows all shortened URLs with their click counts, paginated at **10 rows per page**.

| Feature | Detail |
|---|---|
| Pagination | 10 rows/page, controlled by `app.page-size=10` in `application.properties` |
| Sorting | By `id` ascending |
| Click counts | Reflect MySQL — updated every 30s by the scheduler |

```java
// UrlService.java
public Page<UrlResponseDTO> getAllUrls(int pageNo, int pageSize) {
    pageNo = Math.max(0, pageNo - 1); // 1-based from UI → 0-based for JPA
    Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id"));
    return repository.findAll(pageable).map(urlMapper::toShortUrlDTO);
}
```

---

### 🛡️ Rate Limiting — 5 Requests / Minute / IP

Every call to `POST /shorten` is checked against a Redis counter keyed by the caller's IP.
<img width="1200" height="500" alt="rate-limit-flow" src="https://github.com/user-attachments/assets/4f19c984-2dd2-47a6-aebb-83c07f7200af" />

```java
// RateLimitService.java
public boolean isAllowed(String ip) {
    String key = "rate_limit:" + ip;
    try {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) redisTemplate.expire(key, Duration.ofMinutes(1));
        return count <= 5;
    } catch (Exception e) {
        return true; // fail-open: Redis down → still serve the request
    }
}
```

If Redis is unavailable the `catch` block returns `true`, so rate limiting degrades gracefully without taking the whole service down.

---

### ✅ URL Validation & SSRF Protection

Before any URL is shortened, `UrlValidator` runs a series of checks:

```
1. Scheme must be http or https
2. Host cannot be null or "localhost"
3. IP must not be loopback / link-local / site-local (blocks 127.x, 10.x, 192.168.x, etc.)
4. HTTP HEAD request sent to the target — must respond 2xx or 3xx
```

This prevents SSRF attacks (a user could otherwise use the shortener to probe your internal network).

---

## 🔄 Full Request Flows

### Shorten a URL

```
User fills form → POST /shorten
        │
        ▼
   Rate limit check (Redis INCR)
        ├── exceeded → flash error → redirect /
        └── allowed
              │
              ▼
         UrlValidator.isValid()
              ├── invalid → throw InvalidUrlException
              └── valid
                    │
                    ▼
              Save to MySQL (gets AUTO_INCREMENT id)
                    │
                    ▼
              hashids.encode(id) → shortKey
                    │
                    ▼
              Return UrlResponseDTO → flash success → redirect /
```

### Visit a Short URL

```
GET /{shortKey}
      │
      ▼
 Redis.get("url:{shortKey}")
      ├── HIT  → refreshTtl() → incrementClick() → 302 Redirect
      └── MISS → MySQL.findByShortKey()
                    ├── not found → 404
                    └── found → Redis.save() → incrementClick() → 302 Redirect
```

---

## 🖥️ Pages

| Method | Path | Handler | Description |
|---|---|---|---|
| `GET` | `/` | `HomeController` | Home — URL shortening form |
| `POST` | `/shorten` | `HomeController` | Submit URL, returns flash with result |
| `GET` | `/{shortKey}` | `UrlController` | Redirect to original URL |
| `GET` | `/urls` | `UrlController` | Dashboard — all URLs, paginated |

---

## 🧰 Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Core language |
| Spring Boot | Backend framework |
| Spring MVC + Thymeleaf | Web layer & server-rendered HTML views |
| Spring Data JPA | Database access (Hibernate ORM) |
| MySQL | Persistent storage (`short_urls` table) |
| Redis | URL cache, click counters, rate-limit counters |
| Hashids | Collision-free short key generation |
| JUnit 5 + Mockito | Unit testing |
| Lombok | Boilerplate reduction on entities |
| Maven | Build & dependency management |

---

## 📂 Project Structure

```
src/
├── main/
│   ├── java/com/example/aqsar/
│   │   ├── config/
│   │   │   └── HashidsConfig.java        # Hashids bean (salt + min length)
│   │   ├── controller/
│   │   │   ├── HomeController.java        # GET /, POST /shorten
│   │   │   └── UrlController.java         # GET /{shortKey}, GET /urls
│   │   ├── dto/
│   │   │   ├── UrlRequestDTO.java
│   │   │   └── UrlResponseDTO.java
│   │   ├── entity/
│   │   │   └── ShortUrl.java              # JPA entity → short_urls table
│   │   ├── exception/
│   │   │   ├── InvalidUrlException.java
│   │   │   └── ShortUrlNotFoundException.java
│   │   ├── job/
│   │   │   └── ClickSyncJob.java          # @Scheduled — Redis → MySQL every 30s
│   │   ├── mapper/
│   │   │   └── UrlMapper.java
│   │   ├── repository/
│   │   │   └── ShortUrlRepository.java    # Spring Data JPA
│   │   ├── service/
│   │   │   ├── ClickSyncService.java      # flushKey() — batch DB update
│   │   │   ├── RateLimitService.java      # Redis INCR-based rate limiter
│   │   │   ├── UrlCacheService.java       # Redis get/save/refreshTtl/incrementClick
│   │   │   └── UrlService.java            # Core business logic
│   │   └── validator/
│   │       └── UrlValidator.java          # Scheme + SSRF + reachability checks
│   └── resources/
│       ├── application.properties
│       └── templates/
│           ├── home.html                  # Shortening form
│           └── urls.html                  # Dashboard with pagination
└── test/
    └── java/com/example/aqsar/           # JUnit 5 + Mockito unit tests
```

---

## 🧪 Testing

Unit tests cover all core services using **JUnit 5** and **Mockito**, focusing on business logic, Redis interactions, and edge cases.

| Test Class | What it covers |
|---|---|
| `UrlServiceTest` | Key generation, cache delegation, not-found handling |
| `UrlCacheServiceTest` | Cache hit/miss, TTL refresh, silent failure on Redis error |
| `ClickSyncServiceTest` | Batch flush logic, atomic rename, empty buffer handling |
| `RateLimitServiceTest` | Counter increment, TTL assignment, fail-open on Redis down |

### Test Coverage Report

<img width="1920" height="869" alt="localhost_63342_aqsar_target_site_jacoco_index html__ijt=kog3pmpmft2cnacpoqtdgk1cfn _ij_reload=RELOAD_ON_SAVE - Google Chrome 6_16_2026 6_41_53 AM" src="https://github.com/user-attachments/assets/029bc35b-3918-4a95-8964-535fe807d012" />

---

## 🚀 Running Locally

**Prerequisites:** Java 21, Maven, MySQL 8, Redis

```bash
# 1. Clone
git clone https://github.com/omarKenawi/aqsar.git
cd aqsar

# 2. Create the database
mysql -u root -p -e "CREATE DATABASE aqsar;"

# 3. Configure credentials in src/main/resources/application.properties
#    spring.datasource.username=...
#    spring.datasource.password=...
#    app.base-url=http://localhost:8081

# 4. Run
./mvnw spring-boot:run
```

Open [http://localhost:8081](http://localhost:8081) — the home page will appear.

---

## 📈 Scalability Considerations

The current architecture can be extended with:

- **Distributed Redis Cluster** — for multi-node caching and counter sharding
- **Load balancing** — multiple app instances with a shared Redis and MySQL
- **Sliding-window / token-bucket rate limiting** — finer control than the current fixed-window approach
- **Kafka / RabbitMQ** — move click events off the hot path entirely with async messaging
- **CDN** — cache redirects at the edge for globally fast response times

---

## 🏆 Key Design Decisions

**Hashids over random strings** — Random strings need a uniqueness check (DB round-trip). Hashids encodes the already-unique DB id, so no collision check is ever needed.

**Cache-aside over write-through** — Simpler to reason about; the cache is only populated on demand, reducing memory pressure for rarely-accessed URLs.

**Atomic rename for click sync** — A simple read-then-delete would have a race window where clicks could be lost. The Redis RENAME is atomic, so the drain and the live counter are never in conflict.

**Fail-open everywhere** — Every Redis interaction is wrapped in try-catch. A Redis outage degrades performance (more DB reads, rate limiting disabled) but never takes the service down.

---










