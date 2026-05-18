package com.example.springbootbackeng.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MaterialDemandAuditRequest {

    @NotBlank(message = "需求单ID不能为空")
    @Size(max = 64, message = "需求单ID长度不能超过64")
    private String demandId;

    @NotBlank(message = "审批动作不能为空")
    @Pattern(regexp = "APPROVE|PASS|REJECT", message = "审批动作不合法")
    private String action;

    @Size(max = 200, message = "驳回原因长度不能超过200")
    private String rejectReason;
}
