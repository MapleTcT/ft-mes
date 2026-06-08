package com.supcon.supfusion.organization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.organization.dao.mapper.department.DepartmentPersonMapper;
import com.supcon.supfusion.organization.dao.po.company.CompanyPersonPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentPersonPO;
import com.supcon.supfusion.organization.service.DepartmentPersonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DepartmentPersonServiceImpl extends ServiceImpl<DepartmentPersonMapper, DepartmentPersonPO> implements DepartmentPersonService {

    @Override
    public void offDepartmentByPersonId(List<Long> positionIds, Long personId) {
        UpdateWrapper<DepartmentPersonPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("position_id", positionIds);
        updateWrapper.eq("person_id", personId);
        updateWrapper.set("valid", 0);
        update(updateWrapper);
    }

    @Override
    public void deletePerson(Long personId) {
        UpdateWrapper<DepartmentPersonPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("person_id", personId);
        updateWrapper.set("valid", 0);
        update(updateWrapper);
    }

    @Override
    public List<DepartmentPersonPO> getdepartmentPersonByPersonId(Long personId) {
        QueryWrapper<DepartmentPersonPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("person_id", personId);
        queryWrapper.eq("valid", 1);
        return list(queryWrapper);
    }

    @Override
    public boolean saveBatchRel(List<DepartmentPersonPO> tmpInsertDepts) {
        return saveBatch(tmpInsertDepts);
    }
}
