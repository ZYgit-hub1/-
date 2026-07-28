package com.huadianguangdong.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huadianguangdong.common.api.R;
import com.huadianguangdong.common.api.ResultCode;
import com.huadianguangdong.common.entity.enums.PlantScopeAuthType;
import com.huadianguangdong.common.entity.enums.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 厂区权限校验 GatewayFilterFactory
 * <p>
 * 在路由配置中通过 {@code filters: - PlantScopeAuth=PLANT_SCOPE} 引用。
 * 支持参数：
 * <ul>
 *   <li>{@code LOGIN_ONLY} — 仅校验登录（已由 AuthGlobalFilter 处理，此处直接放行）</li>
 *   <li>{@code PLANT_SCOPE} — 校验厂区权限（PLANT_MANAGER/OPERATOR 需要 plant_scope 包含目标电厂）</li>
 *   <li>{@code GROUP_ONLY} — 仅集团级角色（ADMIN/PROD_SAFETY）可访问</li>
 * </ul>
 * <p>
 * 路由配置示例：
 * <pre>
 * spring:
 *   cloud:
 *     gateway:
 *       routes:
 *         - id: plant-service
 *           uri: lb://plant-service
 *           predicates:
 *             - Path=/api/plants/**
 *           filters:
 *             - PlantScopeAuth=PLANT_SCOPE
 * </pre>
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
public class PlantScopeAuthGatewayFilterFactory
        extends AbstractGatewayFilterFactory<PlantScopeAuthGatewayFilterFactory.Config> {

    /** URL 路径中提取电厂 ID 的正则：/(\d+)/ 格式 */
    private static final Pattern PLANT_ID_PATTERN = Pattern.compile("/(\\d+)(?=/|$)");

    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String HEADER_PLANT_SCOPE = "X-Plant-Scope";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlantScopeAuthGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            String role = request.getHeaders().getFirst(HEADER_USER_ROLE);
            String plantScope = request.getHeaders().getFirst(HEADER_PLANT_SCOPE);

            UserRole userRole = UserRole.fromCode(role);

            // LOGIN_ONLY：AuthGlobalFilter 已完成鉴权，直接放行
            if (config.authType == PlantScopeAuthType.LOGIN_ONLY) {
                return chain.filter(exchange);
            }

            // GROUP_ONLY：仅集团级角色可访问
            if (config.authType == PlantScopeAuthType.GROUP_ONLY) {
                if (userRole != null && userRole.isGlobalScope()) {
                    return chain.filter(exchange);
                }
                log.warn("[厂区权限] 非集团级角色访问集团接口 role={} path={}", role, path);
                return forbidden(exchange, "当前角色无集团级数据权限");
            }

            // PLANT_SCOPE：校验厂区数据权限
            if (config.authType == PlantScopeAuthType.PLANT_SCOPE) {
                // 集团级角色跳过厂区校验
                if (userRole != null && userRole.isGlobalScope()) {
                    return chain.filter(exchange);
                }

                // 需要校验 plant_scope
                if (!StringUtils.hasText(plantScope)) {
                    log.warn("[厂区权限] 用户无厂区权限范围 role={} path={}", role, path);
                    return forbidden(exchange, "当前用户未分配厂区权限");
                }

                // 从 URL 提取电厂 ID
                Set<String> allowedPlants = parsePlantScope(plantScope);
                Long targetPlantId = extractPlantIdFromPath(path);

                if (targetPlantId == null) {
                    // 路径中无电厂 ID，仅校验是否有厂区权限即可
                    return chain.filter(exchange);
                }

                if (allowedPlants.contains(String.valueOf(targetPlantId))) {
                    return chain.filter(exchange);
                }

                log.warn("[厂区权限] 厂区权限不匹配 role={} plantScope={} targetPlantId={} path={}",
                        role, plantScope, targetPlantId, path);
                return forbidden(exchange, "无权访问电厂[" + targetPlantId + "]的数据");
            }

            return chain.filter(exchange);
        };
    }

    /**
     * 从请求头 plantScope 字符串解析为 Set
     */
    private Set<String> parsePlantScope(String plantScope) {
        Set<String> plants = new HashSet<>();
        if (!StringUtils.hasText(plantScope)) {
            return plants;
        }
        for (String id : plantScope.split(",")) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                plants.add(trimmed);
            }
        }
        return plants;
    }

    /**
     * 从 URL 路径中提取电厂 ID（第一个数字段）
     * <p>
     * 例：/api/plants/3/detail → 3
     * 例：/api/plants/list → null（列表接口无需电厂级校验）
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
    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
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
        // 路由配置中的简写参数名
        return Arrays.asList("authType");
    }

    /**
     * 过滤器配置
     */
    public static class Config {
        /** 权限类型 */
        private PlantScopeAuthType authType = PlantScopeAuthType.LOGIN_ONLY;

        public PlantScopeAuthType getAuthType() {
            return authType;
        }

        public void setAuthType(PlantScopeAuthType authType) {
            this.authType = authType;
        }
    }
}
