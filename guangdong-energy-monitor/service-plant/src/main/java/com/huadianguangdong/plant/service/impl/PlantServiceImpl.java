package com.huadianguangdong.plant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huadianguangdong.common.api.PageResult;
import com.huadianguangdong.common.util.GeoUtil;
import com.huadianguangdong.plant.entity.Plant;
import com.huadianguangdong.plant.mapper.PlantMapper;
import com.huadianguangdong.plant.service.PlantService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 电厂服务实现
 *
 * @author huadianguangdong
 */
@Service
public class PlantServiceImpl extends ServiceImpl<PlantMapper, Plant> implements PlantService {

    @Override
    public PageResult<Plant> page(int page, int size, String type, String status, String keyword) {
        Page<Plant> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Plant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) {
            wrapper.eq(Plant::getType, type);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Plant::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Plant::getName, keyword);
        }
        wrapper.orderByDesc(Plant::getCreateTime);

        Page<Plant> result = this.page(pageParam, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public List<Plant> listNearby(double lng, double lat, double radiusKm) {
        // 查询全部电厂后，用 GeoUtil.distanceKm 按半径过滤
        List<Plant> all = this.list();
        return all.stream()
                .filter(p -> p.getLng() != null && p.getLat() != null)
                .filter(p -> GeoUtil.distanceKm(lng, lat, p.getLng(), p.getLat()) <= radiusKm)
                .collect(Collectors.toList());
    }
}
