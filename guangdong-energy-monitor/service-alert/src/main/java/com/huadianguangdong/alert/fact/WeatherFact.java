package com.huadianguangdong.alert.fact;

import com.huadianguangdong.common.dto.WeatherDataDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 气象事实对象
 * <p>
 * 封装 {@link WeatherDataDTO} 及电厂上下文，作为 Drools 规则的事实（Fact）插入 KieSession。
 * Drools 通过字段访问（getter）匹配 LHS 条件。
 *
 * @author huadianguangdong
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherFact implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 清洗后的气象数据 */
    private WeatherDataDTO weatherData;

    /** 电厂 ID */
    private Long plantId;

    /** 行政区划代码 */
    private String districtCode;

    /** 数据源标识：gd121 / cma */
    private String source;

    // ===== 便捷访问方法（供 Drools LHS 直接使用字段名匹配） =====

    public Float getTemp() {
        return weatherData == null ? null : weatherData.getTemp();
    }

    public Float getHumidity() {
        return weatherData == null ? null : weatherData.getHumidity();
    }

    public Float getWindSpeed() {
        return weatherData == null ? null : weatherData.getWindSpeed();
    }

    public Short getWindDir() {
        return weatherData == null ? null : weatherData.getWindDir();
    }

    public Float getRain1h() {
        return weatherData == null ? null : weatherData.getRain1h();
    }

    public Float getRainfall() {
        return weatherData == null ? null : weatherData.getRainfall();
    }

    public Float getPressure() {
        return weatherData == null ? null : weatherData.getPressure();
    }
}
