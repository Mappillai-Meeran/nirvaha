package com.nirvaha.nirvaha_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    @Value("${nirvaha.rate-limit:60}")
    private int maxRequests;

    @Value("${nirvaha.rate-limit-window:60}")
    private int windowSeconds;

    @Value("${nirvaha.recovery.rate-limit:5}")
    private int recoveryMaxRequests;

    @Value("${nirvaha.recovery.rate-limit-window:60}")
    private int recoveryWindowSeconds;

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (isPasswordRecoveryEndpoint(path, method)) {
            cleanupStaleBucketsIfNecessary();
            String clientKey = buildClientKey(request, path);
            TokenBucket bucket = buckets.computeIfAbsent(clientKey, k -> new TokenBucket(recoveryMaxRequests, recoveryWindowSeconds));

            if (!bucket.tryConsume()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", String.valueOf(recoveryWindowSeconds));

                String jsonResponse = String.format(
                        "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded for password recovery. Please try again later.\",\"path\":\"%s\"}",
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        path
                );

                response.getWriter().write(jsonResponse);
                return;
            }
        } else if (isHighRiskEndpoint(path, method)) {
            cleanupStaleBucketsIfNecessary();
            String clientKey = buildClientKey(request, path);
            TokenBucket bucket = buckets.computeIfAbsent(clientKey, k -> new TokenBucket(maxRequests, windowSeconds));

            if (!bucket.tryConsume()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", String.valueOf(windowSeconds));

                String jsonResponse = String.format(
                        "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\",\"path\":\"%s\"}",
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        path
                );

                response.getWriter().write(jsonResponse);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    public void clearBuckets() {
        buckets.clear();
    }

    private void cleanupStaleBucketsIfNecessary() {
        if (buckets.size() > 1000) {
            long now = System.currentTimeMillis();
            long maxAgeMillis = windowSeconds * 2000L;
            buckets.entrySet().removeIf(entry -> (now - entry.getValue().getLastRefillTimestamp()) > maxAgeMillis);
        }
    }

    private boolean isPasswordRecoveryEndpoint(String path, String method) {
        if ("POST".equalsIgnoreCase(method)) {
            return path.equals("/api/auth/forgot-password") ||
                   path.equals("/api/auth/reset-password");
        }
        return false;
    }

    private boolean isHighRiskEndpoint(String path, String method) {
        if ("POST".equalsIgnoreCase(method)) {
            return path.equals("/api/auth/login") ||
                   path.equals("/api/users") ||
                   path.equals("/api/incidents") ||
                   path.startsWith("/api/evidence") ||
                   path.contains("/resolutions");
        }
        return false;
    }

    private String buildClientKey(HttpServletRequest request, String path) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identity;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            identity = auth.getName();
        } else {
            identity = getClientIp(request);
        }
        return identity + ":" + path;
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static class TokenBucket {
        private final int capacity;
        private final long windowMillis;
        private int tokens;
        private long lastRefillTimestamp;

        public TokenBucket(int capacity, int windowSeconds) {
            this.capacity = capacity;
            this.windowMillis = windowSeconds * 1000L;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        public synchronized long getLastRefillTimestamp() {
            return lastRefillTimestamp;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            if (now - lastRefillTimestamp >= windowMillis) {
                tokens = capacity;
                lastRefillTimestamp = now;
            }
        }
    }
}
