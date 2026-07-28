package com.huadianguangdong.plant.repository;

import com.huadianguangdong.plant.entity.PowerPlant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 电厂 JPA Repository 接口（PostgreSQL + PostGIS）
 * <p>
 * 与 MyBatis-Plus 的 PowerPlantMapper 并存：
 * <ul>
 *   <li>简单 CRUD + 空间查询 → JPA Repository</li>
 *   <li>复杂 SQL + 动态条件 → MyBatis-Plus Mapper</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@Repository
public interface PowerPlantRepository extends JpaRepository<PowerPlant, Long> {

    /**
     * 按行政区划查询电厂
     */
    List<PowerPlant> findByDistrictCodeAndIsDeletedFalse(String districtCode);

    /**
     * 按流域查询电厂
     */
    List<PowerPlant> findByRiverBasinAndIsDeletedFalse(String riverBasin);

    /**
     * 按类型查询电厂
     */
    List<PowerPlant> findByTypeAndIsDeletedFalse(String type);

    /**
     * 名称唯一查询（部分唯一索引）
     */
    boolean existsByNameAndIsDeletedFalse(String name);

    /**
     * 空间查询：指定经纬度半径 R 米内的电厂（原生 SQL + PostGIS 函数）
     */
    @Query(value = """
            SELECT t.* FROM t_power_plant t
            WHERE t.is_deleted = 0
              AND ST_DWithin(
                    t.location::geography,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusM
                  )
            ORDER BY ST_DistanceSphere(t.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) ASC
            """, nativeQuery = true)
    List<PowerPlant> findWithinRadius(@Param("lng") double lng,
                                      @Param("lat") double lat,
                                      @Param("radiusM") int radiusM);
}
