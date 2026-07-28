package com.huadianguangdong.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huadianguangdong.user.entity.SysPermission;
import com.huadianguangdong.user.mapper.SysPermissionMapper;
import com.huadianguangdong.user.service.SysPermissionService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统权限服务实现
 *
 * @author huadianguangdong
 */
@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements SysPermissionService {

    @Override
    public List<String> listByUserId(Long userId) {
        // TODO 当前为简化实现（查询全部权限编码），实际应通过 t_sys_user_role / t_sys_role_permission 关联表查询
        List<SysPermission> permissions = this.list();
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        return permissions.stream().map(SysPermission::getCode).collect(Collectors.toList());
    }
}
