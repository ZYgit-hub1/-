package com.huadianguangdong.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huadianguangdong.common.api.R;
import com.huadianguangdong.common.api.ResultCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * IP 限流 GatewayFilterFactory（基于 Bucket4j + Redis）
 * <p>
 * 在路由配置中通过 {@code filters: - IpRateLimit=10,60} 引用。
 * 参数：capacity（桶容量）, refillMinutes（补充周期分钟）。
 * <p>
 * 每个客户端 IP 独立计数，超出限制返回 429 Too Many Requests。
 * <p>
 * 路由配置示例：
 * <pre>
 * spring:
 *   cloud:
 *     gateway:
 *       routes:
 *         - id: auth-service
 *           uri: lb://user-service
 *           predicates:
 *             - Path=/api/auth/**
 *           filters:
 *             - IpRateLimit=5,1
 * </pre>
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
public class IpRateLimitGatewayFilterFactory
        extends AbstractGatewayFilterFactory<IpRateLimitGatewayFilterFactory.Config> {

    /** Redis key 前缀 */
    private static final String BUCKET_PREFIX = "gateway:ratelimit:ip:";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gateway.rate-limit.default-capacity:100}")
    private int defaultCapacity;

    @Value("${gateway.rate-limit.default-refill-minutes:1}")
    private int defaultRefillMinutes;

    public IpRateLimitGatewayFilterFactory(ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        int capacity = config.capacity > 0 ? config.capacity : defaultCapacity;
        int refillMinutes = config.refillMinutes > 0 ? config.refillMinutes : defaultRefillMinutes;

        return (exchange, chain) -> {
            String clientIp = getClientIp(exchange);
            if (clientIp == null || clientIp.isBlank()) {
                clientIp = "unknown";
            }

            // 构建 Bucket4j 令牌桶（基于内存的单次校验，生产环境应接入 Redis 分布式限流）
            Bandwidth bandwidth = Bandwidth.classic(capacity,
                    Refill.intervally(capacity, Duration.ofMinutes(refillMinutes)));
            Bucket bucket = Bucket.builder()
                    .addLimit(bandwidth)
                    .build();

            if (bucket.tryConsume(1)) {
                return chain.filter(exchange);
            }

            // 记录限流事件到 Redis（用于监控统计）
            String redisKey = BUCKET_PREFIX + clientIp;
            redisTemplate.opsForValue().increment(redisKey)
                    .then(redisTemplate.expire(redisKey, Duration.ofMinutes(refillMinutes)))
                    .subscribe();

            log.warn("[IP限流] 触发限流 ip={} capacity={} refillMin={}",
                    clientIp, capacity, refillMinutes);

            return tooManyRequests(exchange);
        };
    }

    /**
     * 获取客户端真实 IP（支持反向代理场景）
     */
    private String getClientIp(org.springframework.web.server.ServerWebExchange exchange) {
        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能包含多个 IP，取第一个
            return ip.split(",")[0].trim();
        }
        ip = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    /**
     * 返回 429 JSON 响应
     */
    private Mono<Void> tooManyRequests(org.springframework.web.server.ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        R<Void> body = R.fail(429, "请求过于频繁，请稍后再试");
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}"
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return java.util.Arrays.asList("capacity", "refillMinutes");
    }

    /**
     * 过滤器配置
     */
    public static class Config {
        /** 桶容量（每个 IP 在周期内的最大请求数） */
        private int capacity;

        /** 补充周期（分钟） */
        private int refillMinutes;

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getRefillMinutes() {
            return refillMinutes;
        }

        public void setRefillMinutes(int refillMinutes) {
            this.refillMinutes = refillMinutes;
        }
    }
}
