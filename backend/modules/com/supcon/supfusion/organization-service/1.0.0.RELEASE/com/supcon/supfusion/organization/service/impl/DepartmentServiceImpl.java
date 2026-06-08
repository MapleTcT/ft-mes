package com.supcon.supfusion.organization.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.supcon.supfusion.auth.api.dto.UserDetailDTO;
import com.supcon.supfusion.framework.cloud.common.exception.BizHttpStatusException;
import com.supcon.supfusion.framework.cloud.common.message.Message;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.DepartmentErrorEnum;
import com.supcon.supfusion.organization.common.exception.DepartmentException;
import com.supcon.supfusion.organization.common.exception.OrganizationErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationException;
import com.supcon.supfusion.organization.common.kafka.OrganizationMessage;
import com.supcon.supfusion.organization.common.model.ExcelTitle;
import com.supcon.supfusion.organization.common.utils.ExcelUtils;
import com.supcon.supfusion.organization.common.utils.OrgBaseUtils;
import com.supcon.supfusion.organization.dao.mapper.company.CompanyMapper;
import com.supcon.supfusion.organization.dao.mapper.company.CompanyPersonMapper;
import com.supcon.supfusion.organization.dao.mapper.department.DepartmentMapper;
import com.supcon.supfusion.organization.dao.mapper.department.DepartmentPersonMapper;
import com.supcon.supfusion.organization.dao.mapper.excel.ExcelMapper;
import com.supcon.supfusion.organization.dao.mapper.person.OrganizationManagerMapper;
import com.supcon.supfusion.organization.dao.mapper.person.PersonMapper;
import com.supcon.supfusion.organization.dao.mapper.position.PositionMapper;
import com.supcon.supfusion.organization.dao.mapper.position.PositionPersonMapper;
import com.supcon.supfusion.organization.dao.mapper.position.PositionRoleMapper;
import com.supcon.supfusion.organization.dao.po.company.CompanyPO;
import com.supcon.supfusion.organization.dao.po.company.CompanyPersonPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentAddPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentBaseInfoPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentPersonPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentSynchronizationInfoPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentSynchronizationManagerPO;
import com.supcon.supfusion.organization.dao.po.excel.ExcelPO;
import com.supcon.supfusion.organization.dao.po.person.OrganizationManagerPO;
import com.supcon.supfusion.organization.dao.po.person.PersonAddPO;
import com.supcon.supfusion.organization.dao.po.position.PositionAddPO;
import com.supcon.supfusion.organization.dao.po.position.PositionDeptBasePO;
import com.supcon.supfusion.organization.dao.po.position.PositionPersonPO;
import com.supcon.supfusion.organization.dao.po.position.PositionRolePO;
import com.supcon.supfusion.organization.manager.OrganizationAdapter;
import com.supcon.supfusion.organization.service.BaseServiceService;
import com.supcon.supfusion.organization.service.DepartmentService;
import com.supcon.supfusion.organization.service.ExcelManageService;
import com.supcon.supfusion.organization.service.OrgMnecodeService;
import com.supcon.supfusion.organization.service.OrganizationManagerService;
import com.supcon.supfusion.organization.service.PersonService;
import com.supcon.supfusion.organization.service.PositionPersonService;
import com.supcon.supfusion.organization.service.PositionService;
import com.supcon.supfusion.organization.service.bo.baseService.DepartmentBaseServiceBO;
import com.supcon.supfusion.organization.service.bo.company.CompanyBaseInfoBO;
import com.supcon.supfusion.organization.service.bo.department.*;
import com.supcon.supfusion.organization.service.bo.kafka.CompanySimpleMessageBO;
import com.supcon.supfusion.organization.service.bo.kafka.DepartmentDeleteMessageBO;
import com.supcon.supfusion.organization.service.bo.kafka.DepartmentMessageBO;
import com.supcon.supfusion.organization.service.bo.kafka.ManagerMessageBO;
import com.supcon.supfusion.organization.service.bo.person.*;
import com.supcon.supfusion.organization.service.bo.position.CompanyForPositionSynchronizationInfoBO;
import com.supcon.supfusion.organization.service.bo.position.DepartmentForPositionBaseInfoBO;
import com.supcon.supfusion.organization.service.bo.position.PositionResultBO;
import com.supcon.supfusion.organization.service.common.MultiParamSql;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.supcon.supfusion.organization.common.utils.OrgBaseUtils.responseFormatTime;

/**
 * 部门服务处理实现类
 * @author
 * @date 20-5-20 上午10:48
 */
@Slf4j
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, DepartmentAddPO> implements DepartmentService {


    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private PersonService personService;

    @Autowired
    private PositionService positionService;

    @Autowired
    private OrganizationManagerMapper organizationManagerMapper;

    @Autowired
    private PositionPersonService positionPersonService;

    @Autowired
    private OrganizationManagerService organizationManagerService;

    @Autowired
    private PositionPersonMapper positionPersonMapper;

    @Autowired
    private PersonMapper personMapper;

    @Autowired
    private ExcelMapper excelMapper;

    @Autowired
    private PositionRoleMapper positionRoleMapper;

    @Autowired
    private ExcelManageService excelManageService;

    @Autowired
    private BaseServiceService baseServiceService;

    @Autowired
    private OrganizationAdapter organizationAdapter;

    @Autowired
    private DepartmentPersonMapper departmentPersonMapper;

    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;

    @Autowired
    private OrgMnecodeService orgMnecodeService;

    @Autowired
    private CompanyPersonMapper companyPersonMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    private static final Double FISRT_SORT = 1000d;

    private static final String IMPORT_DIR = "excelFile/department/";

    private static final String EXCEL_FILE_XLSX_PATTERN = "^.+\\.(?i)(xlsx)$";

    private static final String EXCEL_FILE_XLS_PATTERN = "^.+\\.(?i)(xls)$";

    @Autowired
    private OrganizationMessage organizationMessage;

    private static final String DEPARTMENT_TOPIC = "supOS_department_event";

    /**
     * 新增一个部门
     * @param departmentAddPo 新增部门信息
     * @param managerIds
     * @param tenantId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDepartment(DepartmentAddPO departmentAddPo, List<Long> managerIds, String tenantId) {

        addDepartmentWithoutKafka(departmentAddPo, managerIds, tenantId);
        //kafka
        Map<Long, List<Long>> deptIdToManagerIds = new HashMap<>();
        deptIdToManagerIds.put(departmentAddPo.getId(), managerIds);
        List<DepartmentAddPO> departmentAddPOList = new ArrayList<>();
        departmentAddPOList.add(departmentAddPo);
        publishCreateOrUpdateDeptMessage(departmentAddPOList, deptIdToManagerIds, tenantId, "CREATE");
    }

    /**
     * 根据部门id修改部门基本信息
     * @param departmentAddPo 修改部门信息
     * @param managerIds
     * @param tenantId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDepartment(DepartmentAddPO departmentAddPo, List<Long> managerIds, String tenantId) {

        updateDepartmentWithoutKafka(departmentAddPo, managerIds, tenantId);
        //kafka
        Map<Long, List<Long>> deptIdToManagerIds = new HashMap<>();
        deptIdToManagerIds.put(departmentAddPo.getId(), managerIds);
        List<DepartmentAddPO> departmentAddPOList = new ArrayList<>();
        departmentAddPOList.add(departmentAddPo);
        publishCreateOrUpdateDeptMessage(departmentAddPOList, deptIdToManagerIds, tenantId, "UPDATE");

    }

    /**
     * 修改full_path
     * @param curDep 当前部门
     * @param newDep 新的部门
     */
    private void updateFullPath(DepartmentAddPO curDep, DepartmentAddPO newDep) {
        QueryWrapper<DepartmentAddPO> judgeWrapper = new QueryWrapper<DepartmentAddPO>();
        if (newDep.getParentId() == null) {
            judgeWrapper.isNull("parent_id");
        } else {
            judgeWrapper.eq("parent_id", newDep.getParentId());
        }
        judgeWrapper.eq("valid", 1);
        judgeWrapper.eq("name", newDep.getName());
        judgeWrapper.ne("id", newDep.getId());
        judgeWrapper.eq("company_id", curDep.getCompanyId());
        int nameCount = count(judgeWrapper);
        if (nameCount > 0) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_NAME_EXISTS);
        }

        String curName = curDep.getName();
        String curFullPath = curDep.getFullPath();

        String newName = newDep.getName();
        String newFullPath = curFullPath.substring(0, curFullPath.lastIndexOf(curName)) + newName;
        newDep.setFullPath(newFullPath);
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
        queryWrapper.likeRight("full_path", curFullPath + "/");
        queryWrapper.eq("company_id", curDep.getCompanyId());
        List<DepartmentAddPO> subDeps = list(queryWrapper);
        if (subDeps == null || subDeps.size() == 0) {
            return;
        }
        subDeps.stream().forEach(dep -> {
            dep.setFullPath(newFullPath + dep.getFullPath().substring(curFullPath.length(), dep.getFullPath().length()));
        });
        updateBatchById(subDeps);
    }

    /**
     *  根据部门id查询部门详细信息
     * @param depId
     * @return
     */
    @Override
    public DepartmentDetailBO getDepDetail(Long depId) {
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
        queryWrapper.eq("id", depId);
        queryWrapper.eq("valid", 1);
        int count = count(queryWrapper);
        //判断该id的部门是否存在
        if (count == 0) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
        }
        DepartmentDetailBO departmentDetailBO = new DepartmentDetailBO();
        DepartmentAddPO departmentAddPo = getOne(queryWrapper);
        BeanUtils.copyProperties(departmentAddPo, departmentDetailBO);

        CompanyPO company = companyMapper.selectById(departmentAddPo.getCompanyId());
        departmentDetailBO.setFullPath(OrgBaseUtils.splitCompanyFullPath(departmentDetailBO.getFullPath(), company.getShortName(), company.getFullPath()));

        String deptTypeName = organizationAdapter.getSystemCodeName(departmentDetailBO.getType());
        departmentDetailBO.setTypeName(deptTypeName);
        /*if (Constants.DEPT_EMERGENCY_CODE.equals(departmentDetailBO.getType())) {
            departmentDetailBO.setTypeName("应急部门");
        } else {
            departmentDetailBO.setTypeName("普通部门");
        }*/
        QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<PositionAddPO>();
        positionWrapper.eq("dep_id", depId);
        positionWrapper.eq("valid", 1);
        List<PositionAddPO> poses = positionMapper.selectList(positionWrapper);
        departmentDetailBO.setRelPos(poses);

        QueryWrapper<OrganizationManagerPO> managerWrapper = new QueryWrapper<OrganizationManagerPO>();
        managerWrapper.eq("org_id", depId);
        managerWrapper.eq("manager_type", Constants.DEPARTMENT);
        List<OrganizationManagerPO> managers = organizationManagerMapper.selectList(managerWrapper);
        if (managers == null) {
            return departmentDetailBO;
        }
        List<OrganizationManagerBO> list = new ArrayList<OrganizationManagerBO>();
        managers.stream().forEach(manager -> {
            OrganizationManagerBO organizationManagerBO = new OrganizationManagerBO();
            BeanUtils.copyProperties(manager, organizationManagerBO);
            PersonAddPO personAddPO = personMapper.selectById(manager.getManagerId());
            if (personAddPO != null) {
                organizationManagerBO.setManagerName(personAddPO.getName());
                organizationManagerBO.setManagerCode(personAddPO.getCode());
            }
            list.add(organizationManagerBO);
        });
        departmentDetailBO.setManagers(list);
        return departmentDetailBO;
    }

    /**
     * 根据id删除指定部门
     * @param depId
     * @param tenantId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepById(Long depId, String tenantId) {
        DepartmentAddPO departmentAddPo = getById(depId);
        if (departmentAddPo == null) {
            return;
        }
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
        queryWrapper.eq("company_id", departmentAddPo.getCompanyId());
        queryWrapper.likeRight("full_path", departmentAddPo.getFullPath() + "/");
        queryWrapper.eq("valid", 1);
        List<DepartmentAddPO> deptList = list(queryWrapper);
        if (deptList == null) {
            deptList = new ArrayList<>();
        }
        deptList.add(departmentAddPo);
        if (deptList != null && deptList.size() > 0) {
            List<Long> depIds = new ArrayList<Long>();
            deptList.stream().forEach(dep -> {
                depIds.add(dep.getId());
                dep.setValid(false);
            });
            QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
            posWrapper.in("dep_id", depIds);
            posWrapper.eq("valid", 1);
            List<PositionAddPO> positionAddPOS = positionMapper.selectList(posWrapper);
            if (positionAddPOS != null && !positionAddPOS.isEmpty()) {
                List<Long> positionIds = new ArrayList<Long>();
                positionAddPOS.stream().forEach(positionAddPO -> {
                    positionIds.add(positionAddPO.getId());
                    positionAddPO.setValid(false);
                });
                LambdaQueryWrapper<PositionPersonPO> lamba = new QueryWrapper<PositionPersonPO>().lambda()
                        .in(PositionPersonPO::getPositionId, positionIds)
                        .eq(PositionPersonPO::getValid, true);
                List<PositionPersonPO> positionPersonPOS = positionPersonMapper.selectList(lamba);
                if (positionPersonPOS != null && !positionPersonPOS.isEmpty()) {
                    throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_HAVE_POSITION_DELETE_ERROR);
                }
                positionRoleMapper.delete(new QueryWrapper<PositionRolePO>().lambda()
                        .in(PositionRolePO::getPositionId, positionIds));

                //岗位助记码删除
                orgMnecodeService.deleteOrgMnecodeByOrgId(positionIds, Constants.POSITION);
            }
            updateBatchById(deptList);
            //部门助记码删除
            orgMnecodeService.deleteOrgMnecodeByOrgId(depIds, Constants.DEPARTMENT);
            positionService.updateBatchById(positionAddPOS);
            organizationManagerMapper.delete(new QueryWrapper<OrganizationManagerPO>().lambda()
                    .in(OrganizationManagerPO::getOrgId, depIds)
                    .eq(OrganizationManagerPO::getManagerType, "Department"));

            //remove(queryWrapper);
            if (departmentAddPo.getParentId() == null) {
                if (positionAddPOS != null && positionAddPOS.size() > 0) {
                    positionAddPOS.stream().forEach(pos -> {
                        //orgBeanToKafkaMsgService.transferDeletePositionAndSend(pos);
                        positionService.publishDeletePositionMessage(positionAddPOS, tenantId);
                    });
                }
                //kafka
                //orgBeanToKafkaMsgService.transferDeleteDepartmentAndSend(departmentAddPo);
                publishDeleteDepartmentMessage(deptList, tenantId);
                return;

            }
            DepartmentAddPO parentDep = getById(departmentAddPo.getParentId());
            QueryWrapper<DepartmentAddPO> broWrapper = new QueryWrapper<DepartmentAddPO>();
            broWrapper.eq("parent_id", departmentAddPo.getParentId());
            broWrapper.eq("valid", 1);
            int broCount = count(broWrapper);
            if (broCount != 0) {
                if (positionAddPOS != null && positionAddPOS.size() > 0) {
                    positionAddPOS.stream().forEach(pos -> {
                        //orgBeanToKafkaMsgService.transferDeletePositionAndSend(pos);
                        positionService.publishDeletePositionMessage(positionAddPOS, tenantId);
                    });
                }
                //kafka
                //orgBeanToKafkaMsgService.transferDeleteDepartmentAndSend(departmentAddPo);
                publishDeleteDepartmentMessage(deptList, tenantId);
                return;

            }
            parentDep.setLeaf(true);
            updateById(parentDep);
            if (positionAddPOS != null && positionAddPOS.size() > 0) {
                positionAddPOS.stream().forEach(pos -> {
                    //orgBeanToKafkaMsgService.transferDeletePositionAndSend(pos);
                    positionService.publishDeletePositionMessage(positionAddPOS, tenantId);
                });
            }

            //kafka
            publishDeleteDepartmentMessage(deptList, tenantId);

        }
    }

    /**
     * 修改部门的位置
     * @param departmentLocationPo
     * @param tenantId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDepLocation(DepartmentLocationBO departmentLocationPo, String tenantId) {
        DepartmentAddPO currentPo = updateDepLocationWithoutkafka(departmentLocationPo, tenantId);
        //Kafka
        List<DepartmentAddPO> departmentAddPOList = new ArrayList<>();
        departmentAddPOList.add(currentPo);
        QueryWrapper<OrganizationManagerPO> managerPOQueryWrapper = new QueryWrapper<>();
        managerPOQueryWrapper.lambda().eq(OrganizationManagerPO::getOrgId, currentPo.getId());
        List<OrganizationManagerPO> managerPOList = organizationManagerMapper.selectList(managerPOQueryWrapper);
        Map<Long, List<Long>> deptIdToManagerIds = new HashMap<>();
        if (managerPOList != null && managerPOList.size() > 0) {
            List<Long> managerIds = new ArrayList<>();
            managerPOList.stream().forEach(manager -> managerIds.add(manager.getManagerId()));
            deptIdToManagerIds.put(currentPo.getId(), managerIds);
        }

        publishCreateOrUpdateDeptMessage(departmentAddPOList, deptIdToManagerIds, tenantId, "UPDATE");

    }

    /**
     * 全量返回公司下的部门
     * @param companyId 公司id
     * @param keyword 关键字
     * @return
     */
    @Override
    public DepartmentTreeBO getDepTree(Long companyId, String keyword, Long departmentId) {
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
        queryWrapper.eq("company_id", companyId);
        queryWrapper.eq("valid", 1);
        queryWrapper.eq("sys_flag", 0);
        queryWrapper.orderByAsc("lay_no");
        queryWrapper.orderByAsc("sort");
        QueryWrapper<CompanyPO> comJudgeWrapper = new QueryWrapper<CompanyPO>();
        comJudgeWrapper.eq("id", companyId);
        Integer comCount = companyMapper.selectCount(comJudgeWrapper);
        //指定id的公司不存在
        if (comCount == null || comCount == 0) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_COMPANYID_NOT_EXISTS);
        }
        DepartmentTreeBO firstDepOrCom = new DepartmentTreeBO();

        CompanyPO company = companyMapper.selectById(companyId);
        BeanUtils.copyProperties(company, firstDepOrCom);
        firstDepOrCom.setName(company.getShortName());
        firstDepOrCom.setCompanyId(firstDepOrCom.getId());

        if (departmentId != null) {
            DepartmentAddPO currentDept = getById(departmentId);
            if (currentDept == null) {
                throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
            }
            DepartmentTreeBO currentDeptBo = new DepartmentTreeBO();
            BeanUtils.copyProperties(currentDept, currentDeptBo);
            currentDeptBo.setFullPath(OrgBaseUtils.splitCompanyFullPath(currentDeptBo.getFullPath(), company.getShortName(), company.getFullPath()));
            while (currentDeptBo != null && currentDeptBo.getParentId() != null) {

                QueryWrapper<DepartmentPersonPO> deptPersonWrapper = new QueryWrapper<>();
                deptPersonWrapper.eq("dept_id", currentDeptBo.getId()).eq("valid", true);
                List<DepartmentPersonPO> deptPersonRels = departmentPersonMapper.selectList(deptPersonWrapper);
                if (deptPersonRels == null || deptPersonRels.size() == 0) {
                    currentDeptBo.setPersonNum(0);
                } else {
                    Set<Long> relPersonIds = new HashSet<>();
                    deptPersonRels.stream().forEach(rel -> relPersonIds.add(rel.getPersonId()));
                    currentDeptBo.setPersonNum(relPersonIds.size());
                }

                DepartmentAddPO parentDept = getById(currentDeptBo.getParentId());
                DepartmentTreeBO parentDeptBo = new DepartmentTreeBO();
                BeanUtils.copyProperties(parentDept, parentDeptBo);
                //currentDeptBo.setParentId(currentDeptBo.getParentId());
                List<DepartmentTreeBO> children = new ArrayList<DepartmentTreeBO>();
                children.add(currentDeptBo);
                parentDeptBo.setChildren(children);
                currentDeptBo = parentDeptBo;
                currentDeptBo.setFullPath(OrgBaseUtils.splitCompanyFullPath(currentDeptBo.getFullPath(), company.getShortName(), company.getFullPath()));
            }
            QueryWrapper<DepartmentPersonPO> deptPersonWrapper = new QueryWrapper<>();
            deptPersonWrapper.eq("dept_id", currentDeptBo.getId()).eq("valid", true);
            List<DepartmentPersonPO> deptPersonRels = departmentPersonMapper.selectList(deptPersonWrapper);
            if (deptPersonRels == null || deptPersonRels.size() == 0) {
                currentDeptBo.setPersonNum(0);
            } else {
                Set<Long> relPersonIds = new HashSet<>();
                deptPersonRels.stream().forEach(rel -> relPersonIds.add(rel.getPersonId()));
                currentDeptBo.setPersonNum(relPersonIds.size());
            }
            List<DepartmentTreeBO> children = new ArrayList<DepartmentTreeBO>();
            children.add(currentDeptBo);
            firstDepOrCom.setChildren(children);
            return firstDepOrCom;
        }

        //查询所有的部门
        List<DepartmentAddPO> positions = list(queryWrapper);
        if (positions == null || positions.size() == 0) {
            return firstDepOrCom;
        }
        List<DepartmentTreeBO> treeSubDeps = new ArrayList<DepartmentTreeBO>();
        positions.stream().forEach(dep -> {
            DepartmentTreeBO subDep = new DepartmentTreeBO();
            BeanUtils.copyProperties(dep, subDep);
            String fullPath = OrgBaseUtils.splitCompanyFullPath(subDep.getFullPath(), company.getShortName(), company.getFullPath());
            subDep.setFullPath(fullPath);
            treeSubDeps.add(subDep);

            QueryWrapper<DepartmentPersonPO> deptPersonWrapper = new QueryWrapper<>();
            deptPersonWrapper.eq("dept_id", dep.getId()).eq("valid", true);
            List<DepartmentPersonPO> deptPersonRels = departmentPersonMapper.selectList(deptPersonWrapper);
            if (deptPersonRels == null || deptPersonRels.size() == 0) {
                subDep.setPersonNum(0);
            } else {
                Set<Long> relPersonIds = new HashSet<>();
                deptPersonRels.stream().forEach(rel -> relPersonIds.add(rel.getPersonId()));
                subDep.setPersonNum(relPersonIds.size());
            }
        });

        List<DepartmentTreeBO> firsLevel = new ArrayList<DepartmentTreeBO>();
        LinkedList<DepartmentTreeBO> stack = new LinkedList<DepartmentTreeBO>();
        DepartmentTreeBO stackTop = null;
        //构造成树形结构
        while (treeSubDeps.size() > 0) {
            //下层级部门列表
            List<DepartmentTreeBO> subLevel = new ArrayList<DepartmentTreeBO>();

            Iterator<DepartmentTreeBO> it = treeSubDeps.iterator();
            while (it.hasNext()) {
                DepartmentTreeBO dep = it.next();
                if (dep.getLayNo() == 1) {
                    if (keyword != null && !"".equals(keyword.trim()) && dep.getName().contains(keyword)) {
                        dep.setMatch(true);
                    }
                    //保存第一层的部门
                    subLevel.add(dep);
                    //将部门压栈
                    stack.addLast(dep);

                    firsLevel = subLevel;
                    it.remove();
                } else if (stackTop != null) {
                    if (dep.getParentId().equals(stackTop.getId())) {
                        //设置当前部门的上级部门
                        dep.setPreDep(stackTop);
                        if (keyword != null && !"".equals(keyword.trim()) && dep.getName().contains(keyword)) {
                            dep.setMatch(true);
                        }
                        //保存这一层级的部门
                        subLevel.add(dep);
                        //将部门压栈
                        stack.addLast(dep);
                        it.remove();
                    }
                }
            }
            if (stackTop != null) {
                if (subLevel.size() == 0) {
                    stack.addFirst(stackTop);
                } else {
                    stackTop.setChildren(subLevel);
                }
            }
            if (treeSubDeps.size() > 0) {
                stackTop = stack.pollLast();
            }
        }
        firstDepOrCom.setChildren(firsLevel);

        if (keyword == null || "".equals(keyword.trim())) {
            return firstDepOrCom;
        }
        //遍历叶子节点部门，并向上遍历
        Iterator<DepartmentTreeBO> stackIt = stack.iterator();
        while(stackIt.hasNext()) {
            DepartmentTreeBO stackDep = stackIt.next();
            DepartmentTreeBO curDep = stackDep;

            while (curDep != null && (curDep.getChildren() == null || curDep.getChildren().size() == 0)) {
                DepartmentTreeBO preDep = curDep.getPreDep();
                if (curDep.getMatch()) {
                    break;
                }
                if (preDep != null) {
                    preDep.getChildren().remove(curDep);
                    curDep.setPreDep(null);
                } else {
                    firsLevel.remove(curDep);
                }
                if (curDep == stackDep) {
                    stackIt.remove();
                }
                curDep = preDep;
            }
        }
        return firstDepOrCom;
    }

    /**
     * 查询部门树形结构
     * 模糊匹配逻辑：查出所有给定公司id的部门列表，然后先将第一层级部门压入栈的尾部，并在部门列表中删除此部门，出栈上级节点，然后将该节点的下层部门节点入栈，以次逻辑循环，如果没有下级部门则在栈链表的头部插入，到最后栈中剩余的就是叶子节点
     * @param companyId 公司id
     * @param parentId 上级部门id
     * @param keyword 关键字
     * @return
     */
