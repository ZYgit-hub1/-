package com.huadianguangdong.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huadianguangdong.analysis.entity.DailyStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统计日报表 Mapper
 *
 * @author huadianguangdong
 */
@Mapper
public interface DailyStatsMapper extends BaseMapper<DailyStats> {
}
