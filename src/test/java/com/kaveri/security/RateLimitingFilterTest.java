package com.kaveri.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaveri.security.filter.RateLimitingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private ObjectMapper objectMapper;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        filter = new RateLimitingFilter(objectMapper, true, 3, 5, 10);
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("Should allow requests within rate limit")
    void shouldAllowRequestsWithinLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should reject requests exceeding rate limit with HTTP 429")
    void shouldRejectWhenLimitExceeded() throws ServletException, IOException {
        String ip = "192.168.1.101";

        // Auth limit is 3
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr(ip);
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, filterChain);
            assertEquals(200, resp.getStatus());
        }

        // 4th request should fail with 429
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedReq.setRemoteAddr(ip);
        MockHttpServletResponse blockedResp = new MockHttpServletResponse();

        filter.doFilter(blockedReq, blockedResp, filterChain);

        assertEquals(429, blockedResp.getStatus());
        assertEquals("60", blockedResp.getHeader("Retry-After"));
        assertTrue(blockedResp.getContentAsString().contains("Rate limit exceeded"));
    }

    @Test
    @DisplayName("Should bypass rate limiting for static assets")
    void shouldBypassStaticAssets() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/images/menu/burger.jpg");
        req.setRemoteAddr("192.168.1.102");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        // Send 20 requests
        for (int i = 0; i < 20; i++) {
            filter.doFilter(req, resp, filterChain);
        }

        assertEquals(200, resp.getStatus());
        verify(filterChain, times(20)).doFilter(req, resp);
    }
}
