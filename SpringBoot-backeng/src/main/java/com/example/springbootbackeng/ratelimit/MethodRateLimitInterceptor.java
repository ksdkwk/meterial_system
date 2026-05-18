package com.example.springbootbackeng.ratelimit;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MethodRateLimitInterceptor implements HandlerInterceptor {

    private final LocalMethodRateLimitService localMethodRateLimitService;
    private final MeterRegistry meterRegistry;

    public MethodRateLimitInterceptor(LocalMethodRateLimitService localMethodRateLimitService,
                                      MeterRegistry meterRegistry) {
        this.localMethodRateLimitService = localMethodRateLimitService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        MethodRateLimit methodRateLimit = handlerMethod.getMethodAnnotation(MethodRateLimit.class);
        if (methodRateLimit == null) {
            return true;
        }

        String keyPrefix = methodRateLimit.key();
        if (keyPrefix == null || keyPrefix.trim().isEmpty()) {
            keyPrefix = handlerMethod.getBeanType().getName() + "#" + handlerMethod.getMethod().getName();
        }

        String key = keyPrefix + ":" + resolveClientIp(request);
        boolean allowed = localMethodRateLimitService.allow(key, methodRateLimit.limit(), methodRateLimit.period());
        if (allowed) {
            return true;
        }

        meterRegistry.counter("app.ratelimit.blocked.total", "layer", "method").increment();
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":429,\"msg\":\"" + methodRateLimit.message() + "\",\"data\":null}");
        return false;
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
