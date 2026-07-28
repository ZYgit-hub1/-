package com.huadianguangdong.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huadianguangdong.user.entity.SysPermission;

import java.util.List;

/**
 * 系统权限服务接口
 *
 * @author huadianguangdong
 */
public interface SysPermissionService extends IService<SysPermission> {

    /**
     * 根据用户 ID 查询权限编码列表
     *
     * @param userId 用户 ID
     * @return 权限编码集合
     */
    List<String> listByUserId(Long userId);
}
