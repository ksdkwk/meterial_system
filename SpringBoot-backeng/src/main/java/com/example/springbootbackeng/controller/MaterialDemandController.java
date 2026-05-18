package com.example.springbootbackeng.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootbackeng.common.Result;
import com.example.springbootbackeng.dto.MaterialDemandAuditRequest;
import com.example.springbootbackeng.dto.MaterialDemandCreateRequest;
import com.example.springbootbackeng.entity.MaterialDemand;
import com.example.springbootbackeng.entity.MaterialDemandVO;
import com.example.springbootbackeng.security.SecurityUserContext;
import com.example.springbootbackeng.service.MaterialDemandService;
import com.example.springbootbackeng.service.OperationAuditLogger;
import com.example.springbootbackeng.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/material-demands")
@Validated
public class MaterialDemandController {

    private final MaterialDemandService materialDemandService;
    private final UserService userService;
    private final OperationAuditLogger operationAuditLogger;

    public MaterialDemandController(MaterialDemandService materialDemandService,
                                    UserService userService,
                                    OperationAuditLogger operationAuditLogger) {
        this.materialDemandService = materialDemandService;
        this.userService = userService;
        this.operationAuditLogger = operationAuditLogger;
    }

    private boolean isManager(HttpServletRequest request) {
        String roleCode = request.getHeader("X-Role-Code");
        String userId = request.getHeader("X-User-Id");
        return userService.isManagerRoleCode(roleCode) && userService.isManagerByUserId(userId);
    }

    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) Integer demandStatus,
            @RequestParam(required = false) @Size(max = 64, message = "关键词长度不能超过64") String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页数量不能小于1") @Max(value = 200, message = "每页数量不能超过200") Integer pageSize
    ) {
        List<MaterialDemandVO> all = materialDemandService.listAll(demandStatus, keyword);

        int total = all.size();
        int validPageSize = Math.max(1, pageSize == null ? 10 : pageSize);
        int validPageNum = Math.max(1, pageNum == null ? 1 : pageNum);
        int from = (validPageNum - 1) * validPageSize;
        int to = Math.min(from + validPageSize, total);

        List<MaterialDemandVO> records = from >= total
                ? Collections.emptyList()
                : all.subList(from, to);

        Map<String, Object> pageData = new HashMap<>();
        pageData.put("records", records);
        pageData.put("total", total);
        pageData.put("pageNum", validPageNum);
        pageData.put("pageSize", validPageSize);

        return Result.success(pageData);
    }

    @PostMapping
    public Result<?> create(@Valid @RequestBody MaterialDemandCreateRequest request,
                            HttpServletRequest httpRequest) {
        MaterialDemand demand = new MaterialDemand();
        demand.setDemandUserId(trimToNull(request.getDemandUserId()));
        demand.setMaterialId(trimToNull(request.getMaterialId()));
        demand.setDemandQuantity(request.getDemandQuantity());
        demand.setSitePhotoUrl(trimToNull(request.getSitePhotoUrl()));

        String useTimeRaw = trimToNull(request.getUseTime());
        LocalDateTime useTime = parseUseTime(useTimeRaw);
        if (useTimeRaw != null && !useTimeRaw.trim().isEmpty() && useTime == null) {
            return Result.error("期望使用时间格式错误，支持 yyyy-MM-ddTHH:mm 或 yyyy/MM/dd HH:mm");
        }
        demand.setUseTime(useTime);

        String msg = materialDemandService.create(demand);
        if (msg == null) {
            String operatorId = SecurityUserContext.getCurrentUserId();
            if (operatorId == null || operatorId.trim().isEmpty()) {
                operatorId = httpRequest.getHeader("X-User-Id");
            }
            if (operatorId == null || operatorId.trim().isEmpty()) {
                operatorId = demand.getDemandUserId();
            }
            operationAuditLogger.log(
                    operatorId,
                    "物料需求",
                    "ADD",
                    "新增物料需求单: " + demand.getDemandId(),
                    httpRequest.getRemoteAddr()
            );
        }
        return msg == null ? Result.success("新增需求成功") : Result.error(msg);
    }

    @PostMapping("/audit")
    public Result<?> audit(@Valid @RequestBody MaterialDemandAuditRequest payload, HttpServletRequest request) {
        if (!isManager(request)) {
            return Result.error("无权限审批需求");
        }

        String demandId = trimToNull(payload.getDemandId());
        String action = trimToNull(payload.getAction());
        String rejectReason = trimToNull(payload.getRejectReason());
        String auditorId = request.getHeader("X-User-Id");

        Integer status;
        if ("APPROVE".equalsIgnoreCase(action) || "PASS".equalsIgnoreCase(action)) {
            status = 1;
        } else if ("REJECT".equalsIgnoreCase(action)) {
            status = 2;
        } else {
            return Result.error("审批动作不合法");
        }

        String msg = materialDemandService.audit(demandId, status, rejectReason, auditorId);
        if (msg == null) {
            operationAuditLogger.log(
                    auditorId,
                    "需求审批",
                    "AUDIT",
                    "审批物料需求单: " + demandId + "，结果: " + (status == 1 ? "通过" : "驳回"),
                    request.getRemoteAddr()
            );
        }
        return msg == null ? Result.success("审批操作成功") : Result.error(msg);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private LocalDateTime parseUseTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String value = text.trim();
        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return null;
    }

    @GetMapping("/options/users")
    public Result<?> userOptions() {
        return Result.success(materialDemandService.listUserOptions());
    }

    @GetMapping("/options/materials")
    public Result<?> materialOptions() {
        return Result.success(materialDemandService.listMaterialOptions());
    }
}
