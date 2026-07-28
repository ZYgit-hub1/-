package com.huadianguangdong.plant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huadianguangdong.plant.entity.HydroStation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 水文站 Mapper（PostgreSQL + PostGIS）
 *
 * @author huadianguangdong
 */
@Mapper
public interface HydroStationMapper extends BaseMapper<HydroStation> {

    /**
     * 按流域查询水文站
     */
    @Select("""
            SELECT t.id, t.name, t.river_basin, t.location, t.warning_level, t.guarantee_level,
                   t.historical_max, t.flow_capacity, t.city, t.status
            FROM t_hydro_station t
            WHERE t.is_deleted = 0
              AND t.river_basin = #{riverBasin}
            ORDER BY t.id
            """)
    List<HydroStation> findByRiverBasin(@Param("riverBasin") String riverBasin);

    /**
     * 按城市查询水文站
     */
    @Select("""
            SELECT t.id, t.name, t.river_basin, t.location, t.warning_level, t.guarantee_level,
                   t.historical_max, t.flow_capacity, t.city, t.status
            FROM t_hydro_station t
            WHERE t.is_deleted = 0
              AND t.city = #{city}
            ORDER BY t.id
            """)
    List<HydroStation> findByCity(@Param("city") String city);

    /**
     * 查询指定经纬度半径 R 米内的水文站（按距离升序）
     */
    @Select("""
            SELECT t.id, t.name, t.river_basin, t.warning_level, t.status,
                   ST_DistanceSphere(t.location, ST_SetSRID(ST_MakePoint(#{lng}, #{lat}), 4326)) AS distance_m
            FROM t_hydro_station t
            WHERE t.is_deleted = 0
              AND ST_DWithin(
                    t.location::geography,
                    ST_SetSRID(ST_MakePoint(#{lng}, #{lat}), 4326)::geography,
                    #{radiusM}
                  )
            ORDER BY distance_m ASC
            """)
    List<StationWithinRadius> findStationsWithinRadius(@Param("lng") double lng,
                                                       @Param("lat") double lat,
                                                       @Param("radiusM") int radiusM);

    /**
     * 空间查询结果 DTO
     */
    record StationWithinRadius(Long id, String name, String riverBasin, Double warningLevel,
                               String status, Double distanceM) {
    }
}
