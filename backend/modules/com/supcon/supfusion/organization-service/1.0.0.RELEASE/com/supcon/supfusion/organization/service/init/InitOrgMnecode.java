package com.supcon.supfusion.organization.service.init;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfoLocalStorage;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.utils.SystemUtil;
import com.supcon.supfusion.organization.dao.mapper.company.CompanyMapper;
import com.supcon.supfusion.organization.dao.mapper.department.DepartmentMapper;
import com.supcon.supfusion.organization.dao.mapper.mnecode.CompanyMneCodeMapper;
import com.supcon.supfusion.organization.dao.mapper.mnecode.DepartmentMneCodeMapper;
import com.supcon.supfusion.organization.dao.mapper.mnecode.PersonMneCodeMapper;
import com.supcon.supfusion.organization.dao.mapper.mnecode.PositionMneCodeMapper;
import com.supcon.supfusion.organization.dao.mapper.person.PersonMapper;
import com.supcon.supfusion.organization.dao.mapper.position.PositionMapper;
import com.supcon.supfusion.organization.dao.po.company.CompanyPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentAddPO;
import com.supcon.supfusion.organization.dao.po.mnecode.CompanyMnecodePO;
import com.supcon.supfusion.organization.dao.po.person.PersonAddPO;
import com.supcon.supfusion.organization.dao.po.position.PositionAddPO;
import com.supcon.supfusion.organization.service.OrgMnecodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Set;

@Component
public class InitOrgMnecode implements ApplicationRunner {
    @Autowired
    private CompanyMapper companyMapper;
    @Autowired
    private DepartmentMapper departmentMapper;
    @Autowired
    private PositionMapper positionMapper;
    @Autowired
    private PersonMapper personMapper;
    @Autowired
    private CompanyMneCodeMapper companyMneCodeMapper;
    @Autowired
    private DepartmentMneCodeMapper departmentMneCodeMapper;
    @Autowired
    private PositionMneCodeMapper positionMneCodeMapper;
    @Autowired
    private PersonMneCodeMapper personMneCodeMapper;
    @Autowired
    private OrgMnecodeService orgMnecodeService;
    @Autowired
    private DataId dataId;


    @Override
    public void run(ApplicationArguments args) {
        if (Constants.LINUX.equalsIgnoreCase(SystemUtil.getOS())) {
            Set<TenantInfo> tenantInfoSet = TenantInfoLocalStorage.getAll();
            tenantInfoSet.forEach(tenantInfo -> {
                RpcContext rpcContext = RpcContext.getContext();
                rpcContext.setTenantId(tenantInfo.getId());
                this.commonInit();
            });
        } else {
            this.commonInit();
        }
    }

