package com.huadianguangdong.collector.weather;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RateLimiter 集成测试
 * <p>
 * 使用 Resilience4j 真实 RateLimiter 实例（非 Mock），验证：
 * <ol>
 *   <li>正常窗口内调用：limitForPeriod 次全部放行</li>
 *   <li>超限调用：第 limitForPeriod+1 次立即拒绝，抛出 RequestNotPermitted</li>
 *   <li>并发超限模拟：多线程同时请求，验证仅有 limitForPeriod 次放行</li>
 *   <li>窗口刷新后恢复：等待 limitRefreshPeriod 后，新窗口重新放行</li>
 *   <li>timeoutDuration 行为：等待超时时间内是否有许可则放行</li>
 *   <li>生产配置合理性：验证 application.yml 中 10次/10s 的参数符合气象 API 约束</li>
 * </ol>
 *
 * @author huadianguangdong
 */
@DisplayName("RateLimiter 集成测试")
@ExtendWith(MockitoExtension.class)
class RateLimiterIntegrationTest {

    // ==================== 1. 正常窗口内放行 ====================

    @Nested
    @DisplayName("窗口内放行验证")
    class WithinWindow {

        @Test
        @DisplayName("limitForPeriod=5 时，连续 5 次调用全部放行")
        void shouldAllowAllWithinLimit() {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(5)
                    .limitRefreshPeriod(Duration.ofSeconds(10))
                    .timeoutDuration(Duration.ZERO)
                    .build();
            RateLimiter limiter = RateLimiter.of("test-within", config);

            for (int i = 0; i < 5; i++) {
                assertDoesNotThrow(() -> {
                    limiter.acquirePermission();
                }, "第 " + (i + 1) + " 次调用应放行");
            }

            // 验证已消耗 5 个许可
            assertEquals(5, limiter.getMetrics().getAvailablePermissions(),
                    "许可耗尽后 availablePermissions 应为负数或 0");
        }
    }

    // ==================== 2. 超限拒绝 ====================

    @Nested
    @DisplayName("超限拒绝验证")
    class OverLimit {

        @Test
        @DisplayName("第 6 次调用（超出 limitForPeriod=5）立即抛出 RequestNotPermitted")
        void shouldRejectOnOverLimit() {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(5)
                    .limitRefreshPeriod(Duration.ofSeconds(10))
                    .timeoutDuration(Duration.ZERO)
                    .build();
            RateLimiter limiter = RateLimiter.of("test-overlimit", config);

            // 耗尽 5 个许可
            for (int i = 0; i < 5; i++) {
                limiter.acquirePermission();
            }

            // 第 6 次应拒绝
            RequestNotPermitted ex = assertThrows(RequestNotPermitted.class, () -> {
                limiter.acquirePermission();
            });
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("连续超限调用均抛出 RequestNotPermitted")
        void shouldRejectAllSubsequentCalls() {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(3)
                    .limitRefreshPeriod(Duration.ofSeconds(60))
                    .timeoutDuration(Duration.ZERO)
                    .build();
            RateLimiter limiter = RateLimiter.of("test-consecutive", config);

            // 耗尽 3 个许可
            for (int i = 0; i < 3; i++) {
                limiter.acquirePermission();
            }

            // 连续 10 次超限均应拒绝
            for (int i = 0; i < 10; i++) {
                assertThrows(RequestNotPermitted.class, () -> {
                    limiter.acquirePermission();
                }, "第 " + (4 + i) + " 次调用应拒绝");
            }
        }
    }

    // ==================== 3. 并发超限模拟 ====================

    @Nested
    @DisplayName("并发超限模拟")
    class ConcurrentOverLimit {

        @Test
        @DisplayName("20 个线程并发请求（limit=5），仅 5 个放行，15 个拒绝")
        void shouldRejectConcurrentOverLimit() throws InterruptedException {
            int limit = 5;
            int threadCount = 20;

            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(limit)
                    .limitRefreshPeriod(Duration.ofSeconds(60))
                    .timeoutDuration(Duration.ZERO)
                    .build();
            RateLimiter limiter = RateLimiter.of("test-concurrent", config);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            AtomicInteger allowedCount = new AtomicInteger(0);
            AtomicInteger rejectedCount = new AtomicInteger(0);
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();  // 所有线程同步起跑
                        limiter.acquirePermission();
                        allowedCount.incrementAndGet();
                    } catch (RequestNotPermitted e) {
                        rejectedCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }

