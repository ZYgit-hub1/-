package com.huadianguangdong.analysis.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 预测辅助 Mapper
 *
 * <p>为预测服务提供历史数据查询能力（水位历史、气象历史）。
 *
 * @author huadianguangdong
 */
@Mapper
public interface PredictMapper {

    /**
     * 查询水文站最近 N 小时的水位历史。
     *
     * @param stationId 水文站 ID
     * @param hours     小时数
     * @return 每组 reading_time 与 water_level
     */
    @Select("SELECT DATE_FORMAT(reading_time, '%Y-%m-%d %H:%i:%s') AS reading_time, water_level " +
            "FROM hydro_data WHERE station_id = #{stationId} " +
            "AND reading_time >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) " +
            "ORDER BY reading_time ASC")
    List<Map<String, Object>> selectWaterLevelHistory(@Param("stationId") Long stationId,
                                                     @Param("hours") int hours);

    /**
     * 查询电厂最近 N 小时的气象数据。
     *
     * @param plantId 电厂 ID
     * @param hours   小时数
     * @return 每组的气象字段
     */
    @Select("SELECT DATE_FORMAT(reading_time, '%Y-%m-%d %H:%i:%s') AS time, temp, humidity, wind_speed, rainfall " +
            "FROM weather_data WHERE plant_id = #{plantId} " +
            "AND reading_time >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) " +
            "ORDER BY reading_time ASC")
    List<Map<String, Object>> selectWeatherHistory(@Param("plantId") Long plantId,
                                                   @Param("hours") int hours);
}
