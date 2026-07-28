package com.huadianguangdong.analysis.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 水位预测请求 VO
 *
 * <p>发送给 Python 预测服务的历史数据载荷。
 *
 * @author huadianguangdong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterLevelPredictRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 水文站 ID */
    private Long stationId;

    /** 历史水位序列 */
    private List<Double> historyData;

    /** 历史时间点序列（与 historyData 一一对应） */
    private List<String> historyTimes;
}
