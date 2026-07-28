package com.huadianguangdong.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huadianguangdong.alert.entity.Alarm;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报警 Mapper
 *
 * @author huadianguangdong
 */
@Mapper
public interface AlarmMapper extends BaseMapper<Alarm> {
}
