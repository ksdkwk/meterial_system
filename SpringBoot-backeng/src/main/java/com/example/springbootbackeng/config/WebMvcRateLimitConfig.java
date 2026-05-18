package com.example.springbootbackeng.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.springbootbackeng.ratelimit.MethodRateLimitInterceptor;

@Configuration
public class WebMvcRateLimitConfig implements WebMvcConfigurer {

    private final MethodRateLimitInterceptor methodRateLimitInterceptor;

    public WebMvcRateLimitConfig(MethodRateLimitInterceptor methodRateLimitInterceptor) {
        this.methodRateLimitInterceptor = methodRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(methodRateLimitInterceptor).addPathPatterns("/**");
    }
}
