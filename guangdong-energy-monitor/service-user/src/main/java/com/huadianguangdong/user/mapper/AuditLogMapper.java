package com.huadianguangdong.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huadianguangdong.user.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper
 *
 * @author huadianguangdong
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
