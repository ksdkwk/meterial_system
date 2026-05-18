package com.example.springbootbackeng.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseTriggerFixRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseTriggerFixRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseTriggerFixRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void fixAfterInsertMaterialDemandTriggerIfNeeded() {
        try {
            String sql = """
                    SELECT ACTION_STATEMENT
                    FROM INFORMATION_SCHEMA.TRIGGERS
                    WHERE TRIGGER_SCHEMA = DATABASE()
                      AND TRIGGER_NAME = 'after_insert_tb_material_demand'
                    LIMIT 1
                    """;

            String actionStatement = jdbcTemplate.query(sql, rs -> rs.next() ? rs.getString(1) : null);
            if (actionStatement == null) {
                return;
            }

            if (!actionStatement.contains("user_name")) {
                return;
            }

            log.warn("检测到触发器 after_insert_tb_material_demand 使用了不存在字段 user_name，开始自动修复...");

            jdbcTemplate.execute("DROP TRIGGER IF EXISTS after_insert_tb_material_demand");

            jdbcTemplate.execute("""
                    CREATE TRIGGER after_insert_tb_material_demand
                    AFTER INSERT ON tb_material_demand
                    FOR EACH ROW
                    BEGIN
                        DECLARE v_review_id VARCHAR(20);
                        DECLARE v_apply_name VARCHAR(100);
                        DECLARE v_approve_id VARCHAR(32);
                        DECLARE v_approve_name VARCHAR(100);

                        SELECT real_name INTO v_apply_name FROM tb_user WHERE user_id = NEW.demand_user_id LIMIT 1;

                        SET v_approve_id = 'system';
                        SET v_approve_name = '系统';

                        CALL generate_review_id(v_review_id);

                        INSERT INTO tb_review_log (
                            review_id,
                            review_demand_id,
                            review_apply_id,
                            review_apply_name,
                            review_approve_id,
                            review_approve_name,
                            review_state,
                            review_result,
                            review_reason
                        ) VALUES (
                            v_review_id,
                            NEW.demand_id,
                            NEW.demand_user_id,
                            IFNULL(v_apply_name, '未知'),
                            v_approve_id,
                            v_approve_name,
                            '待审核',
                            '待处理',
                            '物料需求申请已提交'
                        );
                    END
                    """);

            log.info("触发器 after_insert_tb_material_demand 自动修复完成。");
        } catch (Exception ex) {
            log.warn("触发器兼容修复未执行成功，不影响系统启动: {}", ex.getMessage());
        }
    }
}
