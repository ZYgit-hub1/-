package com.huadianguangdong.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huadianguangdong.collector.entity.WeatherWarning;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 气象预警 Mapper（PostgreSQL 按月分区表）
 *
 * @author huadianguangdong
 */
@Mapper
public interface WeatherWarningMapper extends BaseMapper<WeatherWarning> {

    /**
     * 查询某行政区划当前生效的预警
     */
    @Select("""
            SELECT t.id, t.district_code, t.type, t.level, t.publish_time, t.expire_time,
                   t.content_json, t.source, t.status
            FROM t_weather_warning t
            WHERE t.is_deleted = 0
              AND t.district_code = #{districtCode}
              AND t.status = 'active'
              AND (t.expire_time IS NULL OR t.expire_time > now())
            ORDER BY t.publish_time DESC
            """)
    List<WeatherWarning> findActiveByDistrict(@Param("districtCode") String districtCode);

    /**
     * 按时间范围查询预警
     */
    @Select("""
            <script>
            SELECT t.id, t.district_code, t.type, t.level, t.publish_time, t.expire_time,
                   t.content_json, t.source, t.status
            FROM t_weather_warning t
            WHERE t.is_deleted = 0
              AND t.publish_time &gt;= #{startTime}
              AND t.publish_time &lt; #{endTime}
            <if test="districtCode != null and districtCode != ''">
              AND t.district_code = #{districtCode}
            </if>
            ORDER BY t.publish_time DESC
            </script>
            """)
    List<WeatherWarning> findByTimeRange(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         @Param("districtCode") String districtCode);

    /**
     * 标记过期预警（定时任务调用）
     */
    @Update("""
            UPDATE t_weather_warning
            SET status = 'expired'
            WHERE status = 'active'
              AND expire_time IS NOT NULL
              AND expire_time &lt;= now()
            """)
    int markExpiredWarnings();

    /**
     * 撤销指定预警
     */
    @Update("""
            UPDATE t_weather_warning
            SET status = 'cancelled'
            WHERE id = #{id}
            """)
    int cancelWarning(@Param("id") Long id);
}
