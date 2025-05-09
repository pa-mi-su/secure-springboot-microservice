package com.example.demo.security;

import io.github.bucket4j.*;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class RateLimitFilter implements Filter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        // Allow 3 tokens per minute
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(3, Refill.greedy(3, Duration.ofMinutes(1))))
                .build();
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Apply only to /api/auth/register
        if (!request.getRequestURI().equals("/api/auth/register")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        Bucket bucket = cache.computeIfAbsent(ip, k -> createNewBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429); // Too Many Requests
            response.getWriter().write("Too many registration attempts. Please try again later.");
        }
    }
}
