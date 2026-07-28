package com.huadianguangdong.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huadianguangdong.alert.entity.Alarm;
import com.huadianguangdong.common.api.PageResult;

/**
 * 报警业务服务接口
 *
 * @author huadianguangdong
 */
public interface AlarmService extends IService<Alarm> {

    /**
     * 分页查询报警
     *
     * @param page      页码
     * @param size      每页大小
     * @param level     报警级别（可空）
     * @param status    报警状态（可空）
     * @param plantId   电厂 ID（可空）
     * @param startTime 触发开始时间（可空，格式 yyyy-MM-dd HH:mm:ss）
     * @param endTime   触发结束时间（可空）
     * @return 分页结果
     */
    PageResult<Alarm> page(int page, int size, String level, String status, Long plantId, String startTime, String endTime);

    /**
     * 根据 ID 查询报警
     *
     * @param id 报警 ID
     * @return 报警对象
     */
    Alarm getById(Long id);

    /**
     * 确认报警
     *
     * @param id      报警 ID
     * @param handler 处理人
     * @return 更新后的报警
     */
    Alarm confirm(Long id, String handler);

    /**
     * 解除报警
     *
     * @param id      报警 ID
     * @param handler 处理人
     * @param remark  备注
     * @return 更新后的报警
     */
    Alarm resolve(Long id, String handler, String remark);

    /**
     * 推送报警通知（多通道）
     *
     * @param alarm 报警对象
     */
    void pushNotification(Alarm alarm);
}
