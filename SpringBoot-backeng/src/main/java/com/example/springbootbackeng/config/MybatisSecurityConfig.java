package com.example.springbootbackeng.config;

import java.sql.Connection;
import java.util.List;
import java.util.Locale;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis SQL 安全拦截：阻断高风险非法 SQL（防注入兜底）。
 *
 * 说明：业务 SQL 统一使用 #{...} 预编译占位符，避免拼接注入。
 */
@Configuration
public class MybatisSecurityConfig {

    @Bean
    public Interceptor mybatisSqlInjectionInterceptor() {
        return new MybatisSqlInjectionInterceptor();
    }

    @Intercepts({
            @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
    })
    static class MybatisSqlInjectionInterceptor implements Interceptor {

        private static final List<String> DANGEROUS_PATTERNS = List.of(
                " or 1=1",
                " and 1=1",
                " union select ",
                " information_schema",
                " sleep(",
                " benchmark(",
                " load_file(",
                " outfile",
                " --",
                "/*",
                "*/",
                ";"
        );

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            Object target = invocation.getTarget();
            if (target instanceof StatementHandler statementHandler) {
                BoundSql boundSql = statementHandler.getBoundSql();
                String sql = boundSql == null ? null : boundSql.getSql();
                if (sql != null && isDangerous(sql)) {
                    throw new IllegalArgumentException("检测到疑似SQL注入风险，已拒绝执行");
                }
            }
            return invocation.proceed();
        }

        @Override
        public Object plugin(Object target) {
            return Plugin.wrap(target, this);
        }

        private boolean isDangerous(String sql) {
            String normalized = sql.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            return DANGEROUS_PATTERNS.stream().anyMatch(normalized::contains);
        }
    }
}
