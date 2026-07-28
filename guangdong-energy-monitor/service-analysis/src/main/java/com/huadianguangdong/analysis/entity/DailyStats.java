package com.huadianguangdong.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 统计日报表实体
 *
 * <p>由定时任务每小时计算并落库，覆盖当日累计统计指标。
 *
 * @author huadianguangdong
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("daily_stats")
public class DailyStats implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 统计日期 */
    private LocalDate statsDate;

    /** 电厂总数 */
    private Integer totalPlants;

    /** 正常运行数量 */
    private Integer normalCount;

    /** 预警数量 */
    private Integer warningCount;

    /** 危险数量 */
    private Integer dangerCount;

    /** 离线数量 */
    private Integer offlineCount;

    /** 当日报警总数 */
    private Integer totalAlarms;

    /** 活跃预警数 */
    private Integer activeWarnings;

    /** 水文站总数 */
    private Integer hydroStationCount;

    /** 水文站告警数 */
    private Integer hydroAlertCount;

    /** 统计生成时间 */
    private java.time.LocalDateTime createTime;
}
