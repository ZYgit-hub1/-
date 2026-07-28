package com.huadianguangdong.alert.notifier;

import com.huadianguangdong.alert.entity.Alarm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信通知通道
 * <p>
 * 简化实现：仅打印日志。TODO 接入实际短信网关（如阿里云短信 / 腾讯云短信）。
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
public class SmsNotifier implements Notifier {

    @Override
    public void notify(Alarm alarm) {
        // TODO 接入实际短信网关 SDK
        log.info("[SMS通知] alarmId={}, level={}, content={}, handler={}",
                alarm.getId(), alarm.getLevel(), alarm.getContent(), alarm.getHandler());
    }

    @Override
    public String channel() {
        return "SMS";
    }
}
