package com.huadianguangdong.analysis.flink;

import com.huadianguangdong.common.constant.CommonConstants;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 流处理作业管理器
 *
 * <p>统一管理 Flink 流处理作业的生命周期：启动 / 停止。在 Bean 初始化时启动
 * 全部预定义作业，在容器销毁时停止全部作业。真实作业由独立 Flink Job 包提交，
 * 此处仅协调本地占位实现。
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamJobManager {

    private final FlinkStreamProcessor flinkStreamProcessor;

    /** 已注册的流处理作业列表（占位，记录作业元信息） */
    private final List<StreamJob> jobs = new ArrayList<>();

    /**
     * 初始化：注册并启动全部流处理作业。
     */
    @PostConstruct
    public void init() {
        log.info("StreamJobManager 初始化：注册流处理作业");
        registerJob(CommonConstants.TOPIC_ALARM, "报警实时统计作业", this::handleAlarm);
        registerJob(CommonConstants.TOPIC_HYDRO_DATA, "水文数据聚合作业", this::handleHydroData);
        registerJob(CommonConstants.TOPIC_WEATHER_DATA, "气象数据关联作业", this::handleWeatherData);
        startAll();
    }

    /**
     * 注册一个流处理作业。
     *
     * @param topic     订阅的 Kafka topic
     * @param jobName   作业名称
     * @param handler   消息处理器
     */
    private void registerJob(String topic, String jobName, StreamProcessor.MessageHandler<String> handler) {
        jobs.add(new StreamJob(topic, jobName, handler));
    }

    /**
     * 启动全部已注册的流处理作业。
     */
    public void startAll() {
        log.info("启动全部流处理作业，共 {} 个", jobs.size());
        for (StreamJob job : jobs) {
            try {
                flinkStreamProcessor.process(job.topic, job.handler);
                log.info("作业已启动: {} (topic={})", job.jobName, job.topic);
            } catch (Exception e) {
                log.error("作业启动失败: {} (topic={})", job.jobName, job.topic, e);
            }
        }
    }

    /**
     * 停止全部流处理作业。
     */
    public void stopAll() {
        log.info("停止全部流处理作业，共 {} 个", jobs.size());
        try {
            flinkStreamProcessor.stop();
        } catch (Exception e) {
            log.error("停止流处理作业失败", e);
        }
    }

    /**
     * 容器销毁时停止全部作业。
     */
    @PreDestroy
    public void destroy() {
        stopAll();
    }

    // ==================== 作业消息处理（占位） ====================

    private void handleAlarm(String message) {
        // TODO: 实际由独立 Flink 作业消费 alarm-topic 并写入实时统计结果表 / Redis
        log.debug("[流处理] 报警消息: {}", message);
    }

    private void handleHydroData(String message) {
        // TODO: 实际由独立 Flink 作业消费 hydro-data-topic 并做窗口聚合
        log.debug("[流处理] 水文消息: {}", message);
    }

    private void handleWeatherData(String message) {
        // TODO: 实际由独立 Flink 作业消费 weather-data-topic 并与电厂关联
        log.debug("[流处理] 气象消息: {}", message);
    }

    /**
     * 流处理作业元信息。
     */
    private record StreamJob(String topic, String jobName, StreamProcessor.MessageHandler<String> handler) {
    }
}
