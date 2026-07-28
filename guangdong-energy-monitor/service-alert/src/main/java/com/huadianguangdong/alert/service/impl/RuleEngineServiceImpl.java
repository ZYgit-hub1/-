package com.huadianguangdong.alert.service.impl;

import com.huadianguangdong.alert.config.DroolsConfig;
import com.huadianguangdong.alert.fact.AlertEventCollector;
import com.huadianguangdong.alert.fact.HydroFact;
import com.huadianguangdong.alert.fact.WeatherFact;
import com.huadianguangdong.alert.service.AlertActionService;
import com.huadianguangdong.alert.service.AlertSuppressionService;
import com.huadianguangdong.alert.service.RuleEngineService;
import com.huadianguangdong.common.dto.AlertEventDTO;
import com.huadianguangdong.common.dto.HydroDataDTO;
import com.huadianguangdong.common.dto.WeatherDataDTO;
import com.huadianguangdong.common.dto.WeatherRawMessage;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 规则引擎服务实现
 * <p>
 * 每次执行创建独立 KieSession（线程安全隔离），插入事实、设置 global、触发规则后立即 dispose。
 * 规则匹配结果通过 {@link AlertEventCollector} 收集，经 {@link AlertSuppressionService} 抑制过滤后返回。
 * <p>
 * 通过 {@link DroolsConfig#getKieContainer()} 获取当前 KieContainer，支持运行时热替换后立即生效。
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
public class RuleEngineServiceImpl implements RuleEngineService {

    /** Drools global 标识：报警动作服务（兼容旧规则） */
    private static final String GLOBAL_ALERT_SERVICE = "alertService";

    /** Drools global 标识：报警事件收集器 */
    private static final String GLOBAL_ALERT_COLLECTOR = "alertCollector";

    @Autowired
    private DroolsConfig droolsConfig;

    @Autowired
    private AlertActionService alertActionService;

    @Autowired
    private AlertSuppressionService suppressionService;

    // ==================== 水文规则 ====================

    @Override
    public void executeHydroRules(HydroDataDTO hydroData, double warningLevel, double guaranteeLevel) {
        HydroFact fact = new HydroFact(hydroData, warningLevel, guaranteeLevel);
        executeHydroRules(fact);
    }

    @Override
    public void executeHydroRules(HydroFact fact) {
        List<AlertEventDTO> events = executeHydroRulesWithResult(fact);
        // 兼容旧逻辑：通过 AlertActionService 回调
        if (events != null && !events.isEmpty()) {
            for (AlertEventDTO event : events) {
                if (!event.isSuppressed()) {
                    alertActionService.createAlarm(fact.getHydroData(), event.getContent());
                }
            }
        }
    }

    @Override
    public List<AlertEventDTO> executeHydroRulesWithResult(HydroFact fact) {
        AlertEventCollector collector = new AlertEventCollector();
        KieContainer kieContainer = droolsConfig.getKieContainer();
        KieSession kieSession = null;
        try {
            kieSession = kieContainer.newKieSession();
            kieSession.setGlobal(GLOBAL_ALERT_SERVICE, alertActionService);
            kieSession.setGlobal(GLOBAL_ALERT_COLLECTOR, collector);
            kieSession.insert(fact);
            int fired = kieSession.fireAllRules();
            log.debug("水文规则执行完成，触发规则数={}, stationId={}", fired,
                    fact.getHydroData() != null ? fact.getHydroData().getStationId() : null);
        } catch (Exception e) {
            log.error("Drools 水文规则执行异常", e);
        } finally {
            if (kieSession != null) {
                kieSession.dispose();
            }
        }

        // 抑制过滤
        if (!collector.hasEvents()) {
            return Collections.emptyList();
        }
        return suppressionService.filter(collector.getEvents());
    }

    // ==================== 气象规则 ====================

    @Override
    public List<AlertEventDTO> executeWeatherRules(WeatherRawMessage rawMessage) {
        if (rawMessage == null || rawMessage.getCleaned() == null) {
            log.warn("[规则引擎] 气象消息为空或 cleaned 字段缺失");
            return Collections.emptyList();
        }

        WeatherDataDTO weatherData = rawMessage.getCleaned();
        WeatherFact fact = new WeatherFact();
        fact.setWeatherData(weatherData);
        fact.setPlantId(rawMessage.getPlantId());
        fact.setDistrictCode(rawMessage.getDistrictCode());
        fact.setSource(rawMessage.getSource());

        return executeWeatherRules(fact);
    }

    @Override
    public List<AlertEventDTO> executeWeatherRules(WeatherFact fact) {
        AlertEventCollector collector = new AlertEventCollector();
        KieContainer kieContainer = droolsConfig.getKieContainer();
        KieSession kieSession = null;
        try {
            kieSession = kieContainer.newKieSession();
            kieSession.setGlobal(GLOBAL_ALERT_SERVICE, alertActionService);
            kieSession.setGlobal(GLOBAL_ALERT_COLLECTOR, collector);
            kieSession.insert(fact);
            int fired = kieSession.fireAllRules();
            log.debug("气象规则执行完成，触发规则数={}, plantId={}", fired, fact.getPlantId());
        } catch (Exception e) {
            log.error("Drools 气象规则执行异常", e);
        } finally {
            if (kieSession != null) {
                kieSession.dispose();
            }
        }

        // 抑制过滤
        if (!collector.hasEvents()) {
            return Collections.emptyList();
        }
        return suppressionService.filter(collector.getEvents());
    }
}
