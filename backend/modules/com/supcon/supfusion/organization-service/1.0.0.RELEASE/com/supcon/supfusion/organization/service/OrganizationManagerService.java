package com.supcon.supfusion.organization.service;

import com.supcon.supfusion.organization.dao.po.person.OrganizationManagerPO;

import java.util.List;

/**
 * 负责人管理
 */
public interface OrganizationManagerService {

    /**
     * 新增负责人
     * @param managerIds
     * @param orgId
     */
    void addManager(List<Long> managerIds, Long orgId, String type);

    /**
     * 删除负责人
     * @param managerIds
     */
    void deleteManagers(List<Long> managerIds);

}
