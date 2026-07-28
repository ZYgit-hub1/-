package com.huadianguangdong.plant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huadianguangdong.common.api.PageResult;
import com.huadianguangdong.plant.entity.Plant;

import java.util.List;

/**
 * 电厂服务接口
 *
 * @author huadianguangdong
 */
public interface PlantService extends IService<Plant> {

    /**
     * 分页查询电厂
     *
     * @param page    页码
     * @param size    每页大小
     * @param type    电厂类型（可空）
     * @param status  运行状态（可空）
     * @param keyword 名称关键字（可空）
     * @return 分页结果
     */
    PageResult<Plant> page(int page, int size, String type, String status, String keyword);

    /**
     * 查询指定坐标半径范围内的电厂
     *
     * @param lng       中心点经度
     * @param lat       中心点纬度
     * @param radiusKm  半径（千米）
     * @return 电厂列表
     */
    List<Plant> listNearby(double lng, double lat, double radiusKm);
}
