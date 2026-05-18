package com.example.springbootbackeng.ratelimit;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AppRedisRateLimitFilter extends OncePerRequestFilter {

    private final RedisTokenBucketRateLimitService rateLimitService;
    private final MeterRegistry meterRegistry;

    @Value("${app.ratelimit.application.enabled:true}")
    private boolean enabled;

    @Value("${app.ratelimit.application.permits-per-second:100}")
    private int permitsPerSecond;

    @Value("${app.ratelimit.application.burst-capacity:100}")
    private int burstCapacity;

    public AppRedisRateLimitFilter(RedisTokenBucketRateLimitService rateLimitService,
                                   MeterRegistry meterRegistry) {
        this.rateLimitService = rateLimitService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null
                || path.startsWith("/actuator/")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = resolveClientIp(request);
        String key = "app:" + ip;

        boolean allowed = rateLimitService.allow(key, permitsPerSecond, burstCapacity);
        if (!allowed) {
            meterRegistry.counter("app.ratelimit.blocked.total", "layer", "application").increment();
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String[] headerNames = new String[]{"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip.trim())) {
                int commaIdx = ip.indexOf(',');
                return (commaIdx > 0 ? ip.substring(0, commaIdx) : ip).trim();
            }
        }
        return request.getRemoteAddr();
    }
}
