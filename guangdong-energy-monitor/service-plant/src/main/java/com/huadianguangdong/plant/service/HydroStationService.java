package com.huadianguangdong.plant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huadianguangdong.common.api.PageResult;
import com.huadianguangdong.plant.entity.HydroStation;

import java.util.List;

/**
 * 水文站服务接口
 *
 * @author huadianguangdong
 */
public interface HydroStationService extends IService<HydroStation> {

    /**
     * 分页查询水文站
     *
     * @param page    页码
     * @param size    每页大小
     * @param city    所属城市（可空）
     * @param river   所属河流（可空）
     * @param keyword 名称关键字（可空）
     * @return 分页结果
     */
    PageResult<HydroStation> page(int page, int size, String city, String river, String keyword);

    /**
     * 按城市查询水文站
     *
     * @param city 城市
     * @return 水文站列表
     */
    List<HydroStation> listByCity(String city);

    /**
     * 按河流查询水文站
     *
     * @param river 河流
     * @return 水文站列表
     */
    List<HydroStation> listByRiver(String river);
}
