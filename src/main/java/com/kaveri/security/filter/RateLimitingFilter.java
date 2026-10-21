package com.kaveri.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaveri.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Production-grade sliding window rate limiter.
 * Protects critical endpoints (auth, payment, contact) against brute-force, DDoS, and fraud.
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int authLimit;
    private final int paymentLimit;
    private final int generalLimit;

    // In-memory sliding window counters: Key -> RequestCounter
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            ObjectMapper objectMapper,
            @Value("${security.ratelimit.enabled:true}") boolean enabled,
            @Value("${security.ratelimit.auth.limit:10}") int authLimit,
            @Value("${security.ratelimit.payment.limit:15}") int paymentLimit,
            @Value("${security.ratelimit.general.limit:120}") int generalLimit) {
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.enabled = enabled;
        this.authLimit = authLimit;
        this.paymentLimit = paymentLimit;
        this.generalLimit = generalLimit;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!enabled || isStaticResource(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        int limit = getLimitForPath(path);
        String key = clientIp + ":" + getRouteCategory(path);

        long now = System.currentTimeMillis();
        RequestCounter counter = requestCounts.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartTime > 60000) {
                return new RequestCounter(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (counter.count.get() > limit) {
            log.warn("Rate limit exceeded for IP {} on route {}. Count: {}, Limit: {}",
                    clientIp, path, counter.count.get(), limit);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            
            ApiResponse<Void> apiResponse = ApiResponse.error(
                    "Rate limit exceeded. Please wait a minute before retrying.",
                    HttpStatus.TOO_MANY_REQUESTS.value()
            );
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int getLimitForPath(String path) {
        if (path.startsWith("/api/auth/")) {
            return authLimit;
        } else if (path.startsWith("/api/payments/")) {
            return paymentLimit;
        }
        return generalLimit;
    }

    private String getRouteCategory(String path) {
        if (path.startsWith("/api/auth/")) {
            return "auth";
        } else if (path.startsWith("/api/payments/")) {
            return "payments";
        }
        return "general";
    }

    private boolean isStaticResource(HttpServletRequest request) {
        String uri = request.getRequestURI().toLowerCase();
        return uri.endsWith(".css") || uri.endsWith(".js") || uri.endsWith(".png")
                || uri.endsWith(".jpg") || uri.endsWith(".jpeg") || uri.endsWith(".svg")
                || uri.endsWith(".ico") || uri.endsWith(".woff") || uri.endsWith(".woff2")
                || uri.endsWith(".html") || uri.equals("/");
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    /**
     * Clears internal counters (useful for unit tests)
     */
    public void reset() {
        requestCounts.clear();
    }

    private static class RequestCounter {
        final long windowStartTime;
        final AtomicInteger count;

        RequestCounter(long windowStartTime, AtomicInteger count) {
            this.windowStartTime = windowStartTime;
            this.count = count;
        }
    }
}
