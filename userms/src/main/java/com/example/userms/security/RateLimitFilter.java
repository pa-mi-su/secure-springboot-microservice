package com.example.userms.security;

import com.example.userms.config.RateLimitProperties;
import io.github.bucket4j.*;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final RateLimitProperties properties;

    @Autowired
    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    private Bucket createNewBucket() {
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(
                        properties.getCapacity(),
                        Refill.greedy(properties.getCapacity(), Duration.ofMinutes(properties.getDurationMinutes()))
                ))
                .build();
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (!request.getRequestURI().equals("/api/auth/register")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        Bucket bucket = cache.computeIfAbsent(ip, k -> createNewBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        // Set headers even if request is blocked
        response.setHeader("X-Rate-Limit-Limit", String.valueOf(properties.getCapacity()));
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(Math.max(0, probe.getRemainingTokens())));

        if (probe.isConsumed()) {
            chain.doFilter(request, response);
        } else {
            long waitForRefillSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setHeader("X-Rate-Limit-Reset", String.valueOf(waitForRefillSeconds));
            response.setStatus(429);
            response.getWriter().write("Too many registration attempts. Please try again later.");
        }
    }
}
