package com.huadianguangdong.analysis.flink;

/**
 * 流处理抽象接口
 *
 * <p>抽象出 Flink 流处理的核心能力：订阅 Kafka topic 并按 handler 处理消息。
 * 抽象接口的好处是：本服务（Spring Boot 应用）只持有「流处理能力」的契约，
 * 不强制引入完整 Flink 依赖，真实作业由独立 Flink Job 包实现并提交到 Flink 集群。
 *
 * @param <T> 消息载荷类型
 * @author huadianguangdong
 */
public interface StreamProcessor<T> {

    /**
     * 启动流处理作业：订阅指定 topic，并使用 handler 处理每条消息。
     *
     * @param topic   Kafka topic
     * @param handler 消息处理器
     */
    void process(String topic, MessageHandler<T> handler);

    /**
     * 停止当前作业。
     */
    void stop();

    /**
     * 消息处理器函数式接口。
     *
     * @param <T> 消息载荷类型
     */
    @FunctionalInterface
    interface MessageHandler<T> {
        /**
         * 处理单条消息。
         *
         * @param message 消息载荷
         */
        void handle(T message);
    }
}
