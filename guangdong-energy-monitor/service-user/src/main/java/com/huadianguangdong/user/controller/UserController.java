package com.huadianguangdong.user.controller;

import com.huadianguangdong.common.api.PageResult;
import com.huadianguangdong.common.api.R;
import com.huadianguangdong.user.entity.SysUser;
import com.huadianguangdong.user.service.SysUserService;
import com.huadianguangdong.user.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理 Controller
 *
 * @author huadianguangdong
 */
@Tag(name = "用户管理", description = "用户 CRUD 与详情查询")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping
    public R<PageResult<SysUser>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String keyword) {
        return R.ok(sysUserService.listUsers(page, size, keyword));
    }

    @Operation(summary = "根据 ID 查询用户详情（含角色、权限）")
    @GetMapping("/{id}")
    public R<LoginVO> getById(@PathVariable Long id) {
        return R.ok(sysUserService.getUserInfo(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    public R<SysUser> create(@Valid @RequestBody SysUser user) {
        sysUserService.save(user);
        return R.ok(user);
    }

    @Operation(summary = "更新用户")
    @PutMapping
    public R<SysUser> update(@Valid @RequestBody SysUser user) {
        sysUserService.updateById(user);
        return R.ok(user);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sysUserService.removeById(id);
        return R.ok();
    }
}
