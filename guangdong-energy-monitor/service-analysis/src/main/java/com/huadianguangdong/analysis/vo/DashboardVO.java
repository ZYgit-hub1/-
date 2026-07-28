package com.huadianguangdong.analysis.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

/**
 * 驾驶舱（Dashboard）统计 VO
 *
 * <p>汇总电厂总数、各级别状态计数、报警与水文站概览等指标。
 *
 * @author huadianguangdong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 电厂总数 */
    private Integer totalPlants;

    /** 正常运行数量 */
    private Integer normalCount;

    /** 预警数量 */
    private Integer warningCount;

    /** 危险（告警）数量 */
    private Integer dangerCount;

    /** 离线数量 */
    private Integer offlineCount;

    /** 报警总数（时间窗口内累计） */
    private Integer totalAlarms;

    /** 活跃预警数（未处理的预警） */
    private Integer activeWarnings;

    /** 水文站总数 */
    private Integer hydroStationCount;

    /** 水文站告警数（超警戒水位等） */
    private Integer hydroAlertCount;
}
