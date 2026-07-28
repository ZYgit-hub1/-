package com.huadianguangdong.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huadianguangdong.common.api.R;
import com.huadianguangdong.common.api.ResultCode;
import com.huadianguangdong.common.util.JwtUtil;
import com.huadianguangdong.gateway.security.GatewaySecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 全局 JWT 鉴权过滤器
 * <p>
 * 职责：
 * <ol>
 *   <li>拦截所有请求，校验 Bearer Token</li>
 *   <li>解析用户信息（userId / username / role / plantScope）</li>
 *   <li>将用户信息写入请求头透传给下游微服务</li>
 * </ol>
 * <p>
 * 透传头清单：
 * <ul>
 *   <li>{@code X-User-Id} — 用户 ID</li>
 *   <li>{@code X-Username} — 用户名</li>
 *   <li>{@code X-User-Role} — 角色编码（ADMIN / PROD_SAFETY / PLANT_MANAGER / OPERATOR）</li>
 *   <li>{@code X-Plant-Scope} — 厂区权限范围（逗号分隔的电厂 ID 列表）</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 白名单路径（无需鉴权） */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/refreshToken",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs",
            "/webjars/**",
            "/actuator/**",
            "/favicon.ico"
    );

    /** Authorization 头前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    // ==================== 透传下游的请求头 ====================
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String HEADER_PLANT_SCOPE = "X-Plant-Scope";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 2. 获取 Authorization 头
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, ResultCode.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        // 3. 同步校验 token（JwtUtil 方法为同步，此处直接调用）
        if (!jwtUtil.validateToken(token)) {
            return unauthorized(exchange, ResultCode.TOKEN_EXPIRED);
        }

        // 4. 解析用户完整信息
        Long userId;
        String username;
        String role;
        String plantScope;
        try {
            userId = jwtUtil.getUserId(token);
            username = jwtUtil.getUsername(token);
            role = jwtUtil.getRole(token);
            plantScope = jwtUtil.getPlantScope(token);
        } catch (Exception e) {
            log.warn("[网关鉴权] 解析 Token 用户信息失败：{}", e.getMessage());
            return unauthorized(exchange, ResultCode.UNAUTHORIZED);
        }

        // 5. 构建安全上下文（供 PreAuthorize / AuditLog / PlantScopeAuth 使用）
        GatewaySecurityContext securityContext = new GatewaySecurityContext(
                userId, username, role, plantScope, null);

        // 6. 将用户信息写入请求头，透传给下游微服务
        ServerHttpRequest.Builder requestMutator = request.mutate()
                .header(HEADER_USER_ID, String.valueOf(userId))
                .header(HEADER_USERNAME, username == null ? "" : username)
                .header(HEADER_USER_ROLE, role != null ? role : "OPERATOR");

        if (StringUtils.hasText(plantScope)) {
            requestMutator.header(HEADER_PLANT_SCOPE, plantScope);
        }

        log.debug("[网关鉴权] userId={} username={} role={} plantScope={} path={}",
                userId, username, role, plantScope, path);

        // 将安全上下文写入 Exchange 属性，供后续过滤器使用
        var mutatedExchange = exchange.mutate()
                .request(requestMutator.build())
                .attribute(GatewaySecurityContext.ATTR_SECURITY_CONTEXT, securityContext)
                .build();

        return chain.filter(mutatedExchange);
    }

    /**
     * 判断路径是否命中白名单
     */
    private boolean isWhiteListed(String path) {
        for (String pattern : WHITE_LIST) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回未认证 JSON 响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, ResultCode resultCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        R<Void> body = R.fail(resultCode);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            log.error("[网关鉴权] 序列化 401 响应体失败", e);
            bytes = "{\"code\":401,\"msg\":\"未认证或认证失效\",\"data\":null}"
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
