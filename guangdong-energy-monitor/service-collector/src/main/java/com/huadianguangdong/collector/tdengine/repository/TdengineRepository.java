package com.huadianguangdong.collector.tdengine.repository;

import com.huadianguangdong.collector.tdengine.entity.HydroLevel;
import com.huadianguangdong.collector.tdengine.entity.WeatherLive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * TDengine 时序数据写入 Repository
 * <p>
 * 使用官方 JDBC Connector（taos-jdbcdriver）通过 JdbcTemplate 操作。
 * <p>
 * 核心写入策略：
 * <ul>
 *   <li>自动建子表：INSERT ... USING <stable> TAGS (...) VALUES (...)，TDengine 自动创建子表</li>
 *   <li>批量写入：单条 PreparedStatement 支持多 VALUES，减少网络往返</li>
 *   <li>幂等性：相同主键（ts + tag）覆盖写入，天然幂等</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@Slf4j
@Repository
public class TdengineRepository {

    private final JdbcTemplate tdengineJdbcTemplate;

    public TdengineRepository(@Qualifier("tdengineJdbcTemplate") JdbcTemplate tdengineJdbcTemplate) {
        this.tdengineJdbcTemplate = tdengineJdbcTemplate;
    }

    // ========================================================================
    // 气象数据写入
    // ========================================================================