    public void commonInit() {
        String dbType = dataId.getDataId();
        //公司
        QueryWrapper<CompanyMnecodePO> companyMneWrapper = new QueryWrapper<>();
        Integer companyCount = companyMneCodeMapper.selectCount(companyMneWrapper);
        if (0 == companyCount) {
            QueryWrapper<CompanyPO> companyWrapper = new QueryWrapper<>();
            companyWrapper.eq("valid", 1);
            List<CompanyPO> companyPOS = companyMapper.selectList(companyWrapper);
            if (!ObjectUtils.isEmpty(companyPOS)) {
                for (CompanyPO companyPO : companyPOS) {
                    orgMnecodeService.addOrgMnecode(
                            companyPO.getId(), companyPO.getShortName(), Constants.COMPANY
                    );
                }
            }
        } else {
            Integer count = companyMneCodeMapper.isChineseMne(dbType);
            if (0 == count) {
                companyMneCodeMapper.delete(new QueryWrapper<>());
                QueryWrapper<CompanyPO> companyWrapper = new QueryWrapper<>();
                companyWrapper.eq("valid", 1);
                List<CompanyPO> companyPOS = companyMapper.selectList(companyWrapper);
                if (!ObjectUtils.isEmpty(companyPOS)) {
                    for (CompanyPO companyPO : companyPOS) {
                        orgMnecodeService.addOrgMnecode(
                                companyPO.getId(), companyPO.getShortName(), Constants.COMPANY
                        );
                    }
                }
            }
        }

        //部门
        Integer deptCount = departmentMneCodeMapper.selectCount(new QueryWrapper<>());
        if (0 == deptCount) {
            List<DepartmentAddPO> departmentAddPOS = departmentMapper.selectList(new QueryWrapper<DepartmentAddPO>().lambda().eq(DepartmentAddPO::getValid, 1));
            if (!ObjectUtils.isEmpty(departmentAddPOS)) {
                departmentAddPOS.forEach(departmentPO -> orgMnecodeService.addOrgMnecode(
                        departmentPO.getId(), departmentPO.getName(), Constants.DEPARTMENT
                ));
            }
        } else {
            Integer count = departmentMneCodeMapper.isChineseMne(dbType);
            if (0 == count) {
                departmentMneCodeMapper.delete(new QueryWrapper<>());
                List<DepartmentAddPO> departmentAddPOS = departmentMapper.selectList(new QueryWrapper<DepartmentAddPO>().lambda().eq(DepartmentAddPO::getValid, 1));
                if (!ObjectUtils.isEmpty(departmentAddPOS)) {
                    departmentAddPOS.forEach(departmentPO -> orgMnecodeService.addOrgMnecode(
                            departmentPO.getId(), departmentPO.getName(), Constants.DEPARTMENT
                    ));
                }
            }
        }

        //岗位
        Integer positionCount = positionMneCodeMapper.selectCount(new QueryWrapper<>());
        if (0 == positionCount) {
            QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<>();
            positionWrapper.eq("valid", 1);
            List<PositionAddPO> positionAddPOS = positionMapper.selectList(positionWrapper);
            if (!ObjectUtils.isEmpty(positionAddPOS)) {
                positionAddPOS.forEach(positionAddPO -> orgMnecodeService.addOrgMnecode(
                        positionAddPO.getId(), positionAddPO.getName(), Constants.POSITION
                ));
            }
        } else {
            Integer count = positionMneCodeMapper.isChineseMne(dbType);
            if (0 == count) {
                positionMneCodeMapper.delete(new QueryWrapper<>());
                QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<>();
                positionWrapper.eq("valid", 1);
                List<PositionAddPO> positionAddPOS = positionMapper.selectList(positionWrapper);
                if (!ObjectUtils.isEmpty(positionAddPOS)) {
                    positionAddPOS.forEach(positionAddPO -> orgMnecodeService.addOrgMnecode(
                            positionAddPO.getId(), positionAddPO.getName(), Constants.POSITION
                    ));
                }
            }
        }

        //人员
        Integer personCount = personMneCodeMapper.selectCount(new QueryWrapper<>());
        if (0 == personCount) {
            QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
            personWrapper.eq("valid", 1);
            List<PersonAddPO> personAddPOS = personMapper.selectList(personWrapper);
            if (!ObjectUtils.isEmpty(personAddPOS)) {
                personAddPOS.forEach(personAddPO -> orgMnecodeService.addOrgMnecode(
                        personAddPO.getId(), personAddPO.getName(), Constants.PERSON
                ));
            }
        } else {
            Integer count = personMneCodeMapper.isChineseMne(dbType);
            if (0 == count) {
                personMneCodeMapper.delete(new QueryWrapper<>());
                QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
                personWrapper.eq("valid", 1);
                List<PersonAddPO> personAddPOS = personMapper.selectList(personWrapper);
                if (!ObjectUtils.isEmpty(personAddPOS)) {
                    personAddPOS.forEach(personAddPO -> orgMnecodeService.addOrgMnecode(
                            personAddPO.getId(), personAddPO.getName(), Constants.PERSON
                    ));
                }
            }
        }
    }

}
