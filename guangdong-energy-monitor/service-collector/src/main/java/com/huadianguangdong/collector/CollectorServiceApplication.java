package com.huadianguangdong.collector;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据采集服务（气象 / 水文 / 应急）启动类
 *
 * @author huadianguangdong
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@MapperScan("com.huadianguangdong.collector.mapper")
public class CollectorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollectorServiceApplication.class, args);
    }
}
