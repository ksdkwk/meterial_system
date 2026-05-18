package com.example.springbootbackeng.entity;

import java.util.Locale;

/**
 * 库存日志业务动作枚举。
 */
public enum InventoryLogAction {
    ADD("ADD", "新增"),
    DELETE("DELETE", "删除"),
    REVOKE_ADD("REVOKE_ADD", "撤回新增"),
    ALLOCATE("ALLOCATE", "分配物料"),
    ADJUST("ADJUST", "调整");

    private final String code;
    private final String label;

    InventoryLogAction(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static InventoryLogAction fromAny(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String raw = value.trim();
        String upper = raw.toUpperCase(Locale.ROOT);
        for (InventoryLogAction action : values()) {
            if (action.code.equals(upper)) {
                return action;
            }
        }

        // 兼容历史中文值
        return switch (raw) {
            case "新增" -> ADD;
            case "删除" -> DELETE;
            case "撤回新增" -> REVOKE_ADD;
            case "分配物料" -> ALLOCATE;
            case "修改", "调整" -> ADJUST;
            default -> null;
        };
    }
}
