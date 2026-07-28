package com.huadianguangdong.analysis.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 统计聚合 Mapper
 *
 * <p>跨业务表（电厂 / 报警 / 水文）进行聚合统计查询。表结构参照
 * {@code docs/DATABASE.md}，字段命名采用下划线风格，结果集以 Map 返回供
 * Service 层组装 VO。
 *
 * @author huadianguangdong
 */
@Mapper
public interface StatsMapper {

    /**
     * 按电厂运行状态分组统计数量。
     *
     * <p>返回每组的 status 与 cnt。
     *
     * @return 分组统计结果
     */
    @Select("SELECT status, COUNT(1) AS cnt FROM plant GROUP BY status")
    List<Map<String, Object>> countPlantByStatus();

    /**
     * 统计电厂总数。
     *
     * @return 电厂总数
     */
    @Select("SELECT COUNT(1) FROM plant")
    Integer countPlant();

    /**
     * 统计指定时间区间内的报警总数。
     *
     * @param startTime 起始时间（yyyy-MM-dd HH:mm:ss）
     * @param endTime   结束时间（yyyy-MM-dd HH:mm:ss）
     * @return 报警总数
     */
    @Select("SELECT COUNT(1) FROM alarm WHERE trigger_time >= #{startTime} AND trigger_time <= #{endTime}")
    Integer countAlarm(@Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 按报警级别分组统计。
     *
     * @param startTime 起始时间
     * @param endTime   结束时间
     * @return 每组的 level 与 cnt
     */
    @Select("SELECT level, COUNT(1) AS cnt FROM alarm " +
            "WHERE trigger_time >= #{startTime} AND trigger_time <= #{endTime} GROUP BY level")
    List<Map<String, Object>> countAlarmByLevel(@Param("startTime") String startTime,
                                                @Param("endTime") String endTime);

    /**
     * 统计活跃（未处理）预警数。
     *
     * @return 活跃预警数
     */
    @Select("SELECT COUNT(1) FROM alarm WHERE status IN ('PENDING','PROCESSING','WARNING')")
    Integer countActiveWarning();

    /**
     * 统计水文站总数。
     *
     * @return 水文站总数
     */
    @Select("SELECT COUNT(1) FROM hydro_station")
    Integer countHydroStation();

    /**
     * 统计当前告警水文站数（水位超过警戒水位）。
     *
     * @return 告警水文站数
     */
    @Select("SELECT COUNT(DISTINCT station_id) FROM hydro_data " +
            "WHERE reading_time = (SELECT MAX(reading_time) FROM hydro_data h2 WHERE h2.station_id = hydro_data.station_id) " +
            "AND water_level > (SELECT warning_level FROM hydro_station WHERE id = hydro_data.station_id)")
    Integer countHydroAlert();

    /**
     * 查询电厂指标趋势（按时间升序）。
     *
     * <p>当前从 hydro_data 取水位、从 plant_status 取功率，简化为同一张表查询，
     * 实际可按 metric 动态路由到不同表。
     *
     * @param plantId   电厂 ID
     * @param metric    指标：waterLevel / power
     * @param startTime 起始时间
     * @param endTime   结束时间
     * @return 每组的时间点与指标值
     */
    @Select("SELECT DATE_FORMAT(reading_time, '%Y-%m-%d %H:%i') AS time_point, water_level AS metric_value " +
            "FROM hydro_data WHERE station_id = #{plantId} " +
            "AND reading_time >= #{startTime} AND reading_time <= #{endTime} " +
            "ORDER BY reading_time ASC")
    List<Map<String, Object>> selectPlantTrend(@Param("plantId") Long plantId,
                                               @Param("metric") String metric,
                                               @Param("startTime") String startTime,
                                               @Param("endTime") String endTime);

    /**
     * 查询电厂名称。
     *
     * @param plantId 电厂 ID
     * @return 电厂名称
     */
    @Select("SELECT name FROM plant WHERE id = #{plantId}")
    String selectPlantName(@Param("plantId") Long plantId);

    /**
     * 查询水文站统计数据（趋势 + 告警）。
     *
     * @param stationId 水文站 ID
     * @param startTime 起始时间
     * @param endTime   结束时间
     * @return 水文站统计数据
     */
    @Select("SELECT DATE_FORMAT(reading_time, '%Y-%m-%d %H:%i') AS time_point, " +
            "water_level, flow_rate, trend, alert_level " +
            "FROM hydro_data WHERE station_id = #{stationId} " +
            "AND reading_time >= #{startTime} AND reading_time <= #{endTime} " +
            "ORDER BY reading_time ASC")
    List<Map<String, Object>> selectHydroStats(@Param("stationId") Long stationId,
                                               @Param("startTime") String startTime,
                                               @Param("endTime") String endTime);
}
