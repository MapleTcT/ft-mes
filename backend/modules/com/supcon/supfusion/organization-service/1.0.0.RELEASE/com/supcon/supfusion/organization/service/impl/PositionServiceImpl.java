package com.supcon.supfusion.organization.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.supcon.supfusion.auth.api.dto.PersonRoleDTO;
import com.supcon.supfusion.auth.api.dto.UserDetailDTO;
import com.supcon.supfusion.framework.cloud.common.exception.BizHttpStatusException;
import com.supcon.supfusion.framework.cloud.common.message.Message;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.DepartmentErrorEnum;
import com.supcon.supfusion.organization.common.exception.DepartmentException;
import com.supcon.supfusion.organization.common.exception.OrganizationErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationException;
import com.supcon.supfusion.organization.common.exception.PositionErrorEnum;
import com.supcon.supfusion.organization.common.exception.PositionException;
import com.supcon.supfusion.organization.common.kafka.OrganizationMessage;
import com.supcon.supfusion.organization.common.model.ExcelTitle;
import com.supcon.supfusion.organization.common.utils.ExcelUtils;
import com.supcon.supfusion.organization.common.utils.OrgBaseUtils;
import com.supcon.supfusion.organization.dao.mapper.company.CompanyMapper;
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
import com.supcon.supfusion.organization.dao.po.department.DepartmentPersonPO;
import com.supcon.supfusion.organization.dao.po.excel.ExcelPO;
import com.supcon.supfusion.organization.dao.po.person.OrganizationManagerPO;
import com.supcon.supfusion.organization.dao.po.person.PersonAddPO;
import com.supcon.supfusion.organization.dao.po.position.PositionAddPO;
import com.supcon.supfusion.organization.dao.po.position.PositionBaseInfoPO;
import com.supcon.supfusion.organization.dao.po.position.PositionPersonPO;
import com.supcon.supfusion.organization.dao.po.position.PositionRolePO;
import com.supcon.supfusion.organization.dao.po.position.PositionSynchronizationInfoPO;
import com.supcon.supfusion.organization.manager.OrganizationAdapter;
import com.supcon.supfusion.organization.service.BaseServiceService;
import com.supcon.supfusion.organization.service.DepartmentService;
import com.supcon.supfusion.organization.service.ExcelManageService;
import com.supcon.supfusion.organization.service.OrgMnecodeService;
import com.supcon.supfusion.organization.service.OrganizationManagerService;
import com.supcon.supfusion.organization.service.PersonService;
import com.supcon.supfusion.organization.service.PositionPersonService;
import com.supcon.supfusion.organization.service.PositionService;
import com.supcon.supfusion.organization.service.bo.baseService.PositionBaseServiceBO;
import com.supcon.supfusion.organization.service.bo.company.CompanyBaseInfoBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentDetailBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentResultBO;
import com.supcon.supfusion.organization.service.bo.kafka.CompanySimpleMessageBO;
import com.supcon.supfusion.organization.service.bo.kafka.DepartmentSimpleMessageBO;
import com.supcon.supfusion.organization.service.bo.kafka.PositionDeleteMessageBO;
import com.supcon.supfusion.organization.service.bo.kafka.PositionMessageBO;
import com.supcon.supfusion.organization.service.bo.person.MainPositionBO;
import com.supcon.supfusion.organization.service.bo.person.OrganizationManagerBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import com.supcon.supfusion.organization.service.bo.person.PersonResultBO;
import com.supcon.supfusion.organization.service.bo.person.RelationDepartmentBO;
import com.supcon.supfusion.organization.service.bo.position.*;
import com.supcon.supfusion.organization.service.common.MultiParamSql;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
 * 岗位服务处理实现类
 * @author
 * @date 20-5-26 上午10:48
 */
@Slf4j
@Service
public class PositionServiceImpl extends ServiceImpl<PositionMapper, PositionAddPO> implements PositionService {

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private PositionPersonService positionPersonService;

    @Autowired
    private PositionPersonMapper positionPersonMapper;

    @Autowired
    private PersonService personService;

    @Autowired
    private PersonMapper personMapper;

    @Autowired
    private OrganizationManagerService organizationManagerService;

    @Autowired
    private PositionRoleMapper positionRoleMapper;

    @Autowired
    private OrganizationAdapter organizationAdapter;

    @Autowired
    private OrganizationManagerMapper organizationManagerMapper;

    @Autowired
    private ExcelMapper excelMapper;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ExcelManageService excelManageService;

    @Autowired
    private BaseServiceService baseServiceService;

    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;
    @Autowired
    private OrgMnecodeService orgMnecodeService;

    @Autowired
    private DepartmentPersonMapper departmentPersonMapper;

    private final Double FISRT_SORT = 1000d;

    @Autowired
    PositionMapper positionMapper;

    @Autowired
    private OrganizationMessage organizationMessage;

    private static final String POSITION_TOPIC = "supOS_position_event";

    /**
     * 新增岗位
     * @param positionAddPo 新增岗位信息
     * @param managerIds
     * @param tenantId

     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPosition(PositionAddPO positionAddPo, List<Long> managerIds, String tenantId) {

        addPositionWithoutKafka(positionAddPo, managerIds);
        //kafka
        List<PositionAddPO> positionAddPOList = new ArrayList<>();
        positionAddPOList.add(positionAddPo);
        publishCreateOrUpdatePositionMessage(positionAddPOList, tenantId, "CREATE");
    }

    /**
     * 修改岗位信息
     * @param positionAddPO 修改岗位信息
     * @param managerIds
     * @param tenantId

     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePosition(PositionAddPO positionAddPO, List<Long> managerIds, String tenantId) {

        updatePositionWithoutKafka(positionAddPO, managerIds);
        //kafka
        List<PositionAddPO> positionAddPOList = new ArrayList<>();
        positionAddPOList.add(positionAddPO);
        publishCreateOrUpdatePositionMessage(positionAddPOList, tenantId, "UPDATE");
    }
    /**
     * 修改full_path
     * @param curPos 当前岗位
     * @param newPos 新的岗位
     */
    private void updateFullPath(PositionAddPO curPos, PositionAddPO newPos) {
        QueryWrapper<PositionAddPO> judgeWrapper = new QueryWrapper<PositionAddPO>();
        if (newPos.getParentId() == null) {
            judgeWrapper.isNull("parent_id");
        } else {
            judgeWrapper.eq("parent_id", newPos.getParentId());
        }
        judgeWrapper.eq("valid", 1);
        judgeWrapper.eq("name", newPos.getName());
        judgeWrapper.ne("id", newPos.getId());
        judgeWrapper.eq("company_id", curPos.getCompanyId());
        int nameCount = count(judgeWrapper);
        if (nameCount > 0) {
            throw new PositionException(PositionErrorEnum.POSITION_NAME_EXISTS);
        }

        String curName = curPos.getName();
        String curFullPath = curPos.getFullPath();

        String newName = newPos.getName();
        String newFullPath = curFullPath.substring(0, curFullPath.lastIndexOf(curName)) + newName;
        newPos.setFullPath(newFullPath);
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
        queryWrapper.likeRight("full_path", curFullPath + "/");
        queryWrapper.eq("company_id", curPos.getCompanyId());
        List<PositionAddPO> subDeps = list(queryWrapper);
        if (subDeps == null || subDeps.size() == 0) {
            return;
        }
        subDeps.stream().forEach(dep -> {
            dep.setFullPath(newFullPath + dep.getFullPath().substring(curFullPath.length(), dep.getFullPath().length()));
        });
        updateBatchById(subDeps);
    }
    /**
     * 获取岗位详情
     * @param posId 岗位id
     * @return
     */
    @Override
    public PositionDetailBO getPosDetail(Long posId) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
        queryWrapper.eq("id", posId);
        queryWrapper.eq("valid", 1);
        int count = count(queryWrapper);
        //判断该id的部门是否存在
        if (count == 0) {
            throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
        }
        PositionAddPO curPos = getOne(queryWrapper);
        PositionDetailBO detailPO = new PositionDetailBO();
        BeanUtils.copyProperties(curPos, detailPO);

        CompanyPO company = companyMapper.selectById(curPos.getCompanyId());
        detailPO.setFullPath(OrgBaseUtils.splitCompanyFullPath(detailPO.getFullPath(), company.getShortName(), company.getFullPath()));
        QueryWrapper<DepartmentAddPO> depWrapper = new QueryWrapper<DepartmentAddPO>();
        depWrapper.eq("id", detailPO.getDepId());
        depWrapper.eq("valid", 1);
        DepartmentAddPO depPo = departmentMapper.selectOne(depWrapper);
        detailPO.setDepName(depPo.getName());

