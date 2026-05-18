package com.example.springbootbackeng.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodRateLimit {

    /**
     * 限流key前缀（最终会自动拼接IP）
     */
    String key() default "";

    /**
     * 限制次数
     */
    int limit() default 100;

    /**
     * 时间窗口（秒）
     */
    int period() default 60;

    /**
     * 被限流时提示
     */
    String message() default "请求过于频繁，请稍后重试";
}
