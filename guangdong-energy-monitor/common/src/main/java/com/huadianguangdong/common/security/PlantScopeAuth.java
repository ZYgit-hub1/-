package com.huadianguangdong.common.security;

import com.huadianguangdong.common.entity.enums.PlantScopeAuthType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 电厂数据权限注解
 * <p>
 * 标注在 Controller 方法上，声明该接口的数据权限要求。
 * Gateway 的 {@code PlantScopeAuthGatewayFilterFactory} 会在路由配置中
 * 拦截标注了此注解的路径，根据用户 role 和 plant_scope 校验数据权限。
 * <p>
 * 使用示例：
 * <pre>
 *     // 仅需登录
 *     &#64;PlantScopeAuth(PlantScopeAuthType.LOGIN_ONLY)
 *     &#64;GetMapping("/api/plants/list")
 *     public R&lt;List&lt;PlantDTO&gt;&gt; list() { ... }
 *
 *     // 需要集团级权限
 *     &#64;PlantScopeAuth(PlantScopeAuthType.GROUP_ONLY)
 *     &#64;GetMapping("/api/stats/dashboard")
 *     public R&lt;DashboardVO&gt; dashboard() { ... }
 * </pre>
 * <p>
 * 注意：此注解在网关层生效，下游微服务通过 {@code X-User-Role} / {@code X-Plant-Scope}
 * 请求头获取已鉴权的用户信息，无需重复校验。
 *
 * @author huadianguangdong
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlantScopeAuth {

    /**
     * 权限类型，默认 LOGIN_ONLY（仅校验登录状态）
     */
    PlantScopeAuthType value() default PlantScopeAuthType.LOGIN_ONLY;

    /**
     * 是否需要校验 plant_scope 与路径中的电厂 ID 匹配。
     * <p>
     * 当 {@code checkPlantMatch = true} 时，网关会从 URL 路径中提取
     *电厂 ID（/{plantId}/ 格式），并与用户的 plant_scope 进行匹配校验。
     * <p>
     * 仅对 PLANT_MANAGER / OPERATOR 角色生效。
     */
    boolean checkPlantMatch() default false;
}
