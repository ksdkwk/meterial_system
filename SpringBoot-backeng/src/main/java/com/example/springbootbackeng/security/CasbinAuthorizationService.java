package com.example.springbootbackeng.security;

import java.nio.file.Path;

import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.persist.file_adapter.FileAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CasbinAuthorizationService {

    private final Enforcer enforcer;

    public CasbinAuthorizationService(
            @Value("${app.authz.casbin.model:casbin-model.conf}") String model,
            @Value("${app.authz.casbin.policy:casbin-policy.csv}") String policy) {
        String modelPath = resolveResourcePath(model);
        String policyPath = resolveResourcePath(policy);
        this.enforcer = new Enforcer(modelPath, new FileAdapter(policyPath));
        this.enforcer.loadPolicy();
        this.enforcer.enableAutoSave(false);
    }

    public boolean enforce(String userId, String roleCode, String requestPath, String method, String clientIp) {
        String normalizedRole = normalizeRole(roleCode);
        String ip = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
        return enforcer.enforce(userId == null ? "anonymous" : userId, normalizedRole, requestPath, method, ip);
    }

    private String normalizeRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return "ANONYMOUS";
        }
        String r = roleCode.trim().toUpperCase();
        return switch (r) {
            case "ROLE_MANAGER", "ROLE_ADMIN", "ROLE_SYSTEM_ADMIN", "ROLE_SUPER_ADMIN" -> "MANAGER";
            case "ROLE_WAREHOUSE", "ROLE_WAREHOUSE_ADMIN", "ROLE_USER_WAREHOUSE" -> "WAREHOUSE";
            case "ROLE_WORKER", "ROLE_USER_WORKER" -> "WORKER";
            case "USER_ADMIN" -> "MANAGER";
            case "ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN", "管理员", "系统管理员", "超级管理员" -> "MANAGER";
            case "WAREHOUSE_ADMIN" -> "WAREHOUSE";
            case "USER_WAREHOUSE" -> "WAREHOUSE";
            case "仓库管理员" -> "WAREHOUSE";
            case "USER_WORKER" -> "WORKER";
            case "施工人员" -> "WORKER";
            default -> r;
        };
    }

    private String resolveResourcePath(String name) {
        try {
            var url = Thread.currentThread().getContextClassLoader().getResource(name);
            if (url == null) {
                throw new IllegalStateException("Casbin resource not found: " + name);
            }
            return Path.of(url.toURI()).toString();
        } catch (RuntimeException | java.net.URISyntaxException ex) {
            throw new IllegalStateException("Load casbin resource failed: " + name, ex);
        }
    }
}
