package com.huadianguangdong.alert.notifier;

import com.huadianguangdong.alert.entity.Alarm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮件通知通道
 * <p>
 * 简化实现：仅打印日志。TODO 接入 JavaMailSender 实现邮件推送。
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
public class EmailNotifier implements Notifier {

    @Override
    public void notify(Alarm alarm) {
        // TODO 注入 JavaMailSender，构造报警邮件发送
        log.info("[EMAIL通知] alarmId={}, level={}, content={}",
                alarm.getId(), alarm.getLevel(), alarm.getContent());
    }

    @Override
    public String channel() {
        return "EMAIL";
    }
}
