package com.huadianguangdong.alert.controller;

import com.huadianguangdong.alert.entity.Warning;
import com.huadianguangdong.alert.service.WarningService;
import com.huadianguangdong.common.api.PageResult;
import com.huadianguangdong.common.api.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预警 Controller
 *
 * @author huadianguangdong
 */
@Tag(name = "预警管理", description = "预警的查询、取消")
@RestController
@RequestMapping("/api/warnings")
public class WarningController {

    @Autowired
    private WarningService warningService;

    @Operation(summary = "分页查询预警")
    @GetMapping
    public R<PageResult<Warning>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String level,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) Long plantId) {
        return R.ok(warningService.page(page, size, level, type, status, plantId));
    }

    @Operation(summary = "根据 ID 查询预警")
    @GetMapping("/{id}")
    public R<Warning> getById(@PathVariable Long id) {
        return R.ok(warningService.getById(id));
    }

    @Operation(summary = "取消预警")
    @PutMapping("/{id}/cancel")
    public R<Warning> cancel(@PathVariable Long id) {
        return R.ok(warningService.cancel(id));
    }
}
