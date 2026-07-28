package com.huadianguangdong.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huadianguangdong.user.entity.AuditLog;
import com.huadianguangdong.user.mapper.AuditLogMapper;
import com.huadianguangdong.user.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审计日志服务实现
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditLogService {

    @Async
    @Override
    public void record(Long userId, String username, String module, String action,
                       String params, String ip, String result, Integer costTime) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setModule(module);
            auditLog.setAction(action);
            auditLog.setParams(params);
            auditLog.setIp(ip);
            auditLog.setResult(result);
            auditLog.setCostTime(costTime);
            auditLog.setCreateTime(LocalDateTime.now());
            this.save(auditLog);
        } catch (Exception e) {
            // 审计日志失败不影响主流程
            log.error("记录审计日志失败：userId={}, module={}, action={}", userId, module, action, e);
        }
    }
}
