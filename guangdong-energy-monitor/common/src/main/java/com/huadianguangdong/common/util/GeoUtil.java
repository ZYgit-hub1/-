package com.huadianguangdong.common.util;

/**
 * 地理位置工具类
 */
public final class GeoUtil {

    /** 地球平均半径（千米） */
    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtil() {
    }

    /**
     * 使用 Haversine 公式计算两个经纬度坐标之间的球面距离（千米）
     *
     * @param lng1 经度 1
     * @param lat1 纬度 1
     * @param lng2 经度 2
     * @param lat2 纬度 2
     * @return 距离（千米）
     */
    public static double distanceKm(double lng1, double lat1, double lng2, double lat2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * 判断坐标是否在指定经纬度边界范围内（包含边界）
     *
     * @param lng    目标经度
     * @param lat    目标纬度
     * @param minLng 最小经度
     * @param maxLng 最大经度
     * @param minLat 最小纬度
     * @param maxLat 最大纬度
     * @return 是否在范围内
     */
    public static boolean isInBounds(double lng, double lat,
                                     double minLng, double maxLng,
                                     double minLat, double maxLat) {
        return lng >= minLng && lng <= maxLng && lat >= minLat && lat <= maxLat;
    }
}