        QueryWrapper<OrganizationManagerPO> managerWrapper = new QueryWrapper<OrganizationManagerPO>();
        managerWrapper.eq("org_id", posId);
        managerWrapper.eq("manager_type", Constants.POSITION);
        List<OrganizationManagerPO> managers = organizationManagerMapper.selectList(managerWrapper);
        if (managers == null) {
            return detailPO;
        }
        List<OrganizationManagerBO> list = new ArrayList<OrganizationManagerBO>();
        managers.stream().forEach(manager -> {
            OrganizationManagerBO organizationManagerBO = new OrganizationManagerBO();
            BeanUtils.copyProperties(manager, organizationManagerBO);
            list.add(organizationManagerBO);
        });
        detailPO.setManagers(list);
        return detailPO;
    }

    /**
     * 根据id删除指定岗位
     * @param posId
     * @param tenantId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePosById(Long posId, String tenantId) {
        PositionAddPO positionAddPo = getById(posId);
        if (positionAddPo == null) {
            return;
        }
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
        queryWrapper.eq("company_id", positionAddPo.getCompanyId());
        queryWrapper.likeRight("full_path", positionAddPo.getFullPath() + "/");
        queryWrapper.eq("valid", 1);
        List<PositionAddPO> positionList = list(queryWrapper);
        if (positionList == null) {
            positionList = new ArrayList<>();
        }
        positionList.add(positionAddPo);

        if (positionList != null && positionList.size() > 0) {
            List<Long> posIds = new ArrayList<Long>();
            positionList.stream().forEach(pos -> {
                posIds.add(pos.getId());
                pos.setValid(false);
            });
            QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
            relationWrapper.in("position_id", posIds);
            relationWrapper.eq("valid", 1);
            int relCount = positionPersonMapper.selectCount(relationWrapper);
            if (relCount > 0) {
                throw new PositionException(PositionErrorEnum.POSITION_HAVE_RELATION_PERSON_DELETE_ERROR);
            }
            positionRoleMapper.delete(new QueryWrapper<PositionRolePO>().lambda()
                    .in(PositionRolePO::getPositionId,posIds));
            orgMnecodeService.deleteOrgMnecodeByOrgId(posIds, Constants.POSITION);
        }
        updateBatchById(positionList);
        //remove(queryWrapper);
        if (positionAddPo.getParentId() == null) {
            //kafka
            //orgBeanToKafkaMsgService.transferDeletePositionAndSend(positionAddPo);
            publishDeletePositionMessage(positionList, tenantId);
            return;
        }
        PositionAddPO parentPos = getById(positionAddPo.getParentId());
        QueryWrapper<PositionAddPO> broWrapper = new QueryWrapper<PositionAddPO>();
        broWrapper.eq("parent_id", positionAddPo.getParentId());
        broWrapper.eq("valid", 1);
        int broCount = count(broWrapper);
        if (broCount != 0) {
            //kafka
            //orgBeanToKafkaMsgService.transferDeletePositionAndSend(positionAddPo);
            publishDeletePositionMessage(positionList, tenantId);
            return;

        }
        parentPos.setLeaf(true);
        updateById(parentPos);
        //kafka
        //orgBeanToKafkaMsgService.transferDeletePositionAndSend(positionAddPo);
        publishDeletePositionMessage(positionList, tenantId);

    }

    /**
     * 修改岗位的位置
     * @param positionLocationPO
     * @param tenantId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePosLocation(PositionLocationBO positionLocationPO, String tenantId) {
        PositionAddPO currentPosition = updatePosLocationWithoutKafka(positionLocationPO);

        //kafka
        List<PositionAddPO> positionAddPOList = new ArrayList<>();
        positionAddPOList.add(currentPosition);
        publishCreateOrUpdatePositionMessage(positionAddPOList, tenantId, "UPDATE");

    }

    /**
     * 查询岗位树形结构,全量返回
     * @param companyId 公司id
     * @param keyword 关键字
     * @return
     */
    @Override
    public PositionTreeBO getPosTree(Long companyId, String keyword, Long positionId) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
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
            throw new PositionException(PositionErrorEnum.POSITION_COMPANYID_NOT_EXISTS);
        }
        PositionTreeBO firstDepOrCom = new PositionTreeBO();

        CompanyPO company = companyMapper.selectById(companyId);
        BeanUtils.copyProperties(company, firstDepOrCom);
        firstDepOrCom.setName(company.getShortName());
        firstDepOrCom.setCompanyId(firstDepOrCom.getId());

        if (positionId != null) {
            PositionAddPO currentPos = getById(positionId);
            if (currentPos == null) {
                throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
            }
            PositionTreeBO currentPosBo = new PositionTreeBO();
            BeanUtils.copyProperties(currentPos, currentPosBo);
            DepartmentAddPO departmentAddPO = departmentMapper.selectById(currentPos.getDepId());
            DepartmentDetailBO departmentDetailBO = new DepartmentDetailBO();
            BeanUtils.copyProperties(departmentAddPO, departmentDetailBO);
            currentPosBo.setDepartment(departmentDetailBO);
            currentPosBo.setFullPath(OrgBaseUtils.splitCompanyFullPath(currentPosBo.getFullPath(), company.getShortName(), company.getFullPath()));
            while (currentPosBo != null && currentPosBo.getParentId() != null) {
                PositionAddPO parentPos = getById(currentPosBo.getParentId());
                PositionTreeBO parentPosBo = new PositionTreeBO();
                BeanUtils.copyProperties(parentPos, parentPosBo);
                DepartmentAddPO parentDeptPO = departmentMapper.selectById(parentPos.getDepId());
                DepartmentDetailBO parentDetailBO = new DepartmentDetailBO();
                BeanUtils.copyProperties(parentDeptPO, parentDetailBO);
                parentPosBo.setDepartment(parentDetailBO);
                //currentPosBo.setParentId(currentPosBo.getParentId());
                List<PositionTreeBO> children = new ArrayList<PositionTreeBO>();
                children.add(currentPosBo);
                parentPosBo.setChildren(children);
                currentPosBo = parentPosBo;
                currentPosBo.setFullPath(OrgBaseUtils.splitCompanyFullPath(currentPosBo.getFullPath(), company.getShortName(), company.getFullPath()));
            }
            List<PositionTreeBO> children = new ArrayList<PositionTreeBO>();
            children.add(currentPosBo);
            firstDepOrCom.setChildren(children);
            return firstDepOrCom;
        }

        //查询所有的岗位
        List<PositionAddPO> positions = list(queryWrapper);
        if (positions == null || positions.size() == 0) {
            return firstDepOrCom;
        }
        List<PositionTreeBO> treeSubDeps = new ArrayList<PositionTreeBO>();
        List<Long> deptIds = new ArrayList<>(positions.size());
        positions.stream().forEach(pos -> {
            deptIds.add(pos.getDepId());
            PositionTreeBO subPos = new PositionTreeBO();
            BeanUtils.copyProperties(pos, subPos);
            String fullPath = OrgBaseUtils.splitCompanyFullPath(subPos.getFullPath(), company.getShortName(), company.getFullPath());
            subPos.setFullPath(fullPath);
            treeSubDeps.add(subPos);
        });

        QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<>();
        deptWrapper.lambda().eq(DepartmentAddPO::getValid, true).and(item -> {
            for (Long depId : deptIds) {
                item.or().eq(DepartmentAddPO::getId, depId);
            }
        });
        List<DepartmentAddPO> deptPos = departmentMapper.selectList(deptWrapper);
        if (deptPos != null && deptPos.size() > 0) {
            Map<Long, DepartmentDetailBO> depIdToDetail = new HashMap<>();
            deptPos.stream().forEach(item -> {
                DepartmentDetailBO departmentDetailBO = new DepartmentDetailBO();
                BeanUtils.copyProperties(item, departmentDetailBO);
                depIdToDetail.put(item.getId(), departmentDetailBO);
            });
            treeSubDeps.stream().forEach(pos -> {
                DepartmentDetailBO departmentDetailBO = depIdToDetail.get(pos.getDepId());
                pos.setDepartment(departmentDetailBO);
            });
        }

        List<PositionTreeBO> firsLevel = new ArrayList<PositionTreeBO>();
        LinkedList<PositionTreeBO> stack = new LinkedList<PositionTreeBO>();
        PositionTreeBO stackTop = null;
        //构造成树形结构
        while (treeSubDeps.size() > 0) {
            //下层级部门列表
            List<PositionTreeBO> subLevel = new ArrayList<PositionTreeBO>();

            Iterator<PositionTreeBO> it = treeSubDeps.iterator();
            while (it.hasNext()) {
                PositionTreeBO dep = it.next();
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
                        dep.setPrePos(stackTop);
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
        Iterator<PositionTreeBO> stackIt = stack.iterator();
        while(stackIt.hasNext()) {
            PositionTreeBO stackDep = stackIt.next();
            PositionTreeBO curDep = stackDep;

            while (curDep != null && (curDep.getChildren() == null || curDep.getChildren().size() == 0)) {
                PositionTreeBO preDep = curDep.getPrePos();
                if (curDep.getMatch()) {
                    break;
                }
                if (preDep != null) {
                    preDep.getChildren().remove(curDep);
                    curDep.setPrePos(null);
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
     * 返回岗位树形结构,层级返回
     * @param companyId 公司id
     * @param parentId 上级部门id
     * @param keyword 关键字
     * @return
     */
    @Override
    public PositionTreeBO getPosTree(Long companyId, Long parentId, String keyword) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
        queryWrapper.eq("company_id", companyId);

        QueryWrapper<CompanyPO> comJudgeWrapper = new QueryWrapper<CompanyPO>();
        comJudgeWrapper.eq("id", companyId);
        Integer comCount = companyMapper.selectCount(comJudgeWrapper);
        //指定id的公司不存在
        if (comCount == null || comCount == 0) {
            throw new PositionException(PositionErrorEnum.POSITION_COMPANYID_NOT_EXISTS);
        }

        PositionTreeBO firstDepOrCom = new PositionTreeBO();


        if (keyword == null || keyword.trim().equals("")) {
            if (parentId == null) {
                QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
                comWrapper.eq("id", companyId);
                CompanyPO company = companyMapper.selectOne(comWrapper);
                BeanUtils.copyProperties(company, firstDepOrCom);
                firstDepOrCom.setName(company.getShortName());
                queryWrapper.isNull("parent_id");
            } else {
                QueryWrapper<PositionAddPO> parentDepWrapper = new QueryWrapper<PositionAddPO>();
                parentDepWrapper.eq("id", parentId);
                PositionAddPO parentDep = getOne(parentDepWrapper);
                BeanUtils.copyProperties(parentDep, firstDepOrCom);
                queryWrapper.eq("parent_id", parentId);
            }
        } else {
/*            QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
            comWrapper.eq("id", companyId);*/
            CompanyPO company = companyMapper.selectById(companyId);
            BeanUtils.copyProperties(company, firstDepOrCom);
            firstDepOrCom.setName(company.getShortName());
        }
        firstDepOrCom.setCompanyId(companyId);
        queryWrapper.orderByAsc("lay_no");

        List<PositionAddPO> deps = list(queryWrapper);

        if (deps == null || deps.size() == 0) {
            return firstDepOrCom;
        }
        List<PositionTreeBO> treeSubDeps = new ArrayList<PositionTreeBO>();
        deps.stream().forEach(dep -> {
            PositionTreeBO subDep = new PositionTreeBO();
            BeanUtils.copyProperties(dep, subDep);
            treeSubDeps.add(subDep);
        });
        firstDepOrCom.setChildren(treeSubDeps);
        if (keyword == null || keyword.trim().equals("")) {
            return firstDepOrCom;
        }
        //第一层级部门列表
        List<PositionTreeBO> firsLevel = new ArrayList<PositionTreeBO>();
        LinkedList<PositionTreeBO> stack = new LinkedList<PositionTreeBO>();
        PositionTreeBO stackTop = null;
        //构造成树形结构
        while (treeSubDeps.size() > 0) {
            //下层级部门列表
            List<PositionTreeBO> subLevel = new ArrayList<PositionTreeBO>();

            Iterator<PositionTreeBO> it = treeSubDeps.iterator();
            while (it.hasNext()) {
                PositionTreeBO dep = it.next();
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
                        dep.setPrePos(stackTop);
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
        Iterator<PositionTreeBO> stackIt = stack.iterator();
        while(stackIt.hasNext()) {
            PositionTreeBO stackDep = stackIt.next();
            PositionTreeBO curDep = stackDep;

            while (curDep != null && (curDep.getChildren() == null || curDep.getChildren().size() == 0)) {
                PositionTreeBO preDep = curDep.getPrePos();
                if (curDep.getMatch()) {
                    break;
                }
                if (preDep != null) {
                    preDep.getChildren().remove(curDep);
                    curDep.setPrePos(null);
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
     * 新增岗位人员关系
     * @param positionPersonBO
     */
    @Override
    public void addPositionPerson(PositionPersonBO positionPersonBO) {
        PositionAddPO position = getById(positionPersonBO.getPositionId());
        if (position == null) {
            throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
        }
        List<Long> persons = positionPersonBO.getPersons().stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<Long>(Comparator.comparing(o -> o.longValue()))), ArrayList::new));

        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<PersonAddPO>();
        personWrapper.in("id", persons);
        int personCount = personMapper.selectCount(personWrapper);
        if (persons.size() != personCount) {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NOT_EXISTS);
        }
        List<PositionPersonPO> relations = new ArrayList<PositionPersonPO>();
        persons.stream().forEach(personId -> {
            PositionPersonPO relation = new PositionPersonPO();
            relation.setPersonId(personId);
            relation.setPositionId(positionPersonBO.getPositionId());
            relations.add(relation);
        });
        positionPersonService.batchSaveOrUpdate(relations);
    }

    /**
     * 查询岗位关联人员列表
     * @param companyId
     * @param positionId
     * @param keyword
     * @param current
     * @param pageSize
     * @param conditionQuery
     * @return
     */
    @Override
    public PageResult<PersonDetailBO> queryPositionPersons(Long companyId, Long positionId, String keyword, Integer current, Integer pageSize, PersonDetailBO conditionQuery, Boolean includeUser) {

        if (companyId == null) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_COMPANYID_NOT_EXISTS);
        }
        PageResult<PersonDetailBO> pageResult = null;
        if (companyId.equals(positionId) || (keyword != null && !"".equals(keyword.trim()))) {

            List<Long> positionIds = null;
            if (positionId != null && !companyId.equals(positionId)) {
                positionIds = new ArrayList<Long>();
                positionIds.add(positionId);
            }
            //pageResult = personService.queryPersonsByCompanyId(companyId, positionIds, keyword, current, pageSize, conditionQuery);
            pageResult = personService.queryPersonsByCompanyIdBetter(companyId, positionIds, keyword, current, pageSize, conditionQuery);
        } else {
            if (positionId == null) {
                throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
            }
            QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<PositionAddPO>();
            positionWrapper.eq("id", positionId);
            int positionCount = count(positionWrapper);
            if (positionCount == 0) {
                throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
            }
            List<Long> positionIds = new ArrayList<>();
            positionIds.add(positionId);
            pageResult = personService.queryPersonsByCompanyIdBetter(companyId, positionIds, keyword, current, pageSize, conditionQuery);
/*            List<Long> presonIds = positionPersonService.queryPersonIdByPositionId(positionId);
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
                List<PositionAddPO> positions = queryPositionByIds(positionIds);
                List<MainPositionBO> positionPathList = new ArrayList<MainPositionBO>();

                List<MainPositionBO> positionList = new ArrayList<MainPositionBO>();

                Set<Long> departmentIds = new HashSet<Long>();

                person.setPositionFullPath(positionPathList);

                person.setPosition(positionList);

                List<Long> currentPositionDept = new ArrayList<Long>(1);
                Map<Long, CompanyPO> depIdToComFullPath = new HashMap<>();
                positions.stream().forEach(posItem -> {
                    MainPositionBO mainPositionBO = new MainPositionBO();
                    mainPositionBO .setCompanyId(posItem.getCompanyId());
                    mainPositionBO.setId(posItem.getId());
                    mainPositionBO.setCode(posItem.getCode());
                    mainPositionBO.setName(posItem.getName());
                    mainPositionBO.setDepId(posItem.getDepId());
                    mainPositionBO.setLayRec(posItem.getLayRec());
                    mainPositionBO.setParentId(posItem.getParentId());
                    departmentIds.add(posItem.getDepId());
                    if (posItem.getCompanyId().equals(companyId)) {
                        if (positionId != null && positionId.equals(posItem.getId())) {
                            currentPositionDept.add(posItem.getDepId());
                        }
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
//                deptWrapper.in("id", departmentIds);
                //deptWrapper.eq("company_id", companyId);
                deptWrapper.eq("valid", 1);
//                List<DepartmentAddPO> depts = departmentMapper.selectList(deptWrapper);
                List<DepartmentAddPO> depts = MultiParamSql.forDepartment(new ArrayList(departmentIds), deptWrapper, "id", departmentMapper);
                List<RelationDepartmentBO> deptFullPaths = new ArrayList<RelationDepartmentBO>();

                List<RelationDepartmentBO> departmentList = new ArrayList<RelationDepartmentBO>();

                depts.stream().forEach(deptItem -> {
                    RelationDepartmentBO relationDepartmentBO = new RelationDepartmentBO();
                    relationDepartmentBO.setCompanyId(deptItem.getCompanyId());
                    relationDepartmentBO.setId(deptItem.getId());
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

                    if (currentPositionDept.size() > 0 && currentPositionDept.get(0).equals(deptItem.getId())) {
                        relationDepartmentBO.setCurrentPosition(true);
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
    public List<PersonDetailBO> queryPositionPersonNoPage(Long companyId, Long positionId, String keyword, PersonDetailBO conditionQuery) {
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
        PageResult<PersonDetailBO> pageResult = queryPositionPersons(companyId, positionId, keyword, 1, count, conditionQuery, false);
        if (pageResult == null || pageResult.getList() == null || pageResult.getList().size() == 0) {
            return null;
        }

        return (List)pageResult.getList();
    }

    private void transferSystemCode(PersonDetailBO person) {
        if (person.getGender() != null) {
            String genderName = organizationAdapter.getSystemCodeName(person.getGender());
            person.setGender(genderName);
/*            if (person.getGender().equals("male")) {
                person.setGender("男");
            } else {
                person.setGender("女");
            }*/
        }
        if (person.getStatus() != null) {
            String statusName = organizationAdapter.getSystemCodeName(person.getStatus());
            person.setStatus(statusName);
/*            if (person.getStatus().equals("onWork")) {
                person.setStatus("在职");
            } else {
                person.setStatus("离职");
            }*/
        }
        if (person.getClassifiedLevel() != null) {
            String classifiedLevelName = organizationAdapter.getSystemCodeName(person.getClassifiedLevel());
            person.setClassifiedLevel(classifiedLevelName);
            /*if (person.getClassifiedLevel().equals("unclassified")) {
                person.setClassifiedLevel("非密");
            } else if (person.getClassifiedLevel().equals("generalClassified")) {
                person.setClassifiedLevel("普通加密");
            } else if (person.getClassifiedLevel().equals("importantClassified")) {
                person.setClassifiedLevel("重要加密");
            } else {
                person.setClassifiedLevel("核心加密");
            }*/
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
     * 根据岗位id查询岗位
     * @param positionIds
     * @return
     */
    @Override
    public List<PositionAddPO> queryPositionByIds(List<Long> positionIds) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
//        queryWrapper.in("id", positionIds);
        queryWrapper.eq("valid", 1);
//        return list(queryWrapper);
        return MultiParamSql.forPosition(positionIds, queryWrapper, "id", positionMapper);
    }

    /**
     * 根据部门id查询所有岗位id
     * @param departmentId
     * @return
     */
    @Override
    public List<Long> queryPositionIdsbyDeptId(Long departmentId) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
        queryWrapper.eq("dep_id", departmentId);
        queryWrapper.eq("valid", 1);
        List<PositionAddPO> poses = list(queryWrapper);
        List<Long> posIds = new ArrayList<Long>();
        if (poses == null || poses.size() == 0) {
            return posIds;
        }
        poses.stream().forEach(item -> {
            posIds.add(item.getId());
        });
        return posIds;
    }

    @Override
    public List<Long> queryPositionIdsbyCompanyId(Long companyId) {
        LambdaQueryWrapper<PositionAddPO> lamba = new QueryWrapper<PositionAddPO>().lambda()
                .eq(PositionAddPO::getCompanyId, companyId)
                .eq(PositionAddPO::getValid, true)
                .eq(PositionAddPO::getSysFlag, 0);

        List<PositionAddPO> poses = list(lamba);
        List<Long> posIds = new ArrayList<Long>();
        if (poses == null || poses.size() == 0) {
            return posIds;
        }
        poses.stream().forEach(item -> {
            posIds.add(item.getId());
        });
        return posIds;
    }


    /**
     * 删除岗位人员关联关系
     * @param positionId 岗位id
     * @param personIds 人员id
     */
    @Override
    public void deleteRelations(Long positionId, Long[] personIds) {
        QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<PositionAddPO>();
        positionWrapper.eq("id", positionId);
        positionWrapper.eq("valid", 1);
        int posCount = count(positionWrapper);
        if (posCount == 0) {
            throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
        }
        if (personIds.length == 0) {
            return;
        }
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<PersonAddPO>();
        personWrapper.in("id", personIds);
        personWrapper.eq("valid", 1);
        int personCount = personMapper.selectCount(personWrapper);
        if (personCount != personIds.length) {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NOT_EXISTS);
        }
        Arrays.stream(personIds).forEach(personId -> {
            QueryWrapper<PositionPersonPO> posPersonWrapper = new QueryWrapper<PositionPersonPO>();
            posPersonWrapper.eq("person_id", personId);
            posPersonWrapper.eq("valid", 1);
            int count = positionPersonMapper.selectCount(posPersonWrapper);
            if (count <= 1) {
                throw new PositionException(OrganizationErrorEnum.PERSON_MUST_HAVE_ONE_POSITION);
            }
            posPersonWrapper.eq("position_id", positionId);
            posPersonWrapper.eq("valid", 1);
            PositionPersonPO relation = positionPersonMapper.selectOne(posPersonWrapper);
            relation.setValid(false);
            positionPersonMapper.updateById(relation);
            PersonAddPO person = personMapper.selectById(personId);
            if (person.getMainPosition().equals(positionId)) {
                posPersonWrapper.clear();
                posPersonWrapper.eq("person_id", personId);
                posPersonWrapper.eq("valid", 1);
                List<PositionPersonPO> relations = positionPersonMapper.selectList(posPersonWrapper);
                if (relations.size() != 0) {
                    person.setMainPosition(relations.get(0).getPositionId());
                    personMapper.updateById(person);
                }
            }
        });
    }

    /**
     * 关键字模糊匹配
     * @param keyword
     * @param companyId
     * @return
     */
    @Override
    public List<PositionKeywordBO> queryPositionsByKeyword(String keyword, Long companyId) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
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
        List<PositionAddPO> results = list(queryWrapper);
        if (results == null || results.size() == 0) {
            return null;
        }
        List<PositionKeywordBO> list = new ArrayList<PositionKeywordBO>();
        List<Long> positionIds = new ArrayList<Long>();
        results.stream().forEach(position -> {
            PositionKeywordBO positionKeywordBO = new PositionKeywordBO();
            BeanUtils.copyProperties(position, positionKeywordBO);
            positionIds.add(position.getId());
            list.add(positionKeywordBO);
        });
        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
        relationWrapper.select("position_id AS POSITIONID", "count(1) AS COUNT");
//        relationWrapper.in("position_id", positionIds);
        relationWrapper.eq("valid", 1);
        relationWrapper.groupBy("position_id");
//        List<Map<String, Object>> groupMapList = positionPersonMapper.selectMaps(relationWrapper);
        List<Map<String, Object>> groupMapList = MultiParamSql.forPersonPositionMap(positionIds, relationWrapper, "position_id", positionPersonMapper);
        if (groupMapList != null) {
            groupMapList.stream().forEach(group -> {
                int index = positionIds.indexOf(Long.valueOf(group.get("POSITIONID").toString()));
                if (index >= 0) {
                    if (group.get("COUNT") == null) {
                        list.get(index).setPersonNum(0L);
                    } else {
                        list.get(index).setPersonNum(Long.valueOf(group.get("COUNT").toString()));
                    }
                }
            });
        }
        return list;
    }

    /**
     * 为岗位添加角色
     * @param roleIds
     * @param positionId
     */
    @Override
    public void addPositionRole(List<Long> roleIds, Long positionId) {
        if (roleIds == null || roleIds.size() == 0) {
            return;
        }
        for (Long roleId : roleIds) {
            QueryWrapper<PositionRolePO> queryWrapper = new QueryWrapper<PositionRolePO>();
            queryWrapper.eq("position_id", positionId);
            queryWrapper.eq("role_id", roleId);
            int count = positionRoleMapper.selectCount(queryWrapper);
            if (count == 0) {
                PositionRolePO positionRolePO = new PositionRolePO();
                positionRolePO.setPositionId(positionId);
                positionRolePO.setRoleId(roleId);
                positionRoleMapper.insert(positionRolePO);
            }
        }
        List<PersonRoleDTO> roles = generatePersonRoleByPositionId(positionId);
        if (roles == null || roles.size() == 0) {
            return;
        }
        Boolean flag = organizationAdapter.informRoleChange(roles);
        if (!flag) {
            throw new OrganizationException(OrganizationErrorEnum.ROLE_CHANGE_ERROR);
        }

    }

    private List<PersonRoleDTO> generatePersonRoleByPositionId(Long positionId) {
        QueryWrapper<PositionPersonPO> personWrapper = new QueryWrapper<>();
        personWrapper.eq("position_id", positionId);
        personWrapper.eq("valid", 1);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(personWrapper);
        if (relations == null || relations.size() == 0) {
            return null;
        }
        List<Long> personIds = new ArrayList<>();
        relations.stream().forEach(rel -> personIds.add(rel.getPersonId()));
        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<>();
        relationWrapper.in("person_id", personIds);
        relationWrapper.eq("valid", 1);
        List<PositionPersonPO> relationsPerToPos = positionPersonMapper.selectList(relationWrapper);
        if (relationsPerToPos == null || relationsPerToPos.size() == 0) {
            return null;
        }
        List<Long> posIds = new ArrayList<>();
        relationsPerToPos.stream().forEach(rptp -> posIds.add(rptp.getPositionId()));
        QueryWrapper<PositionRolePO> roleWrapper = new QueryWrapper<>();
        roleWrapper.in("position_id", posIds);
        List<PositionRolePO> roles = positionRoleMapper.selectList(roleWrapper);
        if (roles == null) {
            roles = new ArrayList<>();
        }
        Map<Long, PersonRoleDTO> perIdToRole = new HashMap<>();
        Map<Long, Set<Long>> posIdToRoleIds = new HashMap<>();
        List<PersonRoleDTO> results = new ArrayList<>();
        for (PositionRolePO positionRolePO : roles) {
            if (!posIdToRoleIds.containsKey(positionRolePO.getPositionId())) {
                Set<Long> roleSet = new HashSet<>();
                roleSet.add(positionRolePO.getRoleId());
                posIdToRoleIds.put(positionRolePO.getPositionId(), roleSet);
            } else {
                posIdToRoleIds.get(positionRolePO.getPositionId()).add(positionRolePO.getRoleId());
            }
        }
        for (PositionPersonPO positionPersonPO : relationsPerToPos) {
            if (!perIdToRole.containsKey(positionPersonPO.getPersonId())) {
                PersonRoleDTO personRoleDTO = new PersonRoleDTO();
                personRoleDTO.setPersonId(positionPersonPO.getPersonId());
                perIdToRole.put(positionPersonPO.getPersonId(), personRoleDTO);
                results.add(personRoleDTO);
            }
            PersonRoleDTO prd = perIdToRole.get(positionPersonPO.getPersonId());
            if (!posIdToRoleIds.containsKey(positionPersonPO.getPositionId())) {
                continue;
            }
            if (prd.getRoleId() == null) {
                prd.setRoleId(new HashSet<>());
            }
            prd.getRoleId().addAll(posIdToRoleIds.get(positionPersonPO.getPositionId()));
        }
        return results;
    }
    /**
     * 删除岗位角色
     * @param roleId
     * @param positionId
     */
    @Override
    public void deletePositionRole(Long roleId, Long positionId) {
        QueryWrapper<PositionRolePO> queryWrapper = new QueryWrapper<PositionRolePO>();
        queryWrapper.eq("position_id", positionId);
        queryWrapper.eq("role_id", roleId);
        positionRoleMapper.delete(queryWrapper);

        List<PersonRoleDTO> roles = generatePersonRoleByPositionId(positionId);
        if (roles == null || roles.size() == 0) {
            return;
        }
        Boolean flag = organizationAdapter.informRoleChange(roles);
        if (!flag) {
            throw new OrganizationException(OrganizationErrorEnum.ROLE_CHANGE_ERROR);
        }
    }

    /**
     * 查询岗位的角色
     * @param positionId
     * @return
     */
    @Override
    public List<PositionRoleBO> queryPositionRole(Long positionId) {
        QueryWrapper<PositionRolePO> queryWrapper = new QueryWrapper<PositionRolePO>();
        queryWrapper.eq("position_id", positionId);
        List<PositionRolePO> positionRolePOS = positionRoleMapper.selectList(queryWrapper);
        List<PositionRoleBO> results = new ArrayList<PositionRoleBO>();
        if (positionRolePOS == null || positionRolePOS.size() == 0) {
            return results;
        }
        List<Long> roleIds = new ArrayList<Long>();
        positionRolePOS.stream().forEach(item -> roleIds.add(item.getRoleId()));
        List<RoleDTO> roles = organizationAdapter.findRoleByIds(roleIds);
        if (roles == null || roles.size() == 0) {
            return results;
        }
        roles.stream().forEach(role -> {
            PositionRoleBO positionRoleBO = new PositionRoleBO();
            positionRoleBO.setId(role.getId());
            positionRoleBO.setName(role.getName());
            results.add(positionRoleBO);
         });
        return results;
    }

    /**
     * 导入excel
     * @param workbook
     * @param taskId
     * @param companyId
     * @param originalFilename
     * @param fileName
     * @param tenantId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importExcel(XSSFWorkbook workbook, Long taskId, Long companyId, String originalFilename, String fileName, String tenantId) {
        ExcelPO excelPO = excelMapper.selectById(taskId);
        if (excelPO == null) {
            return;
        }
        excelPO.setStatus(1);
        //excelMapper.updateById(excelPO);
        excelManageService.excuteExcelState(excelPO);
        Sheet sheet = workbook.getSheetAt(1);

        Row titleRow = sheet.getRow(0);
        HashMap<String, Integer> titleMap = new HashMap<String, Integer>();
        boolean flag = checkImportExcelTitle(ExcelUtils.POSITION_IMPORT_TEMPLATE, titleRow, titleMap);
        if (!flag) {
            excelPO.setStatus(3);
            excelPO.setErrorMessage(OrganizationErrorEnum.EXCEL_IMPORT_TITLE_ERROR.getMessage());
            excelManageService.excuteExcelState(excelPO);
            return;
        }

        List<PositionDetailBO> importPos = new ArrayList<PositionDetailBO>();
        flag = checkExcelData(workbook, titleMap, importPos, companyId);

        if (!flag) {
            try {
                String originFileName = fileName.substring(fileName.indexOf("-") + 1);
                String errorFileName = ExcelUtils.createErrorExcelFile(workbook, originFileName, taskId);

                excelPO.setErrorFile(errorFileName);
                excelPO.setStatus(3);
                //excelMapper.updateById(excelPO);
                return;
            } catch (Exception e) {
                log.warn("岗位导入异常:" + e);
                excelPO.setStatus(3);
                excelPO.setErrorMessage(OrganizationErrorEnum.EXCEL_IMPORT_PROCESS_ERROR.getMessage());
                //excelMapper.updateById(excelPO);
                return;
            } finally {
                excelManageService.excuteExcelState(excelPO);
            }

        } else {
            try {
                //正式导入数据
                excelBatchAddPosition(importPos, companyId, tenantId);
                excelPO.setStatus(2);
                excelPO.setErrorMessage(Constants.EXCEL_IMPORT_SUCESS);
                //excelMapper.updateById(excelPO);
            } catch (Exception e) {
                excelPO.setStatus(3);
                excelPO.setErrorMessage(OrganizationErrorEnum.EXCEL_IMPORT_PROCESS_ERROR.getMessage());
                log.error("岗位导入异常:" + e);
                throw new OrganizationException(OrganizationErrorEnum.EXCEL_IMPORT_PROCESS_ERROR);
            } finally {
                excelManageService.excuteExcelState(excelPO);
            }
        }
    }

    /**
     * 执行实际的导入
     * @param importPos
     * @param tenantId
     */
    @Transactional(rollbackFor = Exception.class)
    public void excelBatchAddPosition(List<PositionDetailBO> importPos, Long companyId, String tenantId) {
        List<PositionAddPO> createPosList = new ArrayList<>();
        List<PositionAddPO> updatePostList = new ArrayList<>();
        for (PositionDetailBO positionDetailBO : importPos) {
            excelAddPosition(positionDetailBO, importPos, companyId, tenantId, createPosList, updatePostList);

        }
        publishCreateOrUpdatePositionMessage(createPosList, tenantId, "CREATE");
        publishCreateOrUpdatePositionMessage(updatePostList, tenantId, "UPDATE");
    }
    /**
     * 插入当前行的岗位，返回当前的ｉｄ
     * @param positionDetailBO
     * @param list
     * @param companyId
     * @param tenantId
     * @param createPosList
     * @param updatePostList
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Long excelAddPosition(PositionDetailBO positionDetailBO, List<PositionDetailBO> list, Long companyId, String tenantId, List<PositionAddPO> createPosList, List<PositionAddPO> updatePostList) {
        //id为空，新增部门
        if (positionDetailBO.getId() == null) {
            PositionAddPO positionAddPO = new PositionAddPO();
            positionAddPO.setCode(positionDetailBO.getCode());
            positionAddPO.setName(positionDetailBO.getName());
            positionAddPO.setCompanyId(companyId);
            positionAddPO.setDepId(positionDetailBO.getDepId());
            positionAddPO.setDescription(positionDetailBO.getDescription());

            List<Long> managerIds = new ArrayList<Long>();
            if (positionDetailBO.getManagers() != null && positionDetailBO.getManagers().size() > 0) {
                //设置负责人
                positionDetailBO.getManagers().stream().forEach(manager -> managerIds.add(manager.getManagerId()));
            }

            if (positionDetailBO.getParentId() != null || (positionDetailBO.getParentId() == null && StringUtils.isBlank(positionDetailBO.getParentCode()))) {
                //数据库中已经存在父节点
                positionAddPO.setParentId(positionDetailBO.getParentId());
            } else if(StringUtils.isNotBlank(positionDetailBO.getParentCode())) {
                for (PositionDetailBO pos : list) {
                    if (pos.getCode().equals(positionDetailBO.getParentCode())) {
                        Long parentId = excelAddPosition(pos, list, companyId, tenantId, createPosList, updatePostList);
                        positionAddPO.setParentId(parentId);
                        break;
                    }
                }
            }
            QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
            queryWrapper.eq("code", positionAddPO.getCode());
            queryWrapper.eq("valid", 1);
            int count = count(queryWrapper);
            if (count == 0) {
                if (managerIds.size() == 0) {
                    addPositionWithoutKafka(positionAddPO, null);
                } else {
                    addPositionWithoutKafka(positionAddPO, managerIds);
                }
                createPosList.add(positionAddPO);
                if (positionDetailBO.getRoleIds() != null && positionDetailBO.getRoleIds().size() > 0) {
                    addPositionRole(positionDetailBO.getRoleIds(), positionAddPO.getId());
                }
            } else {
                PositionAddPO curPos = getOne(queryWrapper);
                return curPos.getId();
            }

            return positionAddPO.getId();
        } else {
            List<Long> managerIds = new ArrayList<Long>();
            if (positionDetailBO.getManagers() != null && positionDetailBO.getManagers().size() > 0) {
                //设置负责人
                positionDetailBO.getManagers().stream().forEach(manager -> managerIds.add(manager.getManagerId()));
            }
            //数据库已经有了当前岗位，修改，或移动位置
            PositionAddPO curPos = getById(positionDetailBO.getId());
            PositionAddPO curParentPos = null;
            if (curPos.getParentId() != null) {
                curParentPos = getById(curPos.getParentId());
            }
            if ((positionDetailBO.getParentId() != null && positionDetailBO.getParentId().equals(curPos.getParentId())) ||
                    (positionDetailBO.getParentId() == null && StringUtils.isBlank(positionDetailBO.getParentCode()) && curPos.getParentId() == null) ||
                    (positionDetailBO.getParentId() == null && StringUtils.isNotBlank(positionDetailBO.getParentCode()) && curParentPos != null && positionDetailBO.getParentCode().equals(curParentPos.getCode()))) {
                //父节点已经在数据库有了,并且父节点没变,只修改信息
                curPos.setName(positionDetailBO.getName());
                curPos.setDescription(positionDetailBO.getDescription());
                if (StringUtils.isNotBlank(positionDetailBO.getDescription())) {
                    curPos.setDescription(positionDetailBO.getDescription());
                }
                if (managerIds.size() == 0) {
                    updatePositionWithoutKafka(curPos, null);
                } else {
                    updatePositionWithoutKafka(curPos, managerIds);
                }
                updatePostList.add(curPos);
                if (positionDetailBO.getRoleIds() != null && positionDetailBO.getRoleIds().size() > 0) {
                    addPositionRole(positionDetailBO.getRoleIds(), positionDetailBO.getId());
                }
                return positionDetailBO.getId();
            } else {
                //节点有移动到其他父节点
                //如果父节点在数据库已经有了,直接移动
                if (StringUtils.isBlank(positionDetailBO.getParentCode())) {
                    PositionLocationBO positionLocationBO = new PositionLocationBO();
                    positionLocationBO.setParentId(positionDetailBO.getParentId());
                    positionLocationBO.setId(positionDetailBO.getId());
                    curPos.setName(positionDetailBO.getName());
                    curPos.setDescription(positionDetailBO.getDescription());
                    if (StringUtils.isNotBlank(positionDetailBO.getDescription())) {
                        curPos.setDescription(positionDetailBO.getDescription());
                    }
                    if (managerIds.size() == 0) {
                        updatePositionWithoutKafka(curPos, null);
                    } else {
                        updatePositionWithoutKafka(curPos, managerIds);
                    }
                    if (positionDetailBO.getRoleIds() != null && positionDetailBO.getRoleIds().size() > 0) {
                        addPositionRole(positionDetailBO.getRoleIds(), positionDetailBO.getId());
                    }
                    PositionAddPO currentPos = updatePosLocationWithoutKafka(positionLocationBO);
                    updatePostList.add(currentPos);
                } else {
                    //父节点在数据库没有,需要递归在excel查询来依次新增
                    for (PositionDetailBO pos : list) {
                        if (pos.getCode().equals(positionDetailBO.getParentCode())) {
                            Long parentId = excelAddPosition(pos, list, companyId, tenantId, createPosList, updatePostList);
                            curPos.setParentId(parentId);
                            break;
                        }
                    }
                }
                return positionDetailBO.getId();
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
    /**
     * 校验Excel中的数据合法性
     * @param workbook
     * @return
     */
    private boolean checkExcelData(XSSFWorkbook workbook, Map<String, Integer> titleMap, List<PositionDetailBO> importPos, Long companyId) {
        boolean flag = true;
        Sheet sheet = workbook.getSheetAt(1);
        Drawing drawing = sheet.createDrawingPatriarch();
        CellStyle cellStyle = ExcelUtils.createImportErrorCellStyle(workbook);
        List<String> parentCodes = new ArrayList<>();
        List<String> parentNames = new ArrayList<String>();
        Iterator<Row> parentCodeIt = sheet.rowIterator();
        parentCodeIt.next();
        List<String> parentAndSub = new ArrayList<String>();
        while (parentCodeIt.hasNext()) {
            Row curRow = parentCodeIt.next();
            Cell codeCell = curRow.getCell(titleMap.get("code"));
            Cell nameCell = curRow.getCell(titleMap.get("name"));
            if (codeCell != null && StringUtils.isNotBlank(getRightTypeCell(codeCell).toString())) {
                parentCodes.add(getRightTypeCell(codeCell).toString());
            } else {
                parentCodes.add(null);
            }
            if (nameCell != null && StringUtils.isNotBlank(getRightTypeCell(nameCell).toString())) {
                parentNames.add(getRightTypeCell(nameCell).toString());
            } else {
                parentNames.add(null);
            }
            Cell parentNameCell = curRow.getCell(titleMap.get("parentName"));

            Cell parentCodeCell = curRow.getCell(titleMap.get("parentCode"));
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
            PositionDetailBO positionDetailBO = new PositionDetailBO();
            Row curRow = rowIt.next();
            int rowY = curRow.getRowNum();
            //岗位编码
            Cell codeCell = curRow.getCell(titleMap.get("code"));


            if (codeCell == null) {
                codeCell = curRow.createCell(titleMap.get("code"), CellType.STRING);
                int codeX = codeCell.getColumnIndex();
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(codeX, rowY, codeX, rowY, codeX, rowY, codeX, rowY));
                comment.setString(new XSSFRichTextString(Constants.POSITION_PARAM_CODE_NECESSARY));
                codeCell.setCellComment(comment);
                codeCell.setCellStyle(cellStyle);
                flag = false;
            } else if (codeCell != null && StringUtils.isBlank(getRightTypeCell(codeCell).toString())) {
                int codeX = codeCell.getColumnIndex();
                ExcelUtils.removeComment(codeCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(codeX, rowY, codeX, rowY, codeX, rowY, codeX, rowY));
                comment.setString(new XSSFRichTextString(Constants.POSITION_PARAM_CODE_NECESSARY));
                codeCell.setCellComment(comment);
                codeCell.setCellStyle(cellStyle);
                flag = false;
            } else if (codeCell != null && getRightTypeCell(codeCell).toString().length() > 50) {
                int codeX = codeCell.getColumnIndex();
                ExcelUtils.removeComment(codeCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(codeX, rowY, codeX, rowY, codeX, rowY, codeX, rowY));
                comment.setString(new XSSFRichTextString(Constants.POSITION_PARAM_CODE_LENGTH_ERROR));
                codeCell.setCellComment(comment);
                codeCell.setCellStyle(cellStyle);
                flag = false;

            } else if (codeCell != null && !Constants.codePattern.matcher(getRightTypeCell(codeCell).toString()).matches()) {
                int codeX = codeCell.getColumnIndex();
                ExcelUtils.removeComment(codeCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(codeX, rowY, codeX, rowY, codeX, rowY, codeX, rowY));
                comment.setString(new XSSFRichTextString(Constants.ORG_CODE_PATTERN));

                codeCell.setCellComment(comment);
                codeCell.setCellStyle(cellStyle);
                flag = false;
            } else {
                int codeX = codeCell.getColumnIndex();
                String code = getRightTypeCell(codeCell).toString();
                if (parentCodes.indexOf(code) != parentCodes.lastIndexOf(code)) {
                    ExcelUtils.removeComment(codeCell);
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(codeX, rowY, codeX, rowY, codeX, rowY, codeX, rowY));
                    comment.setString(new XSSFRichTextString(Constants.EXCEL_POSITION_CODE_DUPLICATION));
                    codeCell.setCellComment(comment);
                    codeCell.setCellStyle(cellStyle);
                    flag = false;
                } else {
                    QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
                    queryWrapper.eq("code", code);
                    queryWrapper.eq("valid", 1);
                    PositionAddPO pos = getOne(queryWrapper);
                    if (pos == null) {
                        positionDetailBO.setCode(code);
                    } else {
                        if (companyId.equals(pos.getCompanyId())) {
                            positionDetailBO.setId(pos.getId());
                        } else {
                            ExcelUtils.removeComment(codeCell);
                            Comment comment = drawing.createCellComment(new XSSFClientAnchor(codeX, rowY, codeX, rowY, codeX, rowY, codeX, rowY));
                            comment.setString(new XSSFRichTextString(Constants.EXCEL_POSITION_CODE_DUPLICATION_DB));
                            codeCell.setCellComment(comment);
                            codeCell.setCellStyle(cellStyle);
                            flag = false;
                        }

                    }
                }
            }


            //所属部门编码
            Cell depCodeCell = curRow.getCell(titleMap.get("departmentCode"));

            //所属部门名称,如果部门名称的部门是1个则按部门名称查询,否则要填写部门编码
            Cell depNameCell = curRow.getCell(titleMap.get("departmentName"));

            if (depNameCell !=null && StringUtils.isBlank(getRightTypeCell(depNameCell).toString())) {
                int depNameX = depNameCell.getColumnIndex();
                ExcelUtils.removeComment(depNameCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(depNameX, rowY, depNameX, rowY, depNameX, rowY, depNameX, rowY));
                comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_PARAM_NAME_NECESSARY));
                depNameCell.setCellComment(comment);
                depNameCell.setCellStyle(cellStyle);
                flag = false;
            } else if (depNameCell == null) {
                depNameCell = curRow.createCell(titleMap.get("departmentName"), CellType.STRING);
                int depNameX = depNameCell.getColumnIndex();
                ExcelUtils.removeComment(depNameCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(depNameX, rowY, depNameX, rowY, depNameX, rowY, depNameX, rowY));
                comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_PARAM_NAME_NECESSARY));
                depNameCell.setCellComment(comment);
                depNameCell.setCellStyle(cellStyle);
                flag = false;
            } else {
                int depNameX = depNameCell.getColumnIndex();
                QueryWrapper<DepartmentAddPO> depWrapper = new QueryWrapper<DepartmentAddPO>();
                depWrapper.eq("name", getRightTypeCell(depNameCell).toString());
                depWrapper.eq("valid", 1);
                depWrapper.eq("company_id", companyId);
                int count = departmentMapper.selectCount(depWrapper);

                if (count == 0) {
                    ExcelUtils.removeComment(depNameCell);
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(depNameX, rowY, depNameX, rowY, depNameX, rowY, depNameX, rowY));
                    comment.setString(new XSSFRichTextString(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS.getMessage()));

                    depNameCell.setCellComment(comment);
                    depNameCell.setCellStyle(cellStyle);
                    flag = false;
                } else if (count > 1) {
                    if (depCodeCell == null) {
                        depCodeCell = curRow.createCell(titleMap.get("departmentCode"), CellType.STRING);
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(depCodeCell.getColumnIndex(), rowY, depCodeCell.getColumnIndex(), rowY, depCodeCell.getColumnIndex(), rowY, depCodeCell.getColumnIndex(), rowY));
                        comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_NAME_DUP_CODE_NEED));

                        depCodeCell.setCellComment(comment);
                        depCodeCell.setCellStyle(cellStyle);
                        flag = false;
                    } else if (depCodeCell != null && StringUtils.isBlank(getRightTypeCell(depCodeCell).toString())) {
                        int depCodeX = depCodeCell.getColumnIndex();
                        ExcelUtils.removeComment(depCodeCell);
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(depCodeX, rowY, depCodeX, rowY, depCodeX, rowY, depCodeX, rowY));
                        comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_NAME_DUP_CODE_NEED));

                        depCodeCell.setCellComment(comment);
                        depCodeCell.setCellStyle(cellStyle);
                        flag = false;
                    } else {
                        int depCodeX = depCodeCell.getColumnIndex();
                        QueryWrapper<DepartmentAddPO> depCodeWrapper = new QueryWrapper<DepartmentAddPO>();
                        depCodeWrapper.eq("code", getRightTypeCell(depCodeCell).toString());
                        depCodeWrapper.eq("valid", 1);
                        depCodeWrapper.eq("company_id", companyId);
                        int codeCount = departmentMapper.selectCount(depCodeWrapper);
                        if (codeCount != 1) {
                            ExcelUtils.removeComment(depCodeCell);
                            Comment comment = drawing.createCellComment(new XSSFClientAnchor(depCodeX, rowY, depCodeX, rowY, depCodeX, rowY, depCodeX, rowY));
                            comment.setString(new XSSFRichTextString(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS.getMessage()));

                            depCodeCell.setCellComment(comment);
                            depCodeCell.setCellStyle(cellStyle);
                            flag = false;
                        } else {
                            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(depCodeWrapper);
                            positionDetailBO.setDepId(departmentAddPO.getId());
                        }
                    }
                } else {
                    DepartmentAddPO departmentAddPO = departmentMapper.selectOne(depWrapper);
                    positionDetailBO.setDepId(departmentAddPO.getId());
                }
            }

            Cell parentCodeCell = curRow.getCell(titleMap.get("parentCode"));

            Cell parentNameCell = curRow.getCell(titleMap.get("parentName"));

            Boolean isDupParent = false;
            if (parentCodeCell != null && StringUtils.isNotBlank(getRightTypeCell(parentCodeCell).toString())) {
                int parentCodeX = parentCodeCell.getColumnIndex();
                QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
                queryWrapper.eq("code", getRightTypeCell(parentCodeCell).toString());
                queryWrapper.eq("valid", 1);
                queryWrapper.eq("company_id", companyId);
                int count = count(queryWrapper);
                if (!parentCodes.contains(getRightTypeCell(parentCodeCell).toString()) && count == 0) {
                    ExcelUtils.removeComment(parentCodeCell);
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(parentCodeX, rowY, parentCodeX, rowY, parentCodeX, rowY, parentCodeX, rowY));
                    comment.setString(new XSSFRichTextString(PositionErrorEnum.POSITION_ID_NOT_EXISTS.getMessage()));

                    parentCodeCell.setCellComment(comment);
                    parentCodeCell.setCellStyle(cellStyle);
                    flag = false;
                } else if (count > 0) {
                    PositionAddPO parentPos = getOne(queryWrapper);
                    positionDetailBO.setParentId(parentPos.getId());
                } else {
                    positionDetailBO.setParentCode(getRightTypeCell(parentCodeCell).toString());
                }
            } else {
                if (parentNameCell != null && StringUtils.isNotBlank(getRightTypeCell(parentNameCell).toString())) {
                    int parentNameX = parentNameCell.getColumnIndex();
                    QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
                    queryWrapper.eq("name", getRightTypeCell(parentNameCell).toString());
                    queryWrapper.eq("valid", 1);
                    queryWrapper.eq("company_id", companyId);
                    int count = count(queryWrapper);
                    if (!parentNames.contains(getRightTypeCell(parentNameCell).toString()) && count == 0) {
                        ExcelUtils.removeComment(parentNameCell);
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(parentNameX, rowY, parentNameX, rowY, parentNameX, rowY, parentNameX, rowY));
                        comment.setString(new XSSFRichTextString(PositionErrorEnum.POSITION_ID_NOT_EXISTS.getMessage()));

                        parentNameCell.setCellComment(comment);
                        parentNameCell.setCellStyle(cellStyle);
                        flag = false;
                    } else if (count > 1) {
                        ExcelUtils.removeComment(parentNameCell);
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(parentNameX, rowY, parentNameX, rowY, parentNameX, rowY, parentNameX, rowY));
                        comment.setString(new XSSFRichTextString(Constants.POSITION_NAME_DUP_CODE_NECESSARY_ERROR));

                        parentNameCell.setCellComment(comment);
                        parentNameCell.setCellStyle(cellStyle);
                        flag = false;
                        isDupParent = true;
                    } else {
                        if (parentNames.contains(getRightTypeCell(parentNameCell).toString()) && count >= 1) {
                            ExcelUtils.removeComment(parentNameCell);
                            Comment comment = drawing.createCellComment(new XSSFClientAnchor(parentNameX, rowY, parentNameX, rowY, parentNameX, rowY, parentNameX, rowY));
                            comment.setString(new XSSFRichTextString(Constants.POSITION_NAME_DUP_CODE_NECESSARY_ERROR));

                            parentNameCell.setCellComment(comment);
                            parentNameCell.setCellStyle(cellStyle);
                            flag = false;
                            isDupParent = true;
                        } else if (parentNames.indexOf(getRightTypeCell(parentNameCell).toString()) != parentNames.lastIndexOf(getRightTypeCell(parentNameCell).toString())) {
                            ExcelUtils.removeComment(parentNameCell);
                            Comment comment = drawing.createCellComment(new XSSFClientAnchor(parentNameX, rowY, parentNameX, rowY, parentNameX, rowY, parentNameX, rowY));
                            comment.setString(new XSSFRichTextString(Constants.POSITION_NAME_DUP_CODE_NECESSARY_ERROR));

                            parentNameCell.setCellComment(comment);
                            parentNameCell.setCellStyle(cellStyle);
                            flag = false;
                            isDupParent = true;
                        } else {
                            if (count == 1) {
                                PositionAddPO parentPos = getOne(queryWrapper);
                                positionDetailBO.setParentId(parentPos.getId());
                            } else {
                                positionDetailBO.setParentCode(parentCodes.get(parentNames.indexOf(getRightTypeCell(parentNameCell).toString())));
                            }
                        }
                    }
                }
            }
            //岗位名称
            Cell nameCell = curRow.getCell(titleMap.get("name"));

            if (nameCell == null) {
                nameCell = curRow.createCell(titleMap.get("name"), CellType.STRING);
                int nameX = nameCell.getColumnIndex();
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(nameX, rowY, nameX, rowY, nameX, rowY, nameX, rowY));
                comment.setString(new XSSFRichTextString(Constants.POSITION_PARAM_NAME_NECESSARY));
                nameCell.setCellComment(comment);
                nameCell.setCellStyle(cellStyle);
                flag = false;
            } else if (nameCell != null && StringUtils.isBlank(getRightTypeCell(nameCell).toString())) {
                int nameX = nameCell.getColumnIndex();
                ExcelUtils.removeComment(nameCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(nameX, rowY, nameX, rowY, nameX, rowY, nameX, rowY));
                comment.setString(new XSSFRichTextString(Constants.POSITION_PARAM_NAME_NECESSARY));

                nameCell.setCellComment(comment);
                nameCell.setCellStyle(cellStyle);
                flag = false;
            } else if (getRightTypeCell(nameCell).toString().length() > 200) {
                int nameX = nameCell.getColumnIndex();
                ExcelUtils.removeComment(nameCell);
                Comment comment = drawing.createCellComment(drawing.createAnchor(nameX, rowY, nameX, rowY, nameX, rowY, nameX, rowY));
                comment.setString(new XSSFRichTextString(Constants.POSITION_PARAM_NAME_LENGTH_ERROR));

                nameCell.setCellComment(comment);
                nameCell.setCellStyle(cellStyle);
                flag = false;

            } else {
                if (!isDupParent) {
                    int nameX = nameCell.getColumnIndex();
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
                        ExcelUtils.removeComment(nameCell);
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(nameX, rowY, nameX, rowY, nameX, rowY, nameX, rowY));
                        comment.setString(new XSSFRichTextString(Constants.POSITION_PARENT_NAME_ONLY));

                        nameCell.setCellComment(comment);
                        nameCell.setCellStyle(cellStyle);
                        flag = false;
                    } else {
                        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
                        queryWrapper.eq("company_id", companyId);
                        if (positionDetailBO.getParentId() == null) {
                            queryWrapper.isNull("parent_id");
                        } else {
                            queryWrapper.eq("parent_id", positionDetailBO.getParentId());
                        }
                        if (codeCell != null && StringUtils.isNotBlank(getRightTypeCell(codeCell).toString())) {
                            queryWrapper.ne("code", getRightTypeCell(codeCell).toString());
                        }
                        queryWrapper.eq("name", getRightTypeCell(nameCell).toString());
                        queryWrapper.eq("valid", 1);
                        int depCount = count(queryWrapper);
                        if (depCount > 0) {
                            ExcelUtils.removeComment(nameCell);
                            Comment comment = drawing.createCellComment(new XSSFClientAnchor(nameX, rowY, nameX, rowY, nameX, rowY, nameX, rowY));
                            comment.setString(new XSSFRichTextString(Constants.DEPARTMENT_PARENT_NAME_ONLY));

                            nameCell.setCellComment(comment);
                            nameCell.setCellStyle(cellStyle);
                            flag = false;
                        } else {
                            positionDetailBO.setName(getRightTypeCell(nameCell).toString());
                        }
                    }
                }

            }

            Cell roleCodeCell = curRow.getCell(titleMap.get("roleCode"));
            if (roleCodeCell != null && StringUtils.isNotBlank(getRightTypeCell(roleCodeCell).toString())) {
                String roleCodes = getRightTypeCell(roleCodeCell).toString();
                String[] roleCodeArr = roleCodes.split(",");
                List<String> roleCodeSet = new ArrayList<String>();
                for (String roleCode : roleCodeArr) {
                    if (StringUtils.isNotBlank(roleCode) && !roleCodeSet.contains(roleCode)) {
                        roleCodeSet.add(roleCode);
                    }
                }
                List<RoleDTO> roleList = organizationAdapter.findRoleByCodes(roleCodeSet);
                if (roleList != null && roleList.size() > 0) {
                    List<Long> roleIds = new ArrayList<Long>();
                    roleList.stream().forEach(roleDTO -> roleIds.add(roleDTO.getId()));
                    positionDetailBO.setRoleIds(roleIds);
                }
            }
            Cell descCell = curRow.getCell(titleMap.get("description"));

            if (descCell != null && StringUtils.isNotBlank(getRightTypeCell(descCell).toString()) && getRightTypeCell(descCell).toString().length() > 500) {
                int descX = descCell.getColumnIndex();
                ExcelUtils.removeComment(descCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(descX, rowY, descX, rowY, descX, rowY, descX, rowY));
                comment.setString(new XSSFRichTextString(Constants.POSITION_PARAM_DESC_LENGTH_ERROR));

                descCell.setCellComment(comment);
                descCell.setCellStyle(cellStyle);
                flag = false;
            } else if (descCell != null) {
                positionDetailBO.setDescription(getRightTypeCell(descCell).toString());
            }

            if (flag) {
                importPos.add(positionDetailBO);
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
     */
    @Override
    public void downlowdExcelTemplate(File file) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        ExcelUtils.createExplainSheet(ExcelUtils.POSITION_TEMPLATE_EXPLAIN, workbook);
        Sheet depSheet = workbook.createSheet(Constants.POSITION_DATA_SHEETNAME);


        for (int i = 0; i < ExcelUtils.POSITION_IMPORT_TEMPLATE.size(); i++) {
            depSheet.setColumnWidth(i, ExcelUtils.COLUMN_LENGTH);
        }
        ExcelUtils.createHeadComments(depSheet, ExcelUtils.POSITION_IMPORT_TEMPLATE);
        //setComment(personSheet, PERSON_COMMENT, language);
        OutputStream outputStream = new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.flush();
    }

    /**
     * 导出岗位数据
     * @param ids
     * @param all
     * @param companyId
     */
    @Override
    public void exportExcelData(List<Long> ids, Boolean all, Long taskId, Long companyId) {
        ExcelPO excelPO = excelMapper.selectById(taskId);
        excelPO.setStatus(1);
        //excelMapper.updateById(excelPO);
        excelManageService.excuteExcelState(excelPO);
        XSSFWorkbook workbook = new XSSFWorkbook();
        ExcelUtils.createExplainSheet(ExcelUtils.POSITION_TEMPLATE_EXPLAIN, workbook);
        Sheet sheet = workbook.createSheet(Constants.POSITION_DATA_SHEETNAME);
        ExcelUtils.createHeadComments(sheet, ExcelUtils.POSITION_IMPORT_TEMPLATE);
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
        queryWrapper.eq("company_id", companyId);
        queryWrapper.eq("valid", 1);
        if (!all) {
            queryWrapper.in("id", ids);
        }
        List<PositionAddPO> poses = list(queryWrapper);
        if (poses == null || poses.size() == 0) {
            try {
                String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.POSITION_FILE, taskId);
                excelPO.setFileName(filePath);
                excelPO.setStatus(2);
                //excelMapper.updateById(excelPO);
                return;
            } catch (IOException e) {
                log.warn("岗位导出异常:" + e);
                excelPO.setStatus(3);
                //excelMapper.updateById(excelPO);
                return;
            } finally {
                excelManageService.excuteExcelState(excelPO);
            }
        }
        List<PositionExcelBO> list = new ArrayList<PositionExcelBO>();
        poses.stream().forEach(pos -> {
            PositionExcelBO positionExcelBO = new PositionExcelBO();
            BeanUtils.copyProperties(pos, positionExcelBO);
            if (pos.getParentId() != null) {
                PositionAddPO parentPos = getById(pos.getParentId());
                if (parentPos != null) {
                    positionExcelBO.setParentCode(parentPos.getCode());
                    positionExcelBO.setParentName(parentPos.getName());
                }
            }
            QueryWrapper<OrganizationManagerPO> managerWrapper = new QueryWrapper<OrganizationManagerPO>();
            managerWrapper.eq("org_id", pos.getId());
            managerWrapper.eq("manager_type", Constants.POSITION);
            List<OrganizationManagerPO> orgManagers = organizationManagerMapper.selectList(managerWrapper);
            if (orgManagers != null && orgManagers.size() > 0) {
                List<OrganizationManagerBO> managers = new ArrayList<OrganizationManagerBO>();
                orgManagers.stream().forEach(orgManager -> {
                    OrganizationManagerBO organizationManagerBO = new OrganizationManagerBO();
                    BeanUtils.copyProperties(orgManager, organizationManagerBO);
                    managers.add(organizationManagerBO);
                });
                positionExcelBO.setManagers(managers);
            }

            DepartmentAddPO departmentAddPO = departmentMapper.selectById(pos.getDepId());
            if (departmentAddPO != null) {
                positionExcelBO.setDeptCode(departmentAddPO.getCode());
                positionExcelBO.setDeptName(departmentAddPO.getName());
            }
            QueryWrapper<PositionRolePO> roleWrapper = new QueryWrapper<PositionRolePO>();
            roleWrapper.eq("position_id", pos.getId());
            List<PositionRolePO> rolesRelations = positionRoleMapper.selectList(roleWrapper);
            if (rolesRelations != null && rolesRelations.size() > 0) {
                List<Long> roleIds = new ArrayList<Long>();
                rolesRelations.stream().forEach(roleR -> roleIds.add(roleR.getRoleId()));
                List<RoleDTO> roles = organizationAdapter.findRoleByIds(roleIds);
                if (roles != null && roles.size() > 0) {
                    List<String> roleCodes = new ArrayList<String>();
                    List<String> roleNames = new ArrayList<String>();
                    roles.stream().forEach(role -> {
                        roleCodes.add(role.getCode());
                        roleNames.add(role.getName());
                    });
                    positionExcelBO.setRoleCodes(roleCodes);
                    positionExcelBO.setRoleNames(roleNames);
                }
            }
            list.add(positionExcelBO);
        });
        int rowIndex = 1;
        for (PositionExcelBO pos : list) {
            Row row = sheet.createRow(rowIndex);
            createCell(row, pos);
            rowIndex++;
        }
        try {
            String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.PERSON_FILE, taskId);
            excelPO.setFileName(filePath);
            excelPO.setStatus(2);
            //excelMapper.updateById(excelPO);
        } catch (IOException e) {
            log.warn("岗位导出异常:" + e);
            excelPO.setStatus(3);
            //excelMapper.updateById(excelPO);
            return;
        } finally {
            excelManageService.excuteExcelState(excelPO);
        }

    }

    private void createCell(Row curRow, PositionExcelBO pos) {

        curRow.createCell(0, CellType.STRING).setCellValue(pos.getCode());
        curRow.createCell(1, CellType.STRING).setCellValue(pos.getName());
        curRow.createCell(2, CellType.STRING).setCellValue(pos.getDeptCode());
        curRow.createCell(3, CellType.STRING).setCellValue(pos.getDeptName());
        curRow.createCell(4, CellType.STRING).setCellValue(pos.getParentCode());
        curRow.createCell(5, CellType.STRING).setCellValue(pos.getParentName());
/*        if (pos.getManagers() != null && pos.getManagers().size() > 0) {
            StringBuilder managerCodes = new StringBuilder();
            StringBuilder managerNames = new StringBuilder();
            for (int i = 0; i < pos.getManagers().size(); i++) {
                OrganizationManagerBO organizationManagerBO = dept.getManagers().get(i);
                PersonAddPO personAddPO = personMapper.selectById(organizationManagerBO.getManagerId());
                if (personAddPO != null) {
                    if (i < (pos.getManagers().size() - 1)) {
                        managerCodes.append(personAddPO.getCode()).append(",");
                    } else {
                        managerCodes.append(personAddPO.getCode());
                    }
                }
                if (i < (pos.getManagers().size() - 1)) {
                    managerNames.append(organizationManagerBO.getManagerName()).append(",");
                } else {
                    managerNames.append(organizationManagerBO.getManagerName());
                }
            }
            curRow.createCell(4, CellType.STRING).setCellValue(managerCodes.toString());
            curRow.createCell(5, CellType.STRING).setCellValue(managerNames.toString());
        }*/
        if (pos.getRoleCodes() != null && pos.getRoleCodes().size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pos.getRoleCodes().size(); i++) {
                if (i < (pos.getRoleCodes().size() - 1)) {
                    sb.append(pos.getRoleCodes().get(i)).append(",");
                } else {
                    sb.append(pos.getRoleCodes().get(i));
                }
            }
            curRow.createCell(6, CellType.STRING).setCellValue(sb.toString());
        }
        if (pos.getRoleNames() != null && pos.getRoleNames().size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pos.getRoleNames().size(); i++) {
                if (i < (pos.getRoleNames().size() - 1)) {
                    sb.append(pos.getRoleNames().get(i)).append(",");
                } else {
                    sb.append(pos.getRoleNames().get(i));
                }
            }
            curRow.createCell(7, CellType.STRING).setCellValue(sb.toString());
        }
        curRow.createCell(8, CellType.STRING).setCellValue(pos.getDescription());

    }

    /**
     * 导出岗位关联人员
     * @param ids
     * @param all
     * @param taskId
     * @param posId
     */
    @Override
    public void exportPersonExcelData(List<Long> ids, Boolean all, Long taskId, Long posId) {
        ExcelPO excelPO = excelMapper.selectById(taskId);
        excelPO.setStatus(1);
        excelMapper.updateById(excelPO);
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet(Constants.PERSON_DATA_SHEETNAME);
            ExcelUtils.createHeadComments(sheet, ExcelUtils.RELATION_PERSON_EXPORT);
            List<Long> personIds = new ArrayList<Long>();
            if (!all) {
                if (ids == null || ids.size() == 0) {
                    try {
                        String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.POSITION_RELATION_FILE, taskId);
                        excelPO.setFileName(filePath);
                        excelPO.setStatus(2);
                        //excelMapper.updateById(excelPO);
                        return;
                    } catch (IOException e) {
                        log.warn("岗位人员导出异常:" + e);
                        excelPO.setStatus(3);
                        //excelMapper.updateById(excelPO);
                        return;
                    } finally {
                        excelManageService.excuteExcelState(excelPO);
                    }

                }
                personIds = ids;
            } else {
                QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
                posWrapper.eq("id", posId);
                posWrapper.eq("valid", 1);
                List<PositionAddPO> posList = list(posWrapper);
                if (posList == null || posList.size() == 0) {
                    try {
                        String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.POSITION_RELATION_FILE, taskId);
                        excelPO.setFileName(filePath);
                        excelPO.setStatus(2);
                        //excelMapper.updateById(excelPO);
                        return;
                    } catch (IOException e) {
                        log.warn("岗位导出异常:" + e);
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
                        String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.POSITION_RELATION_FILE, taskId);
                        excelPO.setFileName(filePath);
                        excelPO.setStatus(2);
                        //excelMapper.updateById(excelPO);
                        return;
                    } catch (IOException e) {
                        log.warn("岗位导出异常:" + e);
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
            personWrapper.in("id", personIds);
            personWrapper.eq("valid", 1);
            List<PersonAddPO> personList = personMapper.selectList(personWrapper);
            if (personList == null || personList.size() == 0) {
                try {
                    String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.POSITION_RELATION_FILE, taskId);
                    excelPO.setFileName(filePath);
                    excelPO.setStatus(2);
                    //excelMapper.updateById(excelPO);
                    return;
                } catch (IOException e) {
                    log.warn("岗位导出异常:" + e);
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
                String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.POSITION_RELATION_FILE, taskId);
                excelPO.setFileName(filePath);
                excelPO.setStatus(2);
                //excelMapper.updateById(excelPO);
            } catch (IOException e) {
                log.warn("岗位导出异常:" + e);
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
                log.warn("岗位导出异常:" + e);
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
    public List<PositionDetailBO> queryPosInfoByIds(List<Long> ids) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
//        queryWrapper.in("id", ids);
//        List<PositionAddPO> list = list(queryWrapper);
        List<PositionAddPO> list = MultiParamSql.forPosition(ids, queryWrapper, "id", positionMapper);
        if (list == null || list.size() == 0) {
            return null;
        }
        List<PositionDetailBO> bos = new ArrayList<PositionDetailBO>();
        list.stream().forEach(po -> {
            PositionDetailBO posDetailBO = new PositionDetailBO();
            BeanUtils.copyProperties(po, posDetailBO);
            bos.add(posDetailBO);
        });
        return bos;
    }
    @Override
    public PositionDetailBO getPosDetailByCode(String code) {

        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
        queryWrapper.eq("code", code);
        queryWrapper.eq("valid", 1);
        queryWrapper.eq("sys_flag", 0);
        int count = count(queryWrapper);
        //判断该id的部门是否存在
        if (count == 0) {
            throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
        }

        PositionAddPO curPos = getOne(queryWrapper);
        Long posId = curPos.getId();
        PositionDetailBO detailPO = new PositionDetailBO();
        BeanUtils.copyProperties(curPos, detailPO);
        QueryWrapper<DepartmentAddPO> depWrapper = new QueryWrapper<DepartmentAddPO>();
        depWrapper.eq("id", detailPO.getDepId());
        depWrapper.eq("valid", 1);
        DepartmentAddPO depPo = departmentMapper.selectOne(depWrapper);
        detailPO.setDepName(depPo.getName());
        CompanyPO company = companyMapper.selectById(curPos.getCompanyId());
        detailPO.setFullPath(OrgBaseUtils.splitCompanyFullPath(detailPO.getFullPath(), company.getShortName(), company.getFullPath()));
        QueryWrapper<OrganizationManagerPO> managerWrapper = new QueryWrapper<OrganizationManagerPO>();
        managerWrapper.eq("org_id", posId);
        managerWrapper.eq("manager_type", Constants.POSITION);
        List<OrganizationManagerPO> managers = organizationManagerMapper.selectList(managerWrapper);
        if (managers == null) {
            return detailPO;
        }
        List<OrganizationManagerBO> list = new ArrayList<OrganizationManagerBO>();
        managers.stream().forEach(manager -> {
            OrganizationManagerBO organizationManagerBO = new OrganizationManagerBO();
            BeanUtils.copyProperties(manager, organizationManagerBO);
            list.add(organizationManagerBO);
        });
        detailPO.setManagers(list);
        return detailPO;
    }

    @Override
    public List<PositionDetailBO> queryPositionByCodes(List<String> codes) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
        queryWrapper.in("code", codes);
        queryWrapper.eq("valid", true);
        List<PositionAddPO> list = list(queryWrapper);
        if (list == null || list.size() == 0) {
            return new ArrayList<PositionDetailBO>();
        }
        List<PositionDetailBO> bos = new ArrayList<PositionDetailBO>();
        list.stream().forEach(pos -> {
            PositionDetailBO posDetailBO = new PositionDetailBO();
            BeanUtils.copyProperties(pos, posDetailBO);
            bos.add(posDetailBO);
        });
        return bos;
    }

    @Override
    public List<Long> querySubPositionIdsByPositionId(List<Long> ids) {
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
        posWrapper.in("id", ids);
        List<PositionAddPO> parentPoses = list(posWrapper);

        if (parentPoses == null || parentPoses.size() == 0) {
            return new ArrayList<Long>();
        }
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
        queryWrapper.eq("valid", 1);

        queryWrapper.and(item -> {

            for (int i = 0; i < parentPoses.size(); i++) {
                item.or().likeRight("full_path", parentPoses.get(i).getFullPath());
            }

        });
        List<PositionAddPO> positions = list(queryWrapper);
        if (positions == null || positions.size() == 0) {
            return new ArrayList<Long>();
        }
        List<Long> subIds = new ArrayList<Long>();
        positions.stream().forEach(pos -> {
            if (!ids.contains(pos.getId())) {
                subIds.add(pos.getId());
            }
        });
        return subIds;
    }

    @Override
    public  boolean updateBatchById(Collection<PositionAddPO> entityList){
        return super.updateBatchById(entityList);
    }

    @Override
    public JSONObject getPositionById(Long id, String includes) {
        PositionAddPO positionAddPO = getById(id);
        if (positionAddPO == null) {
            return new JSONObject();
        }
        PositionBaseServiceBO positionBaseServiceBO = new PositionBaseServiceBO();
        BeanUtils.copyProperties(positionAddPO, positionBaseServiceBO);
        JSONObject posJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(positionBaseServiceBO), includes, Constants.POSITION);
        /*CompanyPO companyPO = companyMapper.selectById(positionAddPO.getCompanyId());
        if (companyPO == null) {
            return posJson;
        }
        CompanyBaseServiceBO companyBaseServiceBO = new CompanyBaseServiceBO();
        BeanUtils.copyProperties(companyPO, companyBaseServiceBO);
        JSONObject companyJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(companyBaseServiceBO), includes, Constants.COMPANY);
        posJson.put("company", companyJson);*/
        return posJson;
    }

    @Override
    public List<JSONObject> getPosTree(Long treeId, Long companyId) {
        /*PositionAddPO parentPos = getById(treeId);
        if (parentPos == null) {
            return new ArrayList<JSONObject>();
        }
        if (parentPos.getLeaf()) {
            return new ArrayList<JSONObject>();
        }*/
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("company_id", companyId);
        if (treeId == -1) {
            queryWrapper.isNull("parent_id");
        } else {
            queryWrapper.eq("parent_id", treeId);
        }
        queryWrapper.eq("valid", 1);
        queryWrapper.eq("sys_flag", 0);
        List<PositionAddPO> poses = list(queryWrapper);
        if (poses == null || poses.size() == 0) {
            return new ArrayList<JSONObject>();
        }
        List<JSONObject> posList = new ArrayList<>();
        poses.stream().forEach(pos -> {
            PositionBaseServiceBO positionBaseServiceBO = new PositionBaseServiceBO();
            BeanUtils.copyProperties(pos, positionBaseServiceBO);
            JSONObject posJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(positionBaseServiceBO), null, Constants.POSITION);
            if (pos.getLeaf()) {
                posJson.put("isParent", false);
            } else {
                posJson.put("isParent", true);
            }
            posList.add(posJson);
        });
        return posList;
    }

    @Override
    public List<PositionDetailBO> querySubPositionByParentId(Long id, Boolean all, Long cid) {
        QueryWrapper<PositionAddPO> subPosWrapper = new QueryWrapper<>();
        subPosWrapper.eq("valid", 1);
        subPosWrapper.eq("sys_flag", 0);
        if (id != null) {
            PositionAddPO positionAddPO = getById(id);
            if (positionAddPO == null) {
                return null;
            }
            if (all != null && all) {
                subPosWrapper.likeRight("full_path", positionAddPO.getFullPath() + "/");
            } else {
                subPosWrapper.eq("parent_id", positionAddPO.getId());
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
        List<PositionAddPO> list = list(subPosWrapper);
        if (list == null || list.size() == 0) {
            return null;
        }
        List<PositionDetailBO> positionDetailBOS = new ArrayList<>();
        list.stream().forEach(pos -> {
            PositionDetailBO positionDetailBO = new PositionDetailBO();
            BeanUtils.copyProperties(pos, positionDetailBO);
            positionDetailBOS.add(positionDetailBO);
        });
        return positionDetailBOS;
    }

    //======================================old version============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOldPosition(JSONObject body, String tenantId) {
        QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
        comWrapper.eq("old_id", body.getString("root"));

        CompanyPO companyPO = companyMapper.selectOne(comWrapper);
        if (companyPO == null) {
            throw new OrganizationException(400, Constants.COMPANY_NOT_EXISTS);
        }
        PositionAddPO positionAddPO = new PositionAddPO();
        positionAddPO.setCode(body.getString("code"));
        positionAddPO.setName(body.getString("showName"));
        positionAddPO.setCompanyId(companyPO.getId());
        if (body.getDouble("sequenceNumber") != null) {
            positionAddPO.setSort(body.getDouble("sequenceNumber"));
        }
        if (body.getJSONObject("parent") != null && StringUtils.isNotBlank(body.getJSONObject("parent").getString("name")) && !body.getJSONObject("parent").getString("name").equals(Constants.ORG_OLD_NAME)) {
            String parentOldId = body.getJSONObject("parent").getString("name");
            QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
            posWrapper.eq("old_id", parentOldId);
            PositionAddPO parentPos = getOne(posWrapper);
            if (parentPos == null) {
                throw new OrganizationException(400, Constants.POSITION_NOT_EXISTS);
            }
            positionAddPO.setParentId(parentPos.getId());
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
        String oldId = "Position_" + uid;
        positionAddPO.setId(uid);
        positionAddPO.setOldId(oldId);
        positionAddPO.setDepId(0L);
        positionAddPO.setValid(true);
        addPosition(positionAddPO, null, tenantId);
    }

    @Override
    public List<PersonDetailBO> queryPositionUsers(Long companyId, Long positionId, String keyword, Boolean onlyUser) {
        CompanyPO companyPO = companyMapper.selectById(companyId);
        List<Long> positionIds = new ArrayList<>();
        if (positionId != null && !companyId.equals(positionId)) {
            //查岗位下的人员
            positionIds.add(positionId);
        } else {
            //查公司全部
            QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("company_id", companyId);
            queryWrapper.eq("valid", 1);
            queryWrapper.eq("sys_flag", 0);
            List<PositionAddPO> positions = list(queryWrapper);
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
        List<PositionAddPO> positions = listByIds(positionIds);
        //岗位id和岗位对应关系
        Map<Long, PositionAddPO> posIdToPO = new HashMap<>();
        QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<>();
        positions.stream().forEach(pos -> {
            deptWrapper.or().eq("id", pos.getDepId());
            posIdToPO.put(pos.getId(), pos);
        });
        List<DepartmentAddPO> depts = departmentMapper.selectList(deptWrapper);
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
            List<RelationDepartmentBO> relationDepartmentBOList = new ArrayList<>();

            List<MainPositionBO> positionList = new ArrayList<>();
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
                mainPositionBO.setId(positionAddPO.getId());
                mainPositionBO.setCompanyId(positionAddPO.getCompanyId());
                mainPositionBO.setFullPath(OrgBaseUtils.splitCompanyFullPath(positionAddPO.getFullPath(), companyPO.getShortName(), companyPO.getFullPath()));
                if (posId.equals(person.getMainPosition())) {
                    mainPositionBO.setMainPosition(true);
                }
                DepartmentAddPO departmentAddPO = depIdToPO.get(positionAddPO.getDepId());
                RelationDepartmentBO relationDepartmentBO = new RelationDepartmentBO();
                relationDepartmentBOList.add(relationDepartmentBO);
                departmentList.add(relationDepartmentBO);
                relationDepartmentBO.setId(departmentAddPO.getId());
                relationDepartmentBO.setCompanyId(departmentAddPO.getCompanyId());
                relationDepartmentBO.setFullPath(departmentAddPO.getFullPath());
                if (positionId != null && posId.equals(positionId)) {
                    relationDepartmentBO.setCurrentPosition(true);
                }
            });
        });
        return personDetailBOS;
    }

    @Override
    public PageResult<PersonDetailBO> queryPositionUsers1(Long companyId, Long positionId, String keyword, Boolean onlyUser, Integer current, Integer pageSize) {
        keyword = ObjectUtils.isEmpty(keyword) ? "" : "%" + keyword + "%";
        String dbType = this.dataId.getDataId();

        List<Long> personIds = Lists.newArrayList();
        //查询岗位下用户
        Integer total = 0;
         if (null != positionId && !companyId.equals(positionId)) {
            List<PositionPersonPO> positionPersonPOS = personMapper.queryUserOfPosition(positionId, keyword, onlyUser, current, pageSize, dbType);
            if (!ObjectUtils.isEmpty(positionPersonPOS)) {
                personIds = positionPersonPOS.stream().map(PositionPersonPO::getPersonId).collect(Collectors.toList());
            }
            total = personMapper.totalUserOfPosition(positionId, keyword, onlyUser);
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
        QueryWrapper<DepartmentPersonPO> departmentPersonWrapper = new QueryWrapper<>();
        departmentPersonWrapper.in("person_id", personIds);
        departmentPersonWrapper.eq("valid", 1);
        List<DepartmentPersonPO> departmentPersonPOS = departmentPersonMapper.selectList(departmentPersonWrapper);
        Map<Long, List<Long>> personDepartmentIdMap = new HashMap<>();
        for (DepartmentPersonPO departmentPersonPO : departmentPersonPOS) {
            List<Long> departmentIds = personDepartmentIdMap.get(departmentPersonPO.getPersonId());
            Long deptId = departmentPersonPO.getDeptId();
            if (null == departmentIds) {
                List<Long> temp = Lists.newArrayList();
                temp.add(deptId);
                personDepartmentIdMap.put(departmentPersonPO.getPersonId(), temp);
            } else {
                if (!departmentIds.contains(deptId)) {
                    departmentIds.add(departmentPersonPO.getDeptId());
                }
            }
        }

        //岗位信息
        Map<Long, List<PositionAddPO>> positionMap = new HashMap<>();
        if (!ObjectUtils.isEmpty(positionId)) {
            QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<>();
            positionWrapper.eq("id", positionId);
            positionWrapper.eq("valid", 1);
            PositionAddPO positionAddPO = getOne(positionWrapper);
            if (!ObjectUtils.isEmpty(positionAddPO)) {
                personIds.forEach(personId -> positionMap.put(personId, Collections.singletonList(positionAddPO)));
            }
        } else {
            for (Long personId : personIds) {
                QueryWrapper<PositionPersonPO> positionPersonWrapper = new QueryWrapper<>();
                positionPersonWrapper.select("position_id");
                positionPersonWrapper.eq("person_id", personId);
                positionPersonWrapper.eq("valid", 1);
                List<PositionPersonPO> positionPersonPOS = positionPersonMapper.selectList(positionPersonWrapper);
                if (!ObjectUtils.isEmpty(positionPersonPOS)) {
                    List<Long> positionIds = positionPersonPOS.stream().map(PositionPersonPO::getPositionId).distinct().collect(Collectors.toList());
                    QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<>();
                    positionWrapper.in("id", positionIds);
                    positionWrapper.eq("valid", 1);
                    List<PositionAddPO> positionAddPOS = list(positionWrapper);
                    positionMap.put(personId, positionAddPOS);
                }
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
            List<Long> departmentIds = personDepartmentIdMap.get(personAddPO.getId());
            List<DepartmentAddPO> departmentAddPOS = new ArrayList<>();
            if (!ObjectUtils.isEmpty(departmentIds)) {
                QueryWrapper<DepartmentAddPO> departmentWrapper = new QueryWrapper<>();
                departmentWrapper.in("id", departmentIds);
                departmentWrapper.eq("valid", 1);
                departmentAddPOS = departmentMapper.selectList(departmentWrapper);
            }
            List<RelationDepartmentBO> departmentBOS = Lists.newArrayList();
            for (DepartmentAddPO departmentAddPO : departmentAddPOS) {
                RelationDepartmentBO relationDepartmentBO = new RelationDepartmentBO();
                relationDepartmentBO.setCompanyId(departmentAddPO.getCompanyId());
                relationDepartmentBO.setId(departmentAddPO.getId());
                relationDepartmentBO.setFullPath(departmentAddPO.getFullPath());
                departmentBOS.add(relationDepartmentBO);
            }
            personDetailBO.setDepartment(departmentBOS);
            personDetailBO.setDepartmentFullPath(departmentBOS);

            //岗位
            List<MainPositionBO> positionList = new ArrayList<>();
            personDetailBO.setPosition(positionList);
            personDetailBO.setPositionFullPath(positionList);
            List<PositionAddPO> positionAddPOS = positionMap.get(personAddPO.getId());
            if (!ObjectUtils.isEmpty(positionAddPOS)) {
                for (PositionAddPO positionAddPO : positionAddPOS) {
                    MainPositionBO mainPositionBO = new MainPositionBO();
                    mainPositionBO.setCompanyId(positionAddPO.getCompanyId());
                    mainPositionBO.setId(positionAddPO.getId());
                    mainPositionBO.setFullPath(OrgBaseUtils.splitCompanyFullPath(positionAddPO.getFullPath(), companyPO.getShortName(), companyPO.getFullPath()));
                    if (positionAddPO.getId().equals(personAddPO.getMainPosition())) {
                        mainPositionBO.setMainPosition(true);
                    }
                    positionList.add(mainPositionBO);
                }
            }

        }

        return new PageResult<>(personDetailBOS, total, pageSize, current);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addVirtualPos(Long companyId, Long depId, String tenantId) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PositionAddPO::getCompanyId, companyId).eq(PositionAddPO::getValid, true).eq(PositionAddPO::getSysFlag, true);
        PositionAddPO virtualPos = getOne(queryWrapper);
        if (virtualPos != null) {
            return virtualPos.getId();
        }
        PositionAddPO positionAddPO = new PositionAddPO();
        positionAddPO.setName("虚拟岗位");
        positionAddPO.setCode("default_position_" + companyId);
        positionAddPO.setCompanyId(companyId);
        positionAddPO.setSysFlag(true);
        positionAddPO.setDepId(depId);

        addPosition(positionAddPO, null, tenantId);
        return positionAddPO.getId();
    }


    @Override
    public PageResult<PositionSynchronizationInfoBO> getPositionsByPage(Integer current, Integer pageSize) {
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();

        posWrapper.eq("valid", true);
        posWrapper.eq("sys_flag", 0);
        // 默认按照升序排序
        //posWrapper.orderByAsc("layNo");
        //posWrapper.orderByAsc("sort");

        int count = count(posWrapper);
        Pagination pagination = new Pagination();
        pagination.setCurrent(current);
        pagination.setPageSize(pageSize);
        pagination.setTotal(count);

        if (count == 0) {
            PageResult<PositionSynchronizationInfoBO> res = new PageResult<>(null, count, pageSize, current);
            res.setPagination(pagination);
            return res;
        }
        Page<PositionAddPO> pageInfo = new Page<PositionAddPO>(current, pageSize, count);

        page(pageInfo, posWrapper);
        PageResult<PositionAddPO> pageResult = new PageResult<>(pageInfo.getRecords(), count, pageSize, current);

        PageResult<PositionSynchronizationInfoBO> res = new PageResult<>();
        List<PositionSynchronizationInfoBO> posSynInfoBoList = new ArrayList<>();
        pageResult.getList().stream().forEach(item ->{
            PositionSynchronizationInfoBO positionSynInfoBO = new PositionSynchronizationInfoBO();
            BeanUtils.copyProperties(item, positionSynInfoBO);

            // 上级岗位编码
            if (item.getParentId() != null){
                PositionAddPO positionAddPO = positionMapper.selectById(item.getParentId());
                positionSynInfoBO.setParentCode(positionAddPO.getCode());
            }

            // 公司
            CompanyForPositionSynchronizationInfoBO companyInfoBO = new CompanyForPositionSynchronizationInfoBO();
            CompanyPO companyPO = companyMapper.selectById(item.getCompanyId());
            companyInfoBO.setCode(companyPO.getCode());
            companyInfoBO.setFullName(companyPO.getFullName());
            companyInfoBO.setShortName(companyPO.getShortName());
            positionSynInfoBO.setCompany(companyInfoBO);

            // 部门
            DepartmentForPositionSynchronizationInfoBO departmentInfoBo = new DepartmentForPositionSynchronizationInfoBO();
            DepartmentAddPO departmentAddPO = departmentMapper.selectById(item.getDepId());
            departmentInfoBo.setCode(departmentAddPO.getCode());
            departmentInfoBo.setName(departmentAddPO.getName());
            positionSynInfoBO.setDepartment(departmentInfoBo);

            posSynInfoBoList.add(positionSynInfoBO);
        });
        res.setList(posSynInfoBoList);
        res.setPagination(pagination);
        return res;
    }

    @Override
    public PageResult<PositionDetailInfoBO> querySubPositionByParentCode(String positionCode, Boolean all, Integer current, Integer pageSize) {
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();
        posWrapper.eq("valid", true);
        posWrapper.eq("code", positionCode);
        PositionAddPO positionAddPO = getOne(posWrapper);
        if (null == positionAddPO) {
            throw new BizHttpStatusException(PositionErrorEnum.POSITION_ID_NOT_EXISTS, 400);
        }

        Long parentId = positionAddPO.getId();
        // Long cid = positionAddPO.getCompanyId();

        QueryWrapper<PositionAddPO> subPosWrapper = new QueryWrapper<>();
        subPosWrapper.eq("valid", true);
        subPosWrapper.eq("sys_flag", 0);

        if (all) {
            subPosWrapper.likeRight("full_path", positionAddPO.getFullPath() + "/");
        } else {
            subPosWrapper.eq("parent_id", parentId);
        }

        //List<PositionAddPO> list = list(subPosWrapper);
        // 排序
        subPosWrapper.orderByAsc("lay_no", "sort");
        Page<PositionAddPO> positionAddPOPage = new Page<>(current, pageSize);
        Page<PositionAddPO> positionResPage = positionMapper.selectPage(positionAddPOPage, subPosWrapper);
        List<PositionAddPO> list = positionResPage.getRecords();
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }

        List<PositionDetailInfoBO> positionDetailInfoBOS = new ArrayList<>();
        list.stream().forEach(pos -> {
            PositionDetailInfoBO positionDetailInfoBO = new PositionDetailInfoBO();
            BeanUtils.copyProperties(pos, positionDetailInfoBO);

            // parentCode
            Long parId = pos.getParentId();
            if (null != parId){
                PositionAddPO posAddPO = positionMapper.selectById(parId);
                positionDetailInfoBO.setParentCode(posAddPO.getCode());
            }

            // 公司
            CompanyPO companyPO = companyMapper.selectById(pos.getCompanyId());
            CompanyForPositionSynchronizationInfoBO companyInfoBo = new CompanyForPositionSynchronizationInfoBO();
            companyInfoBo.setCode(companyPO.getCode());
            companyInfoBo.setShortName(companyPO.getShortName());
            companyInfoBo.setFullName(companyPO.getFullName());

            positionDetailInfoBO.setCompany(companyInfoBo);

            // 部门
            DepartmentAddPO departmentAddPO = departmentMapper.selectById(pos.getDepId());
            DepartmentForPositionSynchronizationInfoBO departmentInfoBo = new DepartmentForPositionSynchronizationInfoBO();
            departmentInfoBo.setName(departmentAddPO.getName());
            departmentInfoBo.setCode(departmentAddPO.getCode());

            positionDetailInfoBO.setDepartment(departmentInfoBo);

            // 角色
            QueryWrapper<PositionRolePO> positionRolePOQueryWrapper = new QueryWrapper<>();
            positionRolePOQueryWrapper.eq("opr.position_id", pos.getId());
            List<Long> roleIds = positionMapper.getPositionRoleId(positionRolePOQueryWrapper);

            if (!CollectionUtils.isEmpty(roleIds)) {
                List<RoleDTO> roleDTOS = organizationAdapter.findRoleByIds(roleIds);
                List<PositionRoleBaseBO> positionRoleBaseBOS = roleDTOS.stream().map(roleDTO -> {
                    PositionRoleBaseBO positionRoleBaseBO = new PositionRoleBaseBO();
                    positionRoleBaseBO.setCode(roleDTO.getCode());
                    positionRoleBaseBO.setName(roleDTO.getName());
                    return positionRoleBaseBO;
                }).collect(Collectors.toList());
                positionDetailInfoBO.setRoles(positionRoleBaseBOS);
            }

            positionDetailInfoBOS.add(positionDetailInfoBO);
        });
        return new PageResult<PositionDetailInfoBO>(positionDetailInfoBOS, positionResPage.getTotal(), pageSize, current);
    }

    @Override
    public String getPositionCodeById(Long positionId) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("code");
        queryWrapper.eq("id", positionId);
        PositionAddPO positionAddPO = getOne(queryWrapper);
        if (!ObjectUtils.isEmpty(positionAddPO)) {
            return positionAddPO.getCode();
        }
        return "";
    }

    @Override
    public List<PositionAddPO> listDepartments() {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
        queryWrapper.eq("valid", 1);
        return list(queryWrapper);
    }

    @Override
    public PageResult<PositionAddPO> loadPositions(Integer current, Integer pageSize, Long fromTime) {
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();

        if (fromTime != null) {
            posWrapper.and(item -> {
                item.gt("modify_time", new Date(fromTime)).or().gt("create_time", new Date(fromTime));
            });
        } else {
            posWrapper.eq("valid", true);
        }
        posWrapper.eq("sys_flag", 0);
        int count = count(posWrapper);
        if (count == 0) {
            return new PageResult<>(null, count, pageSize, current);
        }
        Page<PositionAddPO> pageInfo = new Page<PositionAddPO>(current, pageSize, count);
        page(pageInfo, posWrapper);
        PageResult<PositionAddPO> pageResult = new PageResult<>(pageInfo.getRecords(), count, pageSize, current);
        return pageResult;
    }

    @Override
    public PageResult<PersonResultBO> queryPersonsByPositionId(Long positionIdId, Integer current, Integer pageSize) {
        QueryWrapper<PositionPersonPO> posPersonWrapper = new QueryWrapper<>();
        posPersonWrapper.lambda().eq(PositionPersonPO::getPositionId, positionIdId).eq(PositionPersonPO::getValid, true);
//        List<PositionPersonPO> posRelList = positionPersonMapper.selectList(posPersonWrapper);
        Page<PositionPersonPO> posRelList = positionPersonMapper.selectPage(new Page<>(current, pageSize, 1), posPersonWrapper);
        if (posRelList == null || posRelList.getRecords().size() == 0) {
            return null;
        }
        List<Long> personIds = new ArrayList<>();
        posRelList.getRecords().stream().forEach(posRel -> {
            personIds.add(posRel.getPersonId());
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
        List<PositionAddPO> positions = list(posWrapper);
        if (positions == null || positions.size() == 0) {
            return pageResult;
        }
        QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<>();
        deptWrapper.lambda().in(DepartmentAddPO::getId, deptIds).eq(DepartmentAddPO::getValid, true);
        List<DepartmentAddPO> departments = departmentMapper.selectList(deptWrapper);

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
    public PageResult<PositionDetailBO> queryPositionsPage(Integer current, Integer pageSize) {
        QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<>();
        positionWrapper.lambda().eq(PositionAddPO::getValid, true).eq(PositionAddPO::getSysFlag, 0);
        Integer count = count(positionWrapper);
        if (count == null || count == 0) {
            return null;
        }
        Page<PositionAddPO> page = page(new Page(current, pageSize, count), positionWrapper);
        if (page == null || page.getRecords() == null || page.getRecords().size() == 0) {
            return null;
        }
        List<PositionDetailBO> boList = new ArrayList<>();
        page.getRecords().stream().forEach(positionAddPO -> {
            PositionDetailBO positionDetailBO = new PositionDetailBO();
            BeanUtils.copyProperties(positionAddPO, positionDetailBO);
            boList.add(positionDetailBO);
        });

        return new PageResult<>(boList, count, pageSize, current);
    }

    @Override
    public Boolean checkPositionSupAndSub(Long supPositionId, Long subPositionId) {
        QueryWrapper<PositionAddPO> supQueryWrapper  = new QueryWrapper<>();
        supQueryWrapper.lambda().eq(PositionAddPO::getId, supPositionId).eq(PositionAddPO::getValid, true);
        PositionAddPO supPos = getOne(supQueryWrapper);

        if (supPos == null) {
            return false;
        }

        QueryWrapper<PositionAddPO> subQueryWrapper = new QueryWrapper<>();
        subQueryWrapper.lambda().eq(PositionAddPO::getId, subPositionId).eq(PositionAddPO::getValid, true);
        PositionAddPO subPos = getOne(subQueryWrapper);

        if (subPos == null) {
            return false;
        }

        if (!supPos.getCompanyId().equals(subPos.getCompanyId())) {
            return false;
        }
        while (subPos != null && subPos.getParentId() != null) {
            if (subPos.getParentId().equals(supPositionId)) {
                return true;
            } else {
                subPos = getById(subPos.getParentId());
            }
        }
        return false;
    }

    @Override
    public PageResult<PositionSynchronizationInfoBO> getPositions(String modifyTime, Integer current, Integer pageSize) {
        String dbType = dataId.getDataId();
        Integer total = positionMapper.getPositionsCount(modifyTime, dbType);
        if (total == null || total == 0) {
            return new PageResult<>(new ArrayList<>(), 0, pageSize, current);
        }

        List<PositionSynchronizationInfoPO> positionSynchronizationInfoPOList = positionMapper.getPositions(modifyTime, current, pageSize, dbType);
        if (positionSynchronizationInfoPOList.size() == 0) {
            return new PageResult<>(new ArrayList<>(), 0, pageSize, current);
        }
        List<PositionSynchronizationInfoBO> positionSynchronizationInfoBOList = new ArrayList<>();
        for (PositionSynchronizationInfoPO positionSynchronizationInfoPO : positionSynchronizationInfoPOList) {
            // 时间格式转换
            if (null != positionSynchronizationInfoPO.getModifyTime()) {
                String formatTime = responseFormatTime(positionSynchronizationInfoPO.getModifyTime());
                positionSynchronizationInfoPO.setModifyTime(formatTime);
            }

            PositionSynchronizationInfoBO positionSynchronizationInfoBO = new PositionSynchronizationInfoBO();
            BeanUtils.copyProperties(positionSynchronizationInfoPO, positionSynchronizationInfoBO);
            DepartmentForPositionSynchronizationInfoBO departmentForPositionSynchronizationInfoBO = new DepartmentForPositionSynchronizationInfoBO();
            departmentForPositionSynchronizationInfoBO.setCode(positionSynchronizationInfoPO.getDeptCode());
            departmentForPositionSynchronizationInfoBO.setName(positionSynchronizationInfoPO.getDeptName());
            positionSynchronizationInfoBO.setDepartment(departmentForPositionSynchronizationInfoBO);

            CompanyForPositionSynchronizationInfoBO companyForPositionSynchronizationInfoBO = new CompanyForPositionSynchronizationInfoBO();
            companyForPositionSynchronizationInfoBO.setCode(positionSynchronizationInfoPO.getCompanyCode());
            companyForPositionSynchronizationInfoBO.setFullName(positionSynchronizationInfoPO.getCompanyFullName());
            companyForPositionSynchronizationInfoBO.setShortName(positionSynchronizationInfoPO.getCompanyShortName());
            positionSynchronizationInfoBO.setCompany(companyForPositionSynchronizationInfoBO);

            positionSynchronizationInfoBOList.add(positionSynchronizationInfoBO);
        }
        return new PageResult<>(positionSynchronizationInfoBOList, total, pageSize, current);
    }

    @Override
    public PageResult<PositionBaseInfoBO> getPositionsByCompanyCode(String companyCode, Integer current, Integer pageSize) {
        QueryWrapper<CompanyPO> companyPOQueryWrapper = new QueryWrapper<>();
        companyPOQueryWrapper.lambda().eq(CompanyPO::getValid, true).eq(CompanyPO::getCode, companyCode);
        CompanyPO companyPO = companyMapper.selectOne(companyPOQueryWrapper);
        if (companyPO == null) {
            throw new BizHttpStatusException(OrganizationErrorEnum.COMPANY_PARAM_ID_NECESSARY,400);
        }
        QueryWrapper<PositionAddPO> positionCountWrapper = new QueryWrapper<>();
        positionCountWrapper.lambda().eq(PositionAddPO::getValid, true).eq(PositionAddPO::getCompanyId, companyPO.getId());
        Integer total = count(positionCountWrapper);
        if (total == null || total == 0) {
            new PageResult<>(new ArrayList<>(), 0, pageSize, current);
        }
        String dbType = dataId.getDataId();
        List<PositionBaseInfoPO> positionBaseInfoPOList = positionMapper.getPositionsByCompanyId(companyPO.getId(), current, pageSize, dbType);

        if (positionBaseInfoPOList.size() == 0) {
            new PageResult<>(new ArrayList<>(), 0, pageSize, current);
        }
        List<PositionBaseInfoBO> positionBaseInfoBOList = new ArrayList<>();

        for (PositionBaseInfoPO positionBaseInfoPO : positionBaseInfoPOList) {
            PositionBaseInfoBO positionBaseInfoBO = new PositionBaseInfoBO();
            BeanUtils.copyProperties(positionBaseInfoPO, positionBaseInfoBO);

            DepartmentForPositionBaseInfoBO departmentForPositionBaseInfoBO = new DepartmentForPositionBaseInfoBO();
            departmentForPositionBaseInfoBO.setCode(positionBaseInfoPO.getDeptCode());
            departmentForPositionBaseInfoBO.setName(positionBaseInfoPO.getDeptName());

            CompanyBaseInfoBO companyBaseInfoBO = new CompanyBaseInfoBO();
            companyBaseInfoBO.setCode(positionBaseInfoPO.getCompanyCode());
            companyBaseInfoBO.setFullName(positionBaseInfoPO.getCompanyFullName());
            companyBaseInfoBO.setShortName(positionBaseInfoPO.getCompanyShortName());

            positionBaseInfoBO.setDepartment(departmentForPositionBaseInfoBO);
            positionBaseInfoBO.setCompany(companyBaseInfoBO);

            positionBaseInfoBOList.add(positionBaseInfoBO);
        }
        return new PageResult<>(positionBaseInfoBOList, total, pageSize, current);
    }

    @Override
    public Result<PositionDetailInfoBO> getPositionByCode(String positionCode) {
        PositionDetailInfoBO positionDetailInfoBO = new PositionDetailInfoBO();

        PositionSynchronizationInfoPO positionPO = positionMapper.getPositionByCode(positionCode);
        if (null == positionPO) {
            throw new BizHttpStatusException(PositionErrorEnum.POSITION_ID_NOT_EXISTS, 400);
        }
        // 时间格式转换
        if (null != positionPO.getModifyTime()) {
            String formatTime = responseFormatTime(positionPO.getModifyTime());
            positionPO.setModifyTime(formatTime);
        }

        BeanUtils.copyProperties(positionPO, positionDetailInfoBO);

        DepartmentForPositionSynchronizationInfoBO departmentForPositionSynchronizationInfoBO = new DepartmentForPositionSynchronizationInfoBO();
        departmentForPositionSynchronizationInfoBO.setCode(positionPO.getDeptCode());
        departmentForPositionSynchronizationInfoBO.setName(positionPO.getDeptName());
        positionDetailInfoBO.setDepartment(departmentForPositionSynchronizationInfoBO);

        CompanyForPositionSynchronizationInfoBO companyForPositionSynchronizationInfoBO = new CompanyForPositionSynchronizationInfoBO();
        companyForPositionSynchronizationInfoBO.setCode(positionPO.getCompanyCode());
        companyForPositionSynchronizationInfoBO.setFullName(positionPO.getCompanyFullName());
        companyForPositionSynchronizationInfoBO.setShortName(positionPO.getCompanyShortName());
        positionDetailInfoBO.setCompany(companyForPositionSynchronizationInfoBO);

        QueryWrapper<PositionRolePO> positionRolePOQueryWrapper = new QueryWrapper<>();
        positionRolePOQueryWrapper.eq("opr.position_id", positionPO.getId());
        List<Long> roleIds = positionMapper.getPositionRoleId(positionRolePOQueryWrapper);

        if (!CollectionUtils.isEmpty(roleIds)) {
            List<RoleDTO> roleDTOS = organizationAdapter.findRoleByIds(roleIds);
            List<PositionRoleBaseBO> positionRoleBaseBOS = roleDTOS.stream().map(roleDTO -> {
                PositionRoleBaseBO positionRoleBaseBO = new PositionRoleBaseBO();
                positionRoleBaseBO.setCode(roleDTO.getCode());
                positionRoleBaseBO.setName(roleDTO.getName());
                return positionRoleBaseBO;
            }).collect(Collectors.toList());
            positionDetailInfoBO.setRoles(positionRoleBaseBOS);
        }

        return new Result<>(positionDetailInfoBO);
    }

    @Override
    public List<PositionDetailInfoBO> getPositionsByDepartment(DepartmentDetailBO departmentDetailBO) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dep_id", departmentDetailBO.getId());
        queryWrapper.eq("valid", 1);
        List<PositionAddPO> positionAddPOS = list(queryWrapper);
        List<PositionDetailInfoBO> result = new ArrayList<>();
        for (PositionAddPO positionAddPO : positionAddPOS) {
            Result<PositionDetailInfoBO> positionDetailInfoBOResult = getPositionByCode(positionAddPO.getCode());
            result.add(positionDetailInfoBOResult.getData());
        }
        return result;
    }

    @Override
    public Boolean checkRolesExistPosition(List<Long> roleIds) {
        QueryWrapper<PositionRolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(PositionRolePO::getRoleId, roleIds);
        Integer count = positionRoleMapper.selectCount(queryWrapper);
        if (count > 0) {
            return true;
        }
        return false;
    }

    @Override
    public List<PositionFlowSimpleBO> queryPositionIdByCodes(List<String> codes) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(PositionAddPO::getCode, codes).eq(PositionAddPO::getValid, true);
        List<PositionAddPO> positionAddPOList = list(queryWrapper);
        if (positionAddPOList == null) {
            return null;
        }
        List<PositionFlowSimpleBO> positionFlowSimpleBOList = new ArrayList<>();
        positionAddPOList.stream().forEach(positionAddPO -> {
            PositionFlowSimpleBO positionFlowSimpleBO = new PositionFlowSimpleBO();
            BeanUtils.copyProperties(positionAddPO, positionFlowSimpleBO);
            positionFlowSimpleBOList.add(positionFlowSimpleBO);
        });
        return positionFlowSimpleBOList;
    }

    /**
     * 岗位删除发消息
     * @param positionPoList
     * @param tenantId
     */
    @Override
    public void publishDeletePositionMessage(List<PositionAddPO> positionPoList, String tenantId) {

        List<PositionDeleteMessageBO> messageBody = new ArrayList<>();
        positionPoList.stream().forEach(positionAddPO -> {
            PositionDeleteMessageBO positionDeleteMessageBO = new PositionDeleteMessageBO();
            BeanUtils.copyProperties(positionAddPO, positionDeleteMessageBO);
            positionDeleteMessageBO.setRowVersion(0L);
            messageBody.add(positionDeleteMessageBO);
        });
        Map<String, Object> header = new HashMap<>();
        header.put("encode", "json");
        header.put("event", "DELETE");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        OrganizationMessage.Builder<PositionDeleteMessageBO> messageBuilder = new OrganizationMessage.Builder<>();
        Message message = messageBuilder.setSender("organization")
                .setTenantId(tenantId)
                .setCreateTime(sdf.format(new Date()))
                .setTopic(POSITION_TOPIC)
                .setHeader(header)
                .setBody(messageBody)
                .build();
        organizationMessage.publishMessage(message);
    }

    private void publishCreateOrUpdatePositionMessage(List<PositionAddPO> positionAddPOList, String tenantId, String event) {
        if (positionAddPOList == null || positionAddPOList.size() == 0) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        List<PositionMessageBO> messageBody = new ArrayList<>();
        positionAddPOList.stream().forEach(positionAddPO -> {

            PositionAddPO currentPos = getById(positionAddPO.getId());
            QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
            comWrapper.eq("id", currentPos.getCompanyId());
            comWrapper.eq("valid", 1);
            CompanyPO comPo = companyMapper.selectOne(comWrapper);

            PositionMessageBO positionMessageBO = new PositionMessageBO();
            positionMessageBO.setRowVersion(0L);
            positionMessageBO.setId(currentPos.getId());

            PositionAddPO parentPos = null;
            if (currentPos.getParentId() != null) {
                parentPos = getById(currentPos.getParentId());
                positionMessageBO.setParentId(parentPos.getId());
                positionMessageBO.setParentCode(parentPos.getCode());
            }

            positionMessageBO.setCode(currentPos.getCode());
            positionMessageBO.setName(currentPos.getName());
            positionMessageBO.setModifyTime(currentPos.getModifyTime());

            DepartmentAddPO departmentAddPO = departmentMapper.selectById(currentPos.getDepId());
            DepartmentSimpleMessageBO department = new DepartmentSimpleMessageBO();
            department.setId(departmentAddPO.getId());
            department.setCode(departmentAddPO.getCode());
            department.setName(departmentAddPO.getName());

            positionMessageBO.setDepartment(department);

            CompanySimpleMessageBO companySimpleMessageBO = new CompanySimpleMessageBO();
            companySimpleMessageBO.setId(comPo.getId());
            companySimpleMessageBO.setCode(comPo.getCode());
            companySimpleMessageBO.setFullName(comPo.getFullName());
            companySimpleMessageBO.setShortName(comPo.getShortName());
            positionMessageBO.setCompany(companySimpleMessageBO);

            positionMessageBO.setDescription(currentPos.getDescription());
            positionMessageBO.setFullPath(currentPos.getFullPath());
            positionMessageBO.setLayNo(currentPos.getLayNo());
            positionMessageBO.setSort(currentPos.getSort());

            messageBody.add(positionMessageBO);
        });
        Map<String, Object> header = new HashMap<>();
        header.put("encode", "json");
        header.put("event", event);

        OrganizationMessage.Builder<PositionMessageBO> messageBuilder = new OrganizationMessage.Builder<>();
        Message message = messageBuilder
                .setSender("organization")
                .setTenantId(tenantId)
                .setCreateTime(sdf.format(new Date()))
                .setTopic(POSITION_TOPIC)
                .setHeader(header)
                .setBody(messageBody)
                .build();
        organizationMessage.publishMessage(message);
    }

    /**
     * 不发kafka新增岗位
     * @param positionAddPo
     * @param managerIds
     */
    @Transactional(rollbackFor = Exception.class)
    private void addPositionWithoutKafka(PositionAddPO positionAddPo, List<Long> managerIds) {
        Long uid = IDGenerator.newInstance().generate().longValue();
        String oldId = "Position_" + uid;
        positionAddPo.setId(uid);
        positionAddPo.setOldId(oldId);

        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
        queryWrapper.eq("code", positionAddPo.getCode());
        queryWrapper.eq("valid", 1);
        int count = count(queryWrapper);
        //指定编码的岗位已经存在
        if (count > 0) {
            throw new PositionException(PositionErrorEnum.POSITION_THIS_CODE_EXISTS);
        }
        queryWrapper.eq("company_id", positionAddPo.getCompanyId());

        QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
        comWrapper.eq("id", positionAddPo.getCompanyId());
        comWrapper.eq("valid", 1);
        Integer comCount = companyMapper.selectCount(comWrapper);
        //指定id的公司不存在
        if (comCount == null || comCount == 0) {
            throw new PositionException(PositionErrorEnum.POSITION_COMPANYID_NOT_EXISTS);
        }
        if (positionAddPo.getDepId() != null) {
            QueryWrapper<DepartmentAddPO> depWrapper = new QueryWrapper<DepartmentAddPO>();
            depWrapper.eq("id", positionAddPo.getDepId());
            depWrapper.eq("valid", 1);
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(depWrapper);
            //指定id的部门不存在
            if (departmentAddPO == null) {
                throw new PositionException(PositionErrorEnum.POSITION_DEPID_NOT_EXISTS);
            }
            if (!departmentAddPO.getCompanyId().equals(positionAddPo.getCompanyId())) {
                throw new PositionException(PositionErrorEnum.POSITION_DEPARTMENT_COMPANY_NOT_MATCH);
            }
        } else {
            throw new PositionException(PositionErrorEnum.POSITION_DEPID_NOT_EXISTS);
        }


        //清除条件，为判断上级节点做准备
        queryWrapper.clear();
        //parentId不为空，需要判断该id的上级节点是否存在
        if (positionAddPo.getParentId() != null) {
            queryWrapper.eq("id", positionAddPo.getParentId());
            queryWrapper.eq("valid", 1);
            int parentCount = count(queryWrapper);
            if (parentCount == 0) {
                throw new PositionException(PositionErrorEnum.POSITION_PARENTID_NOT_EXISTS);
            }
            PositionAddPO parentPo = getOne(queryWrapper);
            positionAddPo.setFullPath(parentPo.getFullPath() + "/" + positionAddPo.getName());
            positionAddPo.setLayRec(parentPo.getLayRec() + "-" + positionAddPo.getId());
            positionAddPo.setLayNo(parentPo.getLayNo() + 1);
            parentPo.setLeaf(false);
            updateById(parentPo);
            //清除条件，为判断名称做准备
            queryWrapper.clear();

            queryWrapper.eq("parent_id", positionAddPo.getParentId());
        } else {
            CompanyPO comPO = companyMapper.selectOne(comWrapper);
            positionAddPo.setFullPath(comPO.getFullPath() + "/" + positionAddPo.getName());
            positionAddPo.setLayRec("" + positionAddPo.getId());
            positionAddPo.setLayNo(1);
            queryWrapper.isNull("parent_id");
        }
        queryWrapper.eq("valid", 1);
        queryWrapper.eq("company_id", positionAddPo.getCompanyId());
        //同级岗位数量
        int brotherCount = count(queryWrapper);

        queryWrapper.eq("name", positionAddPo.getName());
        //判断同父节点下的岗位的名称是否重复
        int nameCount = count(queryWrapper);
        if (nameCount > 0) {
            throw new PositionException(PositionErrorEnum.POSITION_NAME_EXISTS);
        }
        positionAddPo.setSort(Double.valueOf(FISRT_SORT + brotherCount * 1000));
        positionAddPo.setLeaf(true);
        save(positionAddPo);

        orgMnecodeService.addOrgMnecode(positionAddPo.getId(), positionAddPo.getName(), Constants.POSITION);
        if (managerIds != null) {
            organizationManagerService.addManager(managerIds, positionAddPo.getId(), Constants.POSITION);
        }
    }

    /**
     * 修改岗位不发kafka
     * @param positionAddPO
     * @param managerIds
     */
    @Transactional(rollbackFor = Exception.class)
    private void updatePositionWithoutKafka(PositionAddPO positionAddPO, List<Long> managerIds) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
        queryWrapper.eq("id", positionAddPO.getId());
        queryWrapper.eq("valid", 1);
        int count = count(queryWrapper);
        if (count == 0) {
            throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
        }
        PositionAddPO curPos = getOne(queryWrapper);
        positionAddPO.setParentId(curPos.getParentId());
        //修改了name则需要修改所有子部门的full_path
        if (positionAddPO != null && !positionAddPO.getName().equals(curPos.getName())) {
            updateFullPath(curPos, positionAddPO);
        }
        orgMnecodeService.deleteOrgMnecodeByOrgId(curPos.getId(), Constants.POSITION);
        orgMnecodeService.addOrgMnecode(curPos.getId(), positionAddPO.getName(), Constants.POSITION);
        if (positionAddPO != null && positionAddPO.getDepId() != null) {
            QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<DepartmentAddPO>();
            deptWrapper.eq("id", positionAddPO.getDepId());
            deptWrapper.eq("valid", 1);
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(deptWrapper);
            if (departmentAddPO == null) {
                throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
            }
            if (!departmentAddPO.getCompanyId().equals(curPos.getCompanyId())) {
                throw new PositionException(PositionErrorEnum.POSITION_DEPARTMENT_COMPANY_NOT_MATCH);
            }
        } else {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
        }
        updateById(positionAddPO);

        if (managerIds != null) {
            organizationManagerService.addManager(managerIds, positionAddPO.getId(), Constants.POSITION);
        }
    }

    /**
     * 移动岗位不发kafka
     * @param positionLocationPO
     */
    @Transactional(rollbackFor = Exception.class)
    private PositionAddPO updatePosLocationWithoutKafka(PositionLocationBO positionLocationPO) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
        queryWrapper.eq("id", positionLocationPO.getId());
        queryWrapper.eq("valid", 1);
        int count = count(queryWrapper);
        if (count == 0) {
            throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
        }
        //当前岗位
        PositionAddPO currentPo = getOne(queryWrapper);

        //前续岗位
        PositionAddPO upPo = null;

        //父级岗位
        PositionAddPO parentPo = null;

        if (positionLocationPO.getUpId() != null) {
            queryWrapper.clear();
            queryWrapper.eq("id", positionLocationPO.getUpId());
            queryWrapper.eq("valid", 1);
            count = count(queryWrapper);
            if (count == 0) {
                throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
            }
            upPo = getOne(queryWrapper);

            if (!upPo.getCompanyId().equals(currentPo.getCompanyId())) {
                //公司不同
                throw new PositionException(PositionErrorEnum.POSITION_LOCATION_PARAM_ERROR);
            }
        }

        if (positionLocationPO.getParentId() != null) {
            queryWrapper.clear();
            queryWrapper.eq("id", positionLocationPO.getParentId());
            queryWrapper.eq("valid", 1);
            count = count(queryWrapper);
            if (count == 0) {
                throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
            }
            parentPo = getOne(queryWrapper);
            if (currentPo.getId().equals(parentPo.getId())) {
                //上级节点是自己
                throw new PositionException(PositionErrorEnum.POSITION_LOCATION_PARAM_ERROR);
            }
            if (parentPo.getFullPath().startsWith(currentPo.getFullPath())) {
                //上级节点是当前节点的孩子节点
                throw new PositionException(PositionErrorEnum.POSITION_LOCATION_PARAM_ERROR);
            }
            if (!parentPo.getCompanyId().equals(currentPo.getCompanyId())) {
                //公司不同
                throw new PositionException(PositionErrorEnum.POSITION_LOCATION_PARAM_ERROR);
            }
        }

        //前续部门和上级岗位参数不匹配
        if ((upPo != null && parentPo != null && !parentPo.getId().equals(upPo.getParentId())) || (upPo != null && parentPo == null && upPo.getParentId() != null)) {
            throw new PositionException(PositionErrorEnum.POSITION_LOCATION_PARAM_ERROR);
        }
        QueryWrapper<PositionAddPO> sonDepWrapper = new QueryWrapper<PositionAddPO>();

        //parentId没变只需要修改sort排序
        if ((parentPo == null && currentPo.getParentId() == null) || (parentPo != null && parentPo.getId().equals(currentPo.getParentId()))) {
            if (currentPo.getParentId() == null) {
                sonDepWrapper.isNull("parent_id");
            } else {
                sonDepWrapper.eq("parent_id", currentPo.getParentId());
            }
            sonDepWrapper.eq("company_id", currentPo.getCompanyId());
        } else {
            //当前layNo
            Integer curLayNo = currentPo.getLayNo();

            Integer newLayNo = 1;

            if (currentPo.getParentId() != null) {
                sonDepWrapper.eq("parent_id", currentPo.getParentId());
            } else {
                sonDepWrapper.isNull("parent_id");
            }

            //当前fullPath
            String curFullPath = currentPo.getFullPath();
            String newFullPath = "";

            //当前id的全路径
            String curLayRec = currentPo.getLayRec();
            String newLayRec = "";

            //parentId有变化时
            if (parentPo == null) {
                //校验新的parent下name是否重复
                QueryWrapper<PositionAddPO> checkNameDupWrapper = new QueryWrapper<>();
                checkNameDupWrapper.isNull("parent_id");
                checkNameDupWrapper.eq("valid", 1);
                checkNameDupWrapper.eq("name", currentPo.getName());
                int nameCount = count(checkNameDupWrapper);
                if (nameCount > 0) {
                    throw new DepartmentException(PositionErrorEnum.POSITION_NAME_EXISTS);
                }

                QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
                comWrapper.eq("id", currentPo.getCompanyId());
                CompanyPO comPO = companyMapper.selectOne(comWrapper);
                newFullPath = comPO.getFullPath() + "/" + currentPo.getName();
                newLayRec = "" + currentPo.getId();

                //currentPo.setParentId(null);
                //sonDepWrapper.isNull("parent_id");
                //判断现在的parent节点，移动后，是不是叶子节点，如果是则设置
                if (currentPo.getParentId() != null) {
                    QueryWrapper<PositionAddPO> parentDepWrapper = new QueryWrapper<PositionAddPO>();
                    parentDepWrapper.eq("parent_id", currentPo.getParentId());
                    parentDepWrapper.eq("valid", 1);
                    int parentCount = count(parentDepWrapper);
                    if (parentCount == 1) {
                        PositionAddPO parentPos = getById(currentPo.getParentId());
                        parentPos.setLeaf(true);
                        updateById(parentPos);
                    }
                }
                currentPo.setParentId(null);
            } else {
                //校验新的parent下name是否重复
                QueryWrapper<PositionAddPO> checkNameDupWrapper = new QueryWrapper<>();
                checkNameDupWrapper.eq("parent_id", parentPo.getId());
                checkNameDupWrapper.eq("valid", 1);
                checkNameDupWrapper.eq("name", currentPo.getName());
                int nameCount = count(checkNameDupWrapper);
                if (nameCount > 0) {
                    throw new DepartmentException(PositionErrorEnum.POSITION_NAME_EXISTS);
                }

                //判断现在的parent节点，移动后，是不是叶子节点，如果是则设置
                if (currentPo.getParentId() != null) {
                    QueryWrapper<PositionAddPO> parentDepWrapper = new QueryWrapper<PositionAddPO>();
                    parentDepWrapper.eq("parent_id", currentPo.getParentId());
                    parentDepWrapper.eq("valid", 1);
                    int parentCount = count(parentDepWrapper);
                    if (parentCount == 1) {
                        PositionAddPO parentPos = getById(currentPo.getParentId());
                        parentPos.setLeaf(true);
                        updateById(parentPos);
                    }
                }

                currentPo.setParentId(parentPo.getId());
                newLayNo = parentPo.getLayNo() + 1;
                newFullPath = parentPo.getFullPath() + "/" + currentPo.getName();
                newLayRec = parentPo.getLayRec() + "-" + currentPo.getId();
                parentPo.setLeaf(false);
                updateById(parentPo);
                //sonDepWrapper.eq("parent_id", parentPo.getId());

            }
            sonDepWrapper.eq("company_id", currentPo.getCompanyId());
            sonDepWrapper.eq("valid", 1);
            Integer diffLayNo = newLayNo - curLayNo;
            String prefixFullPath = newFullPath;
            String prefixLayRec = newLayRec;
            QueryWrapper<PositionAddPO> deepWrapper = new QueryWrapper<PositionAddPO>();
            deepWrapper.likeRight("full_path", curFullPath + "/");
            deepWrapper.eq("valid", 1);

            List<PositionAddPO> deepPoses = list(deepWrapper);

            if (deepPoses != null && deepPoses.size() > 0) {
                deepPoses.stream().forEach(item -> {
                    item.setLayNo(item.getLayNo() + diffLayNo);
                    item.setFullPath(prefixFullPath + item.getFullPath().substring(curFullPath.length(), item.getFullPath().length()));
                    item.setLayRec(prefixLayRec + item.getLayRec().substring(curLayRec.length(), item.getLayRec().length()));
                });
                updateBatchById(deepPoses);
            }

            currentPo.setFullPath(newFullPath);
            currentPo.setLayRec(newLayRec);
            currentPo.setLayNo(newLayNo);
        }
        //放在最上面
        if (upPo == null) {
            Integer sonCount = count(sonDepWrapper);
            sonDepWrapper.orderByDesc("sort");
            Page<PositionAddPO> pageInfo = new Page<PositionAddPO>(1, 1, sonCount);
            page(pageInfo, sonDepWrapper);
            List<PositionAddPO> firstBroDep = pageInfo.getRecords();
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
            Page<PositionAddPO> pageInfo = new Page<PositionAddPO>(1, 1, sonCount);
            page(pageInfo, sonDepWrapper);
            List<PositionAddPO> firstBroDep = pageInfo.getRecords();
            if (firstBroDep == null || firstBroDep.size() == 0) {
                currentPo.setSort(upPo.getSort()/2);
            } else {
                currentPo.setSort((upPo.getSort() + firstBroDep.get(0).getSort())/2);
            }
        }
        updateById(currentPo);

        return currentPo;
    }

    @Override
    public PageResult<PersonDetailBO> queryPositionPersonsBetter(Long companyId, Long positionId, String keyword, Integer current, Integer pageSize, PersonDetailBO conditionQuery, Boolean includeUser) {
        if (companyId == null) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_COMPANYID_NOT_EXISTS);
        }
        PageResult<PersonDetailBO> pageResult = null;
        if (companyId.equals(positionId) || (keyword != null && !"".equals(keyword.trim()))) {

            List<Long> positionIds = null;
            if (positionId != null && !companyId.equals(positionId)) {
                positionIds = new ArrayList<Long>();
                positionIds.add(positionId);
            }
            //pageResult = personService.queryPersonsByCompanyId(companyId, positionIds, keyword, current, pageSize, conditionQuery);
            pageResult = personService.queryPersonsAndOrgDetailByCompanyIdBetter(companyId, positionIds, keyword, current, pageSize, conditionQuery, positionId);
        } else {
            if (positionId == null) {
                throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
            }
            QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<PositionAddPO>();
            positionWrapper.eq("id", positionId);
            int positionCount = count(positionWrapper);
            if (positionCount == 0) {
                throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
            }
            List<Long> positionIds = new ArrayList<>();
            positionIds.add(positionId);
            pageResult = personService.queryPersonsAndOrgDetailByCompanyIdBetter(companyId, positionIds, keyword, current, pageSize, conditionQuery, positionId);
/*            List<Long> presonIds = positionPersonService.queryPersonIdByPositionId(positionId);
            if (presonIds == null || presonIds.size() == 0) {
                return new PageResult<PersonDetailBO>(null, 0, pageSize, current);
            }
            pageResult = personService.queryPersonsById(presonIds, current, pageSize, conditionQuery);*/
        }

        return pageResult;
    }
}
