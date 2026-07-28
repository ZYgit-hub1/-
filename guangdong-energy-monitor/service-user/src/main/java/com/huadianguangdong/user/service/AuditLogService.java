package com.huadianguangdong.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huadianguangdong.user.entity.AuditLog;

/**
 * 审计日志服务接口
 *
 * @author huadianguangdong
 */
public interface AuditLogService extends IService<AuditLog> {

    /**
     * 记录审计日志
     *
     * @param userId   操作用户 ID
     * @param username 操作用户名
     * @param module   业务模块
     * @param action   操作动作
     * @param params   请求参数
     * @param ip       请求 IP
     * @param result   操作结果（success / fail）
     * @param costTime 耗时（ms）
     */
    void record(Long userId, String username, String module, String action,
                String params, String ip, String result, Integer costTime);
}
