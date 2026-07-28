package com.huadianguangdong.gateway.config;

import org.springdoc.core.properties.GroupedOpenApi;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger API 文档聚合配置（WebFlux 版）
 * <p>
 * 将所有下游微服务的 OpenAPI 接口聚合到网关的 Swagger UI 中，
 * 前端开发人员只需访问网关的 {@code /swagger-ui.html} 即可查看全部接口文档。
 * <p>
 * 聚合服务清单：
 * <ul>
 *   <li>user-service — 用户 / 认证 / 角色</li>
 *   <li>plant-service — 电厂 / 水文站</li>
 *   <li>alert-service — 告警 / 规则</li>
 *   <li>analysis-service — 统计 / 预测</li>
 *   <li>collector-service — 采集 / 气象</li>
 * </ul>
 * <p>
 * 每个服务组通过 {@code /v3/api-docs/{group}} 路径拉取下游 OpenAPI JSON，
 * 再由 Swagger UI 渲染。下游服务需在 Nacos 中注册并暴露 {@code /v3/api-docs} 端点。
 *
 * @author huadianguangdong
 */
@Configuration
public class SwaggerAggregationConfig {

    /** 下游服务列表（group-name → service-name 映射） */
    private static final List<ServiceApiGroup> SERVICE_GROUPS = List.of(
            new ServiceApiGroup("user-service", "用户/认证/权限", "user-service"),
            new ServiceApiGroup("plant-service", "电厂/水文站/GIS", "plant-service"),
            new ServiceApiGroup("alert-service", "告警/预警/规则", "alert-service"),
            new ServiceApiGroup("analysis-service", "统计分析/预测", "analysis-service"),
            new ServiceApiGroup("collector-service", "数据采集/气象", "collector-service")
    );

    /**
     * 注册各服务的 GroupedOpenApi Bean
     */
    @Bean
    public List<GroupedOpenApi> groupedOpenApis() {
        return SERVICE_GROUPS.stream()
                .map(group -> GroupedOpenApi.builder()
                        .group(group.groupName)
                        .pathsToMatch("/**")
                        .addOpenApiMethodFilter(method -> true)
                        .build())
                .toList();
    }

    /**
     * 配置 Swagger UI 下拉菜单，展示所有微服务分组
     */
    @Bean
    public SwaggerUiConfigParameters swaggerUiConfigParameters(SwaggerUiConfigProperties swaggerUiConfig) {
        SwaggerUiConfigParameters config = swaggerUiConfig.getConfigParameters();

        for (ServiceApiGroup group : SERVICE_GROUPS) {
            String url = "/v3/api-docs/" + group.groupName;
            config.addGroup(group.groupName);
            config.setUrls(List.of(
                    new org.springdoc.core.properties.SwaggerUiUrl(
                            group.displayName, url, group.serviceName
                    )
            ));
        }

        return config;
    }

    /**
     * 服务 API 分组描述
     */
    private record ServiceApiGroup(String groupName, String displayName, String serviceName) {
    }
}
