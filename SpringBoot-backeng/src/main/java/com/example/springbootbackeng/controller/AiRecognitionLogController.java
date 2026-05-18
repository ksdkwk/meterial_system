package com.example.springbootbackeng.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springbootbackeng.common.Result;
import com.example.springbootbackeng.dto.AiRecognitionRunRequest;
import com.example.springbootbackeng.dto.AiRecognitionSettingsUpdateRequest;
import com.example.springbootbackeng.dto.AiRecognitionVerifyRequest;
import com.example.springbootbackeng.entity.AiRecognitionLogVO;
import com.example.springbootbackeng.service.AiRecognitionLogService;
import com.example.springbootbackeng.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/ai-recognition-logs")
public class AiRecognitionLogController {

    private static final String LOCAL_IMAGE_SCHEME = "local://";
    private static final Path UPLOAD_ROOT = Paths.get(System.getProperty("user.dir"), "uploads", "ai-recognition");

    private final AiRecognitionLogService aiRecognitionLogService;
    private final UserService userService;

    public AiRecognitionLogController(AiRecognitionLogService aiRecognitionLogService, UserService userService) {
        this.aiRecognitionLogService = aiRecognitionLogService;
        this.userService = userService;
    }

    private boolean hasManagePermission(String userId, String roleCode) {
        boolean manager = userService.isManagerRoleCode(roleCode) && userService.isManagerByUserId(userId);
        boolean warehouse = userService.isWarehouseRoleCode(roleCode) && userService.isWarehouseByUserId(userId);
        return manager || warehouse;
    }

