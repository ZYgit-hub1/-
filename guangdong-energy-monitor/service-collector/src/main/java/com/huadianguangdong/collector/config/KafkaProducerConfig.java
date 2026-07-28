package com.huadianguangdong.collector.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 生产者配置
 * <p>
 * 双序列化策略：
 * <ul>
 *   <li>Key: StringSerializer（电厂 ID / 站点 ID 作为分区键）</li>
 *   <li>Value: JsonSerializer（DTO 对象序列化为 JSON）</li>
 * </ul>
 * 支持 weather.raw 等主题的 DTO 直接投递。
 *
 * @author huadianguangdong
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * 生产者工厂（JSON 序列化）
     */
    @Bean
    @Primary
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        // Kafka 服务地址
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Key 序列化：String
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Value 序列化：JSON（支持 DTO 直接投递）
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // 重试次数
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        // 批量发送大小（字节）
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        // 批量发送等待时间（ms）
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        // 生产者缓冲区大小（字节）
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        // acks：all 表示所有副本确认
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        // 启用幂等性（防止重试导致重复消息）
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // 最大 inflight 请求数（幂等性要求 <= 5）
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        // 消息压缩：lz4（兼顾压缩率与 CPU 开销）
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * KafkaTemplate（主模板，JSON 序列化）
     */
    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * 纯字符串 KafkaTemplate（兼容旧链路 String 消息投递）
     */
    @Bean(name = "stringKafkaTemplate")
    public KafkaTemplate<String, String> stringKafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }
}
