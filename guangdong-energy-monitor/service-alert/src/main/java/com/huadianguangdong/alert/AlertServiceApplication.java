package com.huadianguangdong.alert;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 报警服务启动类
 * <p>
 * 基于 Drools 规则引擎实时处理水文 / 气象数据，产生报警 / 预警，并通过多通道推送。
 *
 * @author huadianguangdong
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.huadianguangdong.alert.mapper")
public class AlertServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertServiceApplication.class, args);
    }
}
