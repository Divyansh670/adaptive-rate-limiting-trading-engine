package com.tradingengine.ratelimit;

import com.tradingengine.metrics.MetricsTracker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final MetricsTracker metricsTracker;

    public RateLimitFilter(RateLimitService rateLimitService, MetricsTracker metricsTracker) {
        this.rateLimitService = rateLimitService;
        this.metricsTracker = metricsTracker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/orders")) {
            chain.doFilter(request, response);
            return;
        }

        String clientId = request.getHeader("X-Client-Id");
        String tierHeader = request.getHeader("X-Client-Tier");

        if (clientId == null || clientId.isBlank()) {
            clientId = "anonymous";
        }

        ClientTier tier;
        try {
            tier = tierHeader == null ? ClientTier.RETAIL : ClientTier.valueOf(tierHeader.toUpperCase());
        } catch (IllegalArgumentException e) {
            tier = ClientTier.RETAIL;
        }

        boolean allowed = rateLimitService.isAllowed(tier, clientId);

        if (!allowed) {
            metricsTracker.recordRateLimitRejection();
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"tier\":\"" + tier + "\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}