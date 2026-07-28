package com.huadianguangdong.common.constant;

/**
 * 公共常量
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    // ==================== Kafka Topic ====================

    /** 报警 Topic */
    public static final String TOPIC_ALARM = "alarm-topic";

    /** 水文数据 Topic */
    public static final String TOPIC_HYDRO_DATA = "hydro-data-topic";

    /** 气象数据 Topic */
    public static final String TOPIC_WEATHER_DATA = "weather-data-topic";

    /** 电厂状态 Topic */
    public static final String TOPIC_PLANT_STATUS = "plant-status-topic";

    // ==================== Redis Key 前缀 ====================

    /** 用户 Token 前缀 */
    public static final String REDIS_TOKEN_PREFIX = "auth:token:";

    /** 用户信息前缀 */
    public static final String REDIS_USER_PREFIX = "user:";

    /** 电厂信息前缀 */
    public static final String REDIS_PLANT_PREFIX = "plant:";

    // ==================== Cache 名 ====================

    /** 电厂缓存 */
    public static final String CACHE_PLANT = "plant";

    /** 水文站缓存 */
    public static final String CACHE_HYDRO_STATION = "hydro-station";

    /** 字典缓存 */
    public static final String CACHE_DICT = "dict";

    /** 用户权限缓存 */
    public static final String CACHE_USER_AUTH = "user-auth";

    // ==================== 通用常量 ====================

    /** UTF-8 编码 */
    public static final String UTF_8 = "UTF-8";

    /** 默认页码 */
    public static final int DEFAULT_PAGE = 1;

    /** 默认每页大小 */
    public static final int DEFAULT_SIZE = 10;

    /** 成功标识 */
    public static final String SUCCESS = "success";

    /** 失败标识 */
    public static final String FAIL = "fail";
}
