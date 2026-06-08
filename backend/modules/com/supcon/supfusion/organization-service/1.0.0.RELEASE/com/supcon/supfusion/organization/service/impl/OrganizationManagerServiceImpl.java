package com.supcon.supfusion.organization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.organization.dao.mapper.person.OrganizationManagerMapper;
import com.supcon.supfusion.organization.dao.mapper.person.PersonMapper;
import com.supcon.supfusion.organization.dao.po.person.OrganizationManagerPO;
import com.supcon.supfusion.organization.dao.po.person.PersonAddPO;
import com.supcon.supfusion.organization.service.OrganizationManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 负责人实现
 */
@Slf4j
@Service
public class OrganizationManagerServiceImpl extends ServiceImpl<OrganizationManagerMapper, OrganizationManagerPO> implements OrganizationManagerService {

    @Autowired
    private PersonMapper personMapper;

    /**
     * 新增负责人
     * @param managerIds
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addManager(List<Long> managerIds, Long orgId, String type) {
        if (managerIds == null || managerIds.size() == 0) {
            QueryWrapper<OrganizationManagerPO> managerWrapper = new QueryWrapper<OrganizationManagerPO>();
            managerWrapper.eq("org_id", orgId);
            remove(managerWrapper);
            return;
        }
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
        queryWrapper.in("id", managerIds);
        List<PersonAddPO> persons = personMapper.selectList(queryWrapper);
        if (persons == null) {
            return;
        }
        List<OrganizationManagerPO> list = new ArrayList<OrganizationManagerPO>();
        persons.stream().forEach(person -> {
            OrganizationManagerPO organizationManagerPO = new OrganizationManagerPO();
            organizationManagerPO.setManagerId(person.getId());
            organizationManagerPO.setManagerName(person.getName());
            organizationManagerPO.setOrgId(orgId);
            organizationManagerPO.setManagerType(type);
            list.add(organizationManagerPO);
        });
        QueryWrapper<OrganizationManagerPO> managerWrapper = new QueryWrapper<OrganizationManagerPO>();
        managerWrapper.eq("org_id", orgId);
        managerWrapper.eq("manager_type", type);
        remove(managerWrapper);
        saveBatch(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteManagers(List<Long> managerIds) {
        if (managerIds == null || managerIds.size() == 0) {
            return;
        }
        QueryWrapper<OrganizationManagerPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("manager_id", managerIds);
        remove(queryWrapper);
    }

}
