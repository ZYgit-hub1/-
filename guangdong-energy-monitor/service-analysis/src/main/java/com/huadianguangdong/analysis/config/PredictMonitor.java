package com.huadianguangdong.analysis.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 预测服务监控配置
 * <p>
 * 注册 Micrometer 指标 Binder：
 * <ul>
 *   <li>JVM 内存 / 线程（用于监控预测服务资源消耗）</li>
 *   <li>CPU 核数（用于并发调优参考）</li>
 *   <li>自定义指标在 {@link com.huadianguangdong.analysis.predict.impl.PredictServiceImpl} 中通过 MeterRegistry 直接注册</li>
 * </ul>
 * <p>
 * 自定义指标清单：
 * <table>
 *   <tr><th>指标名</th><th>类型</th><th>标签</th><th>说明</th></tr>
 *   <tr><td>predict.duration</td><td>Timer</td><td>type, success, fallback</td><td>预测调用耗时</td></tr>
 *   <tr><td>predict.calls</td><td>Counter</td><td>type, success, fallback</td><td>预测调用次数</td></tr>
 *   <tr><td>predict.forecast.max</td><td>Gauge</td><td>-</td><td>最近一次预测最大值</td></tr>
 *   <tr><td>predict.forecast.min</td><td>Gauge</td><td>-</td><td>最近一次预测最小值</td></tr>
 * </table>
 *
 * @author huadianguangdong
 */
@Configuration
public class PredictMonitor {

    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }
}
