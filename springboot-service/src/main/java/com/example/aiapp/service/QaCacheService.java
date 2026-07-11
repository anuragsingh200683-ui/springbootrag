package com.example.aiapp.service;

import com.example.aiapp.dto.QaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Caches Q&A answers in Redis so repeated questions about the same document
 * don't re-trigger embedding + Claude calls.
 */
@Service
public class QaCacheService {

    private static final Logger log = LoggerFactory.getLogger(QaCacheService.class);
    private static final String KEY_PREFIX = "qa:cache:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final long ttlSeconds;

    public QaCacheService(RedisTemplate<String, Object> redisTemplate,
                           @Value("${app.cache.qa-ttl-seconds}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.ttlSeconds = ttlSeconds;
    }

    public Optional<QaResponse> get(UUID documentId, String question) {
        try {
            Object cached = redisTemplate.opsForValue().get(buildKey(documentId, question));
            if (cached instanceof QaResponse response) {
                return Optional.of(response);
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed, continuing without cache: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public void put(UUID documentId, String question, QaResponse response) {
        try {
            redisTemplate.opsForValue().set(buildKey(documentId, question), response, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("Redis cache write failed, continuing without cache: {}", e.getMessage());
        }
    }

    private String buildKey(UUID documentId, String question) {
        String docPart = documentId == null ? "all" : documentId.toString();
        return KEY_PREFIX + docPart + ":" + sha256(question.trim().toLowerCase());
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
