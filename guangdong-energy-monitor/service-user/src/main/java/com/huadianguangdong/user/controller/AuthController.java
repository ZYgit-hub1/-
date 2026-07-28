package com.huadianguangdong.user.controller;

import com.huadianguangdong.common.api.R;
import com.huadianguangdong.common.api.ResultCode;
import com.huadianguangdong.common.constant.CommonConstants;
import com.huadianguangdong.common.exception.BusinessException;
import com.huadianguangdong.common.util.JwtUtil;
import com.huadianguangdong.common.util.RedisUtil;
import com.huadianguangdong.user.service.AuditLogService;
import com.huadianguangdong.user.service.SysUserService;
import com.huadianguangdong.user.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 认证 Controller
 *
 * @author huadianguangdong
 */
@Slf4j
@Tag(name = "认证管理", description = "登录、登出、刷新 Token")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final AuditLogService auditLogService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@RequestParam String username,
                            @RequestParam String password,
                            HttpServletRequest request) {
        LoginVO vo = sysUserService.login(username, password);
        auditLogService.record(vo.getUserId(), vo.getUsername(), "认证", "登录",
                "username=" + username, getClientIp(request), CommonConstants.SUCCESS, 0);
        return R.ok(vo);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                          HttpServletRequest request) {
        if (authorization == null || authorization.isBlank()) {
            return R.ok();
        }
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        try {
            Long userId = jwtUtil.getUserId(token);
            String username = jwtUtil.getUsername(token);
            // 清除 Redis 中的 token 与用户缓存
            redisUtil.del(CommonConstants.REDIS_TOKEN_PREFIX + userId);
            redisUtil.del(CommonConstants.REDIS_USER_PREFIX + userId);
            auditLogService.record(userId, username, "认证", "登出",
                    null, getClientIp(request), CommonConstants.SUCCESS, 0);
        } catch (Exception e) {
            log.warn("退出登录解析 token 失败：{}", e.getMessage());
        }
        return R.ok();
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refreshToken")
    public R<LoginVO> refreshToken(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   HttpServletRequest request) {
        if (authorization == null || authorization.isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未携带 Token");
        }
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED, "Token 已失效，请重新登录");
        }
        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);
        // 重新生成 token 并刷新缓存
        String newToken = jwtUtil.generateToken(userId, username);
        redisUtil.setEx(CommonConstants.REDIS_TOKEN_PREFIX + userId, newToken, 2, TimeUnit.HOURS);
        LoginVO vo = sysUserService.getUserInfo(userId);
        vo.setToken(newToken);
        auditLogService.record(userId, username, "认证", "刷新Token",
                null, getClientIp(request), CommonConstants.SUCCESS, 0);
        return R.ok(vo);
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
