package com.huadianguangdong.plant.mapper;

import com.huadianguangdong.plant.dto.PlantMapRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 电厂地图聚合 Mapper
 * <p>
 * 使用 PostGIS 空间函数查询可视区域内的电厂，并关联气象预警表计算最高预警等级。
 *
 * @author huadianguangdong
 */
@Mapper
public interface PlantMapMapper {

    /**
     * 查询可视边界范围内的电厂（含最高预警等级 + 最新温度）
     * <p>
     * 使用 PostGIS ST_MakeEnvelope + ST_Within 进行空间过滤，
     * 关联 t_weather_warning 取当前最高预警等级（red > orange > yellow > blue），
     * 关联最新气象实况取温度。
     *
     * @param minLng 最小经度
     * @param minLat 最小纬度
     * @param maxLng 最大经度
     * @param maxLat 最大纬度
     * @return 电厂地图行数据列表
     */
    List<PlantMapRow> selectPlantsInBounds(@Param("minLng") double minLng,
                                           @Param("minLat") double minLat,
                                           @Param("maxLng") double maxLng,
                                           @Param("maxLat") double maxLat);
}
