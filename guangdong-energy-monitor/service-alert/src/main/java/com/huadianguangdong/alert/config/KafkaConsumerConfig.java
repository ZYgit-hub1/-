package com.huadianguangdong.alert.config;

import com.huadianguangdong.common.dto.HydroDataDTO;
import com.huadianguangdong.common.dto.WeatherRawMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer 配置
 * <p>
 * 消费 weather.raw / hydro_level 主题，使用 JsonDeserializer 反序列化为 DTO。
 * 手动提交 offset（配合 MANUAL_IMMEDIATE ack-mode），concurrency=3 并发消费。
 *
 * @author huadianguangdong
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:alert-service-group}")
    private String groupId;

    @Value("${spring.kafka.listener.concurrency:3}")
    private Integer concurrency;

    /**
     * Consumer 工厂（JSON 反序列化，信任内部 DTO 包）
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // 信任内部 DTO 包，允许 JsonDeserializer 反序列化
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.huadianguangdong.*");
        // 类型宽松：允许 WeatherRawMessage / HydroDataDTO 等多类型混入
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Kafka 监听容器工厂
     * <p>
     * 手动提交（MANUAL_IMMEDIATE），concurrency 个并发消费者。
     */
    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    /**
     * 专用 ConsumerFactory：WeatherRawMessage
     */
    @Bean(name = "weatherConsumerFactory")
    public ConsumerFactory<String, WeatherRawMessage> weatherConsumerFactory() {
        Map<String, Object> props = baseConsumerProps();
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, WeatherRawMessage.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * 专用 ConsumerFactory：HydroDataDTO
     */
    @Bean(name = "hydroConsumerFactory")
    public ConsumerFactory<String, HydroDataDTO> hydroConsumerFactory() {
        Map<String, Object> props = baseConsumerProps();
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, HydroDataDTO.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * 基础 Consumer 属性
     */
    private Map<String, Object> baseConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.huadianguangdong.*");
        return props;
    }
}
