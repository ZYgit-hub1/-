package com.huadianguangdong.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huadianguangdong.collector.entity.WeatherData;
import org.apache.ibatis.annotations.Mapper;

/**
 * 气象数据 Mapper
 *
 * @author huadianguangdong
 */
@Mapper
public interface WeatherDataMapper extends BaseMapper<WeatherData> {
}
