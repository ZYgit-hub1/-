package com.huadianguangdong.analysis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 统计分析服务启动类
 *
 * <p>职责：
 * <ul>
 *     <li>统计计算：Dashboard / 电厂趋势 / 报警 / 水文统计聚合</li>
 *     <li>Flink 流处理：以抽象接口表达，真实作业单独打包提交 Flink 集群</li>
 *     <li>Python 预测对接：水位预测 / 发电预测（Feign 调用）</li>
 *     <li>实时统计：Kafka 消费报警 / 水文数据更新 Redis 计数器</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@MapperScan("com.huadianguangdong.analysis.mapper")
public class AnalysisServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalysisServiceApplication.class, args);
    }
}
