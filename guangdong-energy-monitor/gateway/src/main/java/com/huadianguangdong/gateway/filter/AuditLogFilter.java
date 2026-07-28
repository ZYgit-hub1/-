package com.huadianguangdong.gateway.filter;

import com.huadianguangdong.gateway.security.GatewaySecurityContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 网关访问审计日志过滤器
 * <p>
 * 在所有请求处理完成后（post-filter），记录结构化审计日志并注册 Micrometer 指标。
 * <p>
 * 审计日志内容包括：
 * <ul>
 *   <li>时间戳</li>
 *   <li>用户 ID / 用户名 / 角色</li>
 *   <li>请求方法 / 路径</li>
 *   <li>客户端 IP</li>
 *   <li>响应状态码</li>
 *   <li>处理耗时（ms）</li>
 * </ul>
 * <p>
 * Micrometer 指标：
 * <ul>
 *   <li>{@code gateway.request.duration} — Timer：请求耗时（tag: method, path, status）</li>
 *   <li>{@code gateway.request.count} — Counter：请求计数（tag: method, status）</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogFilter implements GlobalFilter, Ordered {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MeterRegistry meterRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        String path = request.getURI().getPath();
        String clientIp = getClientIp(request);
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    ServerHttpResponse response = exchange.getResponse();
                    int status = response.getStatusCode() != null
                            ? response.getStatusCode().value() : 0;

                    GatewaySecurityContext ctx = exchange.getAttribute(
                            GatewaySecurityContext.ATTR_SECURITY_CONTEXT);

                    String userId = ctx != null ? String.valueOf(ctx.getUserId()) : "-";
                    String username = ctx != null ? ctx.getUsername() : "-";
                    String role = ctx != null ? ctx.getRole() : "-";

                    // 记录审计日志（INFO 级别，方便日志采集系统收集）
                    log.info("[网关审计] time={} userId={} username={} role={} ip={} method={} path={} status={} duration={}ms",
                            LocalDateTime.now().format(TS_FMT),
                            userId, username, role,
                            clientIp,
                            method != null ? method.name() : "-",
                            path,
                            status,
                            duration);

                    // 记录 Micrometer 指标
                    recordMetrics(method, path, status, duration);
                }));
    }

    /**
     * 记录请求指标
     */
    private void recordMetrics(HttpMethod method, String path, int status, long durationMs) {
        Timer.builder("gateway.request.duration")
                .tag("method", method != null ? method.name() : "unknown")
                .tag("path", normalizePath(path))
                .tag("status", String.valueOf(status))
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);

        meterRegistry.counter("gateway.request.count",
                "method", method != null ? method.name() : "unknown",
                "status", String.valueOf(status)
        ).increment();
    }

    /**
     * 路径归一化：将数字段替换为占位符（如 /api/plants/3 → /api/plants/{id}）
     */
    private String normalizePath(String path) {
        if (path == null) {
            return "unknown";
        }
        return path.replaceAll("/\\d+(?=/|$)", "/{id}");
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeaders().getFirst("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() {
        // 最后执行：在 AuthGlobalFilter(-100) 和所有路由 Filter 之后
        return Ordered.LOWEST_PRECEDENCE;
    }
}
