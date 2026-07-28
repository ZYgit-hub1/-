package com.huadianguangdong.user.controller;

import com.huadianguangdong.common.api.R;
import com.huadianguangdong.user.entity.SysRole;
import com.huadianguangdong.user.service.SysRoleService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理 Controller
 *
 * @author huadianguangdong
 */
@Tag(name = "角色管理", description = "角色 CRUD")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleService sysRoleService;

    @Operation(summary = "查询全部角色")
    @GetMapping
    public R<List<SysRole>> list() {
        return R.ok(sysRoleService.list());
    }

    @Operation(summary = "根据 ID 查询角色")
    @GetMapping("/{id}")
    public R<SysRole> getById(@PathVariable Long id) {
        return R.ok(sysRoleService.getById(id));
    }

    @Operation(summary = "创建角色")
    @PostMapping
    public R<SysRole> create(@Valid @RequestBody SysRole role) {
        sysRoleService.save(role);
        return R.ok(role);
    }

    @Operation(summary = "更新角色")
    @PutMapping
    public R<SysRole> update(@Valid @RequestBody SysRole role) {
        sysRoleService.updateById(role);
        return R.ok(role);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sysRoleService.removeById(id);
        return R.ok();
    }
}
