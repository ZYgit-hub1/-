package com.huadianguangdong.alert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huadianguangdong.alert.entity.Warning;
import com.huadianguangdong.alert.mapper.WarningMapper;
import com.huadianguangdong.alert.service.WarningService;
import com.huadianguangdong.common.api.PageResult;
import com.huadianguangdong.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 预警业务服务实现
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
public class WarningServiceImpl extends ServiceImpl<WarningMapper, Warning> implements WarningService {

    @Override
    public PageResult<Warning> page(int page, int size, String level, String type, String status, Long plantId) {
        Page<Warning> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Warning> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(level)) {
            wrapper.eq(Warning::getLevel, level);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(Warning::getType, type);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Warning::getStatus, status);
        }
        if (plantId != null) {
            wrapper.eq(Warning::getPlantId, plantId);
        }
        wrapper.orderByDesc(Warning::getStartTime);

        Page<Warning> result = this.page(pageParam, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public Warning getById(Long id) {
        Warning warning = super.getById(id);
        if (warning == null) {
            throw new BusinessException(404, "预警不存在: " + id);
        }
        return warning;
    }

    @Override
    public Warning cancel(Long id) {
        Warning warning = getById(id);
        if (!"active".equals(warning.getStatus())) {
            throw new BusinessException("仅活跃状态的预警可取消");
        }
        LambdaUpdateWrapper<Warning> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Warning::getId, id)
                .set(Warning::getStatus, "cancelled")
                .set(Warning::getEndTime, LocalDateTime.now());
        boolean ok = this.update(updateWrapper);
        if (!ok) {
            throw new BusinessException("预警取消失败: " + id);
        }
        return getById(id);
    }
}
