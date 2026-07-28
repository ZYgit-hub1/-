package com.huadianguangdong.collector.weather;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 数据源健康检查器
 * <p>
 * 监控主数据源（open.gd121.cn）连续失败次数，超过阈值（默认 3 次）自动切换至备用源（weather.cma.cn）。
 * <p>
 * 切换策略：
 * <ul>
 *   <li>主源连续失败 {@code failThreshold} 次后切换到备用源</li>
 *   <li>备用源每 {@code probeIntervalSec} 秒探测一次主源是否恢复</li>
 *   <li>主源探测成功后切回主源</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
public class DataSourceHealthChecker {

    /** 连续失败阈值（达到即切换备用源） */
    @Value("${collector.weather.fail-threshold:3}")
    private int failThreshold;

    /** 探测间隔（秒） */
    @Value("${collector.weather.probe-interval-sec:60}")
    private int probeIntervalSec;

    /** 当前连续失败次数 */
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    /** 当前活跃数据源 */
    private final AtomicReference<ActiveSource> activeSource = new AtomicReference<>(ActiveSource.PRIMARY);

    /** 上次探测主源的时间戳 */
    private volatile Instant lastProbeTime = Instant.EPOCH;

    /**
     * 记录一次成功
     */
    public void recordSuccess() {
        int prev = consecutiveFailures.getAndSet(0);
        if (prev > 0) {
            log.info("[数据源健康] {} 恢复正常，重置失败计数（之前连续失败 {} 次）", activeSource.get(), prev);
        }
    }

    /**
     * 记录一次失败，返回是否需要切换到备用源
     *
     * @return true 表示已触发切换，应使用备用源
     */
    public boolean recordFailure() {
        int current = consecutiveFailures.incrementAndGet();
        log.warn("[数据源健康] {} 失败第 {}/{} 次", activeSource.get(), current, failThreshold);

        if (current >= failThreshold && activeSource.get() == ActiveSource.PRIMARY) {
            activeSource.set(ActiveSource.FALLBACK);
            log.error("[数据源健康] 主源连续失败 {} 次，切换到备用源 {}", failThreshold, ActiveSource.FALLBACK);
            return true;
        }
        return activeSource.get() == ActiveSource.FALLBACK;
    }

    /**
     * 获取当前应使用的数据源
     * <p>
     * 如果当前是备用源且探测间隔已到，尝试探测主源是否恢复。
     *
     * @return 当前活跃数据源标识
     */
    public ActiveSource getActiveSource() {
        if (activeSource.get() == ActiveSource.FALLBACK) {
            // 探测间隔到达，尝试切回主源
            if (Instant.now().isAfter(lastProbeTime.plusSeconds(probeIntervalSec))) {
                lastProbeTime = Instant.now();
                log.info("[数据源健康] 探测间隔到达，尝试切回主源（实际探测由调用方发起）");
                // 不直接切换，由调用方调用 markPrimaryRecovered() 确认主源恢复后再切
            }
        }
        return activeSource.get();
    }

    /**
     * 标记主源已恢复（探测成功后调用）
     */
    public void markPrimaryRecovered() {
        if (activeSource.compareAndSet(ActiveSource.FALLBACK, ActiveSource.PRIMARY)) {
            consecutiveFailures.set(0);
            log.info("[数据源健康] 主源已恢复，切回主源");
        }
    }

    /**
     * 获取连续失败次数（用于监控指标）
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /**
     * 活跃数据源枚举
     */
    @Getter
    public enum ActiveSource {
        /** 主源：广东省气象局 open.gd121.cn */
        PRIMARY("主源-gd121"),
        /** 备用源：中央气象台 weather.cma.cn */
        FALLBACK("备用-cma");

        private final String label;

        ActiveSource(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
