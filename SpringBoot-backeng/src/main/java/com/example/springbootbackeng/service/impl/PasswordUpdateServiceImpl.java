package com.example.springbootbackeng.service.impl;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springbootbackeng.entity.PasswordUpdate;
import com.example.springbootbackeng.entity.User;
import com.example.springbootbackeng.mapper.PasswordUpdateMapper;
import com.example.springbootbackeng.service.PasswordUpdateService;

@Service
public class PasswordUpdateServiceImpl implements PasswordUpdateService {

    private final PasswordUpdateMapper passwordUpdateMapper;

    public PasswordUpdateServiceImpl(PasswordUpdateMapper passwordUpdateMapper) {
        this.passwordUpdateMapper = passwordUpdateMapper;
    }

    @Override
    public void createRecoveryRecord(User user,
                                     String verifyMethod,
                                     String sentCode,
                                     String ipAddress,
                                     String userAgent) {
        if (user == null) {
            return;
        }

        PasswordUpdate rec = new PasswordUpdate();
        rec.setRecordId(UUID.randomUUID().toString().replace("-", ""));
        rec.setUserId(user.getUserId());
        rec.setUsername(nvl(user.getUsername(), ""));
        rec.setRealName(nvl(user.getRealName(), ""));

        rec.setApplyTime(LocalDateTime.now());
        rec.setUpdateStatus(0); // 处理中
        rec.setFailReason(null);

        rec.setOsType(parseOsType(userAgent));
        rec.setBrowserType(parseBrowserType(userAgent));
        rec.setIpAddress(nvl(ipAddress, "unknown"));
        rec.setPhysicalLocation("未知");

        rec.setVerifyMethod(nvl(verifyMethod, "验证码"));
        rec.setUpdateVerifyCode(maskCode(sentCode));
        rec.setVerifyCode(null);

        rec.setCompleteTime(null);
        rec.setHandlerId(null);

        rec.setIsSuspicious(0);
        rec.setRiskLevel(1);

        rec.setRemark("验证码已发送，等待用户验证");
        rec.setExtraInfo(buildExtraInfoJson(userAgent, verifyMethod));

        passwordUpdateMapper.insert(rec);
    }

    @Override
    public void markVerifySuccess(String username,
                                  String verifyMethod,
                                  String inputCode) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }

        String method = verifyMethod == null ? null : verifyMethod.trim();
        LambdaQueryWrapper<PasswordUpdate> wrapper = new LambdaQueryWrapper<PasswordUpdate>()
                .eq(PasswordUpdate::getUsername, username.trim())
            .eq(method != null && !method.isEmpty(), PasswordUpdate::getVerifyMethod, method)
                .eq(PasswordUpdate::getUpdateStatus, 0)
                .orderByDesc(PasswordUpdate::getApplyTime)
                .last("LIMIT 1");

        PasswordUpdate rec = passwordUpdateMapper.selectOne(wrapper);
        if (rec == null) {
            return;
        }

        rec.setUpdateStatus(1); // 成功
        rec.setFailReason(null);
        rec.setVerifyCode(maskCode(inputCode));
        rec.setCompleteTime(LocalDateTime.now());
        rec.setRemark("验证码校验通过");

        passwordUpdateMapper.updateById(rec);
    }

    @Override
    public void markVerifyFailed(String username,
                                 String verifyMethod,
                                 String inputCode,
                                 String failReason) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }

        String method = verifyMethod == null ? null : verifyMethod.trim();
        LambdaQueryWrapper<PasswordUpdate> wrapper = new LambdaQueryWrapper<PasswordUpdate>()
                .eq(PasswordUpdate::getUsername, username.trim())
                .eq(method != null && !method.isEmpty(), PasswordUpdate::getVerifyMethod, method)
                .eq(PasswordUpdate::getUpdateStatus, 0)
                .orderByDesc(PasswordUpdate::getApplyTime)
                .last("LIMIT 1");

        PasswordUpdate rec = passwordUpdateMapper.selectOne(wrapper);
        if (rec == null) {
            return;
        }

        rec.setUpdateStatus(2); // 失败
        rec.setFailReason(nvl(failReason, "验证码错误或已过期"));
        rec.setVerifyCode(maskCode(inputCode));
        rec.setCompleteTime(LocalDateTime.now());
        rec.setRemark("验证码校验失败");

        passwordUpdateMapper.updateById(rec);
    }

    private String buildExtraInfoJson(String userAgent, String verifyMethod) {
        String ua = escapeJson(nvl(userAgent, ""));
        String method = escapeJson(nvl(verifyMethod, ""));
        String capturedAt = escapeJson(LocalDateTime.now().toString());
        return "{\"userAgent\":\"" + ua + "\",\"verifyMethod\":\"" + method + "\",\"capturedAt\":\"" + capturedAt + "\"}";
    }

    private String parseOsType(String userAgent) {
        if (userAgent == null) return "Other";
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) return "iOS";
        if (ua.contains("mac os") || ua.contains("macintosh")) return "macOS";
        if (ua.contains("linux")) return "Linux";
        return "Other";
    }

    private String parseBrowserType(String userAgent) {
        if (userAgent == null) return "Other";
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("chrome/")) return "Chrome";
        if (ua.contains("safari/") && !ua.contains("chrome/")) return "Safari";
        if (ua.contains("micromessenger")) return "微信内置浏览器";
        if (ua.contains("alipayclient")) return "支付宝内置浏览器";
        return "Other";
    }

    private String maskCode(String code) {
        if (code == null) return null;
        String c = code.trim();
        if (c.isEmpty()) return c;
        if (c.length() <= 2) return "**";
        return c.substring(0, 1) + "****" + c.substring(c.length() - 1);
    }

    private String nvl(String s, String dft) {
        return s == null ? dft : s;
    }

    private String escapeJson(String s) {
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
