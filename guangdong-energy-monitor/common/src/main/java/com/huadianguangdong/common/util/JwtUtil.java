package com.huadianguangdong.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（基于 jjwt 0.12.x API）
 */
@Slf4j
@Component
public class JwtUtil {

    /** Token 中用户 ID 的声明键 */
    private static final String CLAIM_USER_ID = "userId";

    /** Token 中用户名的声明键 */
    private static final String CLAIM_USERNAME = "username";

    /** Token 中角色的声明键 */
    private static final String CLAIM_ROLE = "role";

    /** Token 中厂区权限范围的声明键（逗号分隔的电厂 ID 列表，如 "1,2,3"） */
    private static final String CLAIM_PLANT_SCOPE = "plantScope";

    /** 从配置注入的密钥（base64 或明文，至少 32 字节） */
    @Value("${jwt.secret:huadianguangdong-energy-monitor-default-secret-key-32bytes}")
    private String secret;

    /** 过期时间（毫秒），默认 2 小时 */
    @Value("${jwt.expire:7200000}")
    private long expire;

    /** 签名密钥对象 */
    private SecretKey key;

    @PostConstruct
    public void init() {
        // jjwt 0.12.x 要求 HS256 密钥至少 32 字节（256 bit）
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token
     *
     * @param userId     用户 ID
     * @param username   用户名
     * @param role       用户角色（ADMIN / PROD_SAFETY / PLANT_MANAGER / OPERATOR）
     * @param plantScope 厂区权限范围（逗号分隔的电厂 ID 列表，集团级传 null）
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username, String role, String plantScope) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expire);
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role != null ? role : "OPERATOR")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key);
        if (plantScope != null && !plantScope.isBlank()) {
            builder.claim(CLAIM_PLANT_SCOPE, plantScope);
        }
        return builder.compact();
    }

    /**
     * 生成 Token（兼容旧调用：仅 userId + username，默认 OPERATOR 角色）
     */
    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, "OPERATOR", null);
    }

    /**
     * 解析 Token，返回 Claims
     *
     * @param token JWT 字符串
     * @return Claims 声明集合
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验 Token 是否有效
     *
     * @param token JWT 字符串
     * @return true 有效；false 无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期：{}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Token 无效：{}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Token 为空或格式错误：{}", e.getMessage());
        }
        return false;
    }

    /**
     * 从 Token 中获取用户 ID
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get(CLAIM_USER_ID);
        if (userId instanceof Number num) {
            return num.longValue();
        }
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT 字符串
     * @return 用户名
     */
    public String getUsername(String token) {
        return parseToken(token).get(CLAIM_USERNAME, String.class);
    }

    /**
     * 从 Token 中获取用户角色
     *
     * @param token JWT 字符串
     * @return 角色编码（ADMIN / PROD_SAFETY / PLANT_MANAGER / OPERATOR）
     */
    public String getRole(String token) {
        String role = parseToken(token).get(CLAIM_ROLE, String.class);
        return role != null ? role : "OPERATOR";
    }

    /**
     * 从 Token 中获取厂区权限范围
     *
     * @param token JWT 字符串
     * @return 逗号分隔的电厂 ID 列表（如 "1,2,3"），集团级返回 null
     */
    public String getPlantScope(String token) {
        return parseToken(token).get(CLAIM_PLANT_SCOPE, String.class);
    }
}
