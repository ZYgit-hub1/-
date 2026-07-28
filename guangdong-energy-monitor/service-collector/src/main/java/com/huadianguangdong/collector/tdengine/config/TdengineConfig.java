package com.huadianguangdong.collector.tdengine.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * TDengine 数据源配置
 * <p>
 * 使用官方 JDBC Connector：com.taosdata.jdbc:taos-jdbcdriver
 * <p>
 * 连接方式：
 * <ul>
 *   <li>原生连接：jdbc:TAOS://host:6030/db（需安装 TDengine 客户端）</li>
 *   <li>RESTful 连接：jdbc:TAOS-RS://host:6041/db（无需客户端，跨平台推荐）</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@Configuration
public class TdengineConfig {

    /**
     * TDengine 数据源（HikariCP 连接池）
     */
    @Bean(name = "tdengineDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.tdengine.hikari")
    public DataSource tdengineDataSource(
            @Value("${spring.datasource.tdengine.url}") String url,
            @Value("${spring.datasource.tdengine.username}") String username,
            @Value("${spring.datasource.tdengine.password}") String password,
            @Value("${spring.datasource.tdengine.driver-class-name:com.taosdata.jdbc.rs.RestfulDriver}") String driver
    ) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driver);
        // TDengine 连接池建议参数
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setConnectionTimeout(10_000);
        ds.setIdleTimeout(300_000);
        ds.setMaxLifetime(600_000);
        // 写入失败快速失败，不重试（TDengine 写入应保证幂等，由上层 Kafka 消费重试）
        ds.setConnectionTestQuery("SELECT SERVER_STATUS()");
        return ds;
    }

    /**
     * TDengine 专用 JdbcTemplate
     */
    @Bean(name = "tdengineJdbcTemplate")
    public JdbcTemplate tdengineJdbcTemplate(@Qualifier("tdengineDataSource") DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        // TDengine 批量写入建议关闭自动提交，手动控制批次
        template.setFetchSize(1_000);
        return template;
    }
}
