package com.huadianguangdong.plant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huadianguangdong.plant.entity.PowerPlant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 电厂 Mapper（PostgreSQL + PostGIS）
 * <p>
 * 提供基础 CRUD（继承 BaseMapper）+ 空间查询方法。
 *
 * @author huadianguangdong
 */
@Mapper
public interface PowerPlantMapper extends BaseMapper<PowerPlant> {

    /**
     * 查询指定经纬度半径 R 米内的电厂（按距离升序）
     * <p>
     * 使用 PostGIS ST_DWithin + ST_DistanceSphere 函数，走 GiST 空间索引。
     *
     * @param lng     中心点经度
     * @param lat     中心点纬度
     * @param radiusM 半径（米）
     * @return 电厂列表（含距离）
     */
    @Select("""
            SELECT t.id, t.name, t.type, t.status,
                   ST_DistanceSphere(t.location, ST_SetSRID(ST_MakePoint(#{lng}, #{lat}), 4326)) AS distance_m
            FROM t_power_plant t
            WHERE t.is_deleted = 0
              AND ST_DWithin(
                    t.location::geography,
                    ST_SetSRID(ST_MakePoint(#{lng}, #{lat}), 4326)::geography,
                    #{radiusM}
                  )
            ORDER BY distance_m ASC
            """)
    List<PlantWithinRadius> findPlantsWithinRadius(@Param("lng") double lng,
                                                   @Param("lat") double lat,
                                                   @Param("radiusM") int radiusM);

    /**
     * 按行政区划代码查询电厂
     */
    @Select("""
            SELECT t.id, t.name, t.type, t.status
            FROM t_power_plant t
            WHERE t.is_deleted = 0
              AND t.district_code = #{districtCode}
            ORDER BY t.name
            """)
    List<PowerPlant> findByDistrictCode(@Param("districtCode") String districtCode);

    /**
     * 按流域查询电厂
     */
    @Select("""
            SELECT t.id, t.name, t.type, t.location, t.district_code, t.river_basin, t.capacity, t.status, t.address
            FROM t_power_plant t
            WHERE t.is_deleted = 0
              AND t.river_basin = #{riverBasin}
            ORDER BY t.id
            """)
    List<PowerPlant> findByRiverBasin(@Param("riverBasin") String riverBasin);

    /**
     * 按类型统计电厂数量（大屏分类统计）
     */
    @Select("""
            SELECT t.type, COUNT(*) AS cnt
            FROM t_power_plant t
            WHERE t.is_deleted = 0
            GROUP BY t.type
            """)
    List<TypeCount> countByType();

    /**
     * 空间查询结果 DTO：半径范围内的电厂
     */
    record PlantWithinRadius(Long id, String name, String type, String status, Double distanceM) {
    }

    /**
     * 类型统计结果 DTO
     */
    record TypeCount(String type, Long cnt) {
    }
}
