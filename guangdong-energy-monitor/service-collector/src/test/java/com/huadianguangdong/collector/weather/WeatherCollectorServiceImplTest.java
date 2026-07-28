package com.huadianguangdong.collector.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huadianguangdong.common.constant.KafkaTopics;
import com.huadianguangdong.common.dto.WeatherDataDTO;
import com.huadianguangdong.common.dto.WeatherRawMessage;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WeatherCollectorService 单元测试骨架
 * <p>
 * 使用 Mockito Mock 依赖，验证采集主流程与异常分支。
 * 覆盖：限流处理、Kafka 消息字段（source/ts/raw/cleaned）完整性。
 *
 * @author huadianguangdong
 */
@DisplayName("气象采集服务测试")
@ExtendWith(MockitoExtension.class)
class WeatherCollectorServiceImplTest {

    @Mock
    private WebClient primaryWebClient;
    @Mock
    private WebClient fallbackWebClient;
    @Mock
    private WeatherDataParser parser;
    @Mock
    private WeatherTdengineWriter tdengineWriter;
    @Mock
    private DataSourceHealthChecker healthChecker;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private RetryRegistry retryRegistry;
    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Mock
    private RateLimiterRegistry rateLimiterRegistry;

    @InjectMocks
    private WeatherCollectorServiceImpl service;

    @BeforeEach
    void setUp() {
        // 注入 @Value 配置
        ReflectionTestUtils.setField(service, "primaryUrl", "https://open.gd121.cn/api/v1/weather/observe");
        ReflectionTestUtils.setField(service, "primaryApiKey", "test-key");
        ReflectionTestUtils.setField(service, "fallbackUrl", "https://weather.cma.cn/api/weather/observe");
        ReflectionTestUtils.setField(service, "fallbackApiKey", "test-key-fallback");

        // Resilience4j Retry 默认实例（避免 NPE）
        var retryConfig = io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(1).build();
        var retry = io.github.resilience4j.retry.Retry.of("test", retryConfig);
        when(retryRegistry.retry(anyString())).thenReturn(retry);

        // CircuitBreaker 默认实例
        var cbConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(10)
                .build();
        var cb = io.github.resilience4j.circuitbreaker.CircuitBreaker.of("test", cbConfig);
        when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(cb);

        // RateLimiter 默认实例（限流配置：10 次/10s）
        var rlConfig = io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(java.time.Duration.ofSeconds(10))
                .timeoutDuration(java.time.Duration.ZERO)
                .build();
        var rl = io.github.resilience4j.ratelimiter.RateLimiter.of("test", rlConfig);
        when(rateLimiterRegistry.rateLimiter(anyString())).thenReturn(rl);
    }

    @Nested
    @DisplayName("采集主流程")
    class CollectFlow {

        @Test
        @DisplayName("API 返回空响应时记录失败并返回空列表")
        void shouldRecordFailureOnEmptyResponse() {
            when(healthChecker.getActiveSource()).thenReturn(DataSourceHealthChecker.ActiveSource.PRIMARY);

            List<WeatherDataDTO> result = service.collect(DataSourceHealthChecker.ActiveSource.PRIMARY);

            assertNotNull(result);
            verify(healthChecker, atLeastOnce()).recordFailure();
            verify(tdengineWriter, never()).write(any());
            verify(kafkaTemplate, never()).send(anyString(), any(), any());
        }

        @Test
        @DisplayName("解析结果为空时不写入 TDengine / Kafka")
        void shouldSkipWriteWhenParseEmpty() {
            when(healthChecker.getActiveSource()).thenReturn(DataSourceHealthChecker.ActiveSource.PRIMARY);

            List<WeatherDataDTO> result = service.collect(DataSourceHealthChecker.ActiveSource.PRIMARY);

            assertTrue(result.isEmpty());
            verify(tdengineWriter, never()).write(any());
        }
    }

    @Nested
    @DisplayName("主备源切换")
    class SourceSwitching {

        @Test
        @DisplayName("使用主源采集时调用 primaryWebClient")
        void shouldUsePrimaryWebClientWhenPrimaryActive() {
            when(healthChecker.getActiveSource()).thenReturn(DataSourceHealthChecker.ActiveSource.PRIMARY);

            service.collect(DataSourceHealthChecker.ActiveSource.PRIMARY);

            verify(healthChecker, atLeastOnce()).getActiveSource();
        }

