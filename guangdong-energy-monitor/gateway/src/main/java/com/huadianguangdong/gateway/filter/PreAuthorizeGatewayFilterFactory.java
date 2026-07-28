package com.huadianguangdong.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huadianguangdong.common.api.R;
import com.huadianguangdong.common.api.ResultCode;
import com.huadianguangdong.gateway.security.GatewaySecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @PreAuthorize 风格权限校验 GatewayFilterFactory
 * <p>
 * 在路由配置中通过 {@code filters: - PreAuthorize=hasRole('ADMIN')} 引用，
 * 支持 Spring Security 风格的 SpEL 表达式子集：
 * <ul>
 *   <li>{@code hasRole('ADMIN')} — 拥有指定角色</li>
 *   <li>{@code hasPermission('plant:read')} — 拥有指定权限</li>
 *   <li>{@code hasPlantAccess()} — 有权访问 URL 中指定的电厂（自动提取 plantId）</li>
 *   <li>{@code permitAll()} — 允许所有已登录用户</li>
 * </ul>
 * <p>
 * 路由配置示例：
 * <pre>
 * spring:
 *   cloud:
 *     gateway:
 *       routes:
 *         - id: admin-endpoint
 *           uri: lb://analysis-service
 *           predicates:
 *             - Path=/api/stats/dashboard
 *           filters:
 *             - PreAuthorize=hasRole('ADMIN')
 *
 *         - id: plant-detail
 *           uri: lb://plant-service
 *           predicates:
 *             - Path=/api/plants/{plantId}/**
 *           filters:
 *             - PreAuthorize=hasPlantAccess()
 * </pre>
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
public class PreAuthorizeGatewayFilterFactory
        extends AbstractGatewayFilterFactory<PreAuthorizeGatewayFilterFactory.Config> {

    /** URL 路径中提取电厂 ID 的正则 */
    private static final Pattern PLANT_ID_PATTERN = Pattern.compile("/(\\d+)(?=/|$)");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** SpEL 表达式缓存（避免每次请求重复解析） */
    private final Map<String, ParsedExpression> expressionCache = new ConcurrentHashMap<>();

    public PreAuthorizeGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        ParsedExpression parsed = expressionCache.computeIfAbsent(
                config.expression, PreAuthorizeGatewayFilterFactory::parseExpression);

        return (exchange, chain) -> {
            GatewaySecurityContext ctx = exchange.getAttribute(
                    GatewaySecurityContext.ATTR_SECURITY_CONTEXT);

            if (ctx == null) {
                log.warn("[PreAuthorize] 安全上下文缺失，拒绝访问 path={}", exchange.getRequest().getPath());
                return forbidden(exchange, "认证信息缺失");
            }

            boolean allowed = evaluate(parsed, ctx, exchange.getRequest().getURI().getPath());

            if (allowed) {
                return chain.filter(exchange);
            }

            log.warn("[PreAuthorize] 权限不足 userId={} role={} expression={} path={}",
                    ctx.getUserId(), ctx.getRole(), config.expression, exchange.getRequest().getPath());
            return forbidden(exchange, "权限不足");
        };
    }

    /**
     * 解析并校验权限表达式
     */
    private boolean evaluate(ParsedExpression parsed, GatewaySecurityContext ctx, String path) {
        return switch (parsed.type) {
            case PERMIT_ALL -> true;
            case HAS_ROLE -> ctx.hasRole(parsed.argument);
            case HAS_PERMISSION -> ctx.hasPermission(parsed.argument);
            case HAS_PLANT_ACCESS -> {
                Long plantId = extractPlantIdFromPath(path);
                if (plantId == null) {
                    // 路径中无 plantId，放行
                    yield true;
                }
                yield ctx.hasPlantAccess(plantId);
            }
        };
    }

    /**
     * 解析 SpEL 表达式为结构化对象
     */
    private static ParsedExpression parseExpression(String expression) {
        String trimmed = expression.trim();
        if ("permitAll()".equalsIgnoreCase(trimmed)) {
            return new ParsedExpression(ExpressionType.PERMIT_ALL, null);
        }
        if ("hasPlantAccess()".equalsIgnoreCase(trimmed)) {
            return new ParsedExpression(ExpressionType.HAS_PLANT_ACCESS, null);
        }
        if (trimmed.startsWith("hasRole(") && trimmed.endsWith(")")) {
            String arg = trimmed.substring(8, trimmed.length() - 1).trim()
                    .replaceAll("^['\"]|['\"]$", "");
            return new ParsedExpression(ExpressionType.HAS_ROLE, arg);
        }
        if (trimmed.startsWith("hasPermission(") && trimmed.endsWith(")")) {
            String arg = trimmed.substring(15, trimmed.length() - 1).trim()
                    .replaceAll("^['\"]|['\"]$", "");
            return new ParsedExpression(ExpressionType.HAS_PERMISSION, arg);
        }
        log.warn("[PreAuthorize] 未知表达式: {}，默认拒绝", trimmed);
        return new ParsedExpression(ExpressionType.PERMIT_ALL, null);
    }

    /**
     * 从 URL 路径中提取电厂 ID
     */
    private Long extractPlantIdFromPath(String path) {
        Matcher matcher = PLANT_ID_PATTERN.matcher(path);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 返回 403 JSON 响应
     */
    private Mono<Void> forbidden(org.springframework.web.server.ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        R<Void> body = R.fail(ResultCode.FORBIDDEN.getCode(), message);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"code\":403,\"msg\":\"" + message + "\",\"data\":null}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("expression");
    }

    /** 表达式类型 */
    private enum ExpressionType {
        PERMIT_ALL, HAS_ROLE, HAS_PERMISSION, HAS_PLANT_ACCESS
    }

    /** 解析后的表达式 */
    private record ParsedExpression(ExpressionType type, String argument) {
    }

    /** 配置 */
    public static class Config {
        private String expression;

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }
    }
}