/*    @Override
    public DepartmentTreeBO getDepTree(Long companyId, Long parentId, String keyword) {
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
        queryWrapper.eq("company_id", companyId);

        DepartmentTreeBO firstDepOrCom = new DepartmentTreeBO();

        if (keyword == null || "".equals(keyword.trim())) {
            if (parentId == null) {
                QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
                comWrapper.eq("id", companyId);
                CompanyPO company = companyMapper.selectOne(comWrapper);
                BeanUtils.copyProperties(company, firstDepOrCom);
                firstDepOrCom.setName(company.getShortName());
                queryWrapper.isNull("parent_id");
            } else {
                QueryWrapper<DepartmentAddPO> parentDepWrapper = new QueryWrapper<DepartmentAddPO>();
                parentDepWrapper.eq("id", parentId);
                DepartmentAddPO parentDep = getOne(parentDepWrapper);
                BeanUtils.copyProperties(parentDep, firstDepOrCom);
                queryWrapper.eq("parent_id", parentId);
            }
        } else {
            QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
            comWrapper.eq("id", companyId);
            CompanyPO company = companyMapper.selectOne(comWrapper);
            BeanUtils.copyProperties(company, firstDepOrCom);
            firstDepOrCom.setName(company.getShortName());
        }
        firstDepOrCom.setCompanyId(companyId);
        queryWrapper.orderByAsc("lay_no");
        queryWrapper.orderByDesc("sort");
        List<DepartmentAddPO> deps = list(queryWrapper);

        if (deps == null || deps.size() == 0) {
            return firstDepOrCom;
        }
        List<DepartmentTreeBO> treeSubDeps = new ArrayList<DepartmentTreeBO>();
        deps.stream().forEach(dep -> {
            DepartmentTreeBO subDep = new DepartmentTreeBO();
            BeanUtils.copyProperties(dep, subDep);
            treeSubDeps.add(subDep);
        });
        firstDepOrCom.setChildren(treeSubDeps);
        if (keyword == null || "".equals(keyword.trim())) {
            return firstDepOrCom;
        }
        //第一层级部门列表
        List<DepartmentTreeBO> firsLevel = new ArrayList<DepartmentTreeBO>();
        LinkedList<DepartmentTreeBO> stack = new LinkedList<DepartmentTreeBO>();
        DepartmentTreeBO stackTop = null;
        //构造成树形结构
        while (treeSubDeps.size() > 0) {
            //下层级部门列表
            List<DepartmentTreeBO> subLevel = new ArrayList<DepartmentTreeBO>();

            Iterator<DepartmentTreeBO> it = treeSubDeps.iterator();
            while (it.hasNext()) {
                DepartmentTreeBO dep = it.next();
                if (dep.getLayNo() == 1) {
                    if (dep.getName().contains(keyword)) {
                        dep.setMatch(true);
                    }
                    //保存第一层的部门
                    subLevel.add(dep);
                    //将部门压栈
                    stack.addLast(dep);

                    firsLevel = subLevel;
                    it.remove();
                } else if (stackTop != null) {
                    if (dep.getParentId().longValue() == stackTop.getId().longValue()) {
                        //设置当前部门的上级部门
                        dep.setPreDep(stackTop);
                        if (dep.getName().contains(keyword)) {
                            dep.setMatch(true);
                        }
                        //保存这一层级的部门
                        subLevel.add(dep);
                        //将部门压栈
                        stack.addLast(dep);
                        it.remove();
                    }
                }
            }
            if (stackTop != null) {
                if (subLevel.size() == 0) {
                    stack.addFirst(stackTop);
                } else {
                    stackTop.setChildren(subLevel);
                }
            }
            if (treeSubDeps.size() > 0) {
                stackTop = stack.pollLast();
            }
        }
        firstDepOrCom.setChildren(firsLevel);
        //遍历叶子节点部门，并向上遍历
        Iterator<DepartmentTreeBO> stackIt = stack.iterator();
        while(stackIt.hasNext()) {
            DepartmentTreeBO stackDep = stackIt.next();
            DepartmentTreeBO curDep = stackDep;

            while (curDep != null && (curDep.getChildren() == null || curDep.getChildren().size() == 0)) {
                DepartmentTreeBO preDep = curDep.getPreDep();
                if (curDep.getMatch()) {
                    break;
                }
                if (preDep != null) {
                    preDep.getChildren().remove(curDep);
                    curDep.setPreDep(null);
                } else {
                    firsLevel.remove(curDep);
                }
                if (curDep == stackDep) {
                    stackIt.remove();
                }
                curDep = preDep;
            }
        }
        return firstDepOrCom;
    }*/




    /**
     * 查询部门关联的人员
     * @param companyId
     * @param departmentId
     * @param keyword
     * @param current
     * @param pageSize
     * @param conditionQuery
     * @param includeUser
     * @return
     */
    @Override
    public PageResult<PersonDetailBO> queryDepartmentPersons(Long companyId, Long departmentId, String keyword, Integer current, Integer pageSize, PersonDetailBO conditionQuery, Boolean includeUser) {

        if (companyId == null) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_COMPANYID_NOT_EXISTS);
        }
        PageResult<PersonDetailBO> pageResult = null;
        if (companyId.equals(departmentId) || (keyword != null && !"".equals(keyword.trim()))) {

            List<Long> positionIds = null;
            if (departmentId != null && !companyId.equals(departmentId)) {
                QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<DepartmentAddPO>();
                deptWrapper.eq("id", departmentId);
                deptWrapper.eq("valid", 1);
                int positionCount = count(deptWrapper);
                if (positionCount == 0) {
                    throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
                }
                positionIds = positionService.queryPositionIdsbyDeptId(departmentId);
            }

            //pageResult = personService.queryPersonsByCompanyId(companyId, positionIds, keyword, current, pageSize, conditionQuery);
            pageResult = personService.queryPersonsByCompanyIdBetter(companyId, positionIds, keyword, current, pageSize, conditionQuery);
        } else {
            if (departmentId == null) {
                throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
            }
            QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<DepartmentAddPO>();
            deptWrapper.eq("id", departmentId);
            deptWrapper.eq("valid", 1);
            int deptCount = count(deptWrapper);
            if (deptCount == 0) {
                throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
            }
            List<Long> posIds = positionService.queryPositionIdsbyDeptId(departmentId);

            pageResult = personService.queryPersonsByCompanyIdBetter(companyId, posIds, keyword, current, pageSize, conditionQuery);
            /*List<Long> presonIds = positionPersonService.queryPersonIdByPositionIds(posIds);
            if (presonIds == null || presonIds.size() == 0) {
                return new PageResult<PersonDetailBO>(null, 0, pageSize, current);
            }
            pageResult = personService.queryPersonsById(presonIds, current, pageSize, conditionQuery);*/
        }
        CompanyPO company = companyMapper.selectById(companyId);
        List<PersonDetailBO> persons = (List<PersonDetailBO>) pageResult.getList();


        persons.stream().forEach(person -> {
            transferSystemCode(person);
            Long id = person.getId();
            List<PositionPersonPO> relations = positionPersonService.queryPositionByPersonId(id);
            if (relations != null) {
                List<Long> positionIds = new ArrayList<Long>();
                relations.stream().forEach(relation -> positionIds.add(relation.getPositionId()));
                List<PositionAddPO> positions = positionService.queryPositionByIds(positionIds);
                List<MainPositionBO> positionPathList = new ArrayList<MainPositionBO>();

                List<MainPositionBO> positionList = new ArrayList<MainPositionBO>();

                Set<Long> departmentIds = new HashSet<Long>();

                person.setPositionFullPath(positionPathList);

                person.setPosition(positionList);

                Map<Long, CompanyPO> depIdToComFullPath = new HashMap<>();

                positions.stream().forEach(posItem -> {
                    MainPositionBO mainPositionBO = new MainPositionBO();
                    mainPositionBO.setId(posItem.getId());
                    mainPositionBO.setCompanyId(posItem.getCompanyId());
                    mainPositionBO.setCode(posItem.getCode());
                    mainPositionBO.setName(posItem.getName());
                    mainPositionBO.setDepId(posItem.getDepId());
                    mainPositionBO.setLayRec(posItem.getLayRec());
                    mainPositionBO.setParentId(posItem.getParentId());
                    departmentIds.add(posItem.getDepId());
                    if (posItem.getCompanyId().equals(companyId)) {
                        //mainPositionBO.setFullPath(posItem.getFullPath());
                        mainPositionBO.setFullPath(OrgBaseUtils.splitCompanyFullPath(posItem.getFullPath(), company.getShortName(), company.getFullPath()));
                    } else {

                        CompanyPO companyPO = companyMapper.selectById(posItem.getCompanyId());
                        mainPositionBO.setFullPath(OrgBaseUtils.splitCompanyFullPath(posItem.getFullPath(), companyPO.getShortName(), companyPO.getFullPath()));
                        depIdToComFullPath.put(posItem.getDepId(), companyPO);
                    }
                    if (person.getMainPosition().equals(posItem.getId())) {
                        mainPositionBO.setMainPosition(true);
                        positionPathList.add(0, mainPositionBO);
                        positionList.add(0, mainPositionBO);
                    } else {
                        positionPathList.add(mainPositionBO);
                        positionList.add(mainPositionBO);
                    }
                });
                QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<DepartmentAddPO>();
                deptWrapper.in("id", departmentIds);
                //deptWrapper.eq("company_id", companyId);
                deptWrapper.eq("valid", 1);
                List<DepartmentAddPO> depts = list(deptWrapper);
                List<RelationDepartmentBO> deptFullPaths = new ArrayList<RelationDepartmentBO>();

                List<RelationDepartmentBO> departmentList = new ArrayList<RelationDepartmentBO>();

                depts.stream().forEach(deptItem -> {
                    RelationDepartmentBO relationDepartmentBO = new RelationDepartmentBO();
                    relationDepartmentBO.setId(deptItem.getId());
                    relationDepartmentBO.setCompanyId(deptItem.getCompanyId());
                    relationDepartmentBO.setCode(deptItem.getCode());
                    relationDepartmentBO.setName(deptItem.getName());
                    relationDepartmentBO.setParentId(deptItem.getParentId());
                    relationDepartmentBO.setLayRec(deptItem.getLayRec());
                    if (deptItem.getCompanyId().equals(companyId)) {
                        relationDepartmentBO.setFullPath(OrgBaseUtils.splitCompanyFullPath(deptItem.getFullPath(), company.getShortName(), company.getFullPath()));
                    } else {
                        CompanyPO curCompanyPo = depIdToComFullPath.get(deptItem.getId());
                        relationDepartmentBO.setFullPath(OrgBaseUtils.splitCompanyFullPath(deptItem.getFullPath(), curCompanyPo.getShortName(), curCompanyPo.getFullPath()));
                    }

                    deptFullPaths.add(relationDepartmentBO);
                    departmentList.add(relationDepartmentBO);
                });
                person.setDepartmentFullPath(deptFullPaths);
                person.setDepartment(departmentList);
            }
        });
        return pageResult;
    }

    @Override
    public List<PersonDetailBO> queryDepartmentPersonNoPage(Long companyId, Long positionId, String keyword, PersonDetailBO conditionQuery) {
        if (companyId == null) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_COMPANYID_NOT_EXISTS);
        }
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("valid", 1);
        queryWrapper.eq("sys_flag", 0);
        int count = personMapper.selectCount(queryWrapper);
        if (count == 0) {
            return null;
        }
        PageResult<PersonDetailBO> pageResult = queryDepartmentPersons(companyId, positionId, keyword, 1, count, conditionQuery, false);
        if (pageResult == null || pageResult.getList() == null || pageResult.getList().size() == 0) {
            return null;
        }

        return (List)pageResult.getList();
    }

    @Override
    public List<DepartmentKeywordBO> queryDepartmentsByKeyword(String keyword, Long companyId) {
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
        queryWrapper.eq("company_id", companyId);
        //queryWrapper.like("name", keyword);

        String key = dbStringUtil.getString(keyword);
        //获取数据库类型
        String dbType = dataId.getDataId();
        //使用queryWrapper形式
        //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
        if ("oracle".equals(dbType)){
            queryWrapper.apply("name like {0} escape '\\'", "%" + key + "%");
        }else{
            queryWrapper.like("name",key);
        }


        queryWrapper.eq("valid",1);
        queryWrapper.eq("sys_flag", 0);
        List<DepartmentAddPO> results = list(queryWrapper);
        if (results == null || results.size() == 0) {
            return null;
        }
        List<DepartmentKeywordBO> list = new ArrayList<DepartmentKeywordBO>();

        List<Long> deptIds = new ArrayList<Long>();

        results.stream().forEach(dept -> {
            DepartmentKeywordBO deptKeywordBO = new DepartmentKeywordBO();
            BeanUtils.copyProperties(dept, deptKeywordBO);
            list.add(deptKeywordBO);
            deptIds.add(dept.getId());
        });
        QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<PositionAddPO>();
        positionWrapper.in("dep_id", deptIds);
        positionWrapper.eq("valid", 1);
        positionWrapper.select("id", "dep_id");

        List<PositionAddPO> positions = positionMapper.selectList(positionWrapper);
        //List<Map<String, Object>> positions = positionMapper.selectMaps(positionWrapper);

        if (positions == null || positions.size() == 0) {
            return list;
        }
        List<Long> positionIds = new ArrayList<Long>();
        positions.stream().forEach(position -> {
            positionIds.add(position.getId());
        });

        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
        relationWrapper.select("position_id AS POSITIONID", "count(1) AS COUNT");
        relationWrapper.in("position_id", positionIds);
        relationWrapper.eq("valid", 1);
        relationWrapper.groupBy("position_id");
        List<Map<String, Object>> groupMapList = positionPersonMapper.selectMaps(relationWrapper);
        if (groupMapList != null) {
            groupMapList.stream().forEach(group -> {
                int index = positionIds.indexOf(Long.valueOf(group.get("POSITIONID").toString()));
                if (index >= 0) {
                    Long curDepId = positions.get(index).getDepId();
                    int deptIndex = deptIds.indexOf(curDepId);
                    if (deptIndex >= 0) {
                        Long num = list.get(deptIndex).getPersonNum();
                        if (group.get("COUNT") == null) {
                            list.get(deptIndex).setPersonNum(num);
                        } else {
                            list.get(deptIndex).setPersonNum(num + Long.valueOf(group.get("COUNT").toString()));
                        }
                    }
                }
            });
        }

        return list;
    }

    private void transferSystemCode(PersonDetailBO person) {
        if (person.getGender() != null) {
            String genderName = organizationAdapter.getSystemCodeName(person.getGender());
            person.setGender(genderName);

        }
        if (person.getStatus() != null) {
            String statusName = organizationAdapter.getSystemCodeName(person.getStatus());
            person.setStatus(statusName);
        }
        if (person.getClassifiedLevel() != null) {
            String className = organizationAdapter.getSystemCodeName(person.getClassifiedLevel());
            person.setClassifiedLevel(className);
        }

        if (person.getEducation() != null) {
            String educationName = organizationAdapter.getSystemCodeName(person.getEducation());
            person.setEducation(educationName);
        }

        if (person.getTitle() != null) {
            String titleName = organizationAdapter.getSystemCodeName(person.getTitle());
            person.setTitle(titleName);
        }
    }
    /**
     * 导入excel
     * @param workbook
     * @param taskId
     * @param companyId
     * @param fileName
     * @param tenantId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importExcel(XSSFWorkbook workbook, Long taskId, Long companyId, String fileName, String tenantId) {
        ExcelPO excelPO = excelMapper.selectById(taskId);
        if (excelPO == null) {
            return;
        }
        excelPO.setStatus(1);
        excelManageService.excuteExcelState(excelPO);
        //excelMapper.updateById(excelPO);
        Sheet sheet = workbook.getSheetAt(1);
        Row titleRow = sheet.getRow(0);
        HashMap<String, Integer> titleMap = new HashMap<String, Integer>();
        boolean flag = checkImportExcelTitle(ExcelUtils.DEPARTMENT_IMPORT_TEMPLATE, titleRow, titleMap);
        if (!flag) {
            excelPO.setStatus(3);
            excelPO.setErrorMessage(OrganizationErrorEnum.EXCEL_IMPORT_TITLE_ERROR.getMessage());
            excelManageService.excuteExcelState(excelPO);
            return;
        }
        List<DepartmentDetailBO> importDeps = new ArrayList<DepartmentDetailBO>();
        flag = checkExcelData(workbook, titleMap, importDeps, companyId);

        if (!flag) {
            try {
                String errorFileName = ExcelUtils.createErrorExcelFile(workbook, fileName, taskId);

                excelPO.setErrorFile(errorFileName);
                excelPO.setStatus(3);
                //excelMapper.updateById(excelPO);
                excelManageService.excuteExcelState(excelPO);
                return;
            } catch (Exception e) {
                log.error("部门导入异常:" + e);
                excelPO.setStatus(3);
                excelPO.setErrorMessage(OrganizationErrorEnum.EXCEL_IMPORT_PROCESS_ERROR.getMessage());
                //excelMapper.updateById(excelPO);
                excelManageService.excuteExcelState(excelPO);
            }
        } else {
            try {
                //正式导入数据
                excelBatchAddDepartment(importDeps, companyId, tenantId);
                excelPO.setStatus(2);
                excelPO.setErrorMessage(Constants.EXCEL_IMPORT_SUCESS);
                //excelMapper.updateById(excelPO);

            } catch (Exception e) {
                excelPO.setStatus(3);
                excelPO.setErrorMessage(OrganizationErrorEnum.EXCEL_IMPORT_PROCESS_ERROR.getMessage());
                log.error("部门导入数据异常", e.getCause());
                throw new OrganizationException(OrganizationErrorEnum.EXCEL_IMPORT_PROCESS_ERROR);
            } finally {
                excelManageService.excuteExcelState(excelPO);
            }
        }

    }

    /**
     * 执行实际的导入
     * @param importDeps
     * @param tenantId
     */
    private void excelBatchAddDepartment(List<DepartmentDetailBO> importDeps, Long companyId, String tenantId) {

        List<DepartmentAddPO> createDeptList = new ArrayList<>();
        List<DepartmentAddPO> updateDeptList = new ArrayList<>();
        Map<Long, List<Long>> deptIdToManagerIds = new HashMap<>();
        for (DepartmentDetailBO departmentDetailBO : importDeps) {
            excelAddDepartment(departmentDetailBO, importDeps, companyId, tenantId, createDeptList, updateDeptList, deptIdToManagerIds);
            /*if (departmentDetailBO.getId() == null) {
                //id为空，新增部门
                if (departmentDetailBO.getParentId() != null || (departmentDetailBO.getParentId() == null && StringUtils.isBlank(departmentDetailBO.getParentCode()))) {
                    //数据库中已经存在父节点
                    DepartmentAddPO departmentAddPO = new DepartmentAddPO();
                    departmentAddPO.setCode(departmentDetailBO.getCode());
                    departmentAddPO.setName(departmentDetailBO.getName());
                    departmentAddPO.setCompanyId(companyId);
                    departmentAddPO.setParentId(departmentDetailBO.getParentId());
                    List<Long> managerIds = new ArrayList<Long>();
                    if (departmentDetailBO.getManagers() != null && departmentDetailBO.getManagers().size() > 0) {
                        //设置负责人

                        departmentDetailBO.getManagers().stream().forEach(manager -> managerIds.add(manager.getManagerId()));
                    }
                    if (managerIds.size() == 0) {
                        addDepartment(departmentAddPO, null);
                    } else {
                        addDepartment(departmentAddPO, managerIds);
                    }
                } else if(StringUtils.isNotBlank(departmentDetailBO.getParentCode())) {

                }*/
            }
        publishCreateOrUpdateDeptMessage(createDeptList, deptIdToManagerIds, tenantId, "CREATE");
        publishCreateOrUpdateDeptMessage(updateDeptList, deptIdToManagerIds, tenantId, "UPDATE");
    }

    /**
     * 插入当前行的部门，返回当前的ｉｄ
     * @param departmentDetailBO
     * @param list
     * @param companyId
     * @param tenantId
     * @param createDeptList
     * @param updateDeptList
     * @param deptIdToManagerIds
     * @return
     */
    private Long excelAddDepartment(DepartmentDetailBO departmentDetailBO, List<DepartmentDetailBO> list, Long companyId, String tenantId, List<DepartmentAddPO> createDeptList, List<DepartmentAddPO> updateDeptList, Map<Long, List<Long>> deptIdToManagerIds) {
        departmentDetailBO.setType(Constants.DEPT_GENDERAL_CODE);
        //id为空，新增部门
        if (departmentDetailBO.getId() == null) {
            DepartmentAddPO departmentAddPO = new DepartmentAddPO();
            departmentAddPO.setCode(departmentDetailBO.getCode());
            departmentAddPO.setName(departmentDetailBO.getName());
            departmentAddPO.setCompanyId(companyId);
            departmentAddPO.setDescription(departmentDetailBO.getDescription());
            departmentAddPO.setType(Constants.DEPT_GENDERAL_CODE);
            List<Long> managerIds = new ArrayList<Long>();
            if (departmentDetailBO.getManagers() != null && departmentDetailBO.getManagers().size() > 0) {
                //设置负责人
                departmentDetailBO.getManagers().stream().forEach(manager -> managerIds.add(manager.getManagerId()));
            }

            if (departmentDetailBO.getParentId() != null || (departmentDetailBO.getParentId() == null && StringUtils.isBlank(departmentDetailBO.getParentCode()))) {
                //数据库中已经存在父节点
                departmentAddPO.setParentId(departmentDetailBO.getParentId());
            } else if(StringUtils.isNotBlank(departmentDetailBO.getParentCode())) {
                for (DepartmentDetailBO dep : list) {
                    if (dep.getCode().equals(departmentDetailBO.getParentCode())) {
                        Long parentId = excelAddDepartment(dep, list, companyId, tenantId, createDeptList, updateDeptList, deptIdToManagerIds);
                        departmentAddPO.setParentId(parentId);
                        break;
                    }
                }
            }
            QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
            queryWrapper.eq("code", departmentAddPO.getCode());
            queryWrapper.eq("valid", 1);
            int count = count(queryWrapper);
            if (count == 0) {
                if (managerIds.size() == 0) {
                    addDepartmentWithoutKafka(departmentAddPO, null, tenantId);
                } else {
                    addDepartmentWithoutKafka(departmentAddPO, managerIds, tenantId);
                }
                createDeptList.add(departmentAddPO);
                deptIdToManagerIds.put(departmentAddPO.getId(), managerIds);
            } else {
                DepartmentAddPO curDep = getOne(queryWrapper);
                return curDep.getId();
            }

            return departmentAddPO.getId();
        } else {
            List<Long> managerIds = new ArrayList<Long>();
            if (departmentDetailBO.getManagers() != null && departmentDetailBO.getManagers().size() > 0) {
                //设置负责人
                departmentDetailBO.getManagers().stream().forEach(manager -> managerIds.add(manager.getManagerId()));
            }
            //数据库已经有了当前部门，修改，或移动位置
            DepartmentAddPO curDep = getById(departmentDetailBO.getId());
            DepartmentAddPO curParentDep = null;
            if (curDep.getParentId() != null) {
                curParentDep = getById(curDep.getParentId());
            }
            if ((departmentDetailBO.getParentId() != null && departmentDetailBO.getParentId().equals(curDep.getParentId())) ||
                    (departmentDetailBO.getParentId() == null && StringUtils.isBlank(departmentDetailBO.getParentCode()) && curDep.getParentId() == null) ||
                    (departmentDetailBO.getParentId() == null && StringUtils.isNotBlank(departmentDetailBO.getParentCode()) && curParentDep != null && departmentDetailBO.getParentCode().equals(curParentDep.getCode()))) {
                //父节点已经在数据库有了,并且父节点没变,只修改信息
                curDep.setName(departmentDetailBO.getName());
                curDep.setDescription(departmentDetailBO.getDescription());
                if (StringUtils.isNotBlank(departmentDetailBO.getDescription())) {
                    curDep.setDescription(departmentDetailBO.getDescription());
                }
                if (managerIds.size() == 0) {
                    updateDepartmentWithoutKafka(curDep, null, tenantId);
                } else {
                    updateDepartmentWithoutKafka(curDep, managerIds, tenantId);
                }
                updateDeptList.add(curDep);
                deptIdToManagerIds.put(curDep.getId(), managerIds);

                return departmentDetailBO.getId();
            } else {
                //节点有移动到其他父节点
                //如果父节点在数据库已经有了,直接移动
                if (StringUtils.isBlank(departmentDetailBO.getParentCode())) {
                    DepartmentLocationBO departmentLocationBO = new DepartmentLocationBO();
                    departmentLocationBO.setParentId(departmentDetailBO.getParentId());
                    departmentLocationBO.setId(departmentDetailBO.getId());
                    curDep.setName(departmentDetailBO.getName());
                    curDep.setDescription(departmentDetailBO.getDescription());
                    if (StringUtils.isNotBlank(departmentDetailBO.getDescription())) {
                        curDep.setDescription(departmentDetailBO.getDescription());
                    }
                    if (managerIds.size() == 0) {
                        updateDepartmentWithoutKafka(curDep, null, tenantId);
                    } else {
                        updateDepartmentWithoutKafka(curDep, managerIds, tenantId);
                    }
                    DepartmentAddPO currentDep = updateDepLocationWithoutkafka(departmentLocationBO, tenantId);
                    updateDeptList.add(currentDep);
                    deptIdToManagerIds.put(curDep.getId(), managerIds);

                } else {
                    //父节点在数据库没有,需要递归在excel查询来依次新增
                    for (DepartmentDetailBO dep : list) {
                        if (dep.getCode().equals(departmentDetailBO.getParentCode())) {
                            Long parentId = excelAddDepartment(dep, list, companyId, tenantId, createDeptList, updateDeptList, deptIdToManagerIds);
                            curDep.setParentId(parentId);
                            break;
                        }
                    }
                }
                return departmentDetailBO.getId();
            }

        }

    }
    /**
     * 根据标题头校验标题头
     * @param titles 标题模板列表
     * @param titleRow 传入的标题行
     * @param titleRow 传入的标题行
     * @return
     */
    public static Boolean checkImportExcelTitle(List<ExcelTitle> titles, Row titleRow, Map<String, Integer> titleMap) {
        if (titles == null || titles.size() == 0 || titleRow == null || titleRow.getLastCellNum() == 0 || titleRow.getLastCellNum() < titles.size()) {
            return false;
        }
        for (int i = 0; i < titleRow.getLastCellNum(); i++) {
            Cell curCell = titleRow.getCell(i);
            if (curCell == null || curCell.getCellComment() == null || curCell.getCellComment().getString() == null || StringUtils.isBlank(curCell.getCellComment().getString().getString())) {
                continue;
            }
            for (ExcelTitle title : titles) {
                if (title.getComment().equals(curCell.getCellComment().getString().getString())) {
                    titleMap.put(curCell.getCellComment().getString().getString(), i);
                }
            }
        }
        if (titleMap.size() != titles.size()) {
            return false;
        }
        return true;
    }


    /**
     * 校验Excel中的数据合法性
     * @param workbook
     * @return
     */
    private boolean checkExcelData(XSSFWorkbook workbook, Map<String, Integer> titleMap, List<DepartmentDetailBO> importDeps, Long companyId) {
        boolean flag = true;
        Sheet sheet = workbook.getSheetAt(1);
        Drawing drawing = sheet.createDrawingPatriarch();
        CellStyle cellStyle = ExcelUtils.createImportErrorCellStyle(workbook);
        List<String> parentCodes = new ArrayList<>();
        Iterator<Row> parentCodeIt = sheet.rowIterator();
        parentCodeIt.next();
        List<String> parentAndSub = new ArrayList<String>();
        List<String> parentNames = new ArrayList<String>();
        while (parentCodeIt.hasNext()) {
            Row curRow = parentCodeIt.next();
            Cell codeCell = curRow.getCell(titleMap.get("code"));
            if (codeCell != null && StringUtils.isNotBlank(getRightTypeCell(codeCell).toString())) {
                parentCodes.add(getRightTypeCell(codeCell).toString());
            } else {
                parentCodes.add(null);
            }
            Cell parentCodeCell = curRow.getCell(titleMap.get("parentCode"));
            Cell parentNameCell = curRow.getCell(titleMap.get("parentName"));
            Cell nameCell = curRow.getCell(titleMap.get("name"));
            if (nameCell != null && StringUtils.isNotBlank(getRightTypeCell(nameCell).toString())) {
                parentNames.add(getRightTypeCell(nameCell).toString());
            } else {
                parentNames.add(null);
            }
            StringBuilder sb = new StringBuilder();
            if (parentCodeCell != null && StringUtils.isNotBlank(getRightTypeCell(parentCodeCell).toString())) {
                sb.append(getRightTypeCell(parentCodeCell).toString()).append("-");
            } else {
                sb.append("root-");
            }
            if (parentNameCell != null && StringUtils.isNotBlank(getRightTypeCell(parentNameCell).toString())) {
                sb.append(getRightTypeCell(parentNameCell).toString()).append("-");
            } else {
                sb.append("root-");
            }
            if (nameCell != null && StringUtils.isNotBlank(getRightTypeCell(nameCell).toString())) {
                sb.append(getRightTypeCell(nameCell).toString());
                parentAndSub.add(sb.toString());
            }
        }

        Iterator<Row> rowIt = sheet.rowIterator();
        rowIt.next();
        while (rowIt.hasNext()) {
            DepartmentDetailBO departmentDetailBO = new DepartmentDetailBO();
            Row curRow = rowIt.next();
            Cell codeCell = curRow.getCell(titleMap.get("code"));
            if (codeCell == null) {
                codeCell = curRow.createCell(titleMap.get("code"), CellType.STRING);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_PARAM_CODE_NECESSARY));
                codeCell.setCellComment(comment);
                codeCell.setCellStyle(cellStyle);
                flag = false;
            } else if (codeCell != null && StringUtils.isBlank(getRightTypeCell(codeCell).toString())) {
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_PARAM_CODE_NECESSARY));
                ExcelUtils.removeComment(codeCell);
                codeCell.setCellComment(comment);
                codeCell.setCellStyle(cellStyle);
                flag = false;
            } else if (codeCell != null && getRightTypeCell(codeCell).toString().length() > 50) {
                ExcelUtils.removeComment(codeCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_PARAM_CODE_LENGTH_ERROR));
                codeCell.setCellComment(comment);
                codeCell.setCellStyle(cellStyle);
                flag = false;

            } else if (codeCell != null && !Constants.codePattern.matcher(getRightTypeCell(codeCell).toString()).matches()) {
                ExcelUtils.removeComment(codeCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                comment.setString(new XSSFRichTextString(Constants.ORG_CODE_PATTERN));
                codeCell.setCellComment(comment);
                codeCell.setCellStyle(cellStyle);
                flag = false;
            } else if (codeCell != null) {
                String code = getRightTypeCell(codeCell).toString();
                if (parentCodes.indexOf(code) != parentCodes.lastIndexOf(code)) {
                    ExcelUtils.removeComment(codeCell);
                    //表格里有多条记录,编码相同
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                    comment.setString(new XSSFRichTextString(Constants.EXCEL_DEPARTMENT_CODE_DUPLICATION));
                    codeCell.setCellComment(comment);
                    codeCell.setCellStyle(cellStyle);
                    flag = false;
                } else {
                    QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
                    queryWrapper.eq("code", code);
                    queryWrapper.eq("valid", 1);
                    DepartmentAddPO dep = getOne(queryWrapper);
                    if (dep == null) {
                        departmentDetailBO.setCode(code);
                    } else {
                        if (companyId.equals(dep.getCompanyId())) {
                            departmentDetailBO.setId(dep.getId());
                        } else {
                            ExcelUtils.removeComment(codeCell);
                            Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                            comment.setString(new XSSFRichTextString(Constants.EXCEL_DEPARTMENT_CODE_DUPLICATION_DB));
                            codeCell.setCellComment(comment);
                            codeCell.setCellStyle(cellStyle);
                            flag = false;
                        }
                    }
                }
            }

            Cell parentCodeCell = curRow.getCell(titleMap.get("parentCode"));
            Cell parentNameCell = curRow.getCell(titleMap.get("parentName"));
            Boolean isDupParent = false;
            if (parentCodeCell != null && StringUtils.isNotBlank(getRightTypeCell(parentCodeCell).toString())) {
                QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
                queryWrapper.eq("code", getRightTypeCell(parentCodeCell).toString());
                queryWrapper.eq("valid", 1);
                queryWrapper.eq("company_id", companyId);
                int count = count(queryWrapper);
                if (!parentCodes.contains(getRightTypeCell(parentCodeCell).toString()) && count == 0) {
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                    comment.setString(new XSSFRichTextString(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS.getMessage()));
                    ExcelUtils.removeComment(parentCodeCell);
                    parentCodeCell.setCellComment(comment);
                    parentCodeCell.setCellStyle(cellStyle);
                    flag = false;
                } else if (count > 0) {
                    DepartmentAddPO parentDep = getOne(queryWrapper);
                    departmentDetailBO.setParentId(parentDep.getId());
                } else {
                    departmentDetailBO.setParentCode(getRightTypeCell(parentCodeCell).toString());
                }
            } else {
                if (parentNameCell != null && StringUtils.isNotBlank(getRightTypeCell(parentNameCell).toString())) {
                    QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
                    queryWrapper.eq("name", getRightTypeCell(parentNameCell).toString());
                    queryWrapper.eq("valid", 1);
                    queryWrapper.eq("company_id", companyId);
                    int count = count(queryWrapper);
                    if (!parentNames.contains(getRightTypeCell(parentNameCell).toString()) && count == 0) {
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                        comment.setString(new XSSFRichTextString(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS.getMessage()));
                        ExcelUtils.removeComment(parentNameCell);
                        parentNameCell.setCellComment(comment);
                        parentNameCell.setCellStyle(cellStyle);
                        flag = false;
                    } else if (count > 1) {
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                        comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_NAME_DUP_CODE_NEED));
                        ExcelUtils.removeComment(parentNameCell);
                        parentNameCell.setCellComment(comment);
                        parentNameCell.setCellStyle(cellStyle);
                        flag = false;
                        isDupParent = true;
                    } else {
                        if (parentNames.contains(getRightTypeCell(parentNameCell).toString()) && count >= 1) {
                            Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                            comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_NAME_DUP_CODE_NEED));
                            ExcelUtils.removeComment(parentNameCell);
                            parentNameCell.setCellComment(comment);
                            parentNameCell.setCellStyle(cellStyle);
                            flag = false;
                            isDupParent = true;
                        } else if (parentNames.indexOf(getRightTypeCell(parentNameCell).toString()) != parentNames.lastIndexOf(getRightTypeCell(parentNameCell).toString())) {
                            Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                            comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_NAME_DUP_CODE_NEED));
                            ExcelUtils.removeComment(parentNameCell);
                            parentNameCell.setCellComment(comment);
                            parentNameCell.setCellStyle(cellStyle);
                            flag = false;
                            isDupParent = true;
                        } else {
                            if (count == 1) {
                                DepartmentAddPO parentDep = getOne(queryWrapper);
                                departmentDetailBO.setParentId(parentDep.getId());
                            } else {
                                departmentDetailBO.setParentCode(parentCodes.get(parentNames.indexOf(getRightTypeCell(parentNameCell).toString())));
                            }
                        }
                    }
                }
            }
            Cell nameCell = curRow.getCell(titleMap.get("name"));
            if (nameCell == null) {
                nameCell = curRow.createCell(titleMap.get("name"), CellType.STRING);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_PARAM_NAME_NECESSARY));
                nameCell.setCellComment(comment);
                nameCell.setCellStyle(cellStyle);
                flag = false;
            } else if (nameCell != null && StringUtils.isBlank(getRightTypeCell(nameCell).toString())) {
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_PARAM_NAME_NECESSARY));
                ExcelUtils.removeComment(nameCell);
                nameCell.setCellComment(comment);
                nameCell.setCellStyle(cellStyle);
                flag = false;
            } else if (getRightTypeCell(nameCell).toString().length() > 200) {
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_PARAM_NAME_LENGTH_ERROR));
                ExcelUtils.removeComment(nameCell);
                nameCell.setCellComment(comment);
                nameCell.setCellStyle(cellStyle);
                flag = false;

            } else {
                if (!isDupParent) {
                    String parentCodeAndCurName = "";
                    if (parentCodeCell != null && StringUtils.isNotBlank(getRightTypeCell(parentCodeCell).toString())) {
                        parentCodeAndCurName = getRightTypeCell(parentCodeCell).toString() + "-";
                    } else {
                        parentCodeAndCurName = "root-";
                    }
                    if (parentNameCell != null && StringUtils.isNotBlank(getRightTypeCell(parentNameCell).toString())) {
                        parentCodeAndCurName = parentCodeAndCurName + getRightTypeCell(parentNameCell).toString() + "-";
                    } else {
                        parentCodeAndCurName = parentCodeAndCurName + "root-";
                    }
                    parentCodeAndCurName = parentCodeAndCurName + getRightTypeCell(nameCell).toString();
                    if (parentAndSub.indexOf(parentCodeAndCurName) != parentAndSub.lastIndexOf(parentCodeAndCurName)) {
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                        comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_PARENT_NAME_ONLY));
                        ExcelUtils.removeComment(nameCell);
                        nameCell.setCellComment(comment);
                        nameCell.setCellStyle(cellStyle);
                        flag = false;
                    } else {
                        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
                        queryWrapper.eq("company_id", companyId);
                        if (departmentDetailBO.getParentId() == null) {
                            queryWrapper.isNull("parent_id");
                        } else {
                            queryWrapper.eq("parent_id", departmentDetailBO.getParentId());
                        }
                        if (codeCell != null && StringUtils.isNotBlank(getRightTypeCell(codeCell).toString())) {
                            queryWrapper.ne("code", getRightTypeCell(codeCell).toString());
                        }
                        queryWrapper.eq("name", getRightTypeCell(nameCell).toString());
                        queryWrapper.eq("valid", 1);
                        int depCount = count(queryWrapper);
                        if (depCount > 0) {
                            Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                            comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_PARENT_NAME_ONLY));
                            ExcelUtils.removeComment(nameCell);
                            nameCell.setCellComment(comment);
                            nameCell.setCellStyle(cellStyle);
                            flag = false;
                        } else {
                            departmentDetailBO.setName(getRightTypeCell(nameCell).toString());
                        }
                    }
                }
            }

            Cell managerCodeCell = curRow.getCell(titleMap.get("managerCode"));
            if (managerCodeCell != null && StringUtils.isNotBlank(getRightTypeCell(managerCodeCell).toString())) {
                String managerCodes = getRightTypeCell(managerCodeCell).toString();
                String[] managerCodeArr = managerCodes.split(",");
                Set<String> managerCodeSet = new HashSet<String>();
                for (String managerCode : managerCodeArr) {
                    if (StringUtils.isNotBlank(managerCode)) {
                        managerCodeSet.add(managerCode);
                    }
                }
                QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
                queryWrapper.in("code", managerCodeSet);
                queryWrapper.eq("valid", 1);
                int count = personMapper.selectCount(queryWrapper);
                if (managerCodeSet.size() != count) {
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                    comment.setString(new XSSFRichTextString(OrganizationErrorEnum.PERSON_ID_NOT_EXISTS.getMessage()));
                    ExcelUtils.removeComment(managerCodeCell);
                    managerCodeCell.setCellComment(comment);
                    managerCodeCell.setCellStyle(cellStyle);
                    flag = false;
                } else {
                    List<PersonAddPO> persons = personMapper.selectList(queryWrapper);
                    List<OrganizationManagerBO> managers = new ArrayList<OrganizationManagerBO>();
                    persons.stream().forEach(person -> {
                        OrganizationManagerBO organizationManagerBO = new OrganizationManagerBO();
                        organizationManagerBO.setManagerId(person.getId());
                        managers.add(organizationManagerBO);
                    });
                    departmentDetailBO.setManagers(managers);
                }
            }
            Cell descCell = curRow.getCell(titleMap.get("description"));
            if (descCell != null && StringUtils.isNotBlank(getRightTypeCell(descCell).toString()) && getRightTypeCell(descCell).toString().length() > 500) {
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_PARAM_DESC_LENGTH_ERROR));
                ExcelUtils.removeComment(descCell);
                descCell.setCellComment(comment);
                descCell.setCellStyle(cellStyle);
                flag = false;
            } else if (descCell != null) {
                departmentDetailBO.setDescription(getRightTypeCell(descCell).toString());
            }

            if (flag) {
                importDeps.add(departmentDetailBO);
            }
        }

        return flag;
    }

    private static Object getRightTypeCell(Cell cell) {

        Object object = null;
        if (cell != null) {
            if (cell.getCellType().equals(CellType.STRING)) {
                object = cell.getStringCellValue();
            } else if (cell.getCellType().equals(CellType.NUMERIC)) {
                object = (long)cell.getNumericCellValue() + "";
            } else if (cell.getCellType().equals(CellType.FORMULA)) {
                object = cell.getNumericCellValue();
            } else if (cell.getCellType().equals(CellType.BLANK)) {
                object = cell.getStringCellValue();
            } else {
                object = cell.getStringCellValue();
            }
        }
        return object;
    }


    /**
     * 下载模板
     * @param file
     * @throws IOException
     */
    @Override
    public void downlowdExcelTemplate(File file) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        ExcelUtils.createExplainSheet(ExcelUtils.DEPARTMENT_TEMPLATE_EXPLAIN, workbook);
        Sheet depSheet = workbook.createSheet(Constants.DEPARTMENT_DATA_SHEETNAME);


        for (int i = 0; i < ExcelUtils.DEPARTMENT_IMPORT_TEMPLATE.size(); i++) {
            depSheet.setColumnWidth(i, ExcelUtils.COLUMN_LENGTH);
        }
        ExcelUtils.createHeadComments(depSheet, ExcelUtils.DEPARTMENT_IMPORT_TEMPLATE);
        //setComment(personSheet, PERSON_COMMENT, language);
        OutputStream outputStream = new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.flush();
    }

    /**
     * 导出部门数据到excel中
     * @param ids 选择的部门
     * @param all 是否全部
     * @param taskId 任务id
     * @param companyId 公司id
     */
    @Override
    public void exportExcelData(List<Long> ids, Boolean all, Long taskId, Long companyId) {
        ExcelPO excelPO = excelMapper.selectById(taskId);
        excelPO.setStatus(1);
        //excelMapper.updateById(excelPO);
        excelManageService.excuteExcelState(excelPO);
        XSSFWorkbook workbook = new XSSFWorkbook();
        ExcelUtils.createExplainSheet(ExcelUtils.DEPARTMENT_TEMPLATE_EXPLAIN, workbook);
        Sheet sheet = workbook.createSheet(Constants.DEPARTMENT_DATA_SHEETNAME);
        ExcelUtils.createHeadComments(sheet, ExcelUtils.DEPARTMENT_IMPORT_TEMPLATE);
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
        queryWrapper.eq("company_id", companyId);
        queryWrapper.eq("valid", 1);
        if (!all) {
            queryWrapper.in("id", ids);
        }
        List<DepartmentAddPO> deps = list(queryWrapper);
        if (deps == null || deps.size() == 0) {
            try {
                String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.DEPARTMENT_FILE, taskId);
                excelPO.setFileName(filePath);
                excelPO.setStatus(2);
                //excelMapper.updateById(excelPO);
                return;
            } catch (IOException e) {
                log.warn("部门导出失败", e.getCause());
                excelPO.setStatus(3);
                //excelMapper.updateById(excelPO);
                return;
            } finally {
                excelManageService.excuteExcelState(excelPO);
            }
        }
        List<DepartmentExcelBO> list = new ArrayList<DepartmentExcelBO>();
        deps.stream().forEach(dep -> {
            DepartmentExcelBO departmentExcelBO = new DepartmentExcelBO();
            BeanUtils.copyProperties(dep, departmentExcelBO);
            if (dep.getParentId() != null) {
                DepartmentAddPO parentDep = getById(dep.getParentId());
                if (parentDep != null) {
                    departmentExcelBO.setParentCode(parentDep.getCode());
                    departmentExcelBO.setParentName(parentDep.getName());
                }
            }
            QueryWrapper<OrganizationManagerPO> managerWrapper = new QueryWrapper<OrganizationManagerPO>();
            managerWrapper.eq("org_id", dep.getId());
            managerWrapper.eq("manager_type", Constants.DEPARTMENT);
            List<OrganizationManagerPO> orgManagers = organizationManagerMapper.selectList(managerWrapper);
            if (orgManagers != null && orgManagers.size() > 0) {
                List<OrganizationManagerBO> managers = new ArrayList<OrganizationManagerBO>();
                orgManagers.stream().forEach(orgManager -> {
                    OrganizationManagerBO organizationManagerBO = new OrganizationManagerBO();
                    BeanUtils.copyProperties(orgManager, organizationManagerBO);
                    managers.add(organizationManagerBO);
                });
                departmentExcelBO.setManagers(managers);
            }
            list.add(departmentExcelBO);
        });
        int rowIndex = 1;
        for (DepartmentExcelBO dept : list) {
            Row row = sheet.createRow(rowIndex);
            createCell(row, dept);
            rowIndex++;
        }
        try {
            String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.PERSON_FILE, taskId);
            excelPO.setFileName(filePath);
            excelPO.setStatus(2);
            //excelMapper.updateById(excelPO);
            return;
        } catch (IOException e) {
            log.error("部门导出异常", e.getCause());
            excelPO.setStatus(3);
            //excelMapper.updateById(excelPO);
            return;
        } finally {
            excelManageService.excuteExcelState(excelPO);
        }
    }

    private void createCell(Row curRow, DepartmentExcelBO dept) {

        curRow.createCell(0, CellType.STRING).setCellValue(dept.getCode());
        curRow.createCell(1, CellType.STRING).setCellValue(dept.getName());
        curRow.createCell(2, CellType.STRING).setCellValue(dept.getParentCode());
        curRow.createCell(3, CellType.STRING).setCellValue(dept.getParentName());
        if (dept.getManagers() != null && dept.getManagers().size() > 0) {
            StringBuilder managerCodes = new StringBuilder();
            StringBuilder managerNames = new StringBuilder();
            for (int i = 0; i < dept.getManagers().size(); i++) {
                OrganizationManagerBO organizationManagerBO = dept.getManagers().get(i);
                PersonAddPO personAddPO = personMapper.selectById(organizationManagerBO.getManagerId());
                if (personAddPO != null) {
                    if (i < (dept.getManagers().size() - 1)) {
                        managerCodes.append(personAddPO.getCode()).append(",");
                    } else {
                        managerCodes.append(personAddPO.getCode());
                    }
                }
                if (i < (dept.getManagers().size() - 1)) {
                    managerNames.append(organizationManagerBO.getManagerName()).append(",");
                } else {
                    managerNames.append(organizationManagerBO.getManagerName());
                }
            }
            curRow.createCell(4, CellType.STRING).setCellValue(managerCodes.toString());
            curRow.createCell(5, CellType.STRING).setCellValue(managerNames.toString());
        }
        curRow.createCell(6, CellType.STRING).setCellValue(dept.getDescription());

    }

    /**
     * 导出部门关联人员
     * @param ids
     * @param all
     * @param taskId
     * @param deptId
     */
    @Override
    public void exportPersonExcelData(List<Long> ids, Boolean all, Long taskId, Long deptId) {
        ExcelPO excelPO = excelMapper.selectById(taskId);
        excelPO.setStatus(1);
        //excelMapper.updateById(excelPO);
        excelManageService.excuteExcelState(excelPO);
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet(Constants.PERSON_DATA_SHEETNAME);
            ExcelUtils.createHeadComments(sheet, ExcelUtils.RELATION_PERSON_EXPORT);
            List<Long> personIds = new ArrayList<Long>();
            if (!all) {
                if (ids == null || ids.size() == 0) {
                    try {
                        String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.DEPARTMENT_RELATION_FILE, taskId);
                        excelPO.setFileName(filePath);
                        excelPO.setStatus(2);
                        //excelMapper.updateById(excelPO);
                        return;
                    } catch (IOException e) {
                        log.warn("部门人员导出失败", e.getCause());
                        excelPO.setStatus(3);
                        //excelMapper.updateById(excelPO);
                        return;
                    } finally {
                        excelManageService.excuteExcelState(excelPO);
                    }
                }
                personIds = ids;
            } else {
                DepartmentAddPO departmentAddPO = getById(deptId);
                if (departmentAddPO == null) {
                    try {
                        String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.DEPARTMENT_RELATION_FILE, taskId);
                        excelPO.setFileName(filePath);
                        excelPO.setStatus(2);
                        //excelMapper.updateById(excelPO);
                        return;
                    } catch (IOException e) {
                        log.warn("部门人员导出失败", e.getCause());
                        excelPO.setStatus(3);
                        //excelMapper.updateById(excelPO);
                        return;
                    } finally {
                        excelManageService.excuteExcelState(excelPO);
                    }
                }
                QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
                posWrapper.eq("dep_id", departmentAddPO.getId());
                posWrapper.eq("valid", 1);
                List<PositionAddPO> posList = positionMapper.selectList(posWrapper);
                if (posList == null || posList.size() == 0) {
                    try {
                        String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.DEPARTMENT_RELATION_FILE, taskId);
                        excelPO.setFileName(filePath);
                        excelPO.setStatus(2);
                        //excelMapper.updateById(excelPO);
                        return;
                    } catch (IOException e) {
                        log.warn("部门人员导出失败", e.getCause());
                        excelPO.setStatus(3);
                        //excelMapper.updateById(excelPO);
                        return;
                    } finally {
                        excelManageService.excuteExcelState(excelPO);
                    }
                }
                List<Long> posIds = new ArrayList<Long>();
                posList.stream().forEach(pos -> posIds.add(pos.getId()));
                QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
                relationWrapper.in("position_id", posIds);
                relationWrapper.eq("valid", 1);
                List<PositionPersonPO> relationList = positionPersonMapper.selectList(relationWrapper);
                if (relationList == null || relationList.size() == 0) {
                    try {
                        String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.DEPARTMENT_RELATION_FILE, taskId);
                        excelPO.setFileName(filePath);
                        excelPO.setStatus(2);
                        //excelMapper.updateById(excelPO);
                        return;
                    } catch (IOException e) {
                        log.warn("部门人员导出失败", e.getCause());
                        excelPO.setStatus(3);
                        //excelMapper.updateById(excelPO);
                        return;
                    } finally {
                        excelManageService.excuteExcelState(excelPO);
                    }
                }
                for (PositionPersonPO relation : relationList) {
                    personIds.add(relation.getPersonId());
                }
            }

            QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<PersonAddPO>();

            personWrapper.eq("valid", 1);
            personWrapper.eq("sys_flag", 0);
            personWrapper.orderByDesc("create_time");

            // added by xhf on 2021-02-20
            // personWrapper.in("id", personIds);
            List personIdList = personIds;
            personWrapper.and(item -> {
                for (int i = 0; i < personIdList.size(); i++) {
                    item.or().eq("id", personIdList.get(i));
                }
            });

            List<PersonAddPO> personList = personMapper.selectList(personWrapper);
            if (personList == null || personList.size() == 0) {
                try {
                    String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.DEPARTMENT_RELATION_FILE, taskId);
                    excelPO.setFileName(filePath);
                    excelPO.setStatus(2);
                    //excelMapper.updateById(excelPO);
                    return;
                } catch (IOException e) {
                    log.warn("部门人员导出失败", e.getCause());
                    excelPO.setStatus(3);
                    //excelMapper.updateById(excelPO);
                    return;
                } finally {
                    excelManageService.excuteExcelState(excelPO);
                }
            }
            int index = 1;
            for (PersonAddPO person : personList) {
                Row row = sheet.createRow(index);
                createRelationCell(row, person);
                index++;
            }

            try {
                String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.DEPARTMENT_RELATION_FILE, taskId);
                excelPO.setFileName(filePath);
                excelPO.setStatus(2);
                //excelMapper.updateById(excelPO);
            } catch (IOException e) {
                log.warn("部门人员导出失败", e.getCause());
                excelPO.setStatus(3);
                //excelMapper.updateById(excelPO);
                return;
            } finally {
                excelManageService.excuteExcelState(excelPO);
            }
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                log.warn("部门人员导出失败", e.getCause());
            }
        }
    }
    private void createRelationCell(Row curRow, PersonAddPO person) {

        curRow.createCell(0, CellType.STRING).setCellValue(person.getName());
        curRow.createCell(1, CellType.STRING).setCellValue(person.getCode());
        curRow.createCell(2, CellType.STRING).setCellValue(person.getPhone());
        PersonDetailBO personDetailBO = new PersonDetailBO();
        personDetailBO.setGender(person.getGender());
        transferSystemCode(personDetailBO);
        curRow.createCell(3, CellType.STRING).setCellValue(personDetailBO.getGender());

    }

    @Override
    public List<DepartmentDetailBO> queryDeptInfoByIds(List<Long> ids) {
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
//        queryWrapper.in("id", ids);
        queryWrapper.eq("valid", 1);
//        List<DepartmentAddPO> list = list(queryWrapper);
        List<DepartmentAddPO> list = MultiParamSql.forDepartment(ids, queryWrapper, "id", departmentMapper);
        if (list == null || list.size() == 0) {
            return null;
        }
        List<DepartmentDetailBO> bos = new ArrayList<DepartmentDetailBO>();
        list.stream().forEach(po -> {
            DepartmentDetailBO departmentDetailBO = new DepartmentDetailBO();
            BeanUtils.copyProperties(po, departmentDetailBO);
            bos.add(departmentDetailBO);
        });
        return bos;
    }

    @Override
    public List<DepartmentDetailBO> queryDepartmentByCodes(List<String> codes) {
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
        queryWrapper.in("code", codes);
        queryWrapper.eq("valid", true);
        List<DepartmentAddPO> list = list(queryWrapper);
        if (list == null || list.size() == 0) {
            return new ArrayList<DepartmentDetailBO>();
        }
        List<DepartmentDetailBO> bos = new ArrayList<DepartmentDetailBO>();
        list.stream().forEach(dept -> {
            DepartmentDetailBO departmentDetailBO = new DepartmentDetailBO();
            BeanUtils.copyProperties(dept, departmentDetailBO);
            bos.add(departmentDetailBO);
        });
        return bos;
    }

    @Override
    public List<Long> querySubDepartmentIdsByDepartmentId(List<Long> ids) {
        QueryWrapper<DepartmentAddPO> posWrapper = new QueryWrapper<DepartmentAddPO>();
        posWrapper.in("id", ids);
        List<DepartmentAddPO> parentDeptes = list(posWrapper);

        if (parentDeptes == null || parentDeptes.size() == 0) {
            return new ArrayList<Long>();
        }
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
        queryWrapper.eq("valid", 1);


        queryWrapper.and(item -> {

            for (int i = 0; i < parentDeptes.size(); i++) {
                item.or().likeRight("full_path", parentDeptes.get(i).getFullPath());
            }

        });
        List<DepartmentAddPO> positions = list(queryWrapper);
        if (positions == null || positions.size() == 0) {
            return new ArrayList<Long>();
        }
        List<Long> subIds = new ArrayList<Long>();
        positions.stream().forEach(dept -> {
            if (!ids.contains(dept.getId())) {
                subIds.add(dept.getId());
            }
        });
        return subIds;
    }

    @Override
    public DepartmentDetailBO queryCurrentUserDept(String personCode, Long companyId) {

        if (StringUtils.isBlank(personCode) || companyId == null) {
            return null;
        }
        QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<>();
        comWrapper.eq("id", companyId);
        comWrapper.eq("valid", 1);
        CompanyPO companyPO = companyMapper.selectById(companyId);
        if (companyPO == null) {
            return null;
        }
        comWrapper.clear();
        comWrapper.eq("valid", 1);
        comWrapper.likeRight("full_path", companyPO.getFullPath() + "/");
        List<CompanyPO> coms = companyMapper.selectList(comWrapper);
        if (coms == null) {
            coms = new ArrayList<>();
        }
        coms.add(companyPO);
        List<Long> companyIds = new ArrayList<>();
        coms.stream().forEach(com -> companyIds.add(com.getId()));

        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", personCode);
        queryWrapper.eq("valid", 1);
        PersonAddPO personAddPO = personMapper.selectOne(queryWrapper);
        if (personAddPO == null) {
            return null;
        }
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();
        posWrapper.eq("id", personAddPO.getMainPosition());
        posWrapper.eq("valid", 1);
        posWrapper.in("company_id", companyIds);
        PositionAddPO positionAddPO = positionMapper.selectOne(posWrapper);
        if (positionAddPO != null) {
            DepartmentAddPO mainDepartment = getById(positionAddPO.getDepId());
            if (mainDepartment != null) {
                DepartmentDetailBO departmentDetailBO = new DepartmentDetailBO();
                BeanUtils.copyProperties(mainDepartment, departmentDetailBO);
                return departmentDetailBO;
            }
        }
        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<>();
        relationWrapper.eq("person_id", personAddPO.getId());
        relationWrapper.eq("valid", 1);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(relationWrapper);
        if (relations == null || relations.size() == 0) {
            return null;
        }
        List<Long> posIds = new ArrayList<>();
        relations.stream().forEach(relation -> posIds.add(relation.getPositionId()));
        QueryWrapper<PositionAddPO> latestPosWrapper = new QueryWrapper<>();
        latestPosWrapper.in("id", posIds);
        latestPosWrapper.in("company_id", companyIds);
        latestPosWrapper.eq("valid", 1);
        List<PositionAddPO> poses = positionMapper.selectList(latestPosWrapper);
        if (poses == null || poses.size() == 0) {
            return null;
        }
        QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<>();
        deptWrapper.eq("id", poses.get(0).getDepId());
        DepartmentAddPO departmentAddPO = getOne(deptWrapper);
        if (departmentAddPO == null) {
            return null;
        }
        DepartmentDetailBO departmentDetailBO = new DepartmentDetailBO();
        BeanUtils.copyProperties(departmentAddPO, departmentDetailBO);
        return departmentDetailBO;
    }

    @Override
    public DepartmentDetailBO getDepDetailByCode(String code) {
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
        queryWrapper.eq("code", code);
        queryWrapper.eq("valid", 1);
        int count = count(queryWrapper);
        //判断该id的部门是否存在
        if (count == 0) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
        }
        DepartmentAddPO curDep = getOne(queryWrapper);
        Long depId = curDep.getId();
        DepartmentDetailBO departmentDetailBO = new DepartmentDetailBO();
        DepartmentAddPO departmentAddPo = getOne(queryWrapper);
        BeanUtils.copyProperties(departmentAddPo, departmentDetailBO);
        String typeName = organizationAdapter.getSystemCodeName(departmentDetailBO.getType());
        departmentDetailBO.setTypeName(typeName);
        /*if ("emergency".equals(departmentDetailBO.getType())) {
            departmentDetailBO.setTypeName("应急部门");
        } else {
            departmentDetailBO.setTypeName("普通部门");
        }*/
        QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<PositionAddPO>();
        positionWrapper.eq("dep_id", depId);
        positionWrapper.eq("valid", 1);
        List<PositionAddPO> poses = positionMapper.selectList(positionWrapper);
        departmentDetailBO.setRelPos(poses);

        QueryWrapper<OrganizationManagerPO> managerWrapper = new QueryWrapper<OrganizationManagerPO>();
        managerWrapper.eq("org_id", depId);
        managerWrapper.eq("manager_type", Constants.DEPARTMENT);
        List<OrganizationManagerPO> managers = organizationManagerMapper.selectList(managerWrapper);
        if (managers == null) {
            return departmentDetailBO;
        }
        List<OrganizationManagerBO> list = new ArrayList<OrganizationManagerBO>();
        managers.stream().forEach(manager -> {
            OrganizationManagerBO organizationManagerBO = new OrganizationManagerBO();
            BeanUtils.copyProperties(manager, organizationManagerBO);
            PersonAddPO personAddPO = personMapper.selectById(manager.getManagerId());
            if (personAddPO != null) {
                organizationManagerBO.setManagerName(personAddPO.getName());
                organizationManagerBO.setManagerCode(personAddPO.getCode());
            }
            list.add(organizationManagerBO);
        });
        departmentDetailBO.setManagers(list);
        return departmentDetailBO;
    }

    @Override
    public JSONObject getDepartmentById(Long id, String includes) {
        if (StringUtils.isBlank(includes)) {
            return new JSONObject();
        }
        DepartmentAddPO departmentAddPO = getById(id);
        if (departmentAddPO == null) {
            return new JSONObject();
        }
        DepartmentBaseServiceBO departmentDetailBO = new DepartmentBaseServiceBO();
        BeanUtils.copyProperties(departmentAddPO, departmentDetailBO);
        JSONObject deptJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(departmentDetailBO), includes, Constants.DEPARTMENT);

        /*QueryWrapper<OrganizationManagerPO> managerPOQueryWrapper = new QueryWrapper<>();
        managerPOQueryWrapper.eq("orgId", departmentAddPO.getId());
        List<OrganizationManagerPO> managerRelations = organizationManagerMapper.selectList(managerPOQueryWrapper);
        if (managerRelations != null && managerRelations.size() > 0) {
            List<Long> personIds = new ArrayList<>();
            managerRelations.stream().forEach(rel -> personIds.add(rel.getManagerId()));
            QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
            personWrapper.in("id", personIds);
            personWrapper.eq("valid", 1);
            List<PersonAddPO> persons = personMapper.selectList(personWrapper);
            if (persons == null || persons.size() == 0) {
                deptJson.put("manager", new JSONObject());
            } else {
                PersonAddPO person = persons.get(0);
                PersonBaseServiceBO personBaseServiceBO = new PersonBaseServiceBO();
                BeanUtils.copyProperties(person, personBaseServiceBO);
                JSONObject managerJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(personBaseServiceBO), includes, Constants.PERSON);
                deptJson.put("manager", managerJson);
            }
        } else {
            deptJson.put("manager", new JSONObject());
        }

        CompanyPO companyPO = companyMapper.selectById(departmentAddPO.getCompanyId());
        if (companyPO == null) {
            return deptJson;
        }
        CompanyBaseServiceBO companyBaseServiceBO = new CompanyBaseServiceBO();
        BeanUtils.copyProperties(companyPO, companyBaseServiceBO);
        JSONObject companyJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(companyBaseServiceBO), includes, Constants.COMPANY);
        deptJson.put("company", companyJson);*/
        return deptJson;
    }

    @Override
    public List<JSONObject> getDeptTree(Long treeId, Long companyId) {
        /*DepartmentAddPO parentDept = getById(treeId);
        if (parentDept == null) {
            return new ArrayList<JSONObject>();
        }
        if (parentDept.getLeaf()) {
            return new ArrayList<JSONObject>();
        }*/
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("company_id", companyId);
        if (treeId == -1) {
            queryWrapper.isNull("parent_id");
        } else {
            queryWrapper.eq("parent_id", treeId);
        }

        queryWrapper.eq("valid", 1);
        queryWrapper.eq("sys_flag", 0);
        List<DepartmentAddPO> depts = list(queryWrapper);
        if (depts == null || depts.size() == 0) {
            return new ArrayList<JSONObject>();
        }
        List<JSONObject> deptList = new ArrayList<>();
        depts.stream().forEach(dept -> {
            DepartmentBaseServiceBO departmentBaseServiceBO = new DepartmentBaseServiceBO();
            BeanUtils.copyProperties(dept, departmentBaseServiceBO);
            JSONObject deptJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(departmentBaseServiceBO), null, Constants.DEPARTMENT);
            if (dept.getLeaf()) {
                deptJson.put("isParent", false);
            } else {
                deptJson.put("isParent", true);
            }
            deptList.add(deptJson);
        });
        return deptList;
    }

    @Override
    public List<DepartmentDetailBO> querySubDepartmentByParentId(Long id, Boolean all, Long cid) {
        QueryWrapper<DepartmentAddPO> subPosWrapper = new QueryWrapper<>();
        subPosWrapper.eq("valid", 1);
        subPosWrapper.eq("sys_flag", 0);
        if (id != null) {
            DepartmentAddPO departmentAddPO = getById(id);
            if (departmentAddPO == null) {
                return null;
            }
            if (all != null && all) {
                subPosWrapper.likeRight("full_path", departmentAddPO.getFullPath() + "/");
            } else {
                subPosWrapper.eq("parent_id", departmentAddPO.getId());
            }

        } else if (cid != null) {
            if (all != null && all) {
                subPosWrapper.eq("company_id", cid);
            } else {
                subPosWrapper.eq("company_id", cid);
                subPosWrapper.isNull("parent_id");
            }
        } else {
            return null;
        }
        List<DepartmentAddPO> list = list(subPosWrapper);
        if (list == null || list.size() == 0) {
            return null;
        }
        List<DepartmentDetailBO> departmentDetailBOS = new ArrayList<>();
        list.stream().forEach(dept -> {
            DepartmentDetailBO departmentDetailBO = new DepartmentDetailBO();
            BeanUtils.copyProperties(dept, departmentDetailBO);
            departmentDetailBOS.add(departmentDetailBO);
        });
        return departmentDetailBOS;
    }

    @Override
    public boolean updateBatchByIds(Collection<DepartmentAddPO> entityList) {
        return super.updateBatchById(entityList);
    }

    //======================================old version===============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOldDepartment(JSONObject body, String tenantId) {
        QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
        comWrapper.eq("old_id", body.getString("root"));

        CompanyPO companyPO = companyMapper.selectOne(comWrapper);
        if (companyPO == null) {
            throw new OrganizationException(400, Constants.COMPANY_NOT_EXISTS);
        }
        DepartmentAddPO departmentAddPO = new DepartmentAddPO();
        departmentAddPO.setCode(body.getString("code"));
        departmentAddPO.setName(body.getString("showName"));
        departmentAddPO.setCompanyId(companyPO.getId());
        if (body.getDouble("sequenceNumber") != null) {
            departmentAddPO.setSort(body.getDouble("sequenceNumber"));
        }
        if (body.getJSONObject("parent") != null && StringUtils.isNotBlank(body.getJSONObject("parent").getString("name"))
                && !body.getJSONObject("parent").getString("name").equals(Constants.ORG_OLD_NAME)) {
            String parentOldId = body.getJSONObject("parent").getString("name");
            QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<DepartmentAddPO>();
            deptWrapper.eq("old_id", parentOldId);
            DepartmentAddPO parentDept = getOne(deptWrapper);
            if (parentDept == null) {
                throw new OrganizationException(400, Constants.DEPT_NOT_EXISTS);
            }
            departmentAddPO.setParentId(parentDept.getId());
        }
        List<Long> managerIds = null;
        if (StringUtils.isNotBlank(body.getString("managerName"))) {
            String managerName = body.getString("managerName");
            QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<PersonAddPO>();
            personWrapper.eq("old_id", managerName);
            PersonAddPO personAddPO = personMapper.selectOne(personWrapper);
            if (personAddPO == null) {
                throw new OrganizationException(400, Constants.POSITION_NOT_EXISTS);
            }
            managerIds = new ArrayList<Long>();
            managerIds.add(personAddPO.getId());
        }
        Long uid = IDGenerator.newInstance().generate().longValue();
        String oldId = "Department_" + uid;
        departmentAddPO.setId(uid);
        departmentAddPO.setOldId(oldId);
        departmentAddPO.setValid(true);
        addDepartment(departmentAddPO, managerIds, tenantId);
    }

    @Override
    public List<PersonDetailBO> queryDepartmentUsers(Long companyId, Long departmentId, String keyword, Boolean onlyUser) {
        CompanyPO companyPO = companyMapper.selectById(companyId);
        List<Long> positionIds = new ArrayList<>();
        if (departmentId != null && !companyId.equals(departmentId)) {
            QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<>();
            positionWrapper.eq("dep_id", departmentId);
            positionWrapper.eq("valid", 1);
            List<PositionAddPO> positions = positionMapper.selectList(positionWrapper);
            //查岗位下的人员
            if (positions == null || positions.size() == 0) {
                return null;
            }
            positions.stream().forEach(pos -> positionIds.add(pos.getId()));
        } else {
            //查公司全部
            QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("company_id", companyId);
            queryWrapper.eq("valid", 1);
            queryWrapper.eq("sys_flag", 0);
            List<PositionAddPO> positions = positionMapper.selectList(queryWrapper);
            if (positions == null || positions.size() == 0) {
                return null;
            }
            positions.stream().forEach(pos -> positionIds.add(pos.getId()));
        }
        QueryWrapper<PositionPersonPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("valid", 1);
        queryWrapper.and(item -> {
            for (Long posId : positionIds) {
                item.or().eq("position_id", posId);
            }
        });

        List<PositionPersonPO> relations = positionPersonMapper.selectList(queryWrapper);
        if (relations == null || relations.size() == 0) {
            return null;
        }
        Map<Long, List<Long>> personIdToPosIds = new HashMap<>();

        List<Long> personIds = new ArrayList<>();
        positionIds.clear();
        relations.stream().forEach(rel -> {
            personIds.add(rel.getPersonId());
            positionIds.add(rel.getPositionId());
            List<Long> posIds = personIdToPosIds.get(rel.getPersonId());
            if (posIds == null) {
                posIds = new ArrayList<>();
                personIdToPosIds.put(rel.getPersonId(), posIds);
            }
            posIds.add(rel.getPositionId());
        });
        Map<Long, UserDetailDTO> userDetailDTOMap = organizationAdapter.queryUsersByPersonIds(personIds, keyword);
        if ((userDetailDTOMap == null || userDetailDTOMap.size() == 0) && (onlyUser == null || onlyUser)) {
            return null;
        }
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
        if (onlyUser == null || onlyUser) {
            personIds.clear();
            Iterator<Long> it = userDetailDTOMap.keySet().iterator();
            while (it.hasNext()) {
                Long perId = it.next();
                personIds.add(perId);
                personWrapper.or().eq("id", perId);
            }
        } else {
            personIds.stream().forEach(perId -> {
                personWrapper.or().eq("id", perId);
            });
        }

        List<PersonAddPO> persons = personMapper.selectList(personWrapper);
        if (persons == null || persons.size() == 0) {
            return null;
        }
        List<PositionAddPO> positions = positionMapper.selectBatchIds(positionIds);
        //岗位id和岗位对应关系
        Map<Long, PositionAddPO> posIdToPO = new HashMap<>();
        QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<>();
        positions.stream().forEach(pos -> {
            deptWrapper.or().eq("id", pos.getDepId());
            posIdToPO.put(pos.getId(), pos);
        });
        List<DepartmentAddPO> depts = list(deptWrapper);
        //部门id和部门对应关系
        Map<Long, DepartmentAddPO> depIdToPO = new HashMap<>();
        depts.stream().forEach(dep -> {
            depIdToPO.put(dep.getId(), dep);
        });
        List<PersonDetailBO> personDetailBOS = new ArrayList<>();
        persons.stream().forEach(person -> {
            PersonDetailBO personDetailBO = new PersonDetailBO();
            BeanUtils.copyProperties(person, personDetailBO);

            if (userDetailDTOMap != null && userDetailDTOMap.get(person.getId()) != null) {
                UserDetailDTO userDetailDTO = userDetailDTOMap.get(person.getId());
                personDetailBO.setUserName(userDetailDTO.getUserName());
                personDetailBO.setUserId(userDetailDTO.getId());
            }

            personDetailBOS.add(personDetailBO);
            List<Long> posIds = personIdToPosIds.get(person.getId());
            List<MainPositionBO> mainPositionBOList = new ArrayList<>();
            List<MainPositionBO> positionList = new ArrayList<>();

            List<RelationDepartmentBO> relationDepartmentBOList = new ArrayList<>();
            List<RelationDepartmentBO> departmentList = new ArrayList<>();


            personDetailBO.setPositionFullPath(mainPositionBOList);
            personDetailBO.setDepartmentFullPath(relationDepartmentBOList);

            personDetailBO.setPosition(positionList);
            personDetailBO.setDepartment(departmentList);

            posIds.stream().forEach(posId -> {
                PositionAddPO positionAddPO = posIdToPO.get(posId);
                MainPositionBO mainPositionBO = new MainPositionBO();
                mainPositionBOList.add(mainPositionBO);
                positionList.add(mainPositionBO);
                mainPositionBO.setCompanyId(positionAddPO.getCompanyId());
                mainPositionBO.setId(positionAddPO.getId());
                mainPositionBO.setFullPath(OrgBaseUtils.splitCompanyFullPath(positionAddPO.getFullPath(), companyPO.getShortName(), companyPO.getFullPath()));
                if (posId.equals(person.getMainPosition())) {
                    mainPositionBO.setMainPosition(true);
                }
                DepartmentAddPO departmentAddPO = depIdToPO.get(positionAddPO.getDepId());
                RelationDepartmentBO relationDepartmentBO = new RelationDepartmentBO();
                relationDepartmentBOList.add(relationDepartmentBO);
                departmentList.add(relationDepartmentBO);
                relationDepartmentBO.setCompanyId(departmentAddPO.getCompanyId());
                relationDepartmentBO.setId(departmentAddPO.getId());
                relationDepartmentBO.setFullPath(departmentAddPO.getFullPath());
            });
        });
        return personDetailBOS;
    }

    @Override
    public PageResult<PersonDetailBO> queryDepartmentUsers1(Long companyId, Long departmentId, String keyword, Boolean onlyUser, Integer current, Integer pageSize) {
        keyword = ObjectUtils.isEmpty(keyword) ? "" : "%" + keyword + "%";
        String dbType = this.dataId.getDataId();
        List<Long> personIds = Lists.newArrayList();
        //查询部门下用户
        Integer total = 0;
        if (null != departmentId && !companyId.equals(departmentId)) {
            List<DepartmentPersonPO> departmentPersonPOS = personMapper.queryUserOfDept(departmentId, keyword, onlyUser, current, pageSize, dbType);
            if (!ObjectUtils.isEmpty(departmentPersonPOS)) {
                personIds = departmentPersonPOS.stream().map(DepartmentPersonPO::getPersonId).collect(Collectors.toList());
            }
            total = personMapper.totalUserOfDept(departmentId, keyword, onlyUser);
        } else {
            //查询公司下用户
            List<CompanyPersonPO> companyPersonPOS = personMapper.queryUserOfCompany(companyId, keyword, onlyUser, current, pageSize, dbType);
            if (!ObjectUtils.isEmpty(companyPersonPOS)) {
                personIds = companyPersonPOS.stream().map(CompanyPersonPO::getPersonId).collect(Collectors.toList());
            }
            total = personMapper.totalUserOfCompany(companyId, keyword, onlyUser);
        }

        List<PersonDetailBO> personDetailBOS = new ArrayList<>();
        if (ObjectUtils.isEmpty(personIds)) {
            return new PageResult<>(personDetailBOS, total, pageSize, current);
        }

        //人员信息
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
        personWrapper.in("id", personIds);
        List<PersonAddPO> personAddPOS = personMapper.selectList(personWrapper);
        //部门信息
        Map<Long, List<DepartmentAddPO>> departmentMap = new HashMap<>();
        if (!ObjectUtils.isEmpty(departmentId)) {
            QueryWrapper<DepartmentAddPO> departmentWrapper = new QueryWrapper<>();
            departmentWrapper.eq("id", departmentId);
            departmentWrapper.eq("valid", 1);
            DepartmentAddPO departmentAddPO = getOne(departmentWrapper);
            if (!ObjectUtils.isEmpty(departmentAddPO)) {
                personIds.forEach(personId -> departmentMap.put(personId, Collections.singletonList(departmentAddPO)));
            }
        } else {
            for (Long personId : personIds) {
                QueryWrapper<DepartmentPersonPO> departmentPersonWrapper = new QueryWrapper<>();
                departmentPersonWrapper.select("dept_id");
                departmentPersonWrapper.eq("person_id", personId);
                departmentPersonWrapper.eq("valid", 1);
                List<DepartmentPersonPO> departmentPersonPOS = departmentPersonMapper.selectList(departmentPersonWrapper);
                if (!ObjectUtils.isEmpty(departmentPersonPOS)) {
                    List<Long> deptIds = departmentPersonPOS.stream().map(DepartmentPersonPO::getDeptId).distinct().collect(Collectors.toList());
                    QueryWrapper<DepartmentAddPO> departmentWrapper = new QueryWrapper<>();
                    departmentWrapper.in("id", deptIds);
                    departmentWrapper.eq("valid", 1);
                    List<DepartmentAddPO> departmentAddPOS = list(departmentWrapper);
                    departmentMap.put(personId, departmentAddPOS);
                }
            }
        }

        //岗位信息
        QueryWrapper<PositionPersonPO> personPositionWrapper = new QueryWrapper<>();
        personPositionWrapper.in("person_id", personIds);
        personPositionWrapper.eq("valid", 1);
        List<PositionPersonPO> positionPersonPOS = positionPersonMapper.selectList(personPositionWrapper);
        Map<Long, List<Long>> personPositionIdMap = new HashMap<>();
        for (PositionPersonPO positionPersonPO : positionPersonPOS) {
            List<Long> positionIds = personPositionIdMap.get(positionPersonPO.getPersonId());
            if (null == positionIds) {
                List<Long> temp = Lists.newArrayList();
                temp.add(positionPersonPO.getPositionId());
                personPositionIdMap.put(positionPersonPO.getPersonId(), temp);
            } else {
                positionIds.add(positionPersonPO.getPositionId());
            }
        }
        //公司
        CompanyPO companyPO = companyMapper.selectById(companyId);

        //信息整合
        for (PersonAddPO personAddPO : personAddPOS) {
            //人员，用户
            PersonDetailBO personDetailBO = new PersonDetailBO();
            BeanUtils.copyProperties(personAddPO, personDetailBO);
            personDetailBOS.add(personDetailBO);

            //部门
            List<RelationDepartmentBO> department = Lists.newArrayList();
            List<DepartmentAddPO> departmentAddPOS = departmentMap.get(personAddPO.getId());
            if (!ObjectUtils.isEmpty(departmentAddPOS)) {
                for (DepartmentAddPO departmentAddPO : departmentAddPOS) {
                    RelationDepartmentBO relationDepartmentBO = new RelationDepartmentBO();
                    relationDepartmentBO.setCompanyId(departmentAddPO.getCompanyId());
                    relationDepartmentBO.setId(departmentAddPO.getId());
                    relationDepartmentBO.setFullPath(departmentAddPO.getFullPath());
                    department.add(relationDepartmentBO);
                }
            }
            personDetailBO.setDepartment(department);
            personDetailBO.setDepartmentFullPath(department);
            //岗位
            List<MainPositionBO> positionList = new ArrayList<>();
            personDetailBO.setPosition(positionList);
            personDetailBO.setPositionFullPath(positionList);
            List<Long> positionIds = personPositionIdMap.get(personAddPO.getId());
            if (!ObjectUtils.isEmpty(positionIds)) {
                QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<>();
                positionWrapper.in("id", positionIds);
                positionWrapper.eq("valid", 1);
                List<PositionAddPO> positionAddPOS = positionMapper.selectList(positionWrapper);
                for (PositionAddPO positionAddPO : positionAddPOS) {
                    MainPositionBO mainPositionBO = new MainPositionBO();
                    mainPositionBO.setCompanyId(positionAddPO.getCompanyId());
                    mainPositionBO.setId(positionAddPO.getId());
                    mainPositionBO.setFullPath(positionAddPO.getFullPath().substring(1));
                    if (positionAddPO.getId().equals(personAddPO.getMainPosition())) {
                        mainPositionBO.setMainPosition(true);
                    }
                    positionList.add(mainPositionBO);
                }
            }

        }
        return new PageResult<>(personDetailBOS, total, pageSize, current);
    }

    /**
     * 增加虚拟人员
     * @param companyId
     * @param tenantId
     * @return
     */
    @Override
    public Long addVirtualDept(Long companyId, String tenantId) {
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DepartmentAddPO::getCompanyId, companyId).eq(DepartmentAddPO::getValid, true).eq(DepartmentAddPO::getSysFlag, true);
        DepartmentAddPO virtualDept = getOne(queryWrapper);
        if (virtualDept != null) {
            return virtualDept.getId();
        }

        DepartmentAddPO departmentAddPO = new DepartmentAddPO();
        departmentAddPO.setType(Constants.DEPT_GENDERAL_CODE);
        departmentAddPO.setName("虚拟部门");
        departmentAddPO.setCode("default_department_" + companyId);
        departmentAddPO.setCompanyId(companyId);
        departmentAddPO.setSysFlag(true);
        addDepartment(departmentAddPO, null, tenantId);
        return departmentAddPO.getId();
    }

    /**
     * 根据公司id查询公司的信息,supplant
     * @param companyId
     * @return
     */
    @Override
    public JSONObject queryDepartmentDetailRefInfo(Long companyId) {
        QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<>();
        deptWrapper.lambda()
                .eq(DepartmentAddPO::getCompanyId, companyId)
                .eq(DepartmentAddPO::getValid, true)
                .isNull(DepartmentAddPO::getParentId);
        List<DepartmentAddPO> depts = list(deptWrapper);
        if (depts == null || depts.size() == 0) {
            return null;
        }
        JSONObject data = new JSONObject();
        JSONArray result = new JSONArray();
        data.put("result", result);
        depts.stream().forEach(dept -> {
            JSONObject json = new JSONObject();
            json.put("code", dept.getCode());
            json.put("name", dept.getName());
            json.put("id", dept.getId());
            json.put("leaf", dept.getLeaf());

        });

        return null;
    }

    @Override
    public PageResult<DepartmentAddPO> loadDepartments(Integer current, Integer pageSize, Long fromTime) {
        QueryWrapper<DepartmentAddPO> depWrapper = new QueryWrapper<>();

        if (fromTime != null) {
            depWrapper.and(item -> {
                item.gt("modify_time", new Date(fromTime)).or().gt("create_time", new Date(fromTime));
            });
        } else {
            depWrapper.eq("valid", true);
        }
        depWrapper.eq("sys_flag", 0);
        int count = count(depWrapper);
        if (count == 0) {
            return new PageResult<>(null, count, pageSize, current);
        }
        Page<DepartmentAddPO> pageInfo = new Page<DepartmentAddPO>(current, pageSize, count);
        page(pageInfo, depWrapper);
        PageResult<DepartmentAddPO> pageResult = new PageResult<>(pageInfo.getRecords(), count, pageSize, current);
        return pageResult;
    }

    @Override
    public PageResult<PersonResultBO> queryPersonsByDepartmentId(Long departmentId, Integer current, Integer pageSize) {
        QueryWrapper<DepartmentPersonPO> deptPersonWrapper = new QueryWrapper<>();
        deptPersonWrapper.lambda().eq(DepartmentPersonPO::getDeptId, departmentId).eq(DepartmentPersonPO::getValid, true);
        List<DepartmentPersonPO> depRelList = departmentPersonMapper.selectList(deptPersonWrapper);
        if (depRelList == null || depRelList.size() == 0) {
            return null;
        }
        List<Long> personIds = new ArrayList<>();
        depRelList.stream().forEach(deptRel -> {
            personIds.add(deptRel.getPersonId());
        });
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
        personWrapper.lambda().eq(PersonAddPO::getValid, true).in(PersonAddPO::getId, personIds);

        Integer count = personMapper.selectCount(personWrapper);
        if (count == null || count == 0) {
            return null;
        }
        Page<PersonAddPO> page = personMapper.selectPage(new Page<PersonAddPO>(current, pageSize, count), personWrapper);
        if (page == null || page.getRecords() == null || page.getRecords().size() == 0) {
            return null;
        }
        List<PersonResultBO> persons = new ArrayList<>();
        List<Long> pagePersonIds = new ArrayList<>();
        page.getRecords().stream().forEach(personPO -> {
            PersonResultBO personResultBO = new PersonResultBO();
            BeanUtils.copyProperties(personPO, personResultBO);
            persons.add(personResultBO);
            pagePersonIds.add(personPO.getId());
        });
        PageResult<PersonResultBO> pageResult = new PageResult<>(persons, count, pageSize, current);

        QueryWrapper<DepartmentPersonPO> depPersonWapper = new QueryWrapper<>();
        depPersonWapper.lambda().in(DepartmentPersonPO::getValid, true).in(DepartmentPersonPO::getPersonId, pagePersonIds);
        List<DepartmentPersonPO> depPersonList = departmentPersonMapper.selectList(depPersonWapper);
        if (depPersonList == null || depPersonList.size() == 0) {
            return pageResult;
        }
        List<Long> posIds = new ArrayList<>();
        List<Long> deptIds = new ArrayList<>();
        Map<Long, List<Long>> perToDept = new HashMap<>();
        Map<Long, List<Long>> perToPos = new HashMap<>();
        depPersonList.stream().forEach(depPerson -> {
            posIds.add(depPerson.getPositionId());
            deptIds.add(depPerson.getDeptId());
            if (perToDept.get(depPerson.getPersonId()) == null) {
                List<Long> depIds = new ArrayList<>();
                depIds.add(depPerson.getDeptId());
                perToDept.put(depPerson.getPersonId(), depIds);
            } else {
                perToDept.get(depPerson.getPersonId()).add(depPerson.getDeptId());
            }
            if (perToPos.get(depPerson.getPositionId()) == null) {
                List<Long> pospIds = new ArrayList<>();
                pospIds.add(depPerson.getPositionId());
                perToPos.put(depPerson.getPersonId(), pospIds);
            } else {
                perToPos.get(depPerson.getPersonId()).add(depPerson.getPositionId());
            }
        });
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();
        posWrapper.lambda().in(PositionAddPO::getId, posIds).eq(PositionAddPO::getValid, true);
        List<PositionAddPO> positions = positionMapper.selectList(posWrapper);
        if (positions == null || positions.size() == 0) {
            return pageResult;
        }
        QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<>();
        deptWrapper.lambda().in(DepartmentAddPO::getId, deptIds).eq(DepartmentAddPO::getValid, true);
        List<DepartmentAddPO> departments = list(deptWrapper);

        pageResult.getList().stream().forEach(personResultBO -> {
            List<Long> relPosIds = perToPos.get(personResultBO.getId());
            if (relPosIds != null && relPosIds.size() > 0) {
                List<PositionResultBO> positionResultBOList = personResultBO.getPositions();
                if (positionResultBOList == null) {
                    positionResultBOList = new ArrayList<>();
                    personResultBO.setPositions(positionResultBOList);
                }
                for (PositionAddPO positionAddPO : positions) {
                    if (relPosIds.contains(positionAddPO.getId())) {
                        PositionResultBO positionResultBO = new PositionResultBO();
                        BeanUtils.copyProperties(positionAddPO, positionResultBO);
                        positionResultBOList.add(positionResultBO);
                    }
                }
            }
            if (departments != null && departments.size() > 0) {
                List<Long> relDepIds = perToDept.get(personResultBO.getId());
                if (relDepIds != null && relDepIds.size() > 0) {
                    List<DepartmentResultBO> departmentResultBOList = personResultBO.getDepartments();
                    if (departmentResultBOList == null) {
                        departmentResultBOList = new ArrayList<>();
                        personResultBO.setDepartments(departmentResultBOList);
                    }
                    for (DepartmentAddPO departmentAddPO : departments) {
                        if (relDepIds.contains(departmentAddPO.getId())) {
                            DepartmentResultBO departmentResultBO = new DepartmentResultBO();
                            BeanUtils.copyProperties(departmentAddPO, departmentResultBO);
                            departmentResultBOList.add(departmentResultBO);
                        }
                    }
                }
            }
        });
        return pageResult;
    }

    @Override
    public PageResult<DepartmentDetailBO> queryDepartmentsPage(Integer current, Integer pageSize) {
        QueryWrapper<DepartmentAddPO> departmentWrapper = new QueryWrapper<>();
        departmentWrapper.lambda().eq(DepartmentAddPO::getValid, true).eq(DepartmentAddPO::getSysFlag, 0);
        Integer count = count(departmentWrapper);
        if (count == null || count == 0) {
            return null;
        }
        Page<DepartmentAddPO> page = page(new Page(current, pageSize, count), departmentWrapper);
        if (page == null || page.getRecords() == null || page.getRecords().size() == 0) {
            return null;
        }
        List<DepartmentDetailBO> boList = new ArrayList<>();
        page.getRecords().stream().forEach(departmentAddPO -> {
            DepartmentDetailBO departmentDetailBO = new DepartmentDetailBO();
            BeanUtils.copyProperties(departmentAddPO, departmentDetailBO);
            boList.add(departmentDetailBO);
        });

        return new PageResult<>(boList, count, pageSize, current);
    }

    @Override
    public DepartmentDetailBO queryCurrentLoginPersonDepartment(Long personId, Long companyId) {
        QueryWrapper<CompanyPersonPO> companyPersonWrapper = new QueryWrapper<>();
        companyPersonWrapper.eq("company_id", companyId).eq("person_id", personId).eq("valid", true);
        List<CompanyPersonPO> companyPersonPOList = companyPersonMapper.selectList(companyPersonWrapper);
        if (companyPersonPOList == null || companyPersonPOList.size() == 0) {
            return null;
        }
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
        personWrapper.eq("id", personId).eq("valid", true);
        PersonAddPO personAddPO = personMapper.selectOne(personWrapper);
        if (personAddPO == null) {
            return null;
        }

        Long positionId = companyPersonPOList.get(0).getPositionId();
        for (int i = 0; i < companyPersonPOList.size(); i++) {
            if (personAddPO.getMainPosition().equals(companyPersonPOList.get(i).getPositionId())) {
                positionId = personAddPO.getMainPosition();
                break;
            }
        }
        PositionAddPO positionAddPO = positionMapper.selectById(positionId);
        DepartmentAddPO departmentAddPO = getById(positionAddPO.getDepId());
        DepartmentDetailBO departmentDetailBO = new DepartmentDetailBO();
        BeanUtils.copyProperties(departmentAddPO, departmentDetailBO);
        return departmentDetailBO;
    }

    @Override
    public PageResult<DepartmentSynchronizationInfoBO> getDepartments(String modifyTime, Integer current, Integer pageSize) {
        String dbType = dataId.getDataId();
        Integer total = departmentMapper.getDepartmentsCount(modifyTime, dbType);
        if (total == null || total == 0) {
            return new PageResult<>(new ArrayList<>(), 0, pageSize, current);
        }


        List<DepartmentSynchronizationInfoPO> departmentSynchronizationInfoPOList = departmentMapper.getDepartments(modifyTime, current, pageSize, dbType);
        if (departmentSynchronizationInfoPOList.size() == 0) {
            return new PageResult<>(new ArrayList<>(), 0, pageSize, current);
        }
        List<Long> deptIds = new ArrayList<>();
        departmentSynchronizationInfoPOList.stream().forEach(dept -> {
            deptIds.add(dept.getId());
        });
        List<DepartmentSynchronizationManagerPO> managers = departmentMapper.getManagersByDeptIds(deptIds);
        Map<Long, List<ManagerForDepartmentSynchronizationInfoBO>> deptIdToManagers = new HashMap<>();
        if (managers != null) {
            managers.stream().forEach(manager -> {
                if (deptIdToManagers.get(manager.getDeptId()) == null) {
                    deptIdToManagers.put(manager.getDeptId(), new ArrayList<>());
                }
                List<ManagerForDepartmentSynchronizationInfoBO> managerForDepartmentSynchronizationInfoBOList = deptIdToManagers.get(manager.getDeptId());
                ManagerForDepartmentSynchronizationInfoBO managerForDepartmentSynchronizationInfoBO = new ManagerForDepartmentSynchronizationInfoBO();
                managerForDepartmentSynchronizationInfoBO.setCode(manager.getManagerCode());
                managerForDepartmentSynchronizationInfoBO.setName(manager.getManagerName());
                managerForDepartmentSynchronizationInfoBOList.add(managerForDepartmentSynchronizationInfoBO);
            });
        }
        List<DepartmentSynchronizationInfoBO> departmentSynchronizationInfoBOList = new ArrayList<>();

        Map<String, List<SystemCodeResultDTO>> entityCodeMap = new HashMap<>();
        for (DepartmentSynchronizationInfoPO departmentSynchronizationInfoPO : departmentSynchronizationInfoPOList) {
            // 时间格式转换
            if (null != departmentSynchronizationInfoPO.getModifyTime()) {
                String formatTime = responseFormatTime(departmentSynchronizationInfoPO.getModifyTime());
                departmentSynchronizationInfoPO.setModifyTime(formatTime);
            }
            DepartmentSynchronizationInfoBO departmentSynchronizationInfoBO = new DepartmentSynchronizationInfoBO();
            BeanUtils.copyProperties(departmentSynchronizationInfoPO, departmentSynchronizationInfoBO);
            departmentSynchronizationInfoBO.setDeptType(baseServiceService.findSystemCode(departmentSynchronizationInfoPO.getDeptType(), entityCodeMap));
            CompanyForPositionSynchronizationInfoBO companyForPositionSynchronizationInfoBO = new CompanyForPositionSynchronizationInfoBO();
            companyForPositionSynchronizationInfoBO.setCode(departmentSynchronizationInfoPO.getCompanyCode());
            companyForPositionSynchronizationInfoBO.setFullName(departmentSynchronizationInfoPO.getCompanyFullName());
            companyForPositionSynchronizationInfoBO.setShortName(departmentSynchronizationInfoPO.getCompanyShortName());
            departmentSynchronizationInfoBO.setCompany(companyForPositionSynchronizationInfoBO);

            List<ManagerForDepartmentSynchronizationInfoBO> deptManagers = deptIdToManagers.get(departmentSynchronizationInfoPO.getId());
            if (deptManagers == null) {
                deptManagers = new ArrayList<>();
            }
            departmentSynchronizationInfoBO.setManagers(deptManagers);
            departmentSynchronizationInfoBOList.add(departmentSynchronizationInfoBO);
        }


        return new PageResult<>(departmentSynchronizationInfoBOList, total, pageSize, current);
    }

    @Override
    public PageResult<DepartmentBaseInfoBO> getDepartmentsByCompanyCode(String companyCode, Integer current, Integer pageSize) {
        QueryWrapper<CompanyPO> companyPOQueryWrapper = new QueryWrapper<>();
        companyPOQueryWrapper.lambda().eq(CompanyPO::getValid, true).eq(CompanyPO::getCode, companyCode);
        CompanyPO companyPO = companyMapper.selectOne(companyPOQueryWrapper);
        if (companyPO == null) {
            throw new BizHttpStatusException(OrganizationErrorEnum.COMPANY_PARAM_ID_NECESSARY, 400);
        }
        QueryWrapper<DepartmentAddPO> deptCountWrapper = new QueryWrapper<>();
        deptCountWrapper.lambda().eq(DepartmentAddPO::getValid, true).eq(DepartmentAddPO::getCompanyId, companyPO.getId());
        Integer total = count(deptCountWrapper);
        if (total == null || total == 0) {
            new PageResult<>(new ArrayList<>(), 0, pageSize, current);
        }
        String dbType = dataId.getDataId();
        List<DepartmentBaseInfoPO> departmentBaseInfoPOList = departmentMapper.getDepartmentsByCompanyId(companyPO.getId(), current, pageSize, dbType);
        if (departmentBaseInfoPOList.size() == 0) {
            new PageResult<>(new ArrayList<>(), 0, pageSize, current);
        }
        List<DepartmentBaseInfoBO> departmentBaseInfoBOList = new ArrayList<>();

        Map<String, List<SystemCodeResultDTO>> entityCodeMap = new HashMap<>();
        for (DepartmentBaseInfoPO departmentBaseInfoPO : departmentBaseInfoPOList) {
            DepartmentBaseInfoBO departmentBaseInfoBO = new DepartmentBaseInfoBO();
            BeanUtils.copyProperties(departmentBaseInfoPO, departmentBaseInfoBO);
            departmentBaseInfoBO.setDeptType(baseServiceService.findSystemCode(departmentBaseInfoPO.getDeptType(), entityCodeMap));
            CompanyBaseInfoBO companyForDepartmentBaseInfoBO = new CompanyBaseInfoBO();
            companyForDepartmentBaseInfoBO.setCode(departmentBaseInfoPO.getCompanyCode());
            companyForDepartmentBaseInfoBO.setFullName(departmentBaseInfoPO.getCompanyFullName());
            companyForDepartmentBaseInfoBO.setShortName(departmentBaseInfoPO.getCompanyShortName());
            departmentBaseInfoBO.setCompany(companyForDepartmentBaseInfoBO);

            departmentBaseInfoBOList.add(departmentBaseInfoBO);
        }

        return new PageResult<>(departmentBaseInfoBOList, total, pageSize, current);
    }

    @Override
    public Result<DepartmentDetailInfoBO> getDepartmentByCode(String departmentCode) {
        DepartmentDetailInfoBO departmentDetailInfoBO = new DepartmentDetailInfoBO();
        DepartmentSynchronizationInfoPO departmentPO = departmentMapper.getDepartmentByCode(departmentCode);
        if (null == departmentPO) {
            throw new BizHttpStatusException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS, 400);
        }
        // 时间格式转换
        if (null != departmentPO.getModifyTime()) {
            String formatTime = responseFormatTime(departmentPO.getModifyTime());
            departmentPO.setModifyTime(formatTime);
        }
        BeanUtils.copyProperties(departmentPO, departmentDetailInfoBO);

        // 获取岗位
        List<PositionDeptBasePO> positionDeptBasePOList = positionMapper.getPositionsByDeptId(departmentPO.getId());
        ArrayList<DepartmentForPositionBaseInfoBO> deptPositionBaseInfoBOList = new ArrayList<>();
        for (PositionDeptBasePO positionDeptBasePO : positionDeptBasePOList) {
            DepartmentForPositionBaseInfoBO deptPositionBaseInfoBO = new DepartmentForPositionBaseInfoBO();
            deptPositionBaseInfoBO.setCode(positionDeptBasePO.getCode());
            deptPositionBaseInfoBO.setName(positionDeptBasePO.getName());
            deptPositionBaseInfoBOList.add(deptPositionBaseInfoBO);
        }
        departmentDetailInfoBO.setPositons(deptPositionBaseInfoBOList);

        List<DepartmentSynchronizationManagerPO> managers = departmentMapper.getManagerByDeptId(departmentPO.getId());
        Map<Long, List<ManagerForDepartmentSynchronizationInfoBO>> deptIdToManagers = new HashMap<>();
        if (null !=managers ) {
            managers.stream().forEach(manager -> {
                if (deptIdToManagers.get(manager.getDeptId()) == null) {
                    deptIdToManagers.put(manager.getDeptId(), new ArrayList<>());
                }
                List<ManagerForDepartmentSynchronizationInfoBO> managerForDepartmentSynchronizationInfoBOList = deptIdToManagers.get(manager.getDeptId());
                ManagerForDepartmentSynchronizationInfoBO managerForDepartmentSynchronizationInfoBO = new ManagerForDepartmentSynchronizationInfoBO();
                managerForDepartmentSynchronizationInfoBO.setCode(manager.getManagerCode());
                managerForDepartmentSynchronizationInfoBO.setName(manager.getManagerName());
                managerForDepartmentSynchronizationInfoBOList.add(managerForDepartmentSynchronizationInfoBO);
            });
        }
        Map<String, List<SystemCodeResultDTO>> entityCodeMap = new HashMap<>();

        departmentDetailInfoBO.setDeptType(baseServiceService.findSystemCode(departmentPO.getDeptType(), entityCodeMap));
        CompanyForPositionSynchronizationInfoBO companyForPositionSynchronizationInfoBO = new CompanyForPositionSynchronizationInfoBO();
        companyForPositionSynchronizationInfoBO.setCode(departmentPO.getCompanyCode());
        companyForPositionSynchronizationInfoBO.setFullName(departmentPO.getCompanyFullName());
        companyForPositionSynchronizationInfoBO.setShortName(departmentPO.getCompanyShortName());
        departmentDetailInfoBO.setCompany(companyForPositionSynchronizationInfoBO);

        List<ManagerForDepartmentSynchronizationInfoBO> deptManagers = deptIdToManagers.get(departmentPO.getId());
        if (deptManagers == null) {
            deptManagers = new ArrayList<>();
        }
        departmentDetailInfoBO.setManagers(deptManagers);
        BeanUtils.copyProperties(departmentPO, departmentDetailInfoBO);

        return new Result<>(departmentDetailInfoBO);
    }

    @Override
    public List<DepartmentFlowSimpleBO> queryDepartmentIdByCodes(List<String> codes) {
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(DepartmentAddPO::getCode, codes).eq(DepartmentAddPO::getValid, true);
        List<DepartmentAddPO> departmentAddPOList = list(queryWrapper);
        if (departmentAddPOList == null) {
            return null;
        }
        List<DepartmentFlowSimpleBO> departmentFlowSimpleBOList = new ArrayList<>();
        departmentAddPOList.stream().forEach(departmentAddPO -> {
            DepartmentFlowSimpleBO departmentFlowSimpleBO = new DepartmentFlowSimpleBO();
            BeanUtils.copyProperties(departmentAddPO, departmentFlowSimpleBO);
            departmentFlowSimpleBOList.add(departmentFlowSimpleBO);
        });
        return departmentFlowSimpleBOList;
    }
    @Override
    public DepartmentAddPO getDepartmentAddPoByCode(String code) {
        QueryWrapper<DepartmentAddPO> departQueryWrapper = new QueryWrapper<>();
        departQueryWrapper.eq("code", code);
        departQueryWrapper.eq("valid", true);
        DepartmentAddPO departmentAddPO = departmentMapper.selectOne(departQueryWrapper);
        if (departmentAddPO == null) {
            return null;
        }
        return departmentAddPO;
    }

    @Override
    public PageResult<DepartmentDetailInfoBO> querySubDepartmentInfoByParentId(Long id, Boolean all, Long cid, Integer current, Integer pageSize) {
        QueryWrapper<DepartmentAddPO> subPosWrapper = new QueryWrapper<>();
        subPosWrapper.eq("valid", 1);
        subPosWrapper.eq("sys_flag", 0);
        if (id != null) {
            DepartmentAddPO departmentAddPO = getById(id);
            if (departmentAddPO == null) {
                return null;
            }
            if (all != null && all) {
                subPosWrapper.likeRight("full_path", departmentAddPO.getFullPath() + "/");
            } else {
                subPosWrapper.eq("parent_id", departmentAddPO.getId());
            }
        } else if (cid != null) {
            if (all != null && all) {
                subPosWrapper.eq("company_id", cid);
            } else {
                subPosWrapper.eq("company_id", cid);
                subPosWrapper.isNull("parent_id");
            }
        } else {
            return null;
        }
        // todo 排序
        subPosWrapper.orderByAsc("lay_no", "sort");
        //List<DepartmentAddPO> list = list(subPosWrapper);
        // 分页
        Page<DepartmentAddPO> departmentPage = new Page<>(current, pageSize);
        Page<DepartmentAddPO> departmentResPage = departmentMapper.selectPage(departmentPage, subPosWrapper);
        List<DepartmentAddPO> list = departmentResPage.getRecords();
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        List<DepartmentDetailInfoBO> departmentDetailInfoBOS = new ArrayList<>();
        list.stream().forEach(dept -> {
            DepartmentDetailInfoBO departmentDetailInfoBO = new DepartmentDetailInfoBO();
            BeanUtils.copyProperties(dept, departmentDetailInfoBO);
            // 父级编码
            departmentDetailInfoBO.setParentCode(getDepartmentCodeById(dept.getParentId()));

            // 对象数据转换
            // 获取岗位
            List<PositionDeptBasePO> positionDeptBasePOList = positionMapper.getPositionsByDeptId(dept.getId());
            ArrayList<DepartmentForPositionBaseInfoBO> deptPositionBaseInfoBOList = new ArrayList<>();
            for (PositionDeptBasePO positionDeptBasePO : positionDeptBasePOList) {
                DepartmentForPositionBaseInfoBO deptPositionBaseInfoBO = new DepartmentForPositionBaseInfoBO();
                deptPositionBaseInfoBO.setCode(positionDeptBasePO.getCode());
                deptPositionBaseInfoBO.setName(positionDeptBasePO.getName());
                deptPositionBaseInfoBOList.add(deptPositionBaseInfoBO);
            }
            departmentDetailInfoBO.setPositons(deptPositionBaseInfoBOList);

            // 管理者
            List<DepartmentSynchronizationManagerPO> managers = departmentMapper.getManagerByDeptId(dept.getId());
            Map<Long, List<ManagerForDepartmentSynchronizationInfoBO>> deptIdToManagers = new HashMap<>();
            if (null != managers) {
                managers.stream().forEach(manager -> {
                    if (deptIdToManagers.get(manager.getDeptId()) == null) {
                        deptIdToManagers.put(manager.getDeptId(), new ArrayList<>());
                    }
                    List<ManagerForDepartmentSynchronizationInfoBO> managerForDepartmentSynchronizationInfoBOList = deptIdToManagers.get(manager.getDeptId());
                    ManagerForDepartmentSynchronizationInfoBO managerForDepartmentSynchronizationInfoBO = new ManagerForDepartmentSynchronizationInfoBO();
                    managerForDepartmentSynchronizationInfoBO.setCode(manager.getManagerCode());
                    managerForDepartmentSynchronizationInfoBO.setName(manager.getManagerName());
                    managerForDepartmentSynchronizationInfoBOList.add(managerForDepartmentSynchronizationInfoBO);
                });
            }
            Map<String, List<SystemCodeResultDTO>> entityCodeMap = new HashMap<>();

            // 部门类型
            departmentDetailInfoBO.setDeptType(baseServiceService.findSystemCode(dept.getType(), entityCodeMap));
            CompanyForPositionSynchronizationInfoBO companyForPositionSynchronizationInfoBO = new CompanyForPositionSynchronizationInfoBO();

            // 公司
            Long companyId = dept.getCompanyId();
            CompanyPO companyPO = companyMapper.selectById(companyId);
            companyForPositionSynchronizationInfoBO.setCode(companyPO.getCode());
            companyForPositionSynchronizationInfoBO.setFullName(companyPO.getFullName());
            companyForPositionSynchronizationInfoBO.setShortName(companyPO.getShortName());
            departmentDetailInfoBO.setCompany(companyForPositionSynchronizationInfoBO);

            List<ManagerForDepartmentSynchronizationInfoBO> deptManagers = deptIdToManagers.get(dept.getId());
            if (deptManagers == null) {
                deptManagers = new ArrayList<>();
            }
            departmentDetailInfoBO.setManagers(deptManagers);

            departmentDetailInfoBOS.add(departmentDetailInfoBO);
        });
        PageResult<DepartmentDetailInfoBO> res = new PageResult<>(departmentDetailInfoBOS, departmentResPage.getTotal(), pageSize, current);
        return res;
    }

    @Override
    public String getDepartmentCodeById(Long deptId) {
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("code");
        queryWrapper.eq("id", deptId);
        DepartmentAddPO departmentAddPO = getOne(queryWrapper);
        if (!ObjectUtils.isEmpty(departmentAddPO)) {
            return departmentAddPO.getCode();
        }
        return "";
    }

    @Override
    public List<DepartmentAddPO> listDepartments() {
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
        queryWrapper.eq("valid", 1);
        return list(queryWrapper);
    }

    /**
     * 不发kafka消息的部门新增
     * @param departmentAddPo
     * @param managerIds
     * @param tenantId
     */
    @Transactional(rollbackFor = Exception.class)
    public void addDepartmentWithoutKafka(DepartmentAddPO departmentAddPo, List<Long> managerIds, String tenantId) {

        Long uid = IDGenerator.newInstance().generate().longValue();
        String oldId = "Department_" + uid;
        departmentAddPo.setId(uid);
        departmentAddPo.setOldId(oldId);

        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
        queryWrapper.eq("code", departmentAddPo.getCode());
        queryWrapper.eq("valid", 1);
        int count = count(queryWrapper);
        //指定编码的部门已经存在
        if (count > 0) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_THIS_CODE_EXISTS);
        }
        QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
        comWrapper.eq("id", departmentAddPo.getCompanyId());
        comWrapper.eq("valid", 1);
        Integer comCount = companyMapper.selectCount(comWrapper);
        //指定id的公司不存在
        if (comCount == null || comCount == 0) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_COMPANYID_NOT_EXISTS);
        }
        //清除条件，为判断上级节点做准备
        queryWrapper.clear();
        //parentId不为空，需要判断该id的上级节点是否存在
        if (departmentAddPo.getParentId() != null) {
            queryWrapper.eq("id", departmentAddPo.getParentId());
            queryWrapper.eq("valid", 1);
            int parentCount = count(queryWrapper);
            if (parentCount == 0) {
                throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_PARENTID_NOT_EXISTS);
            }
            DepartmentAddPO parentPo = getOne(queryWrapper);
            departmentAddPo.setFullPath(parentPo.getFullPath() + "/" + departmentAddPo.getName());
            departmentAddPo.setLayRec(parentPo.getLayRec() + "-" + departmentAddPo.getId());
            departmentAddPo.setLayNo(parentPo.getLayNo() + 1);
            parentPo.setLeaf(false);
            updateById(parentPo);
            //清除条件，为判断名称做准备
            queryWrapper.clear();

            queryWrapper.eq("parent_id", departmentAddPo.getParentId());
        } else {
            CompanyPO comPo = companyMapper.selectOne(comWrapper);
            departmentAddPo.setFullPath(comPo.getFullPath() + "/" + departmentAddPo.getName());
            departmentAddPo.setLayRec("" + departmentAddPo.getId());
            departmentAddPo.setLayNo(1);
            queryWrapper.isNull("parent_id");
        }
        queryWrapper.eq("valid", 1);
        queryWrapper.eq("company_id", departmentAddPo.getCompanyId());
        //同级部门数量
        int brotherCount = count(queryWrapper);

        queryWrapper.eq("name", departmentAddPo.getName());
        //判断同父节点下的部门的名称是否重复
        int nameCount = count(queryWrapper);
        if (nameCount > 0) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_NAME_EXISTS);
        }
        departmentAddPo.setSort(Double.valueOf(FISRT_SORT + brotherCount * 1000));
        departmentAddPo.setLeaf(true);
        save(departmentAddPo);
        orgMnecodeService.addOrgMnecode(uid, departmentAddPo.getName(), Constants.DEPARTMENT);
        if (managerIds != null) {
            organizationManagerService.addManager(managerIds, departmentAddPo.getId(), Constants.DEPARTMENT);
        }
    }
    /**
     * 不发kafka消息的修改部门
     * @param departmentAddPo
     * @param managerIds
     * @param tenantId
     */
    @Transactional(rollbackFor = Exception.class)
    private void updateDepartmentWithoutKafka(DepartmentAddPO departmentAddPo, List<Long> managerIds, String tenantId) {
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
        queryWrapper.eq("id", departmentAddPo.getId());
        queryWrapper.eq("valid", 1);
        int count = count(queryWrapper);
        if (count == 0) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
        }
        DepartmentAddPO curDep = getOne(queryWrapper);
        departmentAddPo.setParentId(curDep.getParentId());
        //修改了name则需要修改所有子部门的full_path
        if (departmentAddPo != null && departmentAddPo.getName() != null && !departmentAddPo.getName().equals(curDep.getName())) {
            updateFullPath(curDep, departmentAddPo);
        }
        updateById(departmentAddPo);
        orgMnecodeService.deleteOrgMnecodeByOrgId(curDep.getId(), Constants.DEPARTMENT);
        orgMnecodeService.addOrgMnecode(curDep.getId(), departmentAddPo.getName(), Constants.DEPARTMENT);
        organizationManagerService.addManager(managerIds, departmentAddPo.getId(), Constants.DEPARTMENT);
    }

    /**
     * 不发消息的移动部门
     * @param departmentLocationPo
     * @param tenantId
     */
    @Transactional(rollbackFor = Exception.class)
    private DepartmentAddPO updateDepLocationWithoutkafka(DepartmentLocationBO departmentLocationPo, String tenantId) {

        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
        queryWrapper.eq("id", departmentLocationPo.getId());
        queryWrapper.eq("valid", 1);
        int count = count(queryWrapper);
        if (count == 0) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
        }
        //当前部门
        DepartmentAddPO currentPo = getOne(queryWrapper);

        //前续部门
        DepartmentAddPO upPo = null;

        //父级部门
        DepartmentAddPO parentPo = null;

        if (departmentLocationPo.getUpId() != null) {
            queryWrapper.clear();
            queryWrapper.eq("id", departmentLocationPo.getUpId());
            queryWrapper.eq("valid", 1);
            count = count(queryWrapper);
            if (count == 0) {
                throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
            }
            upPo = getOne(queryWrapper);

            if (!upPo.getCompanyId().equals(currentPo.getCompanyId())) {
                //公司不同
                throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_LOCATION_PARAM_ERROR);
            }
        }

        if (departmentLocationPo.getParentId() != null) {
            queryWrapper.clear();
            queryWrapper.eq("id", departmentLocationPo.getParentId());
            queryWrapper.eq("valid", 1);
            count = count(queryWrapper);
            if (count == 0) {
                throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
            }
            parentPo = getOne(queryWrapper);

            if (currentPo.getId().equals(parentPo.getId())) {
                //上级节点是自己
                throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_LOCATION_PARAM_ERROR);
            }
            if (parentPo.getFullPath().startsWith(currentPo.getFullPath())) {
                //上级节点是当前节点的孩子节点
                throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_LOCATION_PARAM_ERROR);
            }
            if (!parentPo.getCompanyId().equals(currentPo.getCompanyId())) {
                //公司不同
                throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_LOCATION_PARAM_ERROR);
            }
        }

        //前续部门和上级部门参数不匹配
        if ((upPo != null && parentPo != null && !parentPo.getId().equals(upPo.getParentId())) || (upPo != null && parentPo == null && upPo.getParentId() != null)) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_LOCATION_PARAM_ERROR);
        }

        QueryWrapper<DepartmentAddPO> sonDepWrapper = new QueryWrapper<DepartmentAddPO>();

        //parentId没变只需要修改sort排序
        if ((parentPo == null && currentPo.getParentId() == null) || (parentPo != null && parentPo.getId().equals(currentPo.getParentId()))) {
            if (currentPo.getParentId() == null) {
                sonDepWrapper.isNull("parent_id");
            } else {
                sonDepWrapper.eq("parent_id", currentPo.getParentId());
            }
            sonDepWrapper.eq("company_id", currentPo.getCompanyId());
        } else {
            //层级变化了,需要校验name的重复情况
            //当前layNo
            Integer curLayNo = currentPo.getLayNo();

            Integer newLayNo = 1;

            //当前fullPath
            String curFullPath = currentPo.getFullPath();
            String newFullPath = "";

            //当前id的全路径
            String curLayRec = currentPo.getLayRec();
            String newLayRec = "";

            if (currentPo.getParentId() != null) {
                sonDepWrapper.eq("parent_id", currentPo.getParentId());
            } else {
                sonDepWrapper.isNull("parent_id");
            }

            //parentId有变化时
            if (parentPo == null) {
                QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
                comWrapper.eq("id", currentPo.getCompanyId());
                CompanyPO comPo = companyMapper.selectOne(comWrapper);
                newFullPath = comPo.getFullPath() + "/" + currentPo.getName();
                newLayRec = "" + currentPo.getId();

                //判断现在的parent节点，移动后，是不是叶子节点，如果是则设置
                if (currentPo.getParentId() != null) {
                    //校验新的parent下name是否重复
                    QueryWrapper<DepartmentAddPO> checkNameDupWrapper = new QueryWrapper<>();
                    checkNameDupWrapper.isNull("parent_id");
                    checkNameDupWrapper.eq("valid", 1);
                    checkNameDupWrapper.eq("name", currentPo.getName());
                    int nameCount = count(checkNameDupWrapper);
                    if (nameCount > 0) {
                        throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_NAME_EXISTS);
                    }

                    QueryWrapper<DepartmentAddPO> parentSonDepWrapper = new QueryWrapper<DepartmentAddPO>();
                    parentSonDepWrapper.eq("parent_id", currentPo.getParentId());
                    parentSonDepWrapper.eq("valid", 1);
                    int parentCount = count(parentSonDepWrapper);

                    if (parentCount == 1) {
                        DepartmentAddPO parentDep = getById(currentPo.getParentId());
                        parentDep.setLeaf(true);
                        updateById(parentDep);
                    }
                }
                currentPo.setParentId(null);
            } else {
                //校验新的parent下name是否重复
                QueryWrapper<DepartmentAddPO> checkNameDupWrapper = new QueryWrapper<>();
                checkNameDupWrapper.eq("parent_id", parentPo.getId());
                checkNameDupWrapper.eq("valid", 1);
                checkNameDupWrapper.eq("name", currentPo.getName());
                int nameCount = count(checkNameDupWrapper);
                if (nameCount > 0) {
                    throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_NAME_EXISTS);
                }

                if (currentPo.getParentId() != null) {
                    QueryWrapper<DepartmentAddPO> parentSonDepWrapper = new QueryWrapper<DepartmentAddPO>();
                    parentSonDepWrapper.eq("parent_id", currentPo.getParentId());
                    parentSonDepWrapper.eq("valid", 1);
                    int parentCount = count(parentSonDepWrapper);
                    if (currentPo.getParentId() != null && parentCount == 1) {
                        DepartmentAddPO parentDep = getById(currentPo.getParentId());
                        parentDep.setLeaf(true);
                        updateById(parentDep);
                    }
                }
                /*QueryWrapper<DepartmentAddPO> parentSonDepWrapper = new QueryWrapper<DepartmentAddPO>();
                if (currentPo.getParentId() == null) {
                    parentSonDepWrapper.isNull("parent_id");
                } else {
                    parentSonDepWrapper.eq("parent_id", currentPo.getParentId());
                }

                parentSonDepWrapper.eq("valid", 1);
                int parentCount = count(parentSonDepWrapper);
                if (currentPo.getParentId() != null && parentCount == 1) {
                    DepartmentAddPO parentDep = getById(currentPo.getParentId());
                    parentDep.setLeaf(true);
                    updateById(parentDep);
                }*/

                parentPo.setLeaf(false);
                updateById(parentPo);

                currentPo.setParentId(parentPo.getId());
                newLayNo = parentPo.getLayNo() + 1;
                newFullPath = parentPo.getFullPath() + "/" + currentPo.getName();
                newLayRec = parentPo.getLayRec() + "-" + currentPo.getId();
            }
            sonDepWrapper.eq("company_id", currentPo.getCompanyId());
            sonDepWrapper.eq("valid", 1);
            Integer diffLayNo = newLayNo - curLayNo;
            String prefixFullPath = newFullPath;
            String prefixLayRec = newLayRec;
            QueryWrapper<DepartmentAddPO> deepWrapper = new QueryWrapper<>();
            deepWrapper.likeRight("full_path", curFullPath + "/");
            deepWrapper.eq("valid", 1);

            List<DepartmentAddPO> deepDeps = list(deepWrapper);
            if (deepDeps != null && deepDeps.size() > 0) {
                deepDeps.stream().forEach(item -> {
                    item.setLayNo(item.getLayNo() + diffLayNo);
                    item.setFullPath(prefixFullPath + item.getFullPath().substring(curFullPath.length(), item.getFullPath().length()));
                    item.setLayRec(prefixLayRec + item.getLayRec().substring(curLayRec.length(), item.getLayRec().length()));
                });
                updateBatchById(deepDeps);
            }

            currentPo.setFullPath(newFullPath);
            currentPo.setLayRec(newLayRec);
            currentPo.setLayNo(newLayNo);
        }
        //放在最上面
        if (upPo == null) {
            Integer sonCount = count(sonDepWrapper);
            sonDepWrapper.orderByDesc("sort");
            Page<DepartmentAddPO> pageInfo = new Page<DepartmentAddPO>(1, 1, sonCount);
            page(pageInfo, sonDepWrapper);
            List<DepartmentAddPO> firstBroDep = pageInfo.getRecords();
            if (firstBroDep == null || firstBroDep.size() == 0) {
                currentPo.setSort(FISRT_SORT);
            } else {
                currentPo.setSort(firstBroDep.get(0).getSort() + 1000);
            }
        } else {
            //放在upPO部门下面
            sonDepWrapper.lt("sort", upPo.getSort());

            Integer sonCount = count(sonDepWrapper);
            sonDepWrapper.orderByDesc("sort");
            Page<DepartmentAddPO> pageInfo = new Page<DepartmentAddPO>(1, 1, sonCount);
            page(pageInfo, sonDepWrapper);
            List<DepartmentAddPO> firstBroDep = pageInfo.getRecords();
            if (firstBroDep == null || firstBroDep.size() == 0) {
                currentPo.setSort(upPo.getSort() / 2);
            } else {
                currentPo.setSort((upPo.getSort() + firstBroDep.get(0).getSort()) / 2);
            }
        }
        updateById(currentPo);

        return currentPo;
    }
    /**
     * 新增部门发送消息
     * @param departmentAddPOList
     */
    public void publishCreateOrUpdateDeptMessage(List<DepartmentAddPO> departmentAddPOList, Map<Long, List<Long>> deptIdToManagerIds, String tenantId, String event) {

        if (departmentAddPOList == null || departmentAddPOList.size() == 0) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        List<DepartmentMessageBO> messageBody = new ArrayList<>();

        departmentAddPOList.stream().forEach(departmentAddPo -> {
            DepartmentAddPO parentDept = null;
            DepartmentAddPO currentDept = getById(departmentAddPo.getId());
            QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
            comWrapper.eq("id", currentDept.getCompanyId());
            comWrapper.eq("valid", 1);
            CompanyPO comPo = companyMapper.selectOne(comWrapper);

            DepartmentMessageBO departmentMessageBO = new DepartmentMessageBO();
            departmentMessageBO.setRowVersion(0L);
            departmentMessageBO.setId(currentDept.getId());
            if (currentDept.getParentId() != null) {
                parentDept = this.getById(currentDept.getParentId());
                departmentMessageBO.setParentId(parentDept.getId());
                departmentMessageBO.setParentCode(parentDept.getCode());
            }
            departmentMessageBO.setCode(currentDept.getCode());
            departmentMessageBO.setName(currentDept.getName());
            SystemCodeBO deptType = new SystemCodeBO();
            List<SystemCodeResultDTO> depTypeList = organizationAdapter.querySystemCodesByEntityCode(Constants.DEPARTMENT_TYPE_ENTITYCODE);
            if (depTypeList != null && depTypeList.size() > 0) {
                for (SystemCodeResultDTO depType : depTypeList) {
                    if (depType.equals(depType.getEntityCode() + "/" + depType.getCode())) {
                        deptType.setCode(depType.getCode());
                        deptType.setName(depType.getDisplayName());
                    }
                }
            }
            departmentMessageBO.setDeptType(deptType);
            departmentMessageBO.setDescription(currentDept.getDescription());
            departmentMessageBO.setFullPath(currentDept.getFullPath());
            departmentMessageBO.setLayNo(currentDept.getLayNo());
            departmentMessageBO.setSort(currentDept.getSort());
            departmentMessageBO.setModifyTime(currentDept.getModifyTime());
            CompanySimpleMessageBO companySimpleMessageBO = new CompanySimpleMessageBO();
            companySimpleMessageBO.setId(comPo.getId());
            companySimpleMessageBO.setCode(comPo.getCode());
            companySimpleMessageBO.setFullName(comPo.getFullName());
            companySimpleMessageBO.setShortName(comPo.getShortName());
            departmentMessageBO.setCompany(companySimpleMessageBO);
            departmentMessageBO.setManagers(new ArrayList<>());
            List<Long> managerIds = deptIdToManagerIds.get(currentDept.getId());
            if (managerIds != null && managerIds.size() > 0) {
                QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
                personWrapper.lambda().in(PersonAddPO::getId, managerIds).eq(PersonAddPO::getValid, true);
                List<PersonAddPO> personList = personMapper.selectList(personWrapper);
                if (personList != null && personList.size() > 0) {
                    List<ManagerMessageBO> managerMessageBOList = departmentMessageBO.getManagers();
                    personList.stream().forEach(personAddPO -> {
                        ManagerMessageBO messageBO = new ManagerMessageBO();
                        BeanUtils.copyProperties(personAddPO, messageBO);
                        managerMessageBOList.add(messageBO);
                    });
                }
            }

            messageBody.add(departmentMessageBO);
        });

        Map<String, Object> header = new HashMap<>();
        header.put("encode", "json");
        header.put("event", event);

        OrganizationMessage.Builder<DepartmentMessageBO> messageBuilder = new OrganizationMessage.Builder<>();
        Message message = messageBuilder.setSender("organization")
                .setTenantId(tenantId)
                .setCreateTime(sdf.format(new Date()))
                .setTopic(DEPARTMENT_TOPIC)
                .setHeader(header)
                .setBody(messageBody)
                .build();
        organizationMessage.publishMessage(message);
    }

    /**
     * 部门删除发消息
     * @param departmentAddPOList
     * @param tenantId
     */
    private void publishDeleteDepartmentMessage(List<DepartmentAddPO> departmentAddPOList, String tenantId) {


        List<DepartmentDeleteMessageBO> messageBody = new ArrayList<>();
        departmentAddPOList.stream().forEach(departmentAddPO -> {
            DepartmentDeleteMessageBO departmentDeleteMessageBO = new DepartmentDeleteMessageBO();
            BeanUtils.copyProperties(departmentAddPO, departmentDeleteMessageBO);
            departmentDeleteMessageBO.setRowVersion(0L);
            messageBody.add(departmentDeleteMessageBO);
        });
        Map<String, Object> header = new HashMap<>();
        header.put("encode", "json");
        header.put("event", "DELETE");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        OrganizationMessage.Builder<DepartmentDeleteMessageBO> messageBuilder = new OrganizationMessage.Builder<>();
        Message message = messageBuilder.setSender("organization")
                .setTenantId(tenantId)
                .setCreateTime(sdf.format(new Date()))
                .setTopic(DEPARTMENT_TOPIC)
                .setHeader(header)
                .setBody(messageBody)
                .build();
        organizationMessage.publishMessage(message);
    }

    /**
     * 查询部门关联的人员
     * @param companyId
     * @param departmentId
     * @param keyword
     * @param current
     * @param pageSize
     * @param conditionQuery
     * @param includeUser
     * @return
     */
    @Override
    public PageResult<PersonDetailBO> queryDepartmentPersonsBetter(Long companyId, Long departmentId, String keyword, Integer current, Integer pageSize, PersonDetailBO conditionQuery, Boolean includeUser) {

        if (companyId == null) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_COMPANYID_NOT_EXISTS);
        }
        PageResult<PersonDetailBO> pageResult = null;
        if (companyId.equals(departmentId) || (keyword != null && !"".equals(keyword.trim()))) {

            List<Long> positionIds = null;
            if (departmentId != null && !companyId.equals(departmentId)) {
                QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<DepartmentAddPO>();
                deptWrapper.eq("id", departmentId);
                deptWrapper.eq("valid", 1);
                int positionCount = count(deptWrapper);
                if (positionCount == 0) {
                    throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
                }
                positionIds = positionService.queryPositionIdsbyDeptId(departmentId);
            }

            //pageResult = personService.queryPersonsByCompanyId(companyId, positionIds, keyword, current, pageSize, conditionQuery);
            pageResult = personService.queryPersonsAndOrgDetailByCompanyIdBetter(companyId, positionIds, keyword, current, pageSize, conditionQuery, null);
        } else {
            if (departmentId == null) {
                throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
            }
            QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<DepartmentAddPO>();
            deptWrapper.eq("id", departmentId);
            deptWrapper.eq("valid", 1);
            int deptCount = count(deptWrapper);
            if (deptCount == 0) {
                throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
            }
            List<Long> posIds = positionService.queryPositionIdsbyDeptId(departmentId);

            pageResult = personService.queryPersonsAndOrgDetailByCompanyIdBetter(companyId, posIds, keyword, current, pageSize, conditionQuery, null);
            /*List<Long> presonIds = positionPersonService.queryPersonIdByPositionIds(posIds);
            if (presonIds == null || presonIds.size() == 0) {
                return new PageResult<PersonDetailBO>(null, 0, pageSize, current);
            }
            pageResult = personService.queryPersonsById(presonIds, current, pageSize, conditionQuery);*/
        }
        return pageResult;
    }
}