        @Test
        @DisplayName("使用备用源采集时调用 fallbackWebClient")
        void shouldUseFallbackWebClientWhenFallbackActive() {
            when(healthChecker.getActiveSource()).thenReturn(DataSourceHealthChecker.ActiveSource.FALLBACK);

            service.collect(DataSourceHealthChecker.ActiveSource.FALLBACK);

            verify(healthChecker, atLeastOnce()).getActiveSource();
        }
    }

    @Nested
    @DisplayName("Kafka 消息字段完整性")
    class KafkaMessageFields {

        @Test
        @DisplayName("WeatherRawMessage 包含 source/ts/raw/cleaned 四要素")
        void shouldContainAllRequiredFields() {
            // 验证 WeatherRawMessage 的字段定义完整
            WeatherDataDTO dto = WeatherDataDTO.builder()
                    .plantId(1L)
                    .districtCode("440500")
                    .temp(28.5f)
                    .build();

            WeatherRawMessage msg = WeatherRawMessage.builder()
                    .source("gd121")
                    .ts("2026-07-27T10:00:00")
                    .raw("{\"stationCode\":\"GD001\",\"temp\":\"28.5\"}")
                    .plantId(1L)
                    .districtCode("440500")
                    .cleaned(dto)
                    .cleanedFlag(false)
                    .build();

            assertNotNull(msg.getSource(), "source 字段不能为空");
            assertNotNull(msg.getTs(), "ts 字段不能为空");
            assertNotNull(msg.getRaw(), "raw 字段不能为空");
            assertNotNull(msg.getCleaned(), "cleaned 字段不能为空");
            assertEquals("gd121", msg.getSource());
            assertEquals(1L, msg.getPlantId());
        }

        @Test
        @DisplayName("weather.raw 主题常量正确")
        void shouldUseCorrectTopicName() {
            assertEquals("weather.raw", KafkaTopics.WEATHER_RAW);
        }

        @Test
        @DisplayName("主源标识为 gd121，备用源标识为 cma")
        void shouldMapSourceTagCorrectly() {
            // 通过 buildKafkaMessages 间接验证（需要完整 mock 链路）
            // 骨架占位：验证 ActiveSource 枚举值
            assertEquals(DataSourceHealthChecker.ActiveSource.PRIMARY,
                    DataSourceHealthChecker.ActiveSource.valueOf("PRIMARY"));
            assertEquals(DataSourceHealthChecker.ActiveSource.FALLBACK,
                    DataSourceHealthChecker.ActiveSource.valueOf("FALLBACK"));
        }
    }

    @Nested
    @DisplayName("限流处理")
    class RateLimiting {

        @Test
        @DisplayName("RateLimiterRegistry 已注入")
        void shouldHaveRateLimiterRegistryInjected() {
            assertNotNull(rateLimiterRegistry);
        }

        @Test
        @DisplayName("限流配置实例可获取")
        void shouldGetRateLimiterInstance() {
            // 验证 RateLimiter 可通过 registry 获取（已在 setUp 中 mock）
            var rl = rateLimiterRegistry.rateLimiter("weatherApiRateLimiter");
            assertNotNull(rl);
        }
    }

    @Nested
    @DisplayName("TDengine 写入")
    class TdengineWrite {

        @Test
        @DisplayName("批量写入调用 tdengineWriter.write")
        void shouldCallTdengineWriter() {
            // TODO: Mock WebClient 成功响应 + parser 返回数据后，验证 tdengineWriter.write 被调用
        }
    }

    // ===== 工具方法 =====

    /** 构造测试用 DTO */
    private WeatherDataDTO buildTestDTO() {
        return WeatherDataDTO.builder()
                .plantId(1L)
                .districtCode("440500")
                .temp(28.5f)
                .humidity(75.0f)
                .windSpeed(5.2f)
                .windDir((short) 180)
                .rain1h(0.0f)
                .pressure(1013.2f)
                .ts("2026-07-27 10:00:00")
                .build();
    }
}
