package com.huadianguangdong.user.controller;

import com.huadianguangdong.common.api.R;
import com.huadianguangdong.user.entity.SysOrg;
import com.huadianguangdong.user.service.SysOrgService;
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
 * 组织架构管理 Controller
 *
 * @author huadianguangdong
 */
@Tag(name = "组织架构管理", description = "组织 CRUD 与树形结构")
@RestController
@RequestMapping("/api/orgs")
@RequiredArgsConstructor
public class OrgController {

    private final SysOrgService sysOrgService;

    @Operation(summary = "查询全部组织（平铺列表）")
    @GetMapping
    public R<List<SysOrg>> list() {
        return R.ok(sysOrgService.list());
    }

    @Operation(summary = "查询组织树")
    @GetMapping("/tree")
    public R<List<SysOrg>> tree() {
        return R.ok(sysOrgService.tree());
    }

    @Operation(summary = "根据 ID 查询组织")
    @GetMapping("/{id}")
    public R<SysOrg> getById(@PathVariable Long id) {
        return R.ok(sysOrgService.getById(id));
    }

    @Operation(summary = "创建组织")
    @PostMapping
    public R<SysOrg> create(@Valid @RequestBody SysOrg org) {
        sysOrgService.save(org);
        return R.ok(org);
    }

    @Operation(summary = "更新组织")
    @PutMapping
    public R<SysOrg> update(@Valid @RequestBody SysOrg org) {
        sysOrgService.updateById(org);
        return R.ok(org);
    }

    @Operation(summary = "删除组织")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sysOrgService.removeById(id);
        return R.ok();
    }
}