    /**
     * 写入单条气象数据（自动建子表）
     * <p>
     * SQL 示例：
     * <pre>
     * INSERT INTO weather_live_1 USING weather_live TAGS (1, '440100')
     * VALUES ('2026-07-27 10:00:00.000', 28.5, 75.0, 5.2, 180, 0.0, 1013.2)
     * </pre>
     */
    public int insertWeather(WeatherLive data) {
        String sql = """
                INSERT INTO weather_live_? USING weather_live TAGS (?, ?)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        return tdengineJdbcTemplate.update(sql,
                data.getPlantId(),
                data.getPlantId(),
                data.getDistrictCode(),
                Timestamp.valueOf(data.getTs()),
                data.getTemp(),
                data.getHumidity(),
                data.getWindSpeed(),
                data.getWindDir(),
                data.getRain1h(),
                data.getPressure()
        );
    }

    /**
     * 批量写入气象数据（性能推荐方式）
     * <p>
     * 使用 PreparedStatement 批处理，单次 RPC 写入多条记录。
     * 写入吞吐：单节点 > 10 万条/秒。
     *
     * @param dataList 气象数据列表（同一电厂）
     */
    public void batchInsertWeather(List<WeatherLive> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }
        // 取第一个元素的 plantId / districtCode 作为 TAG（同一批次同一电厂）
        WeatherLive first = dataList.get(0);
        Long plantId = first.getPlantId();
        String districtCode = first.getDistrictCode();

        String sql = """
                INSERT INTO weather_live_%d USING weather_live TAGS (%d, '%s') VALUES (?, ?, ?, ?, ?, ?, ?)
                """.formatted(plantId, plantId, districtCode);

        tdengineJdbcTemplate.batchUpdate(sql, dataList, 500, (ps, data) -> {
            ps.setTimestamp(1, Timestamp.valueOf(data.getTs()));
            setFloatOrNull(ps, 2, data.getTemp());
            setFloatOrNull(ps, 3, data.getHumidity());
            setFloatOrNull(ps, 4, data.getWindSpeed());
            ps.setShort(5, data.getWindDir() != null ? data.getWindDir() : 0);
            setFloatOrNull(ps, 6, data.getRain1h());
            setFloatOrNull(ps, 7, data.getPressure());
        });
        log.info("[TDengine] 批量写入气象数据 plantId={} count={}", plantId, dataList.size());
    }

    // ========================================================================
    // 水文数据写入
    // ========================================================================

    /**
     * 写入单条水文数据（自动建子表）
     */
    public int insertHydro(HydroLevel data) {
        String sql = """
                INSERT INTO hydro_level_? USING hydro_level TAGS (?)
                VALUES (?, ?, ?, ?)
                """;
        return tdengineJdbcTemplate.update(sql,
                data.getStationId(),
                data.getStationId(),
                Timestamp.valueOf(data.getTs()),
                data.getWaterLevel(),
                data.getFlow(),
                Boolean.TRUE.equals(data.getIsOverWarning())
        );
    }

    /**
     * 批量写入水文数据
     */
    public void batchInsertHydro(List<HydroLevel> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }
        HydroLevel first = dataList.get(0);
        Long stationId = first.getStationId();

        String sql = """
                INSERT INTO hydro_level_%d USING hydro_level TAGS (%d) VALUES (?, ?, ?, ?)
                """.formatted(stationId, stationId);

        tdengineJdbcTemplate.batchUpdate(sql, dataList, 500, (ps, data) -> {
            ps.setTimestamp(1, Timestamp.valueOf(data.getTs()));
            setFloatOrNull(ps, 2, data.getWaterLevel());
            setFloatOrNull(ps, 3, data.getFlow());
            ps.setBoolean(4, Boolean.TRUE.equals(data.getIsOverWarning()));
        });
        log.info("[TDengine] 批量写入水文数据 stationId={} count={}", stationId, dataList.size());
    }

    // ========================================================================
    // 查询方法
    // ========================================================================

    /**
     * 查询电厂最近 N 小时的气象数据
     */
    public List<WeatherLive> queryWeatherLastHours(Long plantId, int hours) {
        String sql = """
                SELECT ts, temp, humidity, wind_speed, wind_dir, rain_1h, pressure, plant_id, district_code
                FROM weather_live
                WHERE plant_id = ?
                  AND ts > now - ?
                ORDER BY ts ASC
                """;
        return tdengineJdbcTemplate.query(sql, new Object[]{plantId, hours + "h"}, (rs, rowNum) -> {
            WeatherLive w = new WeatherLive();
            w.setTs(rs.getTimestamp("ts").toLocalDateTime());
            w.setTemp(rs.getFloat("temp"));
            w.setHumidity(rs.getFloat("humidity"));
            w.setWindSpeed(rs.getFloat("wind_speed"));
            w.setWindDir(rs.getShort("wind_dir"));
            w.setRain1h(rs.getFloat("rain_1h"));
            w.setPressure(rs.getFloat("pressure"));
            w.setPlantId(rs.getLong("plant_id"));
            w.setDistrictCode(rs.getString("district_code"));
            return w;
        });
    }

    /**
     * 查询水文站最近 N 小时的水位数据
     */
    public List<HydroLevel> queryHydroLastHours(Long stationId, int hours) {
        String sql = """
                SELECT ts, water_level, flow, is_over_warning, station_id
                FROM hydro_level
                WHERE station_id = ?
                  AND ts > now - ?
                ORDER BY ts ASC
                """;
        return tdengineJdbcTemplate.query(sql, new Object[]{stationId, hours + "h"}, (rs, rowNum) -> {
            HydroLevel h = new HydroLevel();
            h.setTs(rs.getTimestamp("ts").toLocalDateTime());
            h.setWaterLevel(rs.getFloat("water_level"));
            h.setFlow(rs.getFloat("flow"));
            h.setIsOverWarning(rs.getBoolean("is_over_warning"));
            h.setStationId(rs.getLong("station_id"));
            return h;
        });
    }

    /**
     * 查询电厂最新一条气象数据（利用 CACHELAST 缓存，O(1)）
     */
    public WeatherLive queryLatestWeather(Long plantId) {
        String sql = """
                SELECT LAST_ROW(ts) AS ts, LAST_ROW(temp) AS temp, LAST_ROW(humidity) AS humidity,
                       LAST_ROW(wind_speed) AS wind_speed, LAST_ROW(wind_dir) AS wind_dir,
                       LAST_ROW(rain_1h) AS rain_1h, LAST_ROW(pressure) AS pressure,
                       plant_id, district_code
                FROM weather_live
                WHERE plant_id = ?
                """;
        List<WeatherLive> list = tdengineJdbcTemplate.query(sql, new Object[]{plantId}, (rs, rowNum) -> {
            WeatherLive w = new WeatherLive();
            Timestamp ts = rs.getTimestamp("ts");
            if (ts != null) {
                w.setTs(ts.toLocalDateTime());
            }
            w.setTemp(rs.getFloat("temp"));
            w.setHumidity(rs.getFloat("humidity"));
            w.setWindSpeed(rs.getFloat("wind_speed"));
            w.setWindDir(rs.getShort("wind_dir"));
            w.setRain1h(rs.getFloat("rain_1h"));
            w.setPressure(rs.getFloat("pressure"));
            w.setPlantId(rs.getLong("plant_id"));
            w.setDistrictCode(rs.getString("district_code"));
            return w;
        });
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 降采样查询：电厂过去 N 小时每 5 分钟平均温度
     */
    public List<WeatherLive> queryWeatherDownsample(Long plantId, int hours, String interval) {
        String sql = """
                SELECT FIRST(ts) AS ts, AVG(temp) AS temp, AVG(humidity) AS humidity,
                       AVG(wind_speed) AS wind_speed, SUM(rain_1h) AS rain_1h
                FROM weather_live
                WHERE plant_id = ?
                  AND ts > now - ?
                INTERVAL(?)
                """;
        return tdengineJdbcTemplate.query(sql, new Object[]{plantId, hours + "h", interval}, (rs, rowNum) -> {
            WeatherLive w = new WeatherLive();
            Timestamp ts = rs.getTimestamp("ts");
            if (ts != null) {
                w.setTs(ts.toLocalDateTime());
            }
            w.setTemp(rs.getFloat("temp"));
            w.setHumidity(rs.getFloat("humidity"));
            w.setWindSpeed(rs.getFloat("wind_speed"));
            w.setRain1h(rs.getFloat("rain_1h"));
            return w;
        });
    }

    // ========================================================================
    // 私有工具方法
    // ========================================================================

    /** 处理 Float 为 null 的情况（TDengine 不允许 NULL 值，用 0 兜底） */
    private static void setFloatOrNull(PreparedStatement ps, int idx, Float val) throws java.sql.SQLException {
        if (val != null) {
            ps.setFloat(idx, val);
        } else {
            ps.setFloat(idx, 0f);
        }
    }
}
