package com.huadianguangdong.collector.weather;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataSourceHealthChecker 单元测试
 * <p>
 * 验证主备源切换逻辑：连续失败 3 次自动切换到备用源。
 *
 * @author huadianguangdong
 */
@DisplayName("数据源健康检查器测试")
class DataSourceHealthCheckerTest {

    private DataSourceHealthChecker healthChecker;

    @BeforeEach
    void setUp() {
        healthChecker = new DataSourceHealthChecker();
        // 通过反射注入配置（测试环境无 Spring 容器）
        setField(healthChecker, "failThreshold", 3);
        setField(healthChecker, "probeIntervalSec", 60);
    }

    @Nested
    @DisplayName("初始状态")
    class InitialState {

        @Test
        @DisplayName("默认使用主数据源")
        void shouldStartWithPrimarySource() {
            assertEquals(DataSourceHealthChecker.ActiveSource.PRIMARY,
                    healthChecker.getActiveSource());
        }

        @Test
        @DisplayName("初始连续失败次数为 0")
        void shouldHaveZeroFailuresInitially() {
            assertEquals(0, healthChecker.getConsecutiveFailures());
        }
    }

    @Nested
    @DisplayName("失败计数与切换")
    class FailureTracking {

        @Test
        @DisplayName("失败 1-2 次不切换，仍使用主源")
        void shouldNotSwitchBeforeThreshold() {
            assertFalse(healthChecker.recordFailure());  // 第 1 次
            assertEquals(DataSourceHealthChecker.ActiveSource.PRIMARY,
                    healthChecker.getActiveSource());

            assertFalse(healthChecker.recordFailure());  // 第 2 次
            assertEquals(DataSourceHealthChecker.ActiveSource.PRIMARY,
                    healthChecker.getActiveSource());
        }

        @Test
        @DisplayName("连续失败 3 次后切换到备用源")
        void shouldSwitchToFallbackAfterThreshold() {
            healthChecker.recordFailure();  // 1
            healthChecker.recordFailure();  // 2
            boolean switched = healthChecker.recordFailure();  // 3

            assertTrue(switched, "第 3 次失败应触发切换");
            assertEquals(DataSourceHealthChecker.ActiveSource.FALLBACK,
                    healthChecker.getActiveSource());
        }

        @Test
        @DisplayName("成功调用后重置失败计数")
        void shouldResetFailuresOnSuccess() {
            healthChecker.recordFailure();
            healthChecker.recordFailure();
            healthChecker.recordSuccess();

            assertEquals(0, healthChecker.getConsecutiveFailures());
            assertEquals(DataSourceHealthChecker.ActiveSource.PRIMARY,
                    healthChecker.getActiveSource());
        }
    }

    @Nested
    @DisplayName("主源恢复")
    class PrimaryRecovery {

        @Test
        @DisplayName("切回主源后重置失败计数")
        void shouldResetWhenMarkedRecovered() {
            // 触发切换
            healthChecker.recordFailure();
            healthChecker.recordFailure();
            healthChecker.recordFailure();
            assertEquals(DataSourceHealthChecker.ActiveSource.FALLBACK,
                    healthChecker.getActiveSource());

            // 标记主源恢复
            healthChecker.markPrimaryRecovered();

            assertEquals(DataSourceHealthChecker.ActiveSource.PRIMARY,
                    healthChecker.getActiveSource());
            assertEquals(0, healthChecker.getConsecutiveFailures());
        }

        @Test
        @DisplayName("主源未切换时 markPrimaryRecovered 无副作用")
        void shouldNoopWhenAlreadyPrimary() {
            healthChecker.markPrimaryRecovered();
            assertEquals(DataSourceHealthChecker.ActiveSource.PRIMARY,
                    healthChecker.getActiveSource());
        }
    }

    /** 反射注入字段（测试工具方法） */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            fail("无法注入字段 " + fieldName + ": " + e.getMessage());
        }
    }
}
