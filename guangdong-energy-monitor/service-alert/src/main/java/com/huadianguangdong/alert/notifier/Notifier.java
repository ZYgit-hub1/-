package com.huadianguangdong.alert.notifier;

import com.huadianguangdong.alert.entity.Alarm;

/**
 * 通知通道接口
 * <p>
 * 不同实现代表不同的推送通道（短信、邮件、Webhook 等），由 {@code AlarmService.pushNotification} 统一调度。
 *
 * @author huadianguangdong
 */
public interface Notifier {

    /**
     * 推送报警通知
     *
     * @param alarm 报警对象
     */
    void notify(Alarm alarm);

    /**
     * 通道标识
     *
     * @return 通道名称
     */
    default String channel() {
        return this.getClass().getSimpleName();
    }
}
