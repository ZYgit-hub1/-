package com.huadianguangdong.collector.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * WebClient 配置（响应式 HTTP 客户端）
 * <p>
 * 用于调用广东省气象局 API，配置连接超时 3s、读超时 5s。
 *
 * @author huadianguangdong
 */
@Configuration
public class WebClientConfig {

    /** 连接超时（ms） */
    private static final int CONNECT_TIMEOUT_MS = 3_000;
    /** 读超时（ms）—— 满足需求"超时 5s" */
    private static final int READ_TIMEOUT_MS = 5_000;
    /** 写超时（ms） */
    private static final int WRITE_TIMEOUT_MS = 5_000;

    /**
     * 主数据源 WebClient（广东省气象局 open.gd121.cn）
     */
    @Bean(name = "primaryWeatherWebClient")
    public WebClient primaryWeatherWebClient() {
        return buildWebClient();
    }

    /**
     * 备用数据源 WebClient（中央气象台 weather.cma.cn）
     */
    @Bean(name = "fallbackWeatherWebClient")
    public WebClient fallbackWeatherWebClient() {
        return buildWebClient();
    }

    /**
     * 构建通用 WebClient（含连接池、超时配置）
     */
    private WebClient buildWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS))
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))  // 2MB
                .build();
    }
}
