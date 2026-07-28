package com.huadianguangdong.gateway.security;

import lombok.Getter;

/**
 * 网关安全上下文信息（WebFlux）
 * <p>
 * 在 {@link com.huadianguangdong.gateway.filter.AuthGlobalFilter} 中解析 JWT 后，
 * 将用户权限信息写入 {@link org.springframework.web.server.ServerWebExchange} 的属性中，
 * 供后续的 {@code @PreAuthorize} 校验逻辑使用。
 * <p>
 * 同时通过请求头透传给下游微服务：
 * <ul>
 *   <li>{@code X-User-Id} — 用户 ID</li>
 *   <li>{@code X-Username} — 用户名</li>
 *   <li>{@code X-User-Role} — 角色编码</li>
 *   <li>{@code X-Plant-Scope} — 厂区权限范围</li>
 *   <li>{@code X-User-Permissions} — 权限编码列表（逗号分隔）</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@Getter
public class GatewaySecurityContext {

    /** Exchange 属性 Key */
    public static final String ATTR_SECURITY_CONTEXT = "gatewaySecurityContext";

    private final Long userId;
    private final String username;
    private final String role;
    private final String plantScope;
    private final String permissions;

    public GatewaySecurityContext(Long userId, String username,
                                 String role, String plantScope, String permissions) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.plantScope = plantScope;
        this.permissions = permissions;
    }

    /**
     * 判断用户是否拥有指定权限
     */
    public boolean hasPermission(String requiredPermission) {
        if (permissions == null || permissions.isBlank()) {
            return false;
        }
        // 集团管理员拥有所有权限
        if ("ADMIN".equals(role)) {
            return true;
        }
        String[] perms = permissions.split(",");
        for (String perm : perms) {
            if (perm.trim().equalsIgnoreCase(requiredPermission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断用户是否拥有指定角色
     */
    public boolean hasRole(String requiredRole) {
        if (role == null) {
            return false;
        }
        if ("ADMIN".equals(role)) {
            return true;
        }
        return role.equalsIgnoreCase(requiredRole);
    }

    /**
     * 判断用户是否有权访问指定电厂的数据
     */
    public boolean hasPlantAccess(Long plantId) {
        // 集团级角色可访问所有电厂
        if ("ADMIN".equals(role) || "PROD_SAFETY".equals(role)) {
            return true;
        }
        if (plantScope == null || plantScope.isBlank() || plantId == null) {
            return false;
        }
        for (String id : plantScope.split(",")) {
            try {
                if (Long.parseLong(id.trim()) == plantId) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }
}
