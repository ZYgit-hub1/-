package com.huadianguangdong.alert.controller;

import com.huadianguangdong.alert.entity.Alarm;
import com.huadianguangdong.alert.service.AlarmService;
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
 * 报警 Controller
 *
 * @author huadianguangdong
 */
@Tag(name = "报警管理", description = "报警的查询、确认、解除")
@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

    @Autowired
    private AlarmService alarmService;

    @Operation(summary = "分页查询报警")
    @GetMapping
    public R<PageResult<Alarm>> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) String level,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) Long plantId,
                                     @RequestParam(required = false) String startTime,
                                     @RequestParam(required = false) String endTime) {
        return R.ok(alarmService.page(page, size, level, status, plantId, startTime, endTime));
    }

    @Operation(summary = "根据 ID 查询报警")
    @GetMapping("/{id}")
    public R<Alarm> getById(@PathVariable Long id) {
        return R.ok(alarmService.getById(id));
    }

    @Operation(summary = "确认报警")
    @PutMapping("/{id}/confirm")
    public R<Alarm> confirm(@PathVariable Long id,
                            @RequestParam String handler) {
        return R.ok(alarmService.confirm(id, handler));
    }

    @Operation(summary = "解除报警")
    @PutMapping("/{id}/resolve")
    public R<Alarm> resolve(@PathVariable Long id,
                            @RequestParam String handler,
                            @RequestParam(required = false) String remark) {
        return R.ok(alarmService.resolve(id, handler, remark));
    }
}
