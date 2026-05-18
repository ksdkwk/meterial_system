package com.example.springbootbackeng.controller;

import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootbackeng.common.Result;
import com.example.springbootbackeng.dto.LoginRequest;
import com.example.springbootbackeng.entity.User;
import com.example.springbootbackeng.ratelimit.MethodRateLimit;
import com.example.springbootbackeng.security.JwtTokenService;
import com.example.springbootbackeng.service.MailCodeService;
import com.example.springbootbackeng.service.PasswordUpdateService;
import com.example.springbootbackeng.service.PhoneCodeService;
import com.example.springbootbackeng.service.RedisLoginCaptchaGuardService;
import com.example.springbootbackeng.service.RedisLoginCaptchaStoreService;
import com.example.springbootbackeng.service.RedisRecoveryStoreService;
import com.example.springbootbackeng.service.UserService;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录控制器
 */
@CrossOrigin(origins = "*", maxAge = 3600) // 解决跨域
@RestController
@RequestMapping("/login")
@Validated
public class LoginController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^\\w+([-+. ]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$".replace(" ", ""));
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Duration CAPTCHA_EXPIRE = Duration.ofMinutes(2);
    private static final int CAPTCHA_MAX_FAIL_TIMES = 5;
    private static final Duration CAPTCHA_FAIL_WINDOW = Duration.ofMinutes(10);
    private static final Duration CAPTCHA_LOCK_DURATION = Duration.ofMinutes(10);

    @Autowired
    private UserService userService;

    @Autowired
    private MailCodeService mailCodeService;

    @Autowired
    private PhoneCodeService phoneCodeService;

    @Autowired
    private PasswordUpdateService passwordUpdateService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private RedisRecoveryStoreService redisRecoveryStoreService;

    @Autowired
    private RedisLoginCaptchaGuardService redisLoginCaptchaGuardService;

    @Autowired
    private RedisLoginCaptchaStoreService redisLoginCaptchaStoreService;

    @Autowired
    private org.springframework.core.env.Environment environment;

    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * 登录接口
     */
    @PostMapping
    @MethodRateLimit(key = "login:submit", limit = 60, period = 60, message = "登录请求过于频繁，请稍后重试")
    public Result<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        // 1. 前端参数校验
        if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
            return Result.error("账号不能为空");
        }
        if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (loginRequest.getRoleCode() == null || loginRequest.getRoleCode().trim().isEmpty()) {
            return Result.error("角色不能为空");
        }

        String loginIp = resolveClientIp(request);
        String loginAccount = loginRequest.getUsername().trim();
        String lockMessage = checkCaptchaLockMessage(loginIp, loginAccount);
        if (lockMessage != null) {
            return Result.error(lockMessage);
        }

        if (!verifyCaptcha(loginRequest.getCaptchaId(), loginRequest.getCaptchaCode())) {
            recordCaptchaVerifyFailMetric();
            boolean lockedNow = recordCaptchaFailure(loginIp, loginAccount);
            if (lockedNow) {
                recordCaptchaLockMetric();
                return Result.error("验证码错误次数过多，IP/账号已临时锁定10分钟");
            }
            return Result.error("验证码错误或已过期");
        }
        clearCaptchaFailure(loginIp, loginAccount);

        // 2. 调用服务层验证
        User user = userService.login(loginRequest);
        if (user != null) {
            String token = jwtTokenService.createToken(user);
            Map<String, Object> data = new HashMap<>();
            data.put("accessToken", token);
            data.put("tokenType", "Bearer");
            data.put("expiresIn", jwtTokenService.getExpireSeconds());
            data.put("user", user);
            return Result.success(data);
        } else {
            return Result.error("账号/密码/角色不匹配，或账号已禁用");
        }
    }

    /**
     * 获取登录验证码
     */
    @GetMapping("/captcha")
    @MethodRateLimit(key = "login:captcha", limit = 100, period = 60, message = "验证码获取过于频繁，请稍后再试")
    public Result<?> getLoginCaptcha() {
        String code = randomCaptchaCode(4);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redisLoginCaptchaStoreService.saveCaptcha(captchaId, code, CAPTCHA_EXPIRE);

        Map<String, Object> data = new HashMap<>();
        data.put("captchaId", captchaId);
        data.put("captchaImage", buildSvgCaptchaDataUrl(code));
        data.put("expireSeconds", CAPTCHA_EXPIRE.getSeconds());
        return Result.success(data);
    }

    /**
     * 找回密码-账号校验接口
     */
    @GetMapping("/recovery/check-account")
    public Result<?> checkRecoveryAccount(@RequestParam @NotBlank(message = "账号不能为空") @Size(max = 64, message = "账号长度不能超过64") String username) {
        if (username == null || username.trim().isEmpty()) {
            return Result.error("账号不能为空");
        }

        User user = userService.findActiveUserByUsername(username);
        if (user == null) {
            return Result.error("无法找到该账号");
        }

        String phone = user.getPhone();
        String userMail = user.getUserMail();

        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("hasPhone", phone != null && !phone.trim().isEmpty());
        data.put("hasEmail", userMail != null && !userMail.trim().isEmpty());
        data.put("userMail", userMail);
        data.put("maskedPhone", maskPhone(phone));
        data.put("maskedEmail", maskEmail(userMail));

        return Result.success(data);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "未绑定手机号";
        }
        String p = phone.trim();
        if (p.length() < 7) {
            return "******";
        }
        return p.substring(0, 3) + "****" + p.substring(p.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "未绑定邮箱";
        }
        String e = email.trim();
        int at = e.indexOf('@');
        if (at <= 1) {
            return "***" + e.substring(Math.max(0, at));
        }
        return e.substring(0, 1) + "***" + e.substring(at);
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/recovery/send-email-code")
    public Result<?> sendRecoveryEmailCode(@RequestParam @NotBlank(message = "账号不能为空") @Size(max = 64, message = "账号长度不能超过64") String username,
                                           HttpServletRequest request) {
        if (username == null || username.trim().isEmpty()) {
            return Result.error("账号不能为空");
        }

        User user = userService.findActiveUserByUsername(username);
        if (user == null) {
            return Result.error("无法找到该账号");
        }
        String userMail = user.getUserMail();
        if (userMail == null || userMail.trim().isEmpty()) {
            return Result.error("该账号未绑定邮箱");
        }
        if (!EMAIL_PATTERN.matcher(userMail.trim()).matches()) {
            return Result.error("该账号绑定邮箱格式不合法，请联系管理员");
        }

        try {
            String sentCode = mailCodeService.sendEmailCode(username, userMail.trim());
            passwordUpdateService.createRecoveryRecord(
                    user,
                    "邮箱验证码",
                    sentCode,
                    resolveClientIp(request),
                    request == null ? null : request.getHeader("User-Agent")
            );
            return Result.success("验证码已发送至邮箱");
        } catch (Exception e) {
            return Result.error("验证码发送失败，请稍后重试");
        }
    }

    /**
     * 发送手机验证码
     */
    @PostMapping("/recovery/send-phone-code")
    public Result<?> sendRecoveryPhoneCode(@RequestParam @NotBlank(message = "账号不能为空") @Size(max = 64, message = "账号长度不能超过64") String username,
                                           HttpServletRequest request) {
        if (username == null || username.trim().isEmpty()) {
            return Result.error("账号不能为空");
        }

        User user = userService.findActiveUserByUsername(username);
        if (user == null) {
            return Result.error("无法找到该账号");
        }
        String phone = user.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            return Result.error("该账号未绑定手机号");
        }

        try {
            String sentCode = phoneCodeService.sendPhoneCode(username, phone.trim());
            passwordUpdateService.createRecoveryRecord(
                    user,
                    "手机验证码",
                    sentCode,
                    resolveClientIp(request),
                    request == null ? null : request.getHeader("User-Agent")
            );
            return Result.success("验证码已发送至手机号");
        } catch (Exception e) {
            return Result.error("验证码发送失败，请稍后重试");
        }
    }

    /**
     * 校验邮箱验证码
     */
    @PostMapping("/recovery/verify-email-code")
    public Result<?> verifyRecoveryEmailCode(@RequestParam @NotBlank(message = "账号不能为空") @Size(max = 64, message = "账号长度不能超过64") String username,
                                             @RequestParam @NotBlank(message = "验证码不能为空") @Size(max = 10, message = "验证码长度不合法") String code) {
        if (username == null || username.trim().isEmpty()) {
            return Result.error("账号不能为空");
        }
        if (code == null || code.trim().isEmpty()) {
            return Result.error("验证码不能为空");
        }

        boolean pass = mailCodeService.verifyEmailCode(username, code);
        if (!pass) {
            passwordUpdateService.markVerifyFailed(username, "邮箱验证码", code, "验证码错误或已过期");
            return Result.error("验证码错误或已过期");
        }
        passwordUpdateService.markVerifySuccess(username, "邮箱验证码", code);
        redisRecoveryStoreService.markVerified(username, Duration.ofMinutes(resolveRecoveryExpireMinutes()));
        return Result.success("邮箱验证通过");
    }

    /**
     * 校验手机验证码
     */
    @PostMapping("/recovery/verify-phone-code")
    public Result<?> verifyRecoveryPhoneCode(@RequestParam @NotBlank(message = "账号不能为空") @Size(max = 64, message = "账号长度不能超过64") String username,
                                             @RequestParam @NotBlank(message = "验证码不能为空") @Size(max = 10, message = "验证码长度不合法") String code) {
        if (username == null || username.trim().isEmpty()) {
            return Result.error("账号不能为空");
        }
        if (code == null || code.trim().isEmpty()) {
            return Result.error("验证码不能为空");
        }

        boolean pass = phoneCodeService.verifyPhoneCode(username, code);
        if (!pass) {
            passwordUpdateService.markVerifyFailed(username, "手机验证码", code, "验证码错误或已过期");
            return Result.error("验证码错误或已过期");
        }
        passwordUpdateService.markVerifySuccess(username, "手机验证码", code);
        redisRecoveryStoreService.markVerified(username, Duration.ofMinutes(resolveRecoveryExpireMinutes()));
        return Result.success("手机验证通过");
    }

    /**
     * 找回密码-重置新密码
     */
    @PostMapping("/recovery/reset-password")
    public Result<?> resetRecoveryPassword(@RequestParam @NotBlank(message = "账号不能为空") @Size(max = 64, message = "账号长度不能超过64") String username,
                                           @RequestParam @NotBlank(message = "新密码不能为空") @Size(min = 6, max = 128, message = "新密码长度需在6-128位") String newPassword) {
        if (username == null || username.trim().isEmpty()) {
            return Result.error("账号不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Result.error("新密码不能为空");
        }
        if (newPassword.trim().length() < 6) {
            return Result.error("新密码长度不能少于6位");
        }

        if (!redisRecoveryStoreService.isVerified(username)) {
            return Result.error("请先完成验证码验证");
        }

        User user = userService.findActiveUserByUsername(username);
        if (user == null) {
            return Result.error("无法找到该账号");
        }

        boolean updated = userService.resetPasswordByUsername(username, newPassword);
        if (!updated) {
            return Result.error("重置密码失败，请稍后重试");
        }

        redisRecoveryStoreService.clearVerified(username);
        return Result.success("密码重置成功");
    }

    private int resolveRecoveryExpireMinutes() {
        return environment.getProperty("recovery.code.expire-minutes", Integer.class, 10);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String[] headerNames = new String[]{"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip.trim())) {
                int commaIdx = ip.indexOf(',');
                return (commaIdx > 0 ? ip.substring(0, commaIdx) : ip).trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String checkCaptchaLockMessage(String ip, String account) {
        if (!redisLoginCaptchaGuardService.isLocked(ip, account)) {
            return null;
        }
        long remainSeconds = redisLoginCaptchaGuardService.getRemainLockSeconds(ip, account);
        if (remainSeconds <= 0) {
            return null;
        }
        long remainMinutes = Math.max(1, (remainSeconds + 59) / 60);
        return "操作过于频繁，请" + remainMinutes + "分钟后再试";
    }

    private boolean recordCaptchaFailure(String ip, String account) {
        return redisLoginCaptchaGuardService.recordFailure(
                ip,
                account,
                CAPTCHA_MAX_FAIL_TIMES,
                CAPTCHA_FAIL_WINDOW,
                CAPTCHA_LOCK_DURATION
        );
    }

    private void clearCaptchaFailure(String ip, String account) {
        redisLoginCaptchaGuardService.clearFailures(ip, account);
    }

    private void recordCaptchaVerifyFailMetric() {
        meterRegistry.counter("app.auth.captcha.verify.fail.total").increment();
    }

    private void recordCaptchaLockMetric() {
        meterRegistry.counter("app.auth.captcha.lock.total").increment();
    }

    private boolean verifyCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            return false;
        }
        return redisLoginCaptchaStoreService.verifyAndConsumeCaptcha(captchaId, captchaCode);
    }

    private String randomCaptchaCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = ThreadLocalRandom.current().nextInt(CAPTCHA_CHARS.length());
            sb.append(CAPTCHA_CHARS.charAt(idx));
        }
        return sb.toString();
    }

    private String buildSvgCaptchaDataUrl(String code) {
        int width = 120;
        int height = 40;
        StringBuilder textSvg = new StringBuilder();
        for (int i = 0; i < code.length(); i++) {
            int x = 18 + i * 24 + ThreadLocalRandom.current().nextInt(-2, 3);
            int y = 26 + ThreadLocalRandom.current().nextInt(-3, 4);
            int rotate = ThreadLocalRandom.current().nextInt(-20, 21);
            textSvg.append("<text x='")
                    .append(x)
                    .append("' y='")
                    .append(y)
                    .append("' fill='#")
                    .append(Integer.toHexString(ThreadLocalRandom.current().nextInt(0x333333, 0x999999)))
                    .append("' font-size='22' font-family='Arial' transform='rotate(")
                    .append(rotate)
                    .append(' ')
                    .append(x)
                    .append(' ')
                    .append(y)
                    .append(")'>")
                    .append(code.charAt(i))
                    .append("</text>");
        }

        StringBuilder lineSvg = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            int x1 = ThreadLocalRandom.current().nextInt(0, width);
            int y1 = ThreadLocalRandom.current().nextInt(0, height);
            int x2 = ThreadLocalRandom.current().nextInt(0, width);
            int y2 = ThreadLocalRandom.current().nextInt(0, height);
            lineSvg.append("<line x1='")
                    .append(x1)
                    .append("' y1='")
                    .append(y1)
                    .append("' x2='")
                    .append(x2)
                    .append("' y2='")
                    .append(y2)
                    .append("' stroke='#")
                    .append(Integer.toHexString(ThreadLocalRandom.current().nextInt(0xaaaaaa, 0xeeeeee)))
                    .append("' stroke-width='1'/>");
        }

        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='" + width + "' height='" + height + "' viewBox='0 0 " + width + " " + height + "'>"
                + "<rect width='100%' height='100%' fill='#f7f7f7'/>"
                + lineSvg
                + textSvg
                + "</svg>";
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes());
    }

}