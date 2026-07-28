package com.huadianguangdong.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huadianguangdong.alert.entity.AlertRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 报警规则 Mapper（PostgreSQL）
 *
 * @author huadianguangdong
 */
@Mapper
public interface AlertRuleMapper extends BaseMapper<AlertRule> {

    /**
     * 查询所有启用的规则（按优先级排序）
     * <p>
     * 供 Drools 规则引擎启动时加载。
     */
    @Select("""
            SELECT t.id, t.name, t.rule_type, t.logic_type, t.condition_json, t.priority,
                   t.targets_json, t.dead_zone, t.delay_sec, t.enabled, t.version
            FROM t_alert_rule t
            WHERE t.is_deleted = 0
              AND t.enabled = true
            ORDER BY t.priority ASC, t.id ASC
            """)
    List<AlertRule> findAllEnabledRules();

    /**
     * 按规则类型查询启用的规则
     */
    @Select("""
            SELECT t.id, t.name, t.rule_type, t.logic_type, t.condition_json, t.priority,
                   t.targets_json, t.dead_zone, t.delay_sec, t.enabled, t.version
            FROM t_alert_rule t
            WHERE t.is_deleted = 0
              AND t.enabled = true
              AND t.rule_type = #{ruleType}
            ORDER BY t.priority ASC
            """)
    List<AlertRule> findEnabledByType(@Param("ruleType") String ruleType);

    /**
     * JSONB 条件查询：targets_json 中包含指定电厂 ID 的规则
     * <p>
     * 使用 PostgreSQL jsonb 操作符 @>（包含）
     */
    @Select("""
            SELECT t.id, t.name, t.rule_type, t.logic_type, t.condition_json, t.priority,
                   t.targets_json, t.dead_zone, t.delay_sec, t.enabled, t.version
            FROM t_alert_rule t
            WHERE t.is_deleted = 0
              AND t.enabled = true
              AND t.targets_json @> CAST(#{targetJson} AS jsonb)
            ORDER BY t.priority ASC
            """)
    List<AlertRule> findRulesByTarget(@Param("targetJson") String targetJson);
}
