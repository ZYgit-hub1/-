package com.huadianguangdong.analysis.flink;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Flink 流处理器（本地占位实现）
 *
 * <p><b>重要说明（TODO）：</b>本类为本地占位实现，不真正提交 Flink 作业。
 * 真实的 Flink 作业需单独打包（flink-streaming-java + flink-connector-kafka），
 * 通过 {@code flink run} 命令提交到 Flink 集群，避免在 Spring Boot 应用中引入
 * 完整 Flink 依赖导致依赖冲突（flink-shaded-jackson / asm / protobuf 等）。
 *
 * <p>此处的 {@link #process(String, MessageHandler)} 仅记录日志并同步回调 handler，
 * 便于本地联调与接口契约验证。生产环境下应替换为通过 Flink REST API /
 * CLI 触发远程作业的实现，或直接由独立 Flink Job 项目消费 Kafka 写入结果表。
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
public class FlinkStreamProcessor implements StreamProcessor<String> {

    /** 当前作业是否运行中 */
    private volatile boolean running = false;

    /** 当前订阅的 topic（占位，仅记录单个） */
    private volatile String currentTopic;

    @Override
    public void process(String topic, MessageHandler<String> handler) {
        // TODO: 实际 Flink 作业需单独打包提交到 Flink 集群。
        //  此处仅做占位：记录日志，标记作业为运行态。
        //  若需本地联调，可在此处从 Kafka 消费消息并同步回调 handler。
        this.currentTopic = topic;
        this.running = true;
        log.info("[Flink占位] 流处理作业已启动: topic={}, handler={}", topic, handler.getClass().getName());
    }

    @Override
    public void stop() {
        // TODO: 实际实现应通过 Flink REST API 取消作业（jobId）。
        this.running = false;
        log.info("[Flink占位] 流处理作业已停止: topic={}", currentTopic);
    }

    /**
     * 作业是否运行中。
     *
     * @return 是否运行中
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 当前订阅的 topic。
     *
     * @return topic
     */
    public String getCurrentTopic() {
        return currentTopic;
    }
}
