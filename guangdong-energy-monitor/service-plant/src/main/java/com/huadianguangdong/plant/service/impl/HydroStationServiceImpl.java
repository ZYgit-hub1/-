package com.huadianguangdong.plant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huadianguangdong.common.api.PageResult;
import com.huadianguangdong.plant.entity.HydroStation;
import com.huadianguangdong.plant.mapper.HydroStationMapper;
import com.huadianguangdong.plant.service.HydroStationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 水文站服务实现
 *
 * @author huadianguangdong
 */
@Service
public class HydroStationServiceImpl extends ServiceImpl<HydroStationMapper, HydroStation> implements HydroStationService {

    @Override
    public PageResult<HydroStation> page(int page, int size, String city, String river, String keyword) {
        Page<HydroStation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<HydroStation> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(city)) {
            wrapper.eq(HydroStation::getCity, city);
        }
        if (StringUtils.hasText(river)) {
            wrapper.eq(HydroStation::getRiver, river);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(HydroStation::getName, keyword);
        }
        wrapper.orderByDesc(HydroStation::getCreateTime);

        Page<HydroStation> result = this.page(pageParam, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public List<HydroStation> listByCity(String city) {
        return this.list(new LambdaQueryWrapper<HydroStation>()
                .eq(HydroStation::getCity, city)
                .orderByDesc(HydroStation::getCreateTime));
    }

    @Override
    public List<HydroStation> listByRiver(String river) {
        return this.list(new LambdaQueryWrapper<HydroStation>()
                .eq(HydroStation::getRiver, river)
                .orderByDesc(HydroStation::getCreateTime));
    }
}
