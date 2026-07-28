package com.huadianguangdong.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huadianguangdong.common.api.PageResult;
import com.huadianguangdong.common.api.ResultCode;
import com.huadianguangdong.common.constant.CommonConstants;
import com.huadianguangdong.common.exception.BusinessException;
import com.huadianguangdong.common.util.JwtUtil;
import com.huadianguangdong.common.util.RedisUtil;
import com.huadianguangdong.user.entity.SysPermission;
import com.huadianguangdong.user.entity.SysRole;
import com.huadianguangdong.user.entity.SysUser;
import com.huadianguangdong.user.mapper.SysUserMapper;
import com.huadianguangdong.user.service.SysPermissionService;
import com.huadianguangdong.user.service.SysRoleService;
import com.huadianguangdong.user.service.SysUserService;
import com.huadianguangdong.user.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 系统用户服务实现
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final SysRoleService sysRoleService;
    private final SysPermissionService sysPermissionService;

    @Override
    public LoginVO login(String username, String password) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名或密码不能为空");
        }
        // 根据用户名查询用户
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        // TODO 此处应使用 BCryptPasswordEncoder 校验密码，简化实现以明文/占位校验
        if (!password.equals(user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        // 查询角色与权限
        List<String> roles = listRoleCodesByUserId(user.getId());
        List<String> permissions = sysPermissionService.listByUserId(user.getId());

        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 缓存用户信息至 Redis
        String userKey = CommonConstants.REDIS_USER_PREFIX + user.getId();
        redisUtil.setEx(userKey, user.getUsername(), 2, TimeUnit.HOURS);
        String tokenKey = CommonConstants.REDIS_TOKEN_PREFIX + user.getId();
        redisUtil.setEx(tokenKey, token, 2, TimeUnit.HOURS);

        // 组装返回
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRoles(roles);
        vo.setPermissions(permissions);
        return vo;
    }

    @Override
    public LoginVO getUserInfo(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户 ID 不能为空");
        }
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        List<String> roles = listRoleCodesByUserId(user.getId());
        List<String> permissions = sysPermissionService.listByUserId(user.getId());

        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRoles(roles);
        vo.setPermissions(permissions);
        return vo;
    }

    @Override
    public PageResult<SysUser> listUsers(int page, int size, String keyword) {
        if (page <= 0) {
            page = CommonConstants.DEFAULT_PAGE;
        }
        if (size <= 0) {
            size = CommonConstants.DEFAULT_SIZE;
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getRealName, keyword);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> pageResult = this.page(new Page<>(page, size), wrapper);
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(),
                (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    /**
     * 根据用户 ID 查询角色编码列表
     * <p>
     * TODO 当前为简化实现（查询全部角色），实际应通过 t_sys_user_role 关联表查询。
     *
     * @param userId 用户 ID
     * @return 角色编码集合
     */
    private List<String> listRoleCodesByUserId(Long userId) {
        List<SysRole> roles = sysRoleService.list();
        if (CollUtil.isEmpty(roles)) {
            return Collections.emptyList();
        }
        return roles.stream().map(SysRole::getCode).collect(Collectors.toList());
    }
}
