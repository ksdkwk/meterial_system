package com.example.springbootbackeng.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springbootbackeng.entity.User;
import com.example.springbootbackeng.mapper.UserMapper;

@Component
public class PasswordOfflineMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordOfflineMigrationRunner.class);
    private static final String MIGRATION_KEY = "PASSWORD_BCRYPT_V1";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${security.password-migration.enabled:false}")
    private boolean migrationEnabled;

    @Value("${security.password-migration.force:false}")
    private boolean migrationForce;

    public PasswordOfflineMigrationRunner(UserMapper userMapper,
                                          PasswordEncoder passwordEncoder,
                                          JdbcTemplate jdbcTemplate) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migratePlainPasswordsToBcrypt() {
        if (!migrationEnabled) {
            log.info("离线密码迁移未启用（security.password-migration.enabled=false），已跳过。");
            return;
        }

        try {
            ensureMarkerTable();
            if (!migrationForce && isMigrationMarkedDone()) {
                log.info("检测到离线密码迁移已完成标记（{}），本次自动跳过。", MIGRATION_KEY);
                log.info("如需重新执行，请临时设置 security.password-migration.force=true。");
                return;
            }

            LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
            query.select(User::getUserId, User::getUsername, User::getPassword)
                    .isNotNull(User::getPassword)
                    .ne(User::getPassword, "");

            List<User> users = userMapper.selectList(query);
            int scanned = 0;
            int migrated = 0;
            int skipped = 0;

            for (User user : users) {
                scanned++;
                String pwd = user.getPassword();
                if (pwd == null || pwd.trim().isEmpty() || isBcryptHash(pwd)) {
                    skipped++;
                    continue;
                }

                User toUpdate = new User();
                toUpdate.setUserId(user.getUserId());
                toUpdate.setPassword(passwordEncoder.encode(pwd.trim()));
                if (userMapper.updateById(toUpdate) > 0) {
                    migrated++;
                }
            }

            log.info("离线密码迁移完成：扫描 {} 条，迁移 {} 条，跳过 {} 条。", scanned, migrated, skipped);
            saveMigrationMarker("DONE", "迁移成功", scanned, migrated, skipped);
            log.info("已写入迁移标记。后续即使 enabled=true 也会自动跳过，无需人工回改配置。");
        } catch (Exception ex) {
            saveMigrationMarker("FAILED", ex.getMessage(), 0, 0, 0);
            log.error("离线密码迁移执行失败：{}", ex.getMessage(), ex);
        }
    }

    private void ensureMarkerTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS tb_security_migration_marker (
                    migration_key VARCHAR(64) PRIMARY KEY,
                    migration_name VARCHAR(128) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    scanned_count INT NOT NULL DEFAULT 0,
                    migrated_count INT NOT NULL DEFAULT 0,
                    skipped_count INT NOT NULL DEFAULT 0,
                    message VARCHAR(512) NULL,
                    migrated_at DATETIME NOT NULL,
                    update_time DATETIME NOT NULL
                )
                """);
    }

    private boolean isMigrationMarkedDone() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM tb_security_migration_marker WHERE migration_key = ? AND status = 'DONE'",
                Integer.class,
                MIGRATION_KEY
        );
        return count != null && count > 0;
    }

    private void saveMigrationMarker(String status, String message, int scanned, int migrated, int skipped) {
        try {
            ensureMarkerTable();
            jdbcTemplate.update("""
                    INSERT INTO tb_security_migration_marker
                    (migration_key, migration_name, status, scanned_count, migrated_count, skipped_count, message, migrated_at, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                    ON DUPLICATE KEY UPDATE
                        migration_name = VALUES(migration_name),
                        status = VALUES(status),
                        scanned_count = VALUES(scanned_count),
                        migrated_count = VALUES(migrated_count),
                        skipped_count = VALUES(skipped_count),
                        message = VALUES(message),
                        migrated_at = VALUES(migrated_at),
                        update_time = VALUES(update_time)
                    """,
                    MIGRATION_KEY,
                    "离线明文密码迁移到BCrypt",
                    status,
                    scanned,
                    migrated,
                    skipped,
                    message == null ? "" : message
            );
        } catch (Exception ex) {
            log.warn("写入迁移标记失败：{}", ex.getMessage());
        }
    }

    private boolean isBcryptHash(String password) {
        if (password == null) {
            return false;
        }
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
