package com.huadianguangdong.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huadianguangdong.user.entity.SysOrg;
import com.huadianguangdong.user.mapper.SysOrgMapper;
import com.huadianguangdong.user.service.SysOrgService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统组织架构服务实现
 *
 * @author huadianguangdong
 */
@Service
public class SysOrgServiceImpl extends ServiceImpl<SysOrgMapper, SysOrg> implements SysOrgService {

    @Override
    public List<SysOrg> tree() {
        // 查询全部组织，按排序号升序
        List<SysOrg> all = this.list(new LambdaQueryWrapper<SysOrg>()
                .orderByAsc(SysOrg::getSort));
        if (CollUtil.isEmpty(all)) {
            return new ArrayList<>();
        }
        // 按 parentId 分组，parentId 为空视为根（0）
        Map<Long, List<SysOrg>> grouped = all.stream()
                .collect(Collectors.groupingBy(o -> o.getParentId() == null ? 0L : o.getParentId()));
        // 递归填充 children（直接写入实体，避免引入额外树节点类型，便于前端消费）
        fillChildren(grouped, all);
        // 返回根节点列表
        return grouped.getOrDefault(0L, new ArrayList<>());
    }

    /**
     * 递归为每个节点填充 children 集合
     *
     * @param grouped 按 parentId 分组的组织集合
     * @param nodes   全部节点
     */
    private void fillChildren(Map<Long, List<SysOrg>> grouped, List<SysOrg> nodes) {
        for (SysOrg node : nodes) {
            Long id = node.getId();
            List<SysOrg> children = grouped.getOrDefault(id, new ArrayList<>());
            // 通过 @TableField(exist=false) 修饰的 children 字段承载子节点
            node.setChildren(children);
            // 递归处理子节点
            fillChildren(grouped, children);
        }
    }
}
