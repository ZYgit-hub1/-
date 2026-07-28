package com.huadianguangdong.alert.service;

import com.huadianguangdong.alert.fact.HydroFact;
import com.huadianguangdong.alert.fact.WeatherFact;
import com.huadianguangdong.common.dto.AlertEventDTO;
import com.huadianguangdong.common.dto.HydroDataDTO;
import com.huadianguangdong.common.dto.WeatherRawMessage;

import java.util.List;

/**
 * 规则引擎服务接口
 * <p>
 * 封装 Drools KieSession 的创建、事实插入、规则触发与销毁。
 * 规则匹配结果通过 AlertEventCollector 收集，经抑制过滤后返回。
 *
 * @author huadianguangdong
 */
public interface RuleEngineService {

    /**
     * 执行水文规则（兼容旧接口）
     *
     * @param hydroData      水文数据
     * @param warningLevel   警戒水位
     * @param guaranteeLevel 保证水位
     */
    void executeHydroRules(HydroDataDTO hydroData, double warningLevel, double guaranteeLevel);

    /**
     * 执行水文规则（默认阈值，从 HydroFact 上下文构造）
     *
     * @param fact 水文事实对象
     */
    void executeHydroRules(HydroFact fact);

    /**
     * 执行气象规则
     * <p>
     * 消费 Kafka weather.raw 主题后调用，将 WeatherRawMessage 转为 WeatherFact 注入 Drools。
     *
     * @param rawMessage 气象原始消息（含 source/ts/raw/cleaned）
     * @return 匹配并通过抑制的报警事件列表
     */
    List<AlertEventDTO> executeWeatherRules(WeatherRawMessage rawMessage);

    /**
     * 执行气象规则（直接传入 Fact）
     *
     * @param fact 气象事实对象
     * @return 匹配并通过抑制的报警事件列表
     */
    List<AlertEventDTO> executeWeatherRules(WeatherFact fact);

    /**
     * 执行水文规则并返回报警事件
     *
     * @param fact 水文事实对象
     * @return 匹配并通过抑制的报警事件列表
     */
    List<AlertEventDTO> executeHydroRulesWithResult(HydroFact fact);
}
