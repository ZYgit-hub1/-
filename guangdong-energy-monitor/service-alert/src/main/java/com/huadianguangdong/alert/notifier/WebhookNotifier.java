package com.huadianguangdong.alert.notifier;

import com.huadianguangdong.alert.entity.Alarm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Webhook 通知通道
 * <p>
 * 简化实现：仅打印日志。TODO 接入企业微信 / 钉钉 / 飞书 Webhook，或自建回调地址。
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
public class WebhookNotifier implements Notifier {

    @Override
    public void notify(Alarm alarm) {
        // TODO 使用 RestTemplate / WebClient POST 到配置的 Webhook URL
        log.info("[WEBHOOK通知] alarmId={}, level={}, content={}",
                alarm.getId(), alarm.getLevel(), alarm.getContent());
    }

    @Override
    public String channel() {
        return "WEBHOOK";
    }
}
