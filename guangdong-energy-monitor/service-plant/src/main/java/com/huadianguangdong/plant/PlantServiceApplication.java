package com.huadianguangdong.plant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 电厂 / 水文站 / GIS 空间管理服务启动类
 *
 * @author huadianguangdong
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.huadianguangdong.plant.mapper")
public class PlantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlantServiceApplication.class, args);
    }
}
