package com.example.springbootbackeng.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.springbootbackeng.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;

@Service
public class JwtTokenService {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expire-minutes:120}")
    private long expireMinutes;

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(User user) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + Duration.ofMinutes(expireMinutes).toMillis());

        String roleCode = null;
        if (user.getRole() != null && user.getRole().getRoleCode() != null) {
            roleCode = user.getRole().getRoleCode();
        } else if (user.getRolePermission() != null) {
            roleCode = user.getRolePermission();
        }

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getUserId())
                .claim("roleCode", roleCode)
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(getSignKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parseClaims(token);
            Date exp = claims.getExpiration();
            return exp != null && exp.after(new Date());
        } catch (Exception ignored) {
            return false;
        }
    }

    public long getExpireSeconds() {
        return Duration.ofMinutes(expireMinutes).getSeconds();
    }

    public String resolveToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }

        String queryToken = request.getParameter("access_token");
        if (queryToken != null && !queryToken.trim().isEmpty()) {
            return queryToken.trim();
        }

        return null;
    }
}