    private boolean hasRecognizePermission(String userId, String roleCode) {
        if (hasManagePermission(userId, roleCode)) {
            return true;
        }
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        String rc = roleCode == null ? "" : roleCode.trim().toUpperCase();
        if (!("WORKER".equals(rc) || "USER_WORKER".equals(rc))) {
            return false;
        }
        var user = userService.findByUserId(userId);
        if (user == null) {
            return false;
        }
        if (user.getRole() != null && user.getRole().getRoleCode() != null) {
            String dbRoleCode = user.getRole().getRoleCode().trim().toUpperCase();
            if ("WORKER".equals(dbRoleCode) || "USER_WORKER".equals(dbRoleCode)) {
                return true;
            }
        }
        String rp = user.getRolePermission();
        if (rp == null) {
            return false;
        }
        String dbPermission = rp.trim().toUpperCase();
        return "WORKER".equals(dbPermission) || "USER_WORKER".equals(dbPermission);
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) String keyword,
                          @RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        String roleCode = request.getHeader("X-Role-Code");

        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("未获取到登录用户信息");
        }

        boolean isManager = userService.isManagerRoleCode(roleCode) && userService.isManagerByUserId(userId);
        List<AiRecognitionLogVO> all = aiRecognitionLogService.listVisibleLogs(keyword, userId, isManager);

        int total = all.size();
        int validPageSize = Math.max(1, pageSize == null ? 10 : pageSize);
        int validPageNum = Math.max(1, pageNum == null ? 1 : pageNum);
        int from = (validPageNum - 1) * validPageSize;
        int to = Math.min(from + validPageSize, total);

        List<AiRecognitionLogVO> records = from >= total ? Collections.emptyList() : all.subList(from, to);

        Map<String, Object> pageData = new HashMap<>();
        pageData.put("records", records);
        pageData.put("total", total);
        pageData.put("pageNum", validPageNum);
        pageData.put("pageSize", validPageSize);

        return Result.success(pageData);
    }

    @PostMapping("/recognize")
    public Result<?> recognize(@RequestBody AiRecognitionRunRequest request,
                               HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader("X-User-Id");
        String roleCode = httpRequest.getHeader("X-Role-Code");
        if (!hasRecognizePermission(userId, roleCode)) {
            return Result.error("仅管理人员、仓库管理员或施工人员可执行AI识别");
        }
        try {
            return Result.success(aiRecognitionLogService.runRecognition(userId, request == null ? null : request.getOriginalImageUrl()));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("AI识别执行失败: " + e.getMessage());
        }
    }

    @PostMapping(value = "/recognize/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> recognizeUpload(@RequestParam("file") MultipartFile file,
                                     HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader("X-User-Id");
        String roleCode = httpRequest.getHeader("X-Role-Code");
        if (!hasRecognizePermission(userId, roleCode)) {
            return Result.error("仅管理人员、仓库管理员或施工人员可执行AI识别");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的图片文件");
        }
        try {
            Files.createDirectories(UPLOAD_ROOT);
            String originalFilename = file.getOriginalFilename();
            String safeName = (originalFilename == null || originalFilename.trim().isEmpty())
                    ? "unknown.jpg"
                    : originalFilename.replaceAll("[\\r\\n\\t\\\\/:*?\"<>|]", "_");
            String ext = "";
            int dot = safeName.lastIndexOf('.');
            if (dot > -1 && dot < safeName.length() - 1) {
                ext = safeName.substring(dot);
            }
            String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path savePath = UPLOAD_ROOT.resolve(storedName).normalize();
            file.transferTo(savePath.toFile());

            String localImageRef = LOCAL_IMAGE_SCHEME + storedName;
            return Result.success(aiRecognitionLogService.runRecognition(userId, localImageRef));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("AI识别执行失败: " + e.getMessage());
        }
    }

    @GetMapping("/uploads/{fileName:.+}")
    public ResponseEntity<Resource> loadUploadedImage(@PathVariable String fileName) {
        try {
            String safeName = fileName.replaceAll("[\\r\\n\\t\\\\/]", "");
            if (safeName.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Path filePath = UPLOAD_ROOT.resolve(safeName).normalize();
            if (!filePath.startsWith(UPLOAD_ROOT) || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null || contentType.trim().isEmpty()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            Resource resource = new FileSystemResource(filePath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{recId}/verify")
    public Result<?> verify(@PathVariable String recId,
                            @RequestBody(required = false) AiRecognitionVerifyRequest request,
                            HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader("X-User-Id");
        String roleCode = httpRequest.getHeader("X-Role-Code");
        if (!hasManagePermission(userId, roleCode)) {
            return Result.error("仅管理人员或仓库管理员可核实AI识别记录");
        }
        String msg = aiRecognitionLogService.verifyRecognition(
                recId,
                userId,
                request == null ? null : request.getRecognizeMaterialName(),
                request == null ? null : request.getRecognizeQuantity()
        );
        return msg == null ? Result.success("核实成功") : Result.error(msg);
    }

    @GetMapping("/settings")
    public Result<?> getSettings(HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader("X-User-Id");
        String roleCode = httpRequest.getHeader("X-Role-Code");
        if (!hasManagePermission(userId, roleCode)) {
            return Result.error("仅管理人员或仓库管理员可查看AI识别参数");
        }
        return Result.success(aiRecognitionLogService.getRecognitionSettings());
    }

    @GetMapping("/config-check")
    public Result<?> configCheck(HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader("X-User-Id");
        String roleCode = httpRequest.getHeader("X-Role-Code");
        if (!hasManagePermission(userId, roleCode)) {
            return Result.error("仅管理人员或仓库管理员可查看AI配置");
        }
        return Result.success(aiRecognitionLogService.getAiConfigCheck());
    }

    @PutMapping("/settings")
    public Result<?> updateSettings(@RequestBody(required = false) AiRecognitionSettingsUpdateRequest request,
                                    HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader("X-User-Id");
        String roleCode = httpRequest.getHeader("X-Role-Code");
        if (!hasManagePermission(userId, roleCode)) {
            return Result.error("仅管理人员或仓库管理员可修改AI识别参数");
        }
        String msg = aiRecognitionLogService.updateRecognitionSettings(
                userId,
                request == null ? null : request.getBaikeNum(),
                request == null ? null : request.getMinConfidenceThreshold(),
                request == null ? null : request.getKeywordDict()
        );
        return msg == null ? Result.success("保存成功") : Result.error(msg);
    }
}
