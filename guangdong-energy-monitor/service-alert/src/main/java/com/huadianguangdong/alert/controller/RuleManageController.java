package com.huadianguangdong.alert.controller;

import com.huadianguangdong.alert.config.DroolsConfig;
import com.huadianguangdong.common.api.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 规则管理 Controller
 * <p>
 * 提供 Drools 规则热加载触发与运行状态查询能力。<br>
 * 典型场景：运维人员修改 .drl 文件后，调用 {@code POST /api/rules/reload} 即时生效，无需重启应用。
 *
 * @author huadianguangdong
 */
@Slf4j
@Tag(name = "规则管理", description = "Drools 规则热加载、版本查询")
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleManageController {

    private final DroolsConfig droolsConfig;

    /**
     * 手动触发规则热加载
     * <p>
     * 重新扫描 .drl 文件（classpath + 外部目录）并编译，成功后原子替换 KieContainer。<br>
     * 编译失败时保留旧规则集，返回失败状态。
     *
     * @return 操作结果
     */
    @Operation(summary = "热加载规则文件", description = "重新编译并加载 .drl 文件，无需重启应用")
    @PostMapping("/reload")
    public R<Map<String, Object>> reload() {
        log.info("[规则管理] 收到热加载请求");
        boolean success = droolsConfig.reload();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("ruleVersion", droolsConfig.getRuleVersion());
        result.put("timestamp", System.currentTimeMillis());
        if (success) {
            return R.ok(result);
        }
        return R.fail("规则编译失败，请检查日志");
    }

    /**
     * 查询当前规则版本与运行状态
     *
     * @return 规则版本信息
     */
    @Operation(summary = "查询规则版本", description = "获取当前生效的规则版本标识")
    @GetMapping("/version")
    public R<Map<String, Object>> version() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ruleVersion", droolsConfig.getRuleVersion());
        result.put("timestamp", System.currentTimeMillis());
        return R.ok(result);
    }
}
