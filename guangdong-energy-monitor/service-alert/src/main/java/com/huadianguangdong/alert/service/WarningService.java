package com.huadianguangdong.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huadianguangdong.alert.entity.Warning;
import com.huadianguangdong.common.api.PageResult;

/**
 * 预警业务服务接口
 *
 * @author huadianguangdong
 */
public interface WarningService extends IService<Warning> {

    /**
     * 分页查询预警
     *
     * @param page    页码
     * @param size    每页大小
     * @param level   预警级别（可空）
     * @param type    预警类型（可空）
     * @param status  状态（可空）
     * @param plantId 电厂 ID（可空）
     * @return 分页结果
     */
    PageResult<Warning> page(int page, int size, String level, String type, String status, Long plantId);

    /**
     * 根据 ID 查询预警
     *
     * @param id 预警 ID
     * @return 预警对象
     */
    Warning getById(Long id);

    /**
     * 取消预警
     *
     * @param id 预警 ID
     * @return 更新后的预警
     */
    Warning cancel(Long id);
}
