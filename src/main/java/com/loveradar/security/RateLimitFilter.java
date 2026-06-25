package com.loveradar.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loveradar.exception.ApiError;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Token-bucket rate limiter applied per client IP.
 *
 * ISSUE #5 FIX: The previous version used Bucket4j 6.x / 7.x API:
 *   Bandwidth.classic(capacity, Refill.greedy(...))
 *   Refill.greedy(...)
 * These classes/methods were REMOVED in bucket4j 8.x.
 * The correct 8.x API uses Bandwidth.builder():
 *   Bandwidth.builder().capacity(n).refillGreedy(n, Duration).build()
 * The Refill import is no longer needed and has been removed.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final int capacity;
    private final int refillTokens;
    private final long refillDurationSeconds;

    public RateLimitFilter(
            @Value("${loveradar.rate-limit.capacity}")               int capacity,
            @Value("${loveradar.rate-limit.refill-tokens}")          int refillTokens,
            @Value("${loveradar.rate-limit.refill-duration-seconds}") long refillDurationSeconds) {
        this.capacity             = capacity;
        this.refillTokens         = refillTokens;
        this.refillDurationSeconds = refillDurationSeconds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        // Only rate-limit sensitive endpoints
        if (!path.startsWith("/api/auth") && !path.startsWith("/api/location")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);
        Bucket bucket    = buckets.computeIfAbsent(clientKey, k -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiError error = ApiError.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error("Too Many Requests")
                    .message("Rate limit exceeded. Please slow down and try again shortly.")
                    .path(path)
                    .build();
            response.getWriter().write(objectMapper.writeValueAsString(error));
        }
    }

    private Bucket newBucket() {
        // Bucket4j 8.x API: Bandwidth.builder() replaces Bandwidth.classic()
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, Duration.ofSeconds(refillDurationSeconds))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
