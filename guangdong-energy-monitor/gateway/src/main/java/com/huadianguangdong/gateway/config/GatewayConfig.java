package com.huadianguangdong.gateway.config;

import org.springframework.context.annotation.Configuration;

/**
 * 网关路由配置类
 *
 * <p>说明：网关的路由规则统一在 {@code application.yml} 的
 * {@code spring.cloud.gateway.routes} 节点中声明式配置，便于动态调整与 Nacos 下发，
 * 此处保留空实现仅作为配置占位与扩展入口。</p>
 *
 * <p>如未来需要以 Java 代码方式注册路由，可在此注入
 * {@link org.springframework.cloud.gateway.route.RouteLocator} Bean，
 * 通过 {@code RouteLocatorBuilder} 构建路由链。</p>
 */
@Configuration
public class GatewayConfig {
    // 路由在 application.yml 中定义，无需在此编码
}
