package com.example.springbootbackeng.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CasbinAuthorizationFilter extends OncePerRequestFilter {

    private final CasbinAuthorizationService casbinAuthorizationService;

    @Value("${app.authz.casbin.enabled:true}")
    private boolean enabled;

    public CasbinAuthorizationFilter(CasbinAuthorizationService casbinAuthorizationService) {
        this.casbinAuthorizationService = casbinAuthorizationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!enabled || isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = null;
        String roleCode = null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser au) {
            userId = au.getUserId();
            roleCode = au.getRoleCode();
        }

        if (roleCode == null || roleCode.isBlank()) {
            roleCode = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("")
                    .replace("ROLE_", "");
        }

        if (userId == null || userId.isBlank()) {
            userId = request.getHeader("X-User-Id");
        }
        if (roleCode == null || roleCode.isBlank()) {
            roleCode = request.getHeader("X-Role-Code");
        }

        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = resolveClientIp(request);

        String normalizedRole = roleCode == null ? "" : roleCode.trim().toUpperCase();
        if (isWarehouseRole(normalizedRole) && isWarehouseFeaturePath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean allowed = casbinAuthorizationService.enforce(userId, roleCode, path, method, ip);
        if (!allowed) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":403,\"msg\":\"权限不足（Casbin）\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path == null
                || path.startsWith("/login/")
                || path.equals("/login")
                || path.equals("/error")
                || path.startsWith("/actuator/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/.well-known/");
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

    private boolean isWarehouseRole(String roleCode) {
        return "WAREHOUSE".equals(roleCode)
                || "WAREHOUSE_ADMIN".equals(roleCode)
                || "USER_WAREHOUSE".equals(roleCode)
                || "ROLE_WAREHOUSE".equals(roleCode)
                || "ROLE_WAREHOUSE_ADMIN".equals(roleCode)
                || "ROLE_USER_WAREHOUSE".equals(roleCode)
                || "仓库管理员".equals(roleCode);
    }

    private boolean isWarehouseFeaturePath(String path) {
        if (path == null) return false;
        return path.startsWith("/materials")
                || path.startsWith("/transport-records")
                || path.startsWith("/teams")
                || path.startsWith("/suppliers")
                || path.startsWith("/vehicles")
                || path.startsWith("/ai-analysis/inventory")
                || path.startsWith("/ai-recognition-logs")
                || path.startsWith("/inventories")
                || path.startsWith("/inventory-logs")
                || path.startsWith("/allocation-plans");
    }
}
