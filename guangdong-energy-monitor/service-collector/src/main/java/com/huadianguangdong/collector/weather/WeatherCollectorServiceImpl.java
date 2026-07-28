package com.huadianguangdong.collector.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huadianguangdong.common.constant.KafkaTopics;
import com.huadianguangdong.common.dto.WeatherDataDTO;
import com.huadianguangdong.common.dto.WeatherRawMessage;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.circuitbreaker.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 气象数据采集服务实现
 * <p>
 * 技术栈：WebClient（响应式） + Resilience4j（重试 3 次 + 熔断 50% 错误率 + 超时 5s + 限流）
 * <p>
 * 采集链路：
 * <ol>
 *   <li>Spring Scheduler 每 5 分钟触发</li>
 *   <li>WebClient 调用气象 API（主源 open.gd121.cn / 备用源 weather.cma.cn）</li>
 *   <li>Resilience4j Retry（3 次）+ CircuitBreaker（50% 错误率熔断）+ TimeLimiter（5s 超时）+ RateLimiter（限流）</li>
 *   <li>DataSourceHealthChecker 监控连续失败，自动切换主备源</li>
 *   <li>WeatherDataParser 解析 JSON + 厂区映射 + 异常值清洗（阈值可配置）</li>
 *   <li>WeatherTdengineWriter 写入 TDengine weather_live</li>
 *   <li>KafkaTemplate 推送 WeatherRawMessage（含 source/ts/raw/cleaned）至 weather.raw 主题</li>
 * </ol>
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherCollectorServiceImpl implements WeatherCollectorService {

    // ===== WebClient（主源 + 备用源） =====
    @Qualifier("primaryWeatherWebClient")
    private final WebClient primaryWebClient;
    @Qualifier("fallbackWeatherWebClient")
    private final WebClient fallbackWebClient;

    // ===== 解析器 + 写入器 + 健康检查 =====
    private final WeatherDataParser parser;
    private final WeatherTdengineWriter tdengineWriter;
    private final DataSourceHealthChecker healthChecker;
    private final ObjectMapper objectMapper;

    // ===== Kafka =====
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ===== Resilience4j 注册表 =====
    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    // ===== 配置项 =====
    @Value("${collector.weather.primary.url}")
    private String primaryUrl;

    @Value("${collector.weather.primary.api-key}")
    private String primaryApiKey;

    @Value("${collector.weather.fallback.url}")
    private String fallbackUrl;

    @Value("${collector.weather.fallback.api-key}")
    private String fallbackApiKey;

    /** API 超时（秒），对应需求"超时 5s" */
    private static final int API_TIMEOUT_SEC = 5;

    /**
     * 定时采集：每 5 分钟执行一次
     * <p>
     * cron = "0 0/5 * * * ?" —— 每 5 分钟的第 0 秒触发
     */
    @Override
    @Scheduled(cron = "${collector.weather.cron:0 0/5 * * * ?}")
    public List<WeatherDataDTO> collect() {
        DataSourceHealthChecker.ActiveSource source = healthChecker.getActiveSource();
        return collect(source);
    }

    /**
     * 手动触发采集（指定数据源）
     */
    @Override
    public List<WeatherDataDTO> collect(DataSourceHealthChecker.ActiveSource source) {
        log.info("[气象采集] 开始采集 source={}", source);
        long startMs = System.currentTimeMillis();
        String collectTs = LocalDateTime.now().toString();

        try {
            // 1. 调用 API（响应式 → 阻塞获取结果，含重试 + 熔断 + 超时 + 限流）
            String rawJson = fetchWeatherJson(source)
                    .block(Duration.ofSeconds(API_TIMEOUT_SEC + 2));  // 预留 2s 余量

            if (rawJson == null || rawJson.isBlank()) {
                log.warn("[气象采集] API 返回空响应 source={}", source);
                healthChecker.recordFailure();
                return Collections.emptyList();
            }

            // 2. 成功 → 记录健康
            healthChecker.recordSuccess();

            // 3. 解析 + 清洗（返回 DTO 列表，同时保留 raw 用于 Kafka 溯源）
            List<WeatherDataDTO> dtoList = parser.parse(rawJson);
            if (dtoList.isEmpty()) {
                log.warn("[气象采集] 解析后数据为空 source={}", source);
                return Collections.emptyList();
            }
            log.info("[气象采集] 解析成功 {} 条 source={}", dtoList.size(), source);

            // 4. 写入 TDengine
            int tdCount = tdengineWriter.write(dtoList);

            // 5. 推送 Kafka（封装 WeatherRawMessage，含 source/ts/raw/cleaned）
            List<WeatherRawMessage> kafkaMessages = buildKafkaMessages(rawJson, dtoList, source, collectTs);
            for (WeatherRawMessage msg : kafkaMessages) {
                String key = String.valueOf(msg.getPlantId());
                kafkaTemplate.send(KafkaTopics.WEATHER_RAW, key, msg);
            }
            log.info("[气象采集] Kafka 推送完成 topic={} count={}", KafkaTopics.WEATHER_RAW, kafkaMessages.size());

            long cost = System.currentTimeMillis() - startMs;
            log.info("[气象采集] 采集完成 source={} tdengine写入={} kafka推送={} 耗时={}ms",
                    source, tdCount, kafkaMessages.size(), cost);
            return dtoList;

        } catch (RequestNotPermitted e) {
            // 限流触发
            log.warn("[气象采集] 限流触发 source={} err={}", source, e.getMessage());
            return Collections.emptyList();
        } catch (CallNotPermittedException e) {
            // 熔断器开启，拒绝调用
            log.error("[气象采集] 熔断器开启 source={} err={}", source, e.getMessage());
            healthChecker.recordFailure();
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("[气象采集] 采集失败 source={} err={}", source, e.getMessage(), e);
            boolean switched = healthChecker.recordFailure();
            if (switched) {
                // 主源切换到备用源后，立即用备用源重试一次
                log.info("[气象采集] 主源切换到备用源，立即重试");
                return collect(DataSourceHealthChecker.ActiveSource.FALLBACK);
            }
            return Collections.emptyList();
        }
    }

    /**
     * 构建 Kafka 消息列表（含 source/ts/raw/cleaned 四要素）
     * <p>
     * 从原始 JSON 中提取每条记录的 raw 字符串，与清洗后的 DTO 配对。
     *
     * @param rawJson   API 返回的完整原始 JSON
     * @param dtoList   清洗后的 DTO 列表
     * @param source    数据源标识
     * @param collectTs 采集时间戳
     * @return Kafka 消息列表
     */
    private List<WeatherRawMessage> buildKafkaMessages(String rawJson,
                                                       List<WeatherDataDTO> dtoList,
                                                       DataSourceHealthChecker.ActiveSource source,
                                                       String collectTs) {
        String sourceTag = source == DataSourceHealthChecker.ActiveSource.PRIMARY ? "gd121" : "cma";

        // 尝试从 rawJson 中提取每条原始记录（与 dtoList 顺序对应）
        List<String> rawItems = extractRawItems(rawJson);

        List<WeatherRawMessage> messages = new ArrayList<>();
        for (int i = 0; i < dtoList.size(); i++) {
            WeatherDataDTO dto = dtoList.get(i);
            String rawItem = i < rawItems.size() ? rawItems.get(i) : rawJson;

            WeatherRawMessage msg = WeatherRawMessage.builder()
                    .source(sourceTag)
                    .ts(collectTs)
                    .raw(rawItem)
                    .plantId(dto.getPlantId())
                    .districtCode(dto.getDistrictCode())
                    .cleaned(dto)
                    .cleanedFlag(isCleaned(rawItem, dto))
                    .build();
            messages.add(msg);
        }
        return messages;
    }

    /**
     * 从完整 JSON 中提取 data 数组的每条原始记录字符串
     */
    private List<String> extractRawItems(String rawJson) {
        List<String> items = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode dataArray = root.path("data");
            if (dataArray.isArray()) {
                for (JsonNode item : dataArray) {
                    items.add(item.toString());
                }
            }
        } catch (Exception e) {
            log.warn("[气象采集] 提取 raw items 失败，使用完整 JSON 兜底: {}", e.getMessage());
        }
        return items;
    }

    /**
     * 判断是否发生清洗（简单策略：raw 中含非 null 数值但 cleaned 中对应字段为 null）
     */
    private boolean isCleaned(String rawItem, WeatherDataDTO dto) {
        try {
            JsonNode rawNode = objectMapper.readTree(rawItem);
            // 如果任一字段在 raw 中有值但 cleaned 后为 null，说明触发了清洗
            if (hasValueButNull(rawNode, "temp", dto.getTemp())) return true;
            if (hasValueButNull(rawNode, "humidity", dto.getHumidity())) return true;
            if (hasValueButNull(rawNode, "windSpeed", dto.getWindSpeed())) return true;
            if (hasValueButNull(rawNode, "windDir", dto.getWindDir())) return true;
            if (hasValueButNull(rawNode, "rain1h", dto.getRain1h())) return true;
            if (hasValueButNull(rawNode, "pressure", dto.getPressure())) return true;
        } catch (Exception e) {
            // 解析失败不强制标记
        }
        return false;
    }

    /**
     * 判断 raw 中某字段有值但 cleaned 后为 null
     */
    private boolean hasValueButNull(JsonNode rawNode, String field, Object cleanedValue) {
        JsonNode rawField = rawNode.path(field);
        return !rawField.isMissingNode() && !rawField.isNull() && cleanedValue == null;
    }

    /**
     * 响应式调用气象 API（含 Resilience4j Retry + CircuitBreaker + RateLimiter + 超时）
     * <p>
     * 重试策略：最多 3 次，间隔 1s（指数退避）
     * 熔断策略：50% 错误率触发熔断，打开 30s，半开状态允许 3 个探测请求
     * 限流策略：每 10s 限制 10 次调用（防止突发请求压垮气象 API）
     */
    private Mono<String> fetchWeatherJson(DataSourceHealthChecker.ActiveSource source) {
        boolean isPrimary = source == DataSourceHealthChecker.ActiveSource.PRIMARY;
        WebClient client = isPrimary ? primaryWebClient : fallbackWebClient;
        String url = isPrimary ? primaryUrl : fallbackUrl;
        String apiKey = isPrimary ? primaryApiKey : fallbackApiKey;

        // 构建请求
        Mono<String> apiCall = client.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(extractHost(url))
                        .path(extractPath(url))
                        .queryParam("apiKey", apiKey)
                        .queryParam("stations", "all")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(API_TIMEOUT_SEC))  // 超时 5s
                .doOnError(e -> log.warn("[气象采集] API 调用失败 source={} err={}", source, e.getMessage()));

        // Resilience4j Retry（3 次，指数退避）
        Retry retry = retryRegistry.retry("weatherApiRetry");
        RetryOperator<String> retryOperator = RetryOperator.of(retry);

        // Resilience4j CircuitBreaker（50% 错误率）
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("weatherApiCircuitBreaker");

        // Resilience4j RateLimiter（API 限流）
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("weatherApiRateLimiter");

        return apiCall
                .transformDeferred(retryOperator)                           // 重试
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))  // 熔断
                .transformDeferred(RateLimiterOperator.of(rateLimiter));       // 限流
    }

    /**
     * 从完整 URL 提取 host
     */
    private String extractHost(String url) {
        String cleaned = url.replaceFirst("^https?://", "");
        int slashIdx = cleaned.indexOf('/');
        return slashIdx > 0 ? cleaned.substring(0, slashIdx) : cleaned;
    }

    /**
     * 从完整 URL 提取 path
     */
    private String extractPath(String url) {
        String cleaned = url.replaceFirst("^https?://", "");
        int slashIdx = cleaned.indexOf('/');
        return slashIdx > 0 ? cleaned.substring(slashIdx) : "/";
    }
}
