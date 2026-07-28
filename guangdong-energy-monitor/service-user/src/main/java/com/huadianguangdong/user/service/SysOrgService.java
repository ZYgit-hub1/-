package com.huadianguangdong.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huadianguangdong.user.entity.SysOrg;

import java.util.List;

/**
 * 系统组织架构服务接口
 *
 * @author huadianguangdong
 */
public interface SysOrgService extends IService<SysOrg> {

    /**
     * 构建组织树
     *
     * @return 组织树（根节点列表）
     */
    List<SysOrg> tree();
}
