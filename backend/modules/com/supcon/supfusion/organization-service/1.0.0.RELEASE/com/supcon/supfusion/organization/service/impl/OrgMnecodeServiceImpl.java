package com.supcon.supfusion.organization.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.MneCodeGenterate;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.dao.mapper.mnecode.*;
import com.supcon.supfusion.organization.dao.po.mnecode.*;
import com.supcon.supfusion.organization.service.OrgMnecodeService;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class OrgMnecodeServiceImpl extends ServiceImpl<OrgMnecodeMapper, OrgMnecodePO> implements OrgMnecodeService {

    @Autowired
    private CompanyMneCodeMapper companyMneCodeMapper;
    @Autowired
    private DepartmentMneCodeMapper departmentMneCodeMapper;
    @Autowired
    private PositionMneCodeMapper positionMneCodeMapper;
    @Autowired
    private PersonMneCodeMapper personMneCodeMapper;
    @Autowired
    private DataId dataId;
    @Autowired
    private CompanyMneCodeServiceImpl companyMneCodeService;
    @Autowired
    private DepartmentMneCodeServiceImpl departmentService;
    @Autowired
    private PositionMneCodeServiceImpl positionMneCodeService;
    @Autowired
    private PersonMneCodeServiceImpl personMneCodeService;

    /**
     * 增加助记码
     *
     * @param orgId        公司、部门id、岗位id、人员id
     * @param orgMneSource 原始名
     * @param option       类型
     */
    @Override
    public void addOrgMnecode(Long orgId, String orgMneSource, String option) {
        if (orgId == null || StringUtils.isBlank(orgMneSource)) {
            return;
        }
        List<String> orgMnecodes = MneCodeGenterate.mneCodeTupleGenerate(orgMneSource);
        if (orgMnecodes == null || orgMnecodes.size() == 0) {
            return;
        }

        String dbType = dataId.getDataId();
        switch (option) {
            //公司
            case Constants.COMPANY:
                List<CompanyMnecodePO> companyMnecodePOS = new ArrayList<>();
                orgMnecodes.forEach(code -> {
                    CompanyMnecodePO companyMnecodePO = new CompanyMnecodePO();
                    companyMnecodePO.setId(IDGenerator.newInstance().generate().longValue());
                    companyMnecodePO.setCompanyId(orgId);
                    companyMnecodePO.setMneCode(code);
                    companyMnecodePO.setCompanyShortName(orgMneSource);
                    companyMnecodePO.setRowVersion(0L);
                    companyMnecodePOS.add(companyMnecodePO);
                });
                CompanyMnecodePO companyMnecodePO = new CompanyMnecodePO();
                companyMnecodePO.setId(IDGenerator.newInstance().generate().longValue());
                companyMnecodePO.setCompanyId(orgId);
                companyMnecodePO.setMneCode(orgMneSource);
                companyMnecodePO.setCompanyShortName(orgMneSource);
                companyMnecodePO.setRowVersion(0L);
                companyMnecodePOS.add(companyMnecodePO);
//                companyMneCodeMapper.insertBatch(companyMnecodePOS,dbType);
                companyMneCodeService.saveBatch(companyMnecodePOS);
                break;

            //部门
            case Constants.DEPARTMENT:
                List<DepartmentMnecodePO> departmentMnecodePOS = new ArrayList<>();
                orgMnecodes.forEach(code -> {
                    DepartmentMnecodePO departmentMnecodePO = new DepartmentMnecodePO();
                    departmentMnecodePO.setId(IDGenerator.newInstance().generate().longValue());
                    departmentMnecodePO.setDeptId(orgId);
                    departmentMnecodePO.setMneCode(code);
                    departmentMnecodePO.setDeptName(orgMneSource);
                    departmentMnecodePO.setRowVersion(0L);
                    departmentMnecodePOS.add(departmentMnecodePO);
                });
                DepartmentMnecodePO departmentMnecodePO = new DepartmentMnecodePO();
                departmentMnecodePO.setId(IDGenerator.newInstance().generate().longValue());
                departmentMnecodePO.setDeptId(orgId);
                departmentMnecodePO.setMneCode(orgMneSource);
                departmentMnecodePO.setDeptName(orgMneSource);
                departmentMnecodePO.setRowVersion(0L);
                departmentMnecodePOS.add(departmentMnecodePO);
//                departmentMneCodeMapper.insertBatch(departmentMnecodePOS,dbType);
                departmentService.saveBatch(departmentMnecodePOS);
                break;

            //岗位
            case Constants.POSITION:
                List<PositionMnecodePO> positionMnecodePOS = new ArrayList<>();
                orgMnecodes.forEach(code -> {
                    PositionMnecodePO positionMnecodePO = new PositionMnecodePO();
                    positionMnecodePO.setId(IDGenerator.newInstance().generate().longValue());
                    positionMnecodePO.setPositionId(orgId);
                    positionMnecodePO.setMneCode(code);
                    positionMnecodePO.setPositionName(orgMneSource);
                    positionMnecodePO.setRowVersion(0L);
                    positionMnecodePOS.add(positionMnecodePO);
                });
                PositionMnecodePO positionMnecodePO = new PositionMnecodePO();
                positionMnecodePO.setId(IDGenerator.newInstance().generate().longValue());
                positionMnecodePO.setPositionId(orgId);
                positionMnecodePO.setMneCode(orgMneSource);
                positionMnecodePO.setPositionName(orgMneSource);
                positionMnecodePO.setRowVersion(0L);
                positionMnecodePOS.add(positionMnecodePO);
//                positionMneCodeMapper.insertBatch(positionMnecodePOS,dbType);
                positionMneCodeService.saveBatch(positionMnecodePOS);
                break;

            //人员
            case Constants.PERSON:
                List<PersonMnecodePO> personMnecodePOS = new ArrayList<>();
                orgMnecodes.forEach(code -> {
                    PersonMnecodePO personMnecodePO = new PersonMnecodePO();
                    personMnecodePO.setId(IDGenerator.newInstance().generate().longValue());
                    personMnecodePO.setPersonId(orgId);
                    personMnecodePO.setMneCode(code);
                    personMnecodePO.setPersonName(orgMneSource);
                    personMnecodePO.setRowVersion(0L);
                    personMnecodePOS.add(personMnecodePO);
                });
                PersonMnecodePO personMnecodePO = new PersonMnecodePO();
                personMnecodePO.setId(IDGenerator.newInstance().generate().longValue());
                personMnecodePO.setPersonId(orgId);
                personMnecodePO.setMneCode(orgMneSource);
                personMnecodePO.setPersonName(orgMneSource);
                personMnecodePO.setRowVersion(0L);
                personMnecodePOS.add(personMnecodePO);
//                personMneCodeMapper.insertBatch(personMnecodePOS,dbType);
                personMneCodeService.saveBatch(personMnecodePOS);
                break;
        }
    }

    @Override
    public void deleteOrgMnecodeByOrgId(Long orgId, String option) {
        switch (option) {
            case Constants.COMPANY:
                QueryWrapper<CompanyMnecodePO> companyWrapper = new QueryWrapper<>();
                companyWrapper.eq("company_id", orgId);
                companyMneCodeMapper.delete(companyWrapper);
                break;
            case Constants.DEPARTMENT:
                QueryWrapper<DepartmentMnecodePO> deptWrapper = new QueryWrapper<>();
                deptWrapper.eq("dept_id", orgId);
                departmentMneCodeMapper.delete(deptWrapper);
                break;
            case Constants.POSITION:
                QueryWrapper<PositionMnecodePO> positionWrapper = new QueryWrapper<>();
                positionWrapper.eq("position_id", orgId);
                positionMneCodeMapper.delete(positionWrapper);
                break;
            case Constants.PERSON:
                QueryWrapper<PersonMnecodePO> personWrapper = new QueryWrapper<>();
                personWrapper.eq("person_id", orgId);
                personMneCodeMapper.delete(personWrapper);
                break;
        }
    }

    @Override
    public void deleteOrgMnecodeByOrgId(List<Long> orgIds, String option) {
        switch (option) {
            case Constants.COMPANY:
                QueryWrapper<CompanyMnecodePO> companyWrapper = new QueryWrapper<>();
                companyWrapper.in("company_id", orgIds);
                companyMneCodeMapper.delete(companyWrapper);
                break;
            case Constants.DEPARTMENT:
                QueryWrapper<DepartmentMnecodePO> deptWrapper = new QueryWrapper<>();
                deptWrapper.in("dept_id", orgIds);
                departmentMneCodeMapper.delete(deptWrapper);
                break;
            case Constants.POSITION:
                QueryWrapper<PositionMnecodePO> positionWrapper = new QueryWrapper<>();
                positionWrapper.in("position_id", orgIds);
                positionMneCodeMapper.delete(positionWrapper);
                break;
            case Constants.PERSON:
                QueryWrapper<PersonMnecodePO> personWrapper = new QueryWrapper<>();
                personWrapper.in("person_id", orgIds);
                personMneCodeMapper.delete(personWrapper);
                break;
        }
    }
}
