package com.huadianguangdong.collector.weather;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 电厂厂区代码映射器
 * <p>
 * 维护"气象站点代码 / 城市名 → 电厂 ID + 行政区划代码"的映射关系。
 * <p>
 * 真实场景下应从 PostgreSQL t_power_plant 表加载，此处提供内存映射 + 可刷新接口。
 *
 * @author huadianguangdong
 */
public final class PlantCodeMapper {

    /** 厂区代码 → 电厂映射 */
    private static final Map<String, PlantMapping> CODE_MAP = new ConcurrentHashMap<>();

    static {
        // 初始化默认映射（华电广东电厂）
        put("GD001", "440500", "汕头", 1L);   // 汕头华电
        put("GD002", "440100", "广州", 2L);   // 华电福新广州
        put("GD003", "441800", "清远", 3L);   // 华电清远
        put("GD004", "441300", "惠州", 4L);   // 华电惠州
        put("GD005", "440200", "韶关", 5L);   // 华电坪石
        put("GD006", "440200", "韶关", 6L);   // 华电韶关热电
        put("GD007", "441700", "阳江", 7L);   // 华电阳江海上风电
        put("GD008", "440100", "广州", 8L);   // 华电新能源
        put("GD009", "440300", "深圳", 9L);   // 华电深圳
    }

    private PlantCodeMapper() {
    }

    /** 添加映射 */
    public static void put(String stationCode, String districtCode, String city, Long plantId) {
        CODE_MAP.put(stationCode, new PlantMapping(plantId, districtCode, city));
    }

    /** 按厂区代码查询 */
    public static Optional<PlantMapping> findByCode(String stationCode) {
        if (stationCode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(CODE_MAP.get(stationCode));
    }

    /** 按城市名查询第一个匹配的电厂 */
    public static Optional<PlantMapping> findByCity(String city) {
        if (city == null) {
            return Optional.empty();
        }
        return CODE_MAP.values().stream()
                .filter(m -> city.equals(m.city()))
                .findFirst();
    }

    /**
     * 电厂映射记录
     *
     * @param plantId      电厂 ID
     * @param districtCode 行政区划代码
     * @param city         所在城市
     */
    public record PlantMapping(Long plantId, String districtCode, String city) {
    }
}
