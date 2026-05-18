package com.example.springbootbackeng.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootbackeng.common.Result;
import com.example.springbootbackeng.entity.User;
import com.example.springbootbackeng.security.SecurityUserContext;
import com.example.springbootbackeng.service.MailCodeService;
import com.example.springbootbackeng.service.PhoneCodeService;
import com.example.springbootbackeng.service.UserService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/users/security")
@CrossOrigin(origins = "*", maxAge = 3600)
@Validated
public class UserSecurityController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    private final UserService userService;
    private final MailCodeService mailCodeService;
    private final PhoneCodeService phoneCodeService;

    public UserSecurityController(UserService userService,
                                  MailCodeService mailCodeService,
                                  PhoneCodeService phoneCodeService) {
        this.userService = userService;
        this.mailCodeService = mailCodeService;
        this.phoneCodeService = phoneCodeService;
    }

    private final Map<String, LocalDateTime> currentBindingVerifiedStore = new ConcurrentHashMap<>();

    @GetMapping("/bindings")
    public Result<?> getBindings() {
        User user = requireCurrentUser();
        if (user == null) {
            return Result.error("用户未登录");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("userMail", user.getUserMail());
        data.put("phone", user.getPhone());
        return Result.success(data);
    }

    @PostMapping("/send-current-code")
    public Result<?> sendCurrentCode(@RequestParam @NotBlank(message = "type不能为空") @Size(max = 32, message = "type长度不能超过32") String type) {
        User user = requireCurrentUser();
        if (user == null) {
            return Result.error("用户未登录");
        }

        if (!isValidType(type)) {
            return Result.error("参数type不合法");
        }

        boolean hasBinding = hasCurrentBinding(user, type);
        if (!hasBinding) {
            return Result.success("当前未绑定，无需验证旧绑定");
        }

        String codeKey = buildCodeKey(user.getUserId(), type, "CURRENT");
        if (isEmailType(type)) {
            String mail = user.getUserMail();
            mailCodeService.sendEmailCode(codeKey, mail);
            return Result.success("验证码已发送到当前绑定邮箱");
        }

        String phone = user.getPhone();
        phoneCodeService.sendPhoneCode(codeKey, phone);
        return Result.success("验证码已发送到当前绑定手机号");
    }

    @PostMapping("/verify-current-code")
    public Result<?> verifyCurrentCode(@RequestParam @NotBlank(message = "type不能为空") @Size(max = 32, message = "type长度不能超过32") String type,
                                       @RequestParam @NotBlank(message = "验证码不能为空") @Size(max = 10, message = "验证码长度不合法") String code) {
        User user = requireCurrentUser();
        if (user == null) {
            return Result.error("用户未登录");
        }

        if (!isValidType(type)) {
            return Result.error("参数type不合法");
        }

        if (!hasCurrentBinding(user, type)) {
            return Result.success("当前未绑定，无需验证旧绑定");
        }

        String codeKey = buildCodeKey(user.getUserId(), type, "CURRENT");
        boolean passed = isEmailType(type)
                ? mailCodeService.verifyEmailCode(codeKey, code)
                : phoneCodeService.verifyPhoneCode(codeKey, code);

        if (!passed) {
            return Result.error("验证码错误或已过期");
        }

        currentBindingVerifiedStore.put(buildVerifyKey(user.getUserId(), type), LocalDateTime.now().plusMinutes(10));
        return Result.success("旧绑定校验通过");
    }

    @PostMapping("/send-new-code")
    public Result<?> sendNewCode(@RequestParam @NotBlank(message = "type不能为空") @Size(max = 32, message = "type长度不能超过32") String type,
                                 @RequestParam @NotBlank(message = "目标不能为空") @Size(max = 128, message = "目标长度不能超过128") String target) {
        User user = requireCurrentUser();
        if (user == null) {
            return Result.error("用户未登录");
        }
        if (!isValidType(type)) {
            return Result.error("参数type不合法");
        }

        String value = target == null ? "" : target.trim();
        if (value.isEmpty()) {
            return Result.error("目标不能为空");
        }

        if (isEmailType(type)) {
            if (!EMAIL_PATTERN.matcher(value).matches()) {
                return Result.error("邮箱格式不正确");
            }
            mailCodeService.sendEmailCode(buildCodeKey(user.getUserId(), type, "NEW"), value);
            return Result.success("验证码已发送到新邮箱");
        }

        if (!PHONE_PATTERN.matcher(value).matches()) {
            return Result.error("手机号格式不正确");
        }
        phoneCodeService.sendPhoneCode(buildCodeKey(user.getUserId(), type, "NEW"), value);
        return Result.success("验证码已发送到新手机号");
    }

    @PostMapping("/update-binding")
    public Result<?> updateBinding(@RequestParam @NotBlank(message = "type不能为空") @Size(max = 32, message = "type长度不能超过32") String type,
                                   @RequestParam @NotBlank(message = "目标不能为空") @Size(max = 128, message = "目标长度不能超过128") String target,
                                   @RequestParam @NotBlank(message = "验证码不能为空") @Size(max = 10, message = "验证码长度不合法") String code) {
        User user = requireCurrentUser();
        if (user == null) {
            return Result.error("用户未登录");
        }
        if (!isValidType(type)) {
            return Result.error("参数type不合法");
        }

        String value = target == null ? "" : target.trim();
        if (value.isEmpty()) {
            return Result.error("目标不能为空");
        }

        if (hasCurrentBinding(user, type) && !isCurrentBindingVerified(user.getUserId(), type)) {
            return Result.error("请先验证当前已绑定信息");
        }

        boolean newCodePassed = isEmailType(type)
                ? mailCodeService.verifyEmailCode(buildCodeKey(user.getUserId(), type, "NEW"), code)
                : phoneCodeService.verifyPhoneCode(buildCodeKey(user.getUserId(), type, "NEW"), code);

        if (!newCodePassed) {
            return Result.error("新绑定验证码错误或已过期");
        }

        boolean ok;
        if (isEmailType(type)) {
            if (!EMAIL_PATTERN.matcher(value).matches()) {
                return Result.error("邮箱格式不正确");
            }
            ok = userService.updateUserMail(user.getUserId(), value);
        } else {
            if (!PHONE_PATTERN.matcher(value).matches()) {
                return Result.error("手机号格式不正确");
            }
            ok = userService.updateUserPhone(user.getUserId(), value);
        }

        if (!ok) {
            return Result.error("更新失败，请稍后重试");
        }

        currentBindingVerifiedStore.remove(buildVerifyKey(user.getUserId(), type));
        return Result.success("更新成功");
    }

    private User requireCurrentUser() {
        String userId = SecurityUserContext.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        return userService.findByUserId(userId);
    }

    private boolean isValidType(String type) {
        return "email".equalsIgnoreCase(type) || "phone".equalsIgnoreCase(type);
    }

    private boolean isEmailType(String type) {
        return "email".equalsIgnoreCase(type);
    }

    private boolean hasCurrentBinding(User user, String type) {
        if (user == null) return false;
        if (isEmailType(type)) {
            return user.getUserMail() != null && !user.getUserMail().trim().isEmpty();
        }
        return user.getPhone() != null && !user.getPhone().trim().isEmpty();
    }

    private String buildCodeKey(String userId, String type, String phase) {
        return String.format("SEC_BIND:%s:%s:%s", userId == null ? "" : userId.trim(), type.toUpperCase(), phase.toUpperCase());
    }

    private String buildVerifyKey(String userId, String type) {
        return String.format("SEC_BIND_VERIFY:%s:%s", userId == null ? "" : userId.trim(), type.toUpperCase());
    }

    private boolean isCurrentBindingVerified(String userId, String type) {
        String key = buildVerifyKey(userId, type);
        LocalDateTime expireAt = currentBindingVerifiedStore.get(key);
        if (expireAt == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(expireAt)) {
            currentBindingVerifiedStore.remove(key);
            return false;
        }
        return true;
    }
}
