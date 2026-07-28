package com.huadianguangdong.gateway;

import com.huadianguangdong.common.entity.enums.PlantScopeAuthType;
import com.huadianguangdong.common.entity.enums.UserRole;
import com.huadianguangdong.common.security.PlantScopeAuth;
import com.huadianguangdong.common.util.JwtUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关服务启动类
 *
 * <p>说明：仅通过 {@link Import} 显式引入 Gateway 所需的 common 组件，
 * 而不整体扫描 common 包，以避免 common 模块中基于 Servlet 栈的组件
 * （如 GlobalExceptionHandler、RedisConfig、RedissonConfig）
 * 在 WebFlux 响应式环境下被误加载而引发冲突。</p>
 *
 * <p>引入的 common 组件：
 * <ul>
 *   <li>{@link JwtUtil} — JWT Token 解析/校验</li>
 *   <li>{@link UserRole} — 角色枚举（JWT 解析需要）</li>
 *   <li>{@link PlantScopeAuthType} — 权限类型枚举（过滤器工厂需要）</li>
 *   <li>{@link PlantScopeAuth} — 注解定义（下游服务引用需要）</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@SpringBootApplication
@EnableDiscoveryClient
@Import({JwtUtil.class, UserRole.class, PlantScopeAuthType.class, PlantScopeAuth.class})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
