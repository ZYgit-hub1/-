package com.huadianguangdong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 报警传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 报警 ID */
    private Long id;

    /** 报警级别 */
    private String level;

    /** 报警状态 */
    private String status;

    /** 报警内容 */
    private String content;

    /** 电厂 ID */
    private Long plantId;

    /** 电厂名称 */
    private String plantName;

    /** 触发时间 */
    private String triggerTime;
}
