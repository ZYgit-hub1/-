package com.huadianguangdong.alert.fact;

import com.huadianguangdong.common.dto.AlertEventDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 报警事件收集器（Drools global）
 * <p>
 * 规则 RHS 通过 {@code alertCollector.add(event)} 收集匹配结果，
 * 规则执行完毕后由 RuleEngineService 统一取出进行抑制处理与持久化。
 *
 * @author huadianguangdong
 */
@Data
@NoArgsConstructor
public class AlertEventCollector implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 收集的报警事件列表 */
    private final List<AlertEventDTO> events = new ArrayList<>();

    /**
     * 添加报警事件（供 .drl RHS 调用）
     */
    public void add(AlertEventDTO event) {
        if (event != null) {
            events.add(event);
        }
    }

    /**
     * 是否有报警事件
     */
    public boolean hasEvents() {
        return !events.isEmpty();
    }

    /**
     * 清空收集器
     */
    public void clear() {
        events.clear();
    }
}
