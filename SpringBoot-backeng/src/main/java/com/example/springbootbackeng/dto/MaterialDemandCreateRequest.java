package com.example.springbootbackeng.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MaterialDemandCreateRequest {

    @NotBlank(message = "申请人不能为空")
    @Size(max = 64, message = "申请人长度不能超过64")
    private String demandUserId;

    @NotBlank(message = "物料不能为空")
    @Size(max = 64, message = "物料ID长度不能超过64")
    private String materialId;

    @NotNull(message = "需求数量不能为空")
    @Min(value = 1, message = "需求数量必须大于0")
    private Integer demandQuantity;

    @Size(max = 128, message = "现场照片URL长度不能超过128")
    private String sitePhotoUrl;

    @Size(max = 32, message = "期望使用时间格式串过长")
    private String useTime;
}
