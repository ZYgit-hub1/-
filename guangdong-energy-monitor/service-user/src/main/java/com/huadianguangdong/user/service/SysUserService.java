package com.huadianguangdong.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huadianguangdong.common.api.PageResult;
import com.huadianguangdong.user.entity.SysUser;
import com.huadianguangdong.user.vo.LoginVO;

/**
 * 系统用户服务接口
 *
 * @author huadianguangdong
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录返回信息（含 token、用户、角色、权限）
     */
    LoginVO login(String username, String password);

    /**
     * 获取用户信息（含角色、权限）
     *
     * @param userId 用户 ID
     * @return 登录返回信息（不含 token）
     */
    LoginVO getUserInfo(Long userId);

    /**
     * 分页查询用户列表
     *
     * @param page    当前页码
     * @param size    每页大小
     * @param keyword 关键词（用户名/真实姓名模糊匹配）
     * @return 分页结果
     */
    PageResult<SysUser> listUsers(int page, int size, String keyword);
}
