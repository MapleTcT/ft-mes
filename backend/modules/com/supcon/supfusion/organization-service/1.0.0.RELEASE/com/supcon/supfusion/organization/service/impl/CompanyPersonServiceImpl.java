package com.supcon.supfusion.organization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.organization.dao.mapper.company.CompanyPersonMapper;
import com.supcon.supfusion.organization.dao.po.company.CompanyPersonPO;
import com.supcon.supfusion.organization.service.CompanyPersonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CompanyPersonServiceImpl extends ServiceImpl<CompanyPersonMapper, CompanyPersonPO> implements CompanyPersonService {

    @Override
    public void offDepartmentByPersonId(List<Long> positionIds, Long personId) {
        UpdateWrapper<CompanyPersonPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("position_id", positionIds);
        updateWrapper.eq("person_id", personId);
        updateWrapper.set("valid", 0);
        update(updateWrapper);
    }

    @Override
    public void deletePerson(Long personId) {
        UpdateWrapper<CompanyPersonPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("person_id", personId);
        updateWrapper.set("valid", 0);
        update(updateWrapper);
    }

    @Override
    public List<CompanyPersonPO> getCompanyPersonByPersonId(Long personId) {
        QueryWrapper<CompanyPersonPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("person_id", personId);
        queryWrapper.eq("valid", 1);
        return list(queryWrapper);
    }

    @Override
    public boolean saveBatchRel(List<CompanyPersonPO> tmpInsertComs) {
        return saveBatch(tmpInsertComs);
    }
}
