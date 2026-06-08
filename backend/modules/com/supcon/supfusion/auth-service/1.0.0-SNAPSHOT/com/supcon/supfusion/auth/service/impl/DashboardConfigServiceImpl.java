package com.supcon.supfusion.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.supcon.supfusion.auth.dao.mapper.DashboardConfigMapper;
import com.supcon.supfusion.auth.dao.po.DashboardConfigPO;
import com.supcon.supfusion.auth.service.DashboardConfigService;
import com.supcon.supfusion.auth.service.bo.DashboardConfigBO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DashboardConfigServiceImpl implements DashboardConfigService {
    @Resource
    private DashboardConfigMapper dashboardConfigMapper;

    @Override
    public int insertUserConfigDashboard(DashboardConfigBO dashboardConfigBO) {
        DashboardConfigPO dashboardConfigPO = new DashboardConfigPO();
        BeanUtils.copyProperties(dashboardConfigBO, dashboardConfigPO);
        return dashboardConfigMapper.insert(dashboardConfigPO);
    }

    @Override
    public DashboardConfigBO findUserConfigDashboard(Long userId, String mkey) {
        DashboardConfigPO dashboardConfigPO = dashboardConfigMapper.selectOne(new QueryWrapper<DashboardConfigPO>().lambda().eq(DashboardConfigPO::getMkey, mkey)
                .eq(DashboardConfigPO::getUserId, userId));
        if (dashboardConfigPO != null) {
            DashboardConfigBO dashboardConfigBO = new DashboardConfigBO();
            BeanUtils.copyProperties(dashboardConfigPO, dashboardConfigBO);
            return dashboardConfigBO;
        }
        return null;
    }

    @Override
    public List<DashboardConfigBO> findUserConfigDashboards(Long userId, List<String> mkeys) {
        List<DashboardConfigPO> dashboardConfigPOS = dashboardConfigMapper.selectList(new QueryWrapper<DashboardConfigPO>().lambda().in(DashboardConfigPO::getMkey, mkeys)
                .eq(DashboardConfigPO::getUserId, userId));
        List<DashboardConfigBO> collect = dashboardConfigPOS.stream().map(t -> {
            DashboardConfigBO dashboardConfigBO = new DashboardConfigBO();
            BeanUtils.copyProperties(t, dashboardConfigBO);
            return dashboardConfigBO;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public int updateUserConfigDashboard(DashboardConfigBO dashboardConfigBO) {
        return dashboardConfigMapper.update(null, new UpdateWrapper<DashboardConfigPO>().lambda()
                .set(DashboardConfigPO::getConfigInfo, dashboardConfigBO.getConfigInfo())
                .set(DashboardConfigPO::getFields, dashboardConfigBO.getFields())
                .eq(DashboardConfigPO::getMkey, dashboardConfigBO.getMkey())
                .eq(DashboardConfigPO::getUserId, dashboardConfigBO.getUserId()));
    }


    @Override
    public int deleteUserConfigDashboards(Long userId, List<String> mkeys) {
        return dashboardConfigMapper.delete(new UpdateWrapper<DashboardConfigPO>().lambda()
                .eq(DashboardConfigPO::getUserId, userId)
                .in(DashboardConfigPO::getMkey, mkeys));

    }


    @Override
    public Integer isExistUserConfigDashboard(Long userId, String mkey) {
        return dashboardConfigMapper.selectCount(new QueryWrapper<DashboardConfigPO>().lambda().eq(DashboardConfigPO::getMkey, mkey)
                .eq(DashboardConfigPO::getUserId, userId));
    }
}
