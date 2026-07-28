package com.huadianguangdong.gateway.filter;

import com.huadianguangdong.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;

/**
 * JWT 滑动续期过滤器
 * <p>
 * 在 {@link AuthGlobalFilter}（order=-100）之后执行（order=-90），
 * 对已通过鉴权的请求检查 token 剩余有效期。
 * 当剩余有效期不足总有效期的 {@code refreshThreshold}（默认 20%）时，
 * 自动生成新 token 并通过响应头 {@code X-New-Token} 返回前端。
 * <p>
 * 前端检测到 {@code X-New-Token} 响应头时，应替换本地存储的 token，
 * 实现"无感续期"——用户在正常使用过程中不会因 token 过期而中断操作。
 * <p>
 * 设计决策：
 * <ul>
 *   <li>不修改 Redis 中的 token 缓存（由 AuthController.refreshToken 显式刷新）</li>
 *   <li>续期阈值可通过配置 {@code gateway.jwt.refresh-threshold} 调整</li>
 *   <li>仅对有效 token 续期，过期/无效 token 由 AuthGlobalFilter 拦截</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAutoRefreshFilter implements GlobalFilter, Ordered {

    /** 续期阈值：剩余有效期不足此比例时触发续期（0.0 - 1.0） */
    private static final double DEFAULT_REFRESH_THRESHOLD = 0.2;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_NEW_TOKEN = "X-New-Token";

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            Claims claims = jwtUtil.parseToken(token);
            Date expiration = claims.getExpiration();
            Date issuedAt = claims.getIssuedAt();
            Date now = new Date();

            if (expiration == null || issuedAt == null) {
                return chain.filter(exchange);
            }

            long totalMillis = expiration.getTime() - issuedAt.getTime();
            long remainingMillis = expiration.getTime() - now.getTime();

            if (remainingMillis <= 0) {
                // 已过期，不应到达此处（AuthGlobalFilter 已拦截）
                return chain.filter(exchange);
            }

            double remainingRatio = (double) remainingMillis / totalMillis;

            // 剩余有效期不足阈值时触发续期
            if (remainingRatio < DEFAULT_REFRESH_THRESHOLD) {
                Long userId = jwtUtil.getUserId(token);
                String username = jwtUtil.getUsername(token);
                String role = jwtUtil.getRole(token);
                String plantScope = jwtUtil.getPlantScope(token);

                String newToken = jwtUtil.generateToken(userId, username, role, plantScope);

                log.info("[JWT续期] userId={} username={} 剩余比例={}",
                        userId, username, String.format("%.2f", remainingRatio));

                // 通过响应头返回新 token
                ServerHttpResponse response = exchange.getResponse();
                response.getHeaders().add(HEADER_NEW_TOKEN, newToken);
            }
        } catch (Exception e) {
            // 续期失败不影响正常请求
            log.debug("[JWT续期] 续期检查异常，跳过: {}", e.getMessage());
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 在 AuthGlobalFilter(-100) 之后执行
        return -90;
    }
}
