package com.supcon.supfusion.auth.service;

import com.supcon.supfusion.auth.service.bo.DashboardConfigBO;

import java.util.List;

public interface DashboardConfigService {


    int insertUserConfigDashboard(DashboardConfigBO dashboardConfigBO);

    DashboardConfigBO findUserConfigDashboard(Long userId, String mkey);

    List<DashboardConfigBO> findUserConfigDashboards(Long userId, List<String> mkeys);

    int updateUserConfigDashboard(DashboardConfigBO dashboardConfigBO);

    int deleteUserConfigDashboards(Long userId, List<String> mkeys);

    Integer isExistUserConfigDashboard(Long userId, String mkey);

}
