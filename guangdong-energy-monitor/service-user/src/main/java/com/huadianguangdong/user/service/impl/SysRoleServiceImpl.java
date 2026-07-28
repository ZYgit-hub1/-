package com.huadianguangdong.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huadianguangdong.user.entity.SysRole;
import com.huadianguangdong.user.mapper.SysRoleMapper;
import com.huadianguangdong.user.service.SysRoleService;
import org.springframework.stereotype.Service;

/**
 * 系统角色服务实现
 *
 * @author huadianguangdong
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
}