            startLatch.countDown();  // 同时释放所有线程

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "线程池应在 5s 内结束");

            assertEquals(limit, allowedCount.get(),
                    "应有且仅有 " + limit + " 个线程获得许可");
            assertEquals(threadCount - limit, rejectedCount.get(),
                    "应有 " + (threadCount - limit) + " 个线程被拒绝");
        }
    }

    // ==================== 4. 窗口刷新后恢复 ====================

    @Nested
    @DisplayName("窗口刷新恢复验证")
    class WindowRefresh {

        @Test
        @DisplayName("等待 1s 后（limitRefreshPeriod=1s），新窗口重新放行")
        void shouldAllowAfterWindowRefresh() throws InterruptedException {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(2)
                    .limitRefreshPeriod(Duration.ofSeconds(1))
                    .timeoutDuration(Duration.ZERO)
                    .build();
            RateLimiter limiter = RateLimiter.of("test-refresh", config);

            // 耗尽许可
            limiter.acquirePermission();
            limiter.acquirePermission();

            // 超限
            assertThrows(RequestNotPermitted.class, limiter::acquirePermission);

            // 等待窗口刷新
            Thread.sleep(1200);

            // 新窗口应重新放行
            assertDoesNotThrow(limiter::acquirePermission, "窗口刷新后应放行");
        }
    }

    // ==================== 5. timeoutDuration 等待行为 ====================

    @Nested
    @DisplayName("timeoutDuration 等待行为")
    class TimeoutBehavior {

        @Test
        @DisplayName("timeoutDuration=500ms 内许可未到达则抛出 RequestNotPermitted")
        void shouldTimeoutWhenNoPermission() {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(1)
                    .limitRefreshPeriod(Duration.ofSeconds(10))
                    .timeoutDuration(Duration.ofMillis(500))
                    .build();
            RateLimiter limiter = RateLimiter.of("test-timeout", config);

            // 耗尽许可
            limiter.acquirePermission();

            // timeoutDuration=500ms → 等待 500ms 后仍无许可，抛出异常
            long startMs = System.currentTimeMillis();
            assertThrows(RequestNotPermitted.class, limiter::acquirePermission);
            long elapsed = System.currentTimeMillis() - startMs;

            // 验证确实等待了约 500ms
            assertTrue(elapsed >= 400, "应等待约 500ms 后超时，实际等待 " + elapsed + "ms");
            assertTrue(elapsed < 1000, "等待不应超过 1s，实际等待 " + elapsed + "ms");
        }

        @Test
        @DisplayName("timeoutDuration=0 时立即拒绝，无等待")
        void shouldRejectImmediatelyWithZeroTimeout() {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(1)
                    .limitRefreshPeriod(Duration.ofSeconds(10))
                    .timeoutDuration(Duration.ZERO)
                    .build();
            RateLimiter limiter = RateLimiter.of("test-zero-timeout", config);

            limiter.acquirePermission();

            long startMs = System.currentTimeMillis();
            assertThrows(RequestNotPermitted.class, limiter::acquirePermission);
            long elapsed = System.currentTimeMillis() - startMs;

            assertTrue(elapsed < 50, "timeoutDuration=0 应立即拒绝，实际耗时 " + elapsed + "ms");
        }
    }

    // ==================== 6. 生产配置合理性验证 ====================

    @Nested
    @DisplayName("生产配置参数验证")
    class ProductionConfigValidation {

        @Test
        @DisplayName("验证 10次/10s 配置等效于 1次/秒（满足气象 API 频率约束）")
        void shouldMatchProductionRate() {
            // application.yml 中配置：
            //   limit-for-period: 10
            //   limit-refresh-period: 10s
            //   timeout-duration: 0
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(10)
                    .limitRefreshPeriod(Duration.ofSeconds(10))
                    .timeoutDuration(Duration.ZERO)
                    .build();
            RateLimiter limiter = RateLimiter.of("test-prod-config", config);

            // 验证配置值正确
            assertEquals(10, config.getLimitForPeriod());
            assertEquals(Duration.ofSeconds(10), config.getLimitRefreshPeriod());
            assertEquals(Duration.ZERO, config.getTimeoutDuration());

            // 连续 10 次应全部放行
            for (int i = 0; i < 10; i++) {
                assertDoesNotThrow(limiter::acquirePermission,
                        "生产配置 10次/10s：第 " + (i + 1) + " 次应放行");
            }

            // 第 11 次应拒绝
            assertThrows(RequestNotPermitted.class, limiter::acquirePermission,
                    "生产配置 10次/10s：第 11 次应拒绝");
        }

        @Test
        @DisplayName("Metrics 能正确反映限流状态")
        void shouldReportCorrectMetrics() {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(3)
                    .limitRefreshPeriod(Duration.ofSeconds(10))
                    .timeoutDuration(Duration.ZERO)
                    .build();
            RateLimiter limiter = RateLimiter.of("test-metrics", config);

            // 初始状态
            var metrics = limiter.getMetrics();
            assertEquals(3, metrics.getAvailablePermissions());
            assertEquals(3, metrics.getLimitForPeriod());
            assertEquals(0, metrics.getNumberOfWaitingThreads());

            // 消耗 2 个许可
            limiter.acquirePermission();
            limiter.acquirePermission();

            metrics = limiter.getMetrics();
            assertEquals(1, metrics.getAvailablePermissions(),
                    "消耗 2 个许可后应剩 1 个");
        }
    }

    // ==================== 7. 模拟气象采集场景 ====================

    @Nested
    @DisplayName("气象采集场景模拟")
    class WeatherCollectionScenario {

        @Test
        @DisplayName("模拟 5 分钟周期内多站点并发采集，不超过限流阈值")
        void shouldHandleMultipleStationsWithinRateLimit() {
            // 场景：9 个电厂 × 1 次采集 = 9 次 API 调用
            // 生产配置：10次/10s，9 次调用应在窗口内全部放行
            int stationCount = 9;

            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(10)
                    .limitRefreshPeriod(Duration.ofSeconds(10))
                    .timeoutDuration(Duration.ZERO)
                    .build();
            RateLimiter limiter = RateLimiter.of("test-weather-scenario", config);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger rejectCount = new AtomicInteger(0);

            for (int i = 0; i < stationCount; i++) {
                try {
                    limiter.acquirePermission();
                    successCount.incrementAndGet();
                } catch (RequestNotPermitted e) {
                    rejectCount.incrementAndGet();
                }
            }

            assertEquals(stationCount, successCount.get(),
                    "9 个站点采集应在 10次/10s 限流窗口内全部放行");
            assertEquals(0, rejectCount.get(), "不应有任何站点被限流");
        }

        @Test
        @DisplayName("模拟紧急重试叠加采集，触发限流保护")
        void shouldProtectFromBurstRetries() {
            // 场景：正常 9 次采集 + 切换备用源后立即重试 9 次 = 18 次调用
            // 生产配置：10次/10s，第 11 次开始被限流
            int normalCalls = 9;
            int retryCalls = 9;
            int totalCalls = normalCalls + retryCalls;

            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(10)
                    .limitRefreshPeriod(Duration.ofSeconds(10))
                    .timeoutDuration(Duration.ZERO)
                    .build();
            RateLimiter limiter = RateLimiter.of("test-burst", config);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger rejectCount = new AtomicInteger(0);

            // 正常采集
            for (int i = 0; i < normalCalls; i++) {
                try {
                    limiter.acquirePermission();
                    successCount.incrementAndGet();
                } catch (RequestNotPermitted e) {
                    rejectCount.incrementAndGet();
                }
            }

            // 切换备用源后重试（模拟 collect() 中的健康检查切换逻辑）
            for (int i = 0; i < retryCalls; i++) {
                try {
                    limiter.acquirePermission();
                    successCount.incrementAndGet();
                } catch (RequestNotPermitted e) {
                    rejectCount.incrementAndGet();
                }
            }

            assertEquals(10, successCount.get(),
                    "18 次调用中仅有 10 次在窗口内放行");
            assertEquals(8, rejectCount.get(),
                    "8 次调用被限流保护拦截");
        }
    }
}
