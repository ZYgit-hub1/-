package com.huadianguangdong.alert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huadianguangdong.alert.entity.Alarm;
import com.huadianguangdong.alert.mapper.AlarmMapper;
import com.huadianguangdong.alert.notifier.Notifier;
import com.huadianguangdong.alert.service.AlarmService;
import com.huadianguangdong.common.api.PageResult;
import com.huadianguangdong.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 报警业务服务实现
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
public class AlarmServiceImpl extends ServiceImpl<AlarmMapper, Alarm> implements AlarmService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired(required = false)
    private List<Notifier> notifiers;

    @Override
    public PageResult<Alarm> page(int page, int size, String level, String status, Long plantId, String startTime, String endTime) {
        Page<Alarm> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Alarm> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(level)) {
            wrapper.eq(Alarm::getLevel, level);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Alarm::getStatus, status);
        }
        if (plantId != null) {
            wrapper.eq(Alarm::getPlantId, plantId);
        }
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(Alarm::getTriggerTime, LocalDateTime.parse(startTime, DATE_TIME_FORMATTER));
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(Alarm::getTriggerTime, LocalDateTime.parse(endTime, DATE_TIME_FORMATTER));
        }
        wrapper.orderByDesc(Alarm::getTriggerTime);

        Page<Alarm> result = this.page(pageParam, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public Alarm getById(Long id) {
        Alarm alarm = super.getById(id);
        if (alarm == null) {
            throw new BusinessException(404, "报警不存在: " + id);
        }
        return alarm;
    }

    @Override
    public Alarm confirm(Long id, String handler) {
        Alarm alarm = getById(id);
        if ("resolved".equals(alarm.getStatus())) {
            throw new BusinessException("报警已解除，无法确认");
        }
        LambdaUpdateWrapper<Alarm> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Alarm::getId, id)
                .set(Alarm::getStatus, "confirmed")
                .set(Alarm::getHandler, handler)
                .set(Alarm::getConfirmTime, LocalDateTime.now());
        boolean ok = this.update(updateWrapper);
        if (!ok) {
            throw new BusinessException("报警确认失败: " + id);
        }
        return getById(id);
    }

    @Override
    public Alarm resolve(Long id, String handler, String remark) {
        Alarm alarm = getById(id);
        if (!"confirmed".equals(alarm.getStatus())) {
            throw new BusinessException("仅已确认的报警可解除");
        }
        LambdaUpdateWrapper<Alarm> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Alarm::getId, id)
                .set(Alarm::getStatus, "resolved")
                .set(Alarm::getHandler, handler)
                .set(Alarm::getRemark, remark)
                .set(Alarm::getResolveTime, LocalDateTime.now());
        boolean ok = this.update(updateWrapper);
        if (!ok) {
            throw new BusinessException("报警解除失败: " + id);
        }
        return getById(id);
    }

    @Override
    public void pushNotification(Alarm alarm) {
        if (notifiers == null || notifiers.isEmpty()) {
            log.warn("未配置任何通知通道，跳过推送 alarmId={}", alarm.getId());
            return;
        }
        for (Notifier notifier : notifiers) {
            try {
                notifier.notify(alarm);
            } catch (Exception e) {
                log.error("通知通道推送失败: {} alarmId={}", notifier.getClass().getSimpleName(), alarm.getId(), e);
            }
        }
    }
}
