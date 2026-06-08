package com.supcon.supfusion.organization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.organization.dao.mapper.company.CompanyPersonMapper;
import com.supcon.supfusion.organization.dao.mapper.department.DepartmentPersonMapper;
import com.supcon.supfusion.organization.dao.mapper.position.PositionMapper;
import com.supcon.supfusion.organization.dao.mapper.position.PositionPersonMapper;
import com.supcon.supfusion.organization.dao.po.company.CompanyPersonPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentPersonPO;
import com.supcon.supfusion.organization.dao.po.position.PositionAddPO;
import com.supcon.supfusion.organization.dao.po.position.PositionPersonPO;
import com.supcon.supfusion.organization.service.PositionPersonService;
import com.supcon.supfusion.organization.service.PositionService;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import com.supcon.supfusion.organization.service.common.MultiParamSql;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 岗位关联人员管理服务接口
 * @author shidongsheng
 * @date 20-6-4  下午14:31
 */
@Slf4j
@Service
public class PositionPersonServiceImpl extends ServiceImpl<PositionPersonMapper, PositionPersonPO> implements PositionPersonService {

    @Autowired
    private PositionService positionService;

    @Autowired
    private DepartmentPersonMapper departmentPersonMapper;

    @Autowired
    private CompanyPersonMapper companyPersonMapper;

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private PositionPersonMapper positionPersonMapper;
    /**
     * 岗位关联人员新增
     * @param relations
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPositionPerson(List<PositionPersonPO> relations) {
        saveOrUpdateBatch(relations);
    }

    @Override
    public List<Long> queryPersonIdByPositionId(Long positionId) {
        QueryWrapper<PositionPersonPO> queryWrapper = new QueryWrapper<PositionPersonPO>();
        queryWrapper.eq("position_id", positionId);
        queryWrapper.eq("valid", 1);
        List<PositionPersonPO> relations = list(queryWrapper);
        if (relations == null) {
            return null;
        }
        List<Long> personIds = new ArrayList<Long>();
        relations.stream().forEach(relation -> personIds.add(relation.getPersonId()));
        return personIds;
    }

    @Override
    public List<Long> queryPersonIdByPositionIds(List<Long> positionIds) {
        if (positionIds == null || positionIds.size() == 0) {
            return null;
        }
        QueryWrapper<PositionPersonPO> queryWrapper = new QueryWrapper<PositionPersonPO>();
        queryWrapper.select("person_id");
//        queryWrapper.in("position_id", positionIds);
        queryWrapper.eq("valid", 1);
//        List<PositionPersonPO> relations = list(queryWrapper);
        List<PositionPersonPO> relations = MultiParamSql.forPersonPosition(positionIds, queryWrapper, "position_id", positionPersonMapper);
        if (relations == null) {
            return null;
        }
        List<Long> personIds = new ArrayList<Long>();
        relations.stream().forEach(relation -> personIds.add(relation.getPersonId()));
        return personIds;
    }

    @Override
    public List<PersonDetailBO> queryPersonByCompanyId(Long companyId, String keyword) {
        return null;
    }

    /**
     * 根据人员查询岗位
     * @param personId
     * @return
     */
    @Override
    public List<PositionPersonPO> queryPositionByPersonId(Long personId) {
        QueryWrapper<PositionPersonPO> posWrapper = new QueryWrapper<PositionPersonPO>();
        posWrapper.eq("person_id", personId);
        posWrapper.eq("valid", 1);
        return list(posWrapper);
    }

    /**
     * 批量修改岗位人员关系
     * @param list
     */
    @Override
    public void batchSaveOrUpdate(List<PositionPersonPO> list) {

        if (list == null || list.size() == 0) {
            return;
        }
        List<Long> posIds = new ArrayList<>();
        list.stream().forEach(relation -> {
            posIds.add(relation.getPositionId());
            QueryWrapper<PositionPersonPO> queryWrapper = new QueryWrapper<PositionPersonPO>();
            queryWrapper.eq("position_id", relation.getPositionId());
            queryWrapper.eq("person_id", relation.getPersonId());
            saveOrUpdate(relation, queryWrapper);

            QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();
            posWrapper.eq("id", relation.getPositionId());
            posWrapper.eq("valid", 1);
            PositionAddPO pos = positionMapper.selectOne(posWrapper);
            if (pos != null) {
                QueryWrapper<DepartmentPersonPO> deptWrapper = new QueryWrapper<>();
                deptWrapper.eq("valid", 1);
                deptWrapper.eq("dept_id",  pos.getDepId());
                deptWrapper.eq("person_id", relation.getPersonId());
                deptWrapper.eq("position_id", pos.getId());
                int deptCount = departmentPersonMapper.selectCount(deptWrapper);
                if (deptCount == 0) {
                    DepartmentPersonPO departmentPersonPO = new DepartmentPersonPO();
                    departmentPersonPO.setValid(true);
                    departmentPersonPO.setPersonId(relation.getPersonId());
                    departmentPersonPO.setDeptId(pos.getDepId());
                    departmentPersonPO.setPositionId(pos.getId());
                    departmentPersonMapper.insert(departmentPersonPO);
                }
                QueryWrapper<CompanyPersonPO> comWrapper = new QueryWrapper<>();
                comWrapper.eq("valid", 1);
                comWrapper.eq("company_id", pos.getCompanyId());
                comWrapper.eq("person_id", relation.getPersonId());
                comWrapper.eq("position_id", pos.getId());
                int comCount = companyPersonMapper.selectCount(comWrapper);
                if (comCount == 0) {
                    CompanyPersonPO companyPersonPO = new CompanyPersonPO();
                    companyPersonPO.setValid(true);
                    companyPersonPO.setPersonId(relation.getPersonId());
                    companyPersonPO.setCompanyId(pos.getCompanyId());
                    companyPersonPO.setPositionId(pos.getId());
                    companyPersonMapper.insert(companyPersonPO);
                }
            }
        });
    }

    /**
     * @see com.supcon.supfusion.organization.service.PositionPersonService#deleteByPersonId(java.lang.Long)
     */
    @Override
    public void deleteByPersonId(Long personId) {
        UpdateWrapper<PositionPersonPO> updateWrapper = new UpdateWrapper<PositionPersonPO>();
        updateWrapper.eq("person_id", personId);
        updateWrapper.set("valid", 0);
        update(updateWrapper);
    }

    /**
     * @see com.supcon.supfusion.organization.service.PositionPersonService#batchDeleteByPersonId(java.util.List)
     */
    @Override
    public void batchDeleteByPersonId(List<Long> personIds) {
        UpdateWrapper<PositionPersonPO> updateWrapper = new UpdateWrapper<PositionPersonPO>();
        updateWrapper.in("person_id", personIds);
        updateWrapper.set("valid", 0);
        update(updateWrapper);
    }

    /**
     * 批量删除关系
     * @param updateRels
     */
    @Override
    public void batchDeleteRelations(List<PositionPersonPO> updateRels) {
        updateBatchById(updateRels);
    }

    @Override
    public List<PositionPersonPO> getByPersonId(Long personId) {
        QueryWrapper<PositionPersonPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("person_id", personId);
        queryWrapper.eq("valid", 1);
        return list(queryWrapper);
    }

    @Override
    public boolean saveBatchRel(List<PositionPersonPO> list) {
        return saveBatch(list);
    }
}
