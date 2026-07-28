package com.huadianguangdong.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka 配置
 * <p>
 * 配置 {@link ConcurrentKafkaListenerContainerFactory} 与错误处理器。
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(ConcurrentKafkaListenerContainerFactory.class)
public class KafkaConfig {

    /**
     * 公共 Kafka 监听容器工厂
     */
    @Bean("kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // 并发度
        factory.setConcurrency(3);
        // 批量提交
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        // 失败重试：固定退避，间隔 1s，重试 3 次（3 次后放弃）
        factory.setCommonErrorHandler(
                new org.springframework.kafka.listener.DefaultErrorHandler(
                        new org.springframework.kafka.listener.DeadLetterPublishingRecoverer(),
                        new FixedBackOff(1000L, 3L)));
        return factory;
    }

    /**
     * Kafka 监听错误处理器
     */
    @Bean("kafkaErrorHandler")
    public KafkaListenerErrorHandler kafkaErrorHandler() {
        return (message, exception) -> {
            log.error("Kafka 消息消费失败：message={}", message.getPayload(), exception);
            return null;
        };
    }
}
