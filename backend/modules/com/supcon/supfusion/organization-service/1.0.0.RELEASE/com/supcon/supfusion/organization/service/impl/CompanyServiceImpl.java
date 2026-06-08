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
import com.supcon.supfusion.framework.cloud.common.exception.BizErrorEnum;
import com.supcon.supfusion.framework.cloud.common.exception.BizHttpStatusException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;
import com.supcon.supfusion.framework.cloud.common.message.Message;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.*;
import com.supcon.supfusion.organization.common.kafka.OrganizationMessage;
import com.supcon.supfusion.organization.common.utils.OrgBaseUtils;
import com.supcon.supfusion.organization.dao.mapper.company.CompanyMapper;
import com.supcon.supfusion.organization.dao.mapper.company.CompanyPersonMapper;
import com.supcon.supfusion.organization.dao.mapper.company.CompanyTagMapper;
import com.supcon.supfusion.organization.dao.mapper.department.DepartmentMapper;
import com.supcon.supfusion.organization.dao.mapper.department.DepartmentPersonMapper;
import com.supcon.supfusion.organization.dao.mapper.person.OrganizationManagerMapper;
import com.supcon.supfusion.organization.dao.mapper.person.PersonMapper;
import com.supcon.supfusion.organization.dao.mapper.position.PositionMapper;
import com.supcon.supfusion.organization.dao.mapper.position.PositionPersonMapper;
import com.supcon.supfusion.organization.dao.mapper.position.PositionRoleMapper;
import com.supcon.supfusion.organization.dao.po.company.CompanyDetailInfoPO;
import com.supcon.supfusion.organization.dao.po.company.CompanyPO;
import com.supcon.supfusion.organization.dao.po.company.CompanyPersonPO;
import com.supcon.supfusion.organization.dao.po.company.CompanyTagPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentAddPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentPersonPO;
import com.supcon.supfusion.organization.dao.po.person.OrganizationManagerPO;
import com.supcon.supfusion.organization.dao.po.person.PersonAddPO;
import com.supcon.supfusion.organization.dao.po.position.PositionAddPO;
import com.supcon.supfusion.organization.dao.po.position.PositionPersonPO;
import com.supcon.supfusion.organization.dao.po.position.PositionRolePO;
import com.supcon.supfusion.organization.manager.OrganizationAdapter;
import com.supcon.supfusion.organization.service.*;
import com.supcon.supfusion.organization.service.bo.baseService.CompanyBaseServiceBO;
import com.supcon.supfusion.organization.service.bo.company.CompanyBO;
import com.supcon.supfusion.organization.service.bo.company.CompanyDetailInfoBO;
import com.supcon.supfusion.organization.service.bo.company.CompanyKeywordBO;
import com.supcon.supfusion.organization.service.bo.compatible.CompanyResultBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentResultBO;
import com.supcon.supfusion.organization.service.bo.kafka.CompanyDeleteMessageBO;
import com.supcon.supfusion.organization.service.bo.kafka.CompanyMessageBO;
import com.supcon.supfusion.organization.service.bo.person.MainPositionBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import com.supcon.supfusion.organization.service.bo.person.PersonResultBO;
import com.supcon.supfusion.organization.service.bo.person.RelationDepartmentBO;
import com.supcon.supfusion.organization.service.bo.position.PositionResultBO;
import com.supcon.supfusion.organization.service.bo.position.PositionRoleBO;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.supcon.supfusion.organization.common.utils.OrgBaseUtils.responseFormatTime;

/**
 * @Description: 公司服务实现类
 * @Author:     HUNING
 * @CreateDate: 2020/5/25
 */
@Slf4j
@Service
public class CompanyServiceImpl extends ServiceImpl<CompanyMapper, CompanyPO> implements CompanyService {

    Logger logger = LoggerFactory.getLogger(CompanyServiceImpl.class);
/*    @Autowired
    private CompanyMapper companyMapper;*/


    @Autowired
    private CompanyTagService companyTagService;

    @Autowired
    private OrganizationAdapter organizationAdapter;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private PositionService positionService;

    @Autowired
    private PositionPersonMapper positionPersonMapper;

    @Autowired
    private PersonMapper personMapper;

    @Autowired
    private PositionPersonService positionPersonService;

    @Autowired
    private PositionRoleMapper positionRoleMapper;

    @Autowired
    private OrganizationManagerMapper organizationManagerMapper;

    @Autowired
    private BaseServiceService baseServiceService;

    @Autowired
    private PersonService personService;

    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;

    @Autowired
    private OrgMnecodeService orgMnecodeService;

    @Autowired
    private CompanyPersonMapper companyPersonMapper;

    @Autowired
    private DepartmentPersonMapper departmentPersonMapper;

    @Autowired
    private CompanyTagMapper companyTagMapper;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private OrganizationMessage organizationMessage;

    public static final String COMPANY_TOPIC = "supOS_company_event";

    @Override
    public List<CompanyPO> listCompanies() {
        QueryWrapper<CompanyPO> queryWrapper = new QueryWrapper<CompanyPO>();
        queryWrapper.eq("valid", 1);
        return list(queryWrapper);
    }


    @Override
    public CompanyPO findCompany(Long id) {
        CompanyPO comPO = getById(id);
        return comPO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCompany(CompanyPO comPO, List<String> tags, String userName, String password, String tenantId) {
        checkTagLength(tags);
        QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
        comWrapper.eq("code", comPO.getCode());
        comWrapper.eq("valid", 1);
        int codeCount = count(comWrapper);
        if (codeCount > 0) {
            throw new OrganizationException(OrganizationErrorEnum.COMPANY_CODE_EXISTS);
        }
        QueryWrapper<CompanyPO> comShortWrapper = new QueryWrapper<CompanyPO>();
        comShortWrapper.eq("short_name", comPO.getShortName());
        comShortWrapper.eq("valid", 1);
        int shortCount = count(comShortWrapper);
        if (shortCount > 0) {
            throw new OrganizationException(OrganizationErrorEnum.COMPANY_SHORTNAME_EXISTS);
        }

        QueryWrapper<CompanyPO> comFullWrapper = new QueryWrapper<CompanyPO>();
        comFullWrapper.eq("full_name", comPO.getFullName());
        comFullWrapper.eq("valid", 1);
        int fullCount = count(comFullWrapper);
        if (fullCount > 0) {
            throw new OrganizationException(OrganizationErrorEnum.COMPANY_FULLNAME_EXISTS);
        }

        Long uid = IDGenerator.newInstance().generate().longValue();
        String oldId = "Company_" + uid;
        comPO.setId(uid);
        comPO.setOldId(oldId);

        if (!Objects.isNull(comPO)) {
            log.info("新增公司信息, [" + comPO.toString() + "]");
            comPO.setLayNo(Optional.ofNullable(comPO.getLayNo()).orElse(1));
            comPO.setSort(Optional.ofNullable(comPO.getSort()).orElse(Double.valueOf(0)));
            if (comPO.getParentId() == null) {
                comPO.setParentId(Constants.DEFAULT_COMPANY_ID);
            }
            QueryWrapper<CompanyPO> queryWrapper = new QueryWrapper<CompanyPO>();
            queryWrapper.eq("id", comPO.getParentId());
            CompanyPO parentCom = getOne(queryWrapper);
            comPO.setFullPath(parentCom.getFullPath() + "/" + comPO.getShortName());
            comPO.setLayRec(parentCom.getLayRec() + "-" + comPO.getId());
            comPO.setLayNo(parentCom.getLayNo() + 1);

        }

        saveOrUpdate(comPO);
        List<CompanyTagPO> companyTags = new ArrayList<CompanyTagPO>(16);
        companyTagService.deleteCompanyTag(comPO.getId());
        if (tags != null && tags.size() > 0) {
            tags.stream().forEach(tag -> {
                CompanyTagPO companyTagPO = new CompanyTagPO();
                companyTagPO.setCompanyId(comPO.getId());
                companyTagPO.setName(tag);
                companyTags.add(companyTagPO);
            });
            companyTagService.addCompanyTag(companyTags);
        }

        Long deptId = departmentService.addVirtualDept(uid, tenantId);
        Long posId = positionService.addVirtualPos(uid, deptId, tenantId);

        Long personId = personService.addVirtualPerson(userName, posId, tenantId);

        orgMnecodeService.addOrgMnecode(comPO.getId(), comPO.getShortName(), Constants.COMPANY);

        Result<String> result = organizationAdapter.createUser(userName, password, null, comPO.getId(), null, personId, 1, comPO.getCode(), userName, userName);
        if (result != null && !result.getCode().equals(BizErrorEnum.SYSTEM_OK.getCode())) {
            throw new OrganizationException(new ErrorDefinition() {
                @Override
                public Integer getCode() {
                    return result.getCode();
                }

                @Override
                public String getMessage() {
                    return result.getMessage();
                }

                @Override
                public String getInfo() {
                    return null;
                }
            });
        }

        //kafka消息
        CompanyMessageBO companyMessageBO = new CompanyMessageBO();
        companyMessageBO.setRowVersion(comPO.getRowVersion());
        companyMessageBO.setId(comPO.getId());
        companyMessageBO.setCode(comPO.getCode());
        if (comPO.getParentId() != null) {
            QueryWrapper<CompanyPO> queryWrapper = new QueryWrapper<CompanyPO>();
            queryWrapper.eq("id", comPO.getParentId());
            CompanyPO parentCom = getOne(queryWrapper);
            companyMessageBO.setParentId(parentCom.getId());
            companyMessageBO.setParentCode(parentCom.getCode());
        }
        companyMessageBO.setFullName(comPO.getFullName());
        companyMessageBO.setShortName(comPO.getShortName());
        companyMessageBO.setDescription(comPO.getDescription());
        if (tags != null && tags.size() > 0) {
            companyMessageBO.setTags(tags);
        } else {
            companyMessageBO.setTags(new ArrayList<>());
        }
        companyMessageBO.setFullPath(comPO.getFullPath());
        companyMessageBO.setLayNo(comPO.getLayNo());
        companyMessageBO.setSort(comPO.getSort());

        List<CompanyMessageBO> messageBody = new ArrayList<>();
        messageBody.add(companyMessageBO);

        Map<String, Object> header = new HashMap<>();
        header.put("encode", "json");
        header.put("event", "CREATE");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        OrganizationMessage.Builder<CompanyMessageBO> messageBuilder = new OrganizationMessage.Builder<>();
        Message message = messageBuilder.setSender("organization")
                .setTenantId(tenantId)
                .setCreateTime(sdf.format(new Date()))
                .setTopic(COMPANY_TOPIC)
                .setHeader(header)
                .setBody(messageBody)
                .build();
        organizationMessage.publishMessage(message);

    }


    /**
     * 校验标签长度
     * @param tags
     */
    private void checkTagLength(List<String> tags) {
        if (tags == null || tags.size() == 0) {
            return;
        }
        for (String tag : tags) {
            if (StringUtils.isBlank(tag) || tag.length() > 50) {
                throw new OrganizationException(OrganizationErrorEnum.TAG_LENGTH_BIGGER_THAN_THIRTY);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateCom(CompanyPO comPO, List<String> tags, String tenantId) {
        checkTagLength(tags);
        CompanyPO curCom = getById(comPO.getId());
        if (curCom == null) {
            throw new OrganizationException(OrganizationErrorEnum.COMPANY_PARAM_ID_NECESSARY);
        }
        comPO.setParentId(curCom.getParentId());
        QueryWrapper<CompanyPO> comShortWrapper = new QueryWrapper<CompanyPO>();
        comShortWrapper.eq("short_name", comPO.getShortName());
        comShortWrapper.eq("valid", 1);
        comShortWrapper.ne("id", comPO.getId());
        int shortCount = count(comShortWrapper);
        if (shortCount > 0) {
            throw new OrganizationException(OrganizationErrorEnum.COMPANY_SHORTNAME_EXISTS);
        }

        QueryWrapper<CompanyPO> comFullWrapper = new QueryWrapper<CompanyPO>();
        comFullWrapper.eq("full_name", comPO.getFullName());
        comFullWrapper.eq("valid", 1);
        comFullWrapper.ne("id", comPO.getId());
        int fullCount = count(comFullWrapper);
        if (fullCount > 0) {
            throw new OrganizationException(OrganizationErrorEnum.COMPANY_FULLNAME_EXISTS);
        }
        if (!curCom.getShortName().equals(comPO.getShortName())) {
            String curComFullPath = curCom.getFullPath();
            String curNewFullPath = "";
            if (curCom.getParentId() == null) {
                curNewFullPath = "/" + comPO.getShortName();
            } else {
                CompanyPO comParent = findCompany(comPO.getParentId());
                curNewFullPath = comParent.getFullPath() + "/" + comPO.getShortName();
            }
            QueryWrapper<CompanyPO> subWrapper = new QueryWrapper<>();
            subWrapper.eq("valid", 1);
            subWrapper.likeRight("full_path", curComFullPath + "/");
            List<CompanyPO> subComs = list(subWrapper);
            if (subComs != null && subComs.size() > 0) {
                for (CompanyPO subCom : subComs) {
                    String curFullPath = subCom.getFullPath();
                    String newFullPath = curNewFullPath + curFullPath.substring(curComFullPath.length(), curFullPath.length());
                    subCom.setFullPath(newFullPath);
                }
            } else {
                subComs = new ArrayList<>();
            }
            comPO.setFullPath(curNewFullPath);
            subComs.add(comPO);
            updateBatchById(subComs);

            QueryWrapper<DepartmentAddPO> departmentAddPOQueryWrapper = new QueryWrapper<>();
            departmentAddPOQueryWrapper.eq("valid", 1);
            departmentAddPOQueryWrapper.likeRight("full_path", curComFullPath + "/");
            List<DepartmentAddPO> departmentAddPOS = departmentMapper.selectList(departmentAddPOQueryWrapper);
            if (departmentAddPOS != null && departmentAddPOS.size() > 0) {
                for (DepartmentAddPO departmentAddPO : departmentAddPOS) {
                    String curFullPath = departmentAddPO.getFullPath();
                    String newFullPath = curNewFullPath + curFullPath.substring(curComFullPath.length(), curFullPath.length());
                    departmentAddPO.setFullPath(newFullPath);
                    departmentMapper.updateById(departmentAddPO);
                }
            }

            QueryWrapper<PositionAddPO> positionAddPOQueryWrapper = new QueryWrapper<>();
            positionAddPOQueryWrapper.eq("valid", 1);
            positionAddPOQueryWrapper.likeRight("full_path", curComFullPath + "/");
            List<PositionAddPO> positionAddPOS = positionMapper.selectList(positionAddPOQueryWrapper);
            if (positionAddPOS != null && positionAddPOS.size() > 0) {
                for (PositionAddPO positionAddPO : positionAddPOS) {
                    String curFullPath = positionAddPO.getFullPath();
                    String newFullPath = curNewFullPath + curFullPath.substring(curComFullPath.length(), curFullPath.length());
                    positionAddPO.setFullPath(newFullPath);
                    positionMapper.updateById(positionAddPO);
                }
            }

        } else {
            updateById(comPO);
        }

        saveOrUpdate(comPO);

        if (StringUtils.isNotBlank(comPO.getShortName())) {
            orgMnecodeService.deleteOrgMnecodeByOrgId(comPO.getId(), Constants.COMPANY);
            orgMnecodeService.addOrgMnecode(comPO.getId(), comPO.getShortName(), Constants.COMPANY);
        }

        List<CompanyTagPO> companyTags = new ArrayList<CompanyTagPO>(16);
        companyTagService.deleteCompanyTag(comPO.getId());
        if (tags != null && tags.size() > 0) {
            tags.stream().forEach(tag -> {
                CompanyTagPO companyTagPO = new CompanyTagPO();
                companyTagPO.setCompanyId(comPO.getId());
                companyTagPO.setName(tag);
                companyTags.add(companyTagPO);
            });
            companyTagService.addCompanyTag(companyTags);
        }

        //kafka消息
        CompanyMessageBO companyMessageBO = new CompanyMessageBO();
        companyMessageBO.setRowVersion(comPO.getRowVersion());
        companyMessageBO.setId(comPO.getId());
        companyMessageBO.setCode(comPO.getCode());
        if (comPO.getParentId() != null) {
            QueryWrapper<CompanyPO> queryWrapper = new QueryWrapper<CompanyPO>();
            queryWrapper.eq("id", comPO.getParentId());
            CompanyPO parentCom = getOne(queryWrapper);
            companyMessageBO.setParentId(parentCom.getId());
            companyMessageBO.setParentCode(parentCom.getCode());
        }
        companyMessageBO.setFullName(comPO.getFullName());
        companyMessageBO.setShortName(comPO.getShortName());
        companyMessageBO.setDescription(comPO.getDescription());
        if (tags != null && tags.size() > 0) {
            companyMessageBO.setTags(tags);
        } else {
            companyMessageBO.setTags(new ArrayList<>());
        }
        companyMessageBO.setFullPath(comPO.getFullPath());
        companyMessageBO.setLayNo(comPO.getLayNo());
        companyMessageBO.setSort(comPO.getSort());

        List<CompanyMessageBO> messageBody = new ArrayList<>();
        messageBody.add(companyMessageBO);

        Map<String, Object> header = new HashMap<>();
        header.put("encode", "json");
        header.put("event", "UPDATE");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        OrganizationMessage.Builder<CompanyMessageBO> messageBuilder = new OrganizationMessage.Builder<>();
        Message message = messageBuilder.setSender("organization")
                .setTenantId(tenantId)
                .setCreateTime(sdf.format(new Date()))
                .setTopic(COMPANY_TOPIC)
                .setHeader(header)
                .setBody(messageBody)
                .build();
        organizationMessage.publishMessage(message);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delCompany(Long id, String tenantId) {
        QueryWrapper<CompanyPO> queryWrapper = new QueryWrapper<CompanyPO>();
        queryWrapper.eq("parent_id", id);
        queryWrapper.eq("valid", 1);
        int count = count(queryWrapper);
        if (count > 0) {
            throw new OrganizationException(OrganizationErrorEnum.COMPANY_HAVE_SUB_COMPANY_CAN_NOT_DELETE);
        }
        List<Long> positionIds = positionService.queryPositionIdsbyCompanyId(id);
        if (positionIds != null && !positionIds.isEmpty()) {
            LambdaQueryWrapper<PositionPersonPO> lamba = new QueryWrapper<PositionPersonPO>().lambda()
                    .in(PositionPersonPO::getPositionId, positionIds)
                    .eq(PositionPersonPO::getValid, true);
            Integer num = positionPersonMapper.selectCount(lamba);
            if (num != null && num > 0) {
                throw new OrganizationException(OrganizationErrorEnum.COMPANY_DELETE_ERROR);
            }
        }

        log.info("删除公司信息， id= " + id);
        CompanyPO company = getById(id);
        company.setValid(false);
        updateById(company);
        orgMnecodeService.deleteOrgMnecodeByOrgId(id, Constants.COMPANY);
        if (positionIds != null && positionIds.size() > 0) {
            List<PositionAddPO> positions = positionIds.stream().map(t -> {
                PositionAddPO po = new PositionAddPO();
                po.setId(t);
                po.setValid(false);
                return po;
            }).collect(Collectors.toList());
            positionService.updateBatchById(positions);
            //删除助记码
            orgMnecodeService.deleteOrgMnecodeByOrgId(positionIds, Constants.POSITION);
        }


        LambdaQueryWrapper<DepartmentAddPO> lambda = new QueryWrapper<DepartmentAddPO>().lambda()
                .eq(DepartmentAddPO::getCompanyId, id)
                .eq(DepartmentAddPO::getValid, true);
        //查询所有的部门
        List<DepartmentAddPO> departmentAddPOS = departmentMapper.selectList(lambda);
        if (departmentAddPOS != null && !departmentAddPOS.isEmpty()) {
            List<Long> ids = departmentAddPOS.stream().map(t -> t.getId()).collect(Collectors.toList());
            organizationManagerMapper.delete(new QueryWrapper<OrganizationManagerPO>().lambda()
                    .in(OrganizationManagerPO::getOrgId, ids)
                    .eq(OrganizationManagerPO::getManagerType, "Department")
            );
            List<DepartmentAddPO> departments = ids.stream().map(t -> {
                DepartmentAddPO po = new DepartmentAddPO();
                po.setId(t);
                po.setValid(false);
                return po;
            }).collect(Collectors.toList());
            departmentService.updateBatchByIds(departments);
            orgMnecodeService.deleteOrgMnecodeByOrgId(ids, Constants.DEPARTMENT);
        }

        //delete positions
        LambdaQueryWrapper<PositionAddPO> lambdaPos = new QueryWrapper<PositionAddPO>().lambda()
                .eq(PositionAddPO::getCompanyId, id)
                .eq(PositionAddPO::getValid, true);
        List<PositionAddPO> positionAddPOList = positionMapper.selectList(lambdaPos);
        if (positionAddPOList != null && !positionAddPOList.isEmpty()) {
            List<Long> ids = positionAddPOList.stream().map(t -> t.getId()).collect(Collectors.toList());
            List<PositionAddPO> positionAddPOS = ids.stream().map(t -> {
                PositionAddPO po = new PositionAddPO();
                po.setId(t);
                po.setValid(false);
                return po;
            }).collect(Collectors.toList());
            positionService.updateBatchById(positionAddPOS);
        }

        if (positionIds != null && !positionIds.isEmpty()) {
            positionRoleMapper.delete(new QueryWrapper<PositionRolePO>().lambda()
                    .in(PositionRolePO::getPositionId, positionIds));
        }
        Boolean delUser = organizationAdapter.deleteCompanyUser(id);
        if (!delUser) {
            throw new OrganizationException(OrganizationErrorEnum.DELETE_COMPANY_USER_ERROR);
        }
        Boolean delRbac = organizationAdapter.deleteRbac(id);
        if (!delRbac) {
            throw new OrganizationException(OrganizationErrorEnum.DELETE_COMPANY_RBAC_ERROR);
        }

        //kafka消息
        CompanyDeleteMessageBO companyMessageBO = new CompanyDeleteMessageBO();
        companyMessageBO.setRowVersion(company.getRowVersion());
        companyMessageBO.setId(company.getId());
        companyMessageBO.setCode(company.getCode());

        List<CompanyDeleteMessageBO> messageBody = new ArrayList<>();
        messageBody.add(companyMessageBO);

        Map<String, Object> header = new HashMap<>();
        header.put("encode", "json");
        header.put("event", "DELETE");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        OrganizationMessage.Builder<CompanyMessageBO> messageBuilder = new OrganizationMessage.Builder<>();
        Message message = messageBuilder.setSender("organization")
                .setTenantId(tenantId)
                .setCreateTime(sdf.format(new Date()))
                .setTopic(COMPANY_TOPIC)
                .setHeader(header)
                .setBody(messageBody)
                .build();
        organizationMessage.publishMessage(message);

    }

    @Override
    public List<CompanyPO> getSubCompanies(Long companyId, Long selectCompanyId, String keyword) {

        CompanyPO companyPO = getById(companyId);
        if (companyPO == null) {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_COMPANYID_NOT_EXISTS);
        }
        QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<>();
        comWrapper.likeRight("full_path", companyPO.getFullPath());
        comWrapper.eq("valid", 1);
        comWrapper.orderByAsc("lay_no");
        comWrapper.orderByAsc("sort");
        List<CompanyPO> companies = list(comWrapper);
        if (companies == null || companies.size() == 0) {
            return new ArrayList<CompanyPO>();
        }
        List<CompanyPO> results = new ArrayList<CompanyPO>();
        results.add(companyPO);
        if (selectCompanyId != null) {
            if (selectCompanyId.equals(companyId)) {
                return results;
            }
            CompanyPO selectCom = getById(selectCompanyId);
            if (selectCom == null) {
                return companies;
            }
            Long curId = selectCompanyId;
            while (curId != null) {
                for (CompanyPO com : companies) {
                    if (curId.equals(com.getId()) && !results.contains(com)) {
                        results.add(1, com);
                        curId = com.getParentId();
                        break;
                    } else if (curId.equals(com.getId())) {
                        curId = com.getParentId();
                    }
                }
                if (selectCom.getId().equals(curId)) {
                    break;
                }
                if (companyId.equals(curId)) {
                    break;
                }
            }
            return results;
        }

        if (selectCompanyId == null && StringUtils.isNotBlank(keyword)) {
            int index = 1;
            for (CompanyPO selectCom : companies) {
                if (selectCom.getId().equals(companyId)) {
                    continue;
                }
                if (selectCom.getShortName().contains(keyword)) {

                    Long curId = selectCom.getId();
                    while (curId != null) {
                        for (CompanyPO com : companies) {
                            if (curId.equals(com.getId()) && !results.contains(com)) {
                                results.add(index, com);
                                curId = com.getParentId();
                                break;
                            } else if (curId.equals(com.getId())) {
                                curId = com.getParentId();
                            }
                        }
                        if (selectCom.getId().equals(curId)) {
                            break;
                        }
                        if (companyId.equals(curId)) {
                            break;
                        }
                    }
                    index = results.size();
                }
            }
            return results;
        }

/*        QueryWrapper<CompanyPO> queryWrapper = new QueryWrapper<CompanyPO>();
        queryWrapper.likeRight("full_path", companyPO.getFullPath());
        queryWrapper.eq("valid",1 );
        List<CompanyPO> list = list(queryWrapper);*/

        return companies;

    }

    @Override
    public List<PositionRoleBO> queryCompanyRoles(Long companyId) {

        List<Long> positionIds = new ArrayList<Long>();

        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
        posWrapper.eq("company_id", companyId);
        posWrapper.eq("valid", 1);
        List<PositionAddPO> positions = positionMapper.selectList(posWrapper);
        if (positions == null || positions.size() == 0) {
            return new ArrayList<PositionRoleBO>();
        }

        positionIds.clear();
        positions.stream().forEach(position -> positionIds.add(position.getId()));

        QueryWrapper<PositionRolePO> queryWrapper = new QueryWrapper<PositionRolePO>();
        queryWrapper.in("position_id", positionIds);
        List<PositionRolePO> positionRolePOS = positionRoleMapper.selectList(queryWrapper);
        List<PositionRoleBO> results = new ArrayList<PositionRoleBO>();
        if (positionRolePOS == null || positionRolePOS.size() == 0) {
            return results;
        }
        positionRolePOS.stream().forEach(item -> {
            PositionRoleBO positionRoleBO = new PositionRoleBO();
            positionRoleBO.setId(item.getRoleId());
            results.add(positionRoleBO);
        });
        return results;
    }

    @Override
    public List<CompanyKeywordBO> queryCompaniesByKeyword(String keyword, Long companyId) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }
        CompanyPO root = getById(companyId);
        if (root == null) {
            return null;
        }
        QueryWrapper<CompanyPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("valid", 1);
        queryWrapper.likeRight("full_path", root.getFullPath());

        String key = dbStringUtil.getString(keyword);
        //获取数据库类型
        String dbType = dataId.getDataId();
        //使用queryWrapper形式
        //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
        if ("oracle".equals(dbType)){
            queryWrapper.apply("short_name like {0} escape '\\'", "%" + key + "%");
        }else{
            queryWrapper.like("short_name",key);
        }

        List<CompanyPO> list = list(queryWrapper);
        List<CompanyKeywordBO> results = new ArrayList<>();
        if (list == null || list.size() == 0) {
            return null;
        }
        list.stream().forEach(com -> {
            CompanyKeywordBO companyKeywordBO = new CompanyKeywordBO();
            companyKeywordBO.setFullName(com.getFullName());
            companyKeywordBO.setCode(com.getCode());
            companyKeywordBO.setId(com.getId());
            companyKeywordBO.setShortName(com.getShortName());
            results.add(companyKeywordBO);
        });
        return results;
    }

    @Override
    public JSONObject getCompanyById(Long id, String includes) {
        CompanyPO companyPO = getById(id);
        if (companyPO == null) {
            return new JSONObject();
        }
        CompanyBaseServiceBO companyBaseServiceBO = new CompanyBaseServiceBO();
        BeanUtils.copyProperties(companyPO, companyBaseServiceBO);
        JSONObject companyJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(companyBaseServiceBO), includes, Constants.COMPANY);
        return companyJson;
    }

    @Override
    public CompanyBO getCompanyById(Long id) {
        CompanyPO companyPO = getById(id);
        CompanyBO companyBO = new CompanyBO();
        if (ObjectUtils.isEmpty(companyPO)) {
            return companyBO;
        }
        BeanUtils.copyProperties(companyPO, companyBO);
        return companyBO;
    }

    @Override
    public List<CompanyBO> queryAllCompanies() {
        QueryWrapper<CompanyPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("valid", 1);
        List<CompanyPO> list = list(queryWrapper);
        if (list == null || list.size() == 0) {
            return null;
        }
        List<CompanyBO> companyBOS = new ArrayList<>();
        list.stream().forEach(com -> {
            CompanyBO companyBO = new CompanyBO();
            BeanUtils.copyProperties(com, companyBO);
            companyBOS.add(companyBO);
        });
        return companyBOS;
    }

    //--------------------------------------------------old version---------------

    @Override
    public JSONObject listCompanies(String keywords, Integer page, Integer per_page) {
        if (page == null || page == 0) {
            page = 1;
        }
        if (per_page == null || per_page == 0) {
            per_page = 20;
        }
        QueryWrapper<CompanyPO> queryWrapper = new QueryWrapper<CompanyPO>();
        queryWrapper.eq("valid", 1);
        if (StringUtils.isNotBlank(keywords)) {
            queryWrapper.and(item -> item.like("short_name", keywords).or().like("full_name", keywords).like("description", keywords).like("code", keywords));
        }
        int count = count(queryWrapper);

        Page<CompanyPO> pageInfo = new Page<CompanyPO>(page, per_page, count);
        page(pageInfo, queryWrapper);
        JSONObject json = new JSONObject();
        JSONObject pagination = new JSONObject();
        pagination.put("total", count);
        pagination.put("pageSize", per_page);
        pagination.put("current", page);
        json.put("pagination", pagination);

        if (pageInfo.getRecords() == null) {
            json.put("list", new ArrayList<>());
        } else {
            ArrayList<CompanyResultBO> list = new ArrayList<CompanyResultBO>();
            pageInfo.getRecords().stream().forEach(companyPO -> {
                CompanyResultBO companyResultBO = new CompanyResultBO();
                companyResultBO.setCode(companyPO.getCode());
                companyResultBO.setName(companyPO.getOldId());
                companyResultBO.setShowName(companyPO.getShortName());
                companyResultBO.setAddress(companyPO.getAddress());
                companyResultBO.setFullName(companyPO.getFullName());
                companyResultBO.setDescription(companyPO.getDescription());
                list.add(companyResultBO);
            });
            json.put("list", list);
        }

        return json;
    }

    @Override
    public JSONObject queryCompanyDetail(String orgName) {
        QueryWrapper<CompanyPO> queryWrapper = new QueryWrapper<CompanyPO>();
        queryWrapper.eq("old_id", orgName);
        queryWrapper.eq("valid", 1);
        CompanyPO companyPO = getOne(queryWrapper);

        JSONObject json = new JSONObject();
        if (companyPO != null) {
            json.put("code", companyPO.getCode());
            json.put("name", companyPO.getOldId());
            json.put("showName", companyPO.getFullName());
            json.put("orgType", "Company");
            json.put("address", companyPO.getAddress());
            json.put("fullName", companyPO.getFullName());
            json.put("description", companyPO.getDescription());
            json.put("layNo", companyPO.getLayNo());
            json.put("sequenceNumber", companyPO.getSort());
            json.put("fullPath", companyPO.getFullPath());
            JSONArray persons = new JSONArray();
            json.put("persons", persons);
            if (companyPO.getParentId() != null) {
                CompanyPO parentCom = getById(companyPO.getId());
                JSONObject parentJSON = new JSONObject();
                parentJSON.put("code", parentCom.getCode());
                parentJSON.put("name", parentCom.getOldId());
                parentJSON.put("showName", parentCom.getFullName());
                parentJSON.put("orgType", Constants.COMPANY);
                json.put("parent", parentJSON);
            }
        } else {
            throw new OrganizationException(404, Constants.COMPANY_NOT_EXISTS, 404);
        }
        return json;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCompany(String orgName, JSONObject body, String tenantId) {
        if (body == null || body.size() == 0) {
            return;
        }
        QueryWrapper<CompanyPO> queryWrapper = new QueryWrapper<CompanyPO>();
        queryWrapper.eq("old_id", orgName);
        queryWrapper.eq("valid", 1);
        CompanyPO companyPO = getOne(queryWrapper);
        if (companyPO == null) {
            return;
        }
        CompanyPO updateCom = new CompanyPO();
        updateCom.setId(companyPO.getId());
        if (StringUtils.isNotBlank(body.getString("showName"))) {
            updateCom.setShortName(body.getString("showName"));
        } else {
            updateCom.setShortName(companyPO.getShortName());
        }
        if (StringUtils.isNotBlank(body.getString("fullName"))) {
            updateCom.setFullName(body.getString("fullNa"));
        } else {
            updateCom.setFullName(companyPO.getFullName());
        }
        if (StringUtils.isNotBlank(body.getString("address"))) {
            updateCom.setAddress(body.getString("address"));
        }
        if (StringUtils.isNotBlank(body.getString("description"))) {
            updateCom.setAddress(body.getString("description"));
        }

        saveOrUpdateCom(updateCom, null, tenantId);
    }

    @Override
    public JSONObject queryOrganizationTileStruct(String orgName, String orgType, String keywords, Integer page, Integer per_page) {
        JSONObject result = new JSONObject();
        QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
        comWrapper.eq("old_id", orgName);
        // Company
        CompanyPO companyPO = getOne(comWrapper);
        if (companyPO == null) {
            throw new OrganizationException(400, Constants.COMPANY_NOT_EXISTS);
        }
        if (page == null || page == 0) {
            page = 1;
        }
        if (per_page == null || per_page == 0) {
            per_page = 20;
        }
        JSONArray array = new JSONArray();
        JSONObject companyJson = new JSONObject();
        companyJson.put("fullPath", companyPO.getFullPath());
        companyJson.put("sequenceNumber", companyPO.getSort());
        companyJson.put("showName", companyPO.getShortName());
        companyJson.put("code", companyPO.getCode());
        companyJson.put("root", companyPO.getOldId());
        companyJson.put("name", companyPO.getOldId());
        companyJson.put("orgType", "Company");
        companyJson.put("description", companyPO.getDescription());
        companyJson.put("address", companyPO.getAddress());
        companyJson.put("layNo", 0);
        companyJson.put("managerName", "");
        companyJson.put("managerShowName", "");
        companyJson.put("departmentType", 0);
        companyJson.put("fullName", companyPO.getFullName());

        JSONObject pagination = new JSONObject();
        if (StringUtils.isNotBlank(orgType) && orgType.equals("Department")) {
            queryDepartments(array, companyPO, keywords, page, per_page, pagination);
        } else if (StringUtils.isNotBlank(orgType) && orgType.equals("Position")) {
            queryPositions(array, companyPO, keywords, page, per_page, pagination);
        } else if (StringUtils.isNotBlank(orgType) && orgType.equals("Company")) {
            array.add(companyJson);
        } else {
            array.add(companyJson);
            queryDepartments(array, companyPO, keywords, page, per_page, pagination);
            queryPositions(array, companyPO, keywords, page, per_page, pagination);
        }
        JSONArray pageArray = new JSONArray();
        Integer startIndex = (page - 1) * per_page;
        Integer totalQueryNum = page * per_page;
        Integer endIndex = 0;
        if (array.size() > startIndex) {
            if (totalQueryNum > array.size()) {
                endIndex = array.size();
            } else {
                endIndex = totalQueryNum;
            }
            for (int i = startIndex; i < endIndex; i++) {
                pageArray.add(array.getJSONObject(i));
            }
        }

        result.put("list", pageArray);
        pagination.put("total", array.size());
        result.put("pagination", pagination);
        return result;
    }

    private void queryPositions(JSONArray array, CompanyPO companyPO, String keywords, Integer page, Integer per_page, JSONObject pagination) {
        if (page == null || page == 0) {
            page = 1;
        }
        if (per_page == null || per_page == 0) {
            per_page = 10000;
        }
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
        posWrapper.eq("company_id", companyPO.getId());
        posWrapper.eq("valid", 1);

        if (StringUtils.isNotBlank(keywords)) {
            posWrapper.and(item -> item.like("code", keywords).or().like("name", keywords));
        }
        int count = positionMapper.selectCount(posWrapper);
        posWrapper.orderByAsc("lay_no");
        List<PositionAddPO> positions = positionMapper.selectList(posWrapper);


        pagination.put("current", page);
        pagination.put("pageSize", per_page);
        pagination.put("total", count);
        //Map<Long, String> idToOldId = new HashMap<Long, String>();
        if (positions != null && positions.size() > 0) {
            /*for (PositionAddPO positionAddPO : positions) {
                idToOldId.put(positionAddPO.getId(), positionAddPO.getOldId());
            }*/
            for (PositionAddPO positionAddPO : positions) {
                JSONObject posJson = new JSONObject();
                posJson.put("fullPath", positionAddPO.getFullPath());
                posJson.put("sequenceNumber", positionAddPO.getSort());
                posJson.put("showName", positionAddPO.getName());
                posJson.put("code", positionAddPO.getCode());
                posJson.put("root", companyPO.getOldId());
                posJson.put("name", positionAddPO.getOldId());
                posJson.put("address", "");
                posJson.put("fullName", "");
                posJson.put("orgType", "Position");
                posJson.put("description", positionAddPO.getDescription());
                JSONObject company = new JSONObject();
                company.put("name", companyPO.getOldId());
                company.put("orgType", "Company");
                company.put("code", companyPO.getCode());
                company.put("showName", companyPO.getFullName());
                posJson.put("company", company);
                posJson.put("layNo", positionAddPO.getLayNo());
                JSONObject parent = new JSONObject();
                if (positionAddPO.getParentId() == null || positionAddPO.getParentId().equals(companyPO.getId())) {
                    parent.put("name", companyPO.getOldId());
                    parent.put("orgType", "Company");
                    parent.put("code", companyPO.getCode());
                    parent.put("showName", companyPO.getFullName());
                } else {
                    PositionAddPO parentPos = positionMapper.selectById(positionAddPO.getParentId());
                    parent.put("name", parentPos.getOldId());
                    parent.put("orgType", "Position");
                    parent.put("code", parentPos.getCode());
                    parent.put("showName", parentPos.getName());
                }
                posJson.put("parent", parent);
                array.add(posJson);
            }
        }
    }

    private void queryDepartments(JSONArray array, CompanyPO companyPO, String keywords, Integer page, Integer per_page, JSONObject pagination) {
        if (page == null || page == 0) {
            page = 1;
        }
        if (per_page == null || per_page == 0) {
            per_page = 10000;
        }
        QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<DepartmentAddPO>();
        deptWrapper.eq("company_id", companyPO.getId());
        deptWrapper.eq("valid", 1);

        if (StringUtils.isNotBlank(keywords)) {
            deptWrapper.and(item -> item.like("code", keywords).or().like("name", keywords));
        }
        int count = departmentMapper.selectCount(deptWrapper);
        deptWrapper.orderByAsc("lay_no");
        List<DepartmentAddPO> departments = departmentMapper.selectList(deptWrapper);


        pagination.put("current", page);
        pagination.put("pageSize", per_page);
        pagination.put("total", count);
        //Map<Long, String> idToOldId = new HashMap<Long, String>();
        if (departments != null && departments.size() > 0) {
            /*for (DepartmentAddPO departmentAddPO : departments) {
                idToOldId.put(departmentAddPO.getId(), departmentAddPO.getOldId());
            }*/
            for (DepartmentAddPO departmentAddPO : departments) {
                JSONObject deptJson = new JSONObject();
                deptJson.put("fullPath", departmentAddPO.getFullPath());
                deptJson.put("sequenceNumber", departmentAddPO.getSort());
                deptJson.put("showName", departmentAddPO.getName());
                deptJson.put("code", departmentAddPO.getCode());
                deptJson.put("root", companyPO.getOldId());
                deptJson.put("name", departmentAddPO.getOldId());
                deptJson.put("departmentType", 0);
                deptJson.put("address", "");
                deptJson.put("fullName", "");
                deptJson.put("orgType", "Department");
                deptJson.put("description", departmentAddPO.getDescription());
                deptJson.put("layNo", departmentAddPO.getLayNo());
                JSONObject company = new JSONObject();
                company.put("name", companyPO.getOldId());
                company.put("orgType", "Company");
                company.put("code", companyPO.getCode());
                company.put("showName", companyPO.getFullName());
                deptJson.put("company", company);
                JSONObject parent = new JSONObject();
                if (departmentAddPO.getParentId() == null || departmentAddPO.getParentId().equals(companyPO.getId())) {
                    parent.put("name", companyPO.getOldId());
                    parent.put("orgType", "Company");
                    parent.put("code", companyPO.getCode());
                    parent.put("showName", companyPO.getFullName());
                } else {
                    DepartmentAddPO parentDept = departmentMapper.selectById(departmentAddPO.getParentId());
                    parent.put("name", parentDept.getOldId());
                    parent.put("orgType", "Department");
                    parent.put("code", parentDept.getCode());
                    parent.put("showName", parentDept.getName());
                }
                deptJson.put("parent", parent);
                array.add(deptJson);
            }

        }
    }

    @Override
    public void batchDeleteOrg(JSONObject body, String tenantId) {
        if (body == null || body.getJSONArray("list") == null || body.getJSONArray("list").size() == 0) {
            return;
        }
        QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<DepartmentAddPO>();
        deptWrapper.in("old_id", body.getJSONArray("list"));
        List<DepartmentAddPO> depts = departmentMapper.selectList(deptWrapper);
        if (depts != null && depts.size() > 0) {
            depts.stream().forEach(dept -> {
                departmentService.deleteDepById(dept.getId(), tenantId);
            });
        }
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
        posWrapper.in("old_id", body.getJSONArray("list"));
        List<PositionAddPO> poses = positionMapper.selectList(posWrapper);
        if (poses != null && poses.size() > 0) {
            poses.stream().forEach(pos -> {
                positionService.deletePosById(pos.getId(), tenantId);
            });
        }
    }

    @Override
    public JSONObject queryOrgDetail(String orgName, String nodeName) {
        JSONObject result = new JSONObject();
        QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
        comWrapper.eq("old_id", orgName);
        CompanyPO companyPO = getOne(comWrapper);
        if (companyPO == null) {
            return result;
        }
        JSONObject companyJson = new JSONObject();
        companyJson.put("name", orgName);
        companyJson.put("orgType", "Company");
        companyJson.put("code", companyPO.getCode());
        companyJson.put("showName", companyPO.getShortName());
        if (nodeName.startsWith("Department")) {
            QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
            queryWrapper.eq("old_id", nodeName);
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(queryWrapper);
            if (departmentAddPO == null) {
                return result;
            }
            result.put("fullPath", departmentAddPO.getFullPath());
            result.put("sequenceNumber", departmentAddPO.getSort());
            result.put("code", departmentAddPO.getCode());
            result.put("showName", departmentAddPO.getName());
            result.put("description", departmentAddPO.getDescription());
            if ("emergency".equals(departmentAddPO.getType())) {
                result.put("departmentType", 1);
            } else {
                result.put("departmentType", 0);
            }
            result.put("orgType", "Department");
            result.put("name", nodeName);
            result.put("layNo", departmentAddPO.getLayNo());

            JSONObject parent = new JSONObject();
            if (departmentAddPO.getParentId() == null) {
                parent.put("name", orgName);
                parent.put("orgType", "Company");
                parent.put("code", companyPO.getCode());
                parent.put("showName", companyPO.getShortName());
            } else {
                DepartmentAddPO parentDept = departmentMapper.selectById(departmentAddPO.getParentId());
                if (parentDept != null) {
                    parent.put("name", parentDept.getOldId());
                    parent.put("orgType", "Department");
                    parent.put("code", parentDept.getCode());
                    parent.put("showName", parentDept.getName());
                }
            }
            result.put("parent", parent);
            result.put("company", companyJson);
        } else if (nodeName.startsWith("Position")) {
            QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
            queryWrapper.eq("old_id", nodeName);
            PositionAddPO positionAddPO = positionMapper.selectOne(queryWrapper);
            if (positionAddPO == null) {
                return result;
            }
            result.put("fullPath", positionAddPO.getFullPath());
            result.put("sequenceNumber", positionAddPO.getSort());
            result.put("code", positionAddPO.getCode());
            result.put("showName", positionAddPO.getName());
            result.put("description", positionAddPO.getDescription());

            result.put("orgType", "Position");
            result.put("name", nodeName);
            result.put("layNo", positionAddPO.getLayNo());

            JSONObject parent = new JSONObject();
            if (positionAddPO.getParentId() == null) {
                parent.put("name", orgName);
                parent.put("orgType", "Company");
                parent.put("code", companyPO.getCode());
                parent.put("showName", companyPO.getShortName());
            } else {
                PositionAddPO parentPos = positionMapper.selectById(positionAddPO.getParentId());
                if (parentPos != null) {
                    parent.put("name", parentPos.getOldId());
                    parent.put("orgType", "Department");
                    parent.put("code", parentPos.getCode());
                    parent.put("showName", parentPos.getName());
                }
            }
            result.put("parent", parent);
            result.put("company", companyJson);
        } else if (nodeName.startsWith("Company")) {
            result.put("fullPath", companyPO.getFullPath());
            result.put("sequenceNumber", companyPO.getSort());
            result.put("code", companyPO.getCode());
            result.put("showName", companyPO.getShortName());
            result.put("description", companyPO.getDescription());
            result.put("fullName", companyPO.getFullName());
            result.put("orgType", "Position");
            result.put("name", orgName);
            result.put("layNo", companyPO.getLayNo());
        }
        return result;
    }

    @Override
    public void updateOrg(String orgName, String nodeName, JSONObject body, String tenantId) {
        if (body == null || body.size() == 0) {
            return;
        }
        QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
        comWrapper.eq("old_id", orgName);
        int comCount = count(comWrapper);
        if (comCount == 0) {
            return;
        }
        if (nodeName.startsWith("Department")) {
            QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
            queryWrapper.eq("old_id", nodeName);
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(queryWrapper);
            if (departmentAddPO == null) {
                return;
            }
            if (StringUtils.isNotBlank(body.getString("showName")) && !departmentAddPO.getName().equals(body.getString("showName"))) {
                departmentAddPO.setName(body.getString("showName"));
            }
            departmentAddPO.setDescription(body.getString("description"));
            departmentService.updateDepartment(departmentAddPO, null, tenantId);
        } else if (nodeName.startsWith("Position")) {
            QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
            queryWrapper.eq("old_id", nodeName);
            PositionAddPO positionAddPO = positionMapper.selectOne(queryWrapper);
            if (positionAddPO == null) {
                return;
            }
            if (StringUtils.isNotBlank(body.getString("showName")) && !positionAddPO.getName().equals(body.getString("showName"))) {
                positionAddPO.setName(body.getString("showName"));
            }
            positionAddPO.setDescription(body.getString("description"));
            positionService.updatePosition(positionAddPO, null, tenantId);
        }

        return;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrg(String orgName, String nodeName, String token, String tenantId) {
        QueryWrapper<CompanyPO> comWrapper = new QueryWrapper<CompanyPO>();
        comWrapper.eq("old_id", orgName);
        int comCount = count(comWrapper);
        if (comCount == 0) {
            return;
        }
        if (orgName.equals(nodeName)) {
            CompanyPO companyPO = getOne(comWrapper);
            delCompany(companyPO.getId(), tenantId);
            return;
        }
        if (nodeName.startsWith("Department")) {
            QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
            queryWrapper.eq("old_id", nodeName);
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(queryWrapper);
            if (departmentAddPO == null) {
                return;
            }
            departmentService.deleteDepById(departmentAddPO.getId(), tenantId);
        } else {
            QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
            queryWrapper.eq("old_id", nodeName);
            PositionAddPO positionAddPO = positionMapper.selectOne(queryWrapper);
            if (positionAddPO == null) {
                return;
            }
            positionService.deletePosById(positionAddPO.getId(), tenantId);
        }
    }

    @Override
    public JSONObject queryOrgCorrelation(String nodeName, Integer page, Integer per_page) {
        if (page == null || page == 0) {
            page = 1;
        }
        if (per_page == null || per_page == 0) {
            per_page = 10000;
        }
        JSONObject result = new JSONObject();
        JSONObject pagination = new JSONObject();
        pagination.put("total", 0);
        pagination.put("current", page);
        pagination.put("pageSize", per_page);
        result.put("pagination", pagination);
        if (nodeName.startsWith("Department")) {
            QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
            queryWrapper.eq("old_id", nodeName);
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(queryWrapper);
            if (departmentAddPO == null) {
                return result;
            }
            QueryWrapper<PositionAddPO> relPosWrapper = new QueryWrapper<PositionAddPO>();
            relPosWrapper.eq("dep_id", departmentAddPO.getId());
            relPosWrapper.eq("valid", 1);
            List<PositionAddPO> posList = positionMapper.selectList(relPosWrapper);
            int count = positionMapper.selectCount(relPosWrapper);
            if (posList == null || posList.size() == 0) {
                result.put("list", new JSONArray());
                return result;
            }
            JSONArray array = new JSONArray();
            posList.stream().forEach(pos -> {
                JSONObject posJson = new JSONObject();
                posJson.put("fullPath", pos.getFullPath());
                posJson.put("code", pos.getCode());
                posJson.put("root", "Company_default_org_company");
                posJson.put("name", pos.getOldId());
                array.add(posJson);
            });
            result.put("list", array);
            pagination.put("total", array.size());
            pagination.put("current", page);
            pagination.put("pageSize", per_page);
            result.put("pagination", pagination);

        } else if (nodeName.startsWith("Position")) {
            QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
            queryWrapper.eq("old_id", nodeName);

            PositionAddPO positionAddPO = positionMapper.selectOne(queryWrapper);
            if (positionAddPO == null) {
                return result;
            }
            DepartmentAddPO departmentAddPO = departmentMapper.selectById(positionAddPO.getDepId());
            if (departmentAddPO == null) {
                result.put("list", new JSONArray());
                return result;
            }
            JSONArray array = new JSONArray();
            JSONObject deptJson = new JSONObject();
            deptJson.put("fullPath", departmentAddPO.getFullPath());
            deptJson.put("code", departmentAddPO.getCode());
            deptJson.put("root", "Company_default_org_company");
            deptJson.put("name", departmentAddPO.getOldId());
            array.add(deptJson);
            result.put("list", array);
            pagination.put("total", array.size());
            pagination.put("current", page);
            pagination.put("pageSize", per_page);
        }
        return result;
    }

    @Override
    public JSONObject queryCorrelationPerson(String nodeName, String keywords, Integer page, Integer per_page) {
        if (page == null || page == 0) {
            page = 1;
        }
        if (per_page == null || per_page == 0) {
            per_page = 10000;
        }
        JSONObject result = new JSONObject();
        JSONObject pagination = new JSONObject();
        pagination.put("total", 0);
        pagination.put("current", page);
        pagination.put("pageSize", per_page);
        result.put("pagination", pagination);

        List<Long> posIds = new ArrayList<Long>();
        if (nodeName.startsWith("Department")) {
            QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
            queryWrapper.eq("old_id", nodeName);
            queryWrapper.eq("valid", 1);
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(queryWrapper);
            if (departmentAddPO == null) {
                result.put("list", new JSONArray());
                return result;
            }
            QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
            posWrapper.eq("dep_id", departmentAddPO.getId());
            posWrapper.eq("valid", 1);
            List<PositionAddPO> posList = positionMapper.selectList(posWrapper);
            if (posList == null || posList.size() == 0) {
                result.put("list", new JSONArray());
                return result;
            }
            posList.stream().forEach(pos -> posIds.add(pos.getId()));
        } else if (nodeName.startsWith("Position")) {
            QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
            queryWrapper.eq("old_id", nodeName);

            PositionAddPO positionAddPO = positionMapper.selectOne(queryWrapper);
            if (positionAddPO == null) {
                result.put("list", new JSONArray());
                return result;
            }
            posIds.add(positionAddPO.getId());
        } else {
            QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<DepartmentAddPO>();
            queryWrapper.eq("old_id", nodeName);
            queryWrapper.eq("valid", 1);
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(queryWrapper);
            if (departmentAddPO != null) {
                QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
                posWrapper.eq("dep_id", departmentAddPO.getId());
                posWrapper.eq("valid", 1);
                List<PositionAddPO> posList = positionMapper.selectList(posWrapper);
                if (posList == null || posList.size() == 0) {
                    result.put("list", new JSONArray());
                    return result;
                }
                posList.stream().forEach(pos -> posIds.add(pos.getId()));
            } else {
                QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
                posWrapper.eq("old_id", nodeName);

                PositionAddPO positionAddPO = positionMapper.selectOne(posWrapper);
                if (positionAddPO == null) {
                    result.put("list", new JSONArray());
                    return result;
                }
                posIds.add(positionAddPO.getId());
            }
        }
        QueryWrapper<PositionPersonPO> queryWrapper = new QueryWrapper<PositionPersonPO>();
        queryWrapper.in("position_id", posIds);
        queryWrapper.in("valid", 1);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(queryWrapper);
        if (relations == null || relations.size() == 0) {
            result.put("list", new JSONArray());
            return result;
        }
        List<Long> personIds = new ArrayList<Long>();
        relations.stream().forEach(relation -> personIds.add(relation.getPersonId()));
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<PersonAddPO>();
        personWrapper.in("id", personIds);
        personWrapper.eq("valid", 1);
        if (StringUtils.isNotBlank(keywords)) {
            personWrapper.and(item -> item.like("code", keywords).or().like("name", keywords));
        }
        List<PersonAddPO> persons = personMapper.selectList(personWrapper);
        if (persons == null || persons.size() == 0) {
            result.put("list", new JSONArray());
            return result;
        }

        JSONArray array = new JSONArray();
        Integer startIndex = (page - 1) * per_page;
        if (persons.size() <= startIndex ) {
            result.put("list", new JSONArray());
            return result;
        }
        Integer rowNum = page * per_page;
        Integer endIndex = 0;
        if (rowNum > persons.size()) {
            endIndex = persons.size();
        } else {
            endIndex = rowNum;
        }
        for (int i = startIndex; i < endIndex; i++) {
            PersonAddPO person = persons.get(i);
            JSONObject json = new JSONObject();
            json.put("code", person.getCode());
            json.put("name", person.getOldId());
            json.put("showName", person.getName());
            if (person.getGender().equals("female")) {
                json.put("gender", "0");
            } else {
                json.put("gender", "1");
            }
            if (person.getStatus().equals("onWork")) {
                json.put("status", "0");
            } else {
                json.put("status", "1");
            }
            array.add(json);
        }
        pagination.put("total", persons.size());
        result.put("list", array);
        return result;
    }

    @Override
    public void addCorrelationForOrg(String nodeName, JSONObject body) {
        if (body == null || body.getJSONArray("list") == null || body.getJSONArray("list").size() == 0) {
            return;
        }
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
        posWrapper.eq("old_id", nodeName);
        PositionAddPO positionAddPO = positionMapper.selectOne(posWrapper);
        if (positionAddPO == null) {
            throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
        }
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<PersonAddPO>();
        personWrapper.in("old_id", body.getJSONArray("list"));
        List<PersonAddPO> persons = personMapper.selectList(personWrapper);
        if (persons == null || persons.size() == 0) {
            return;
        }
        List<PositionPersonPO> relations = new ArrayList<PositionPersonPO>();
        persons.stream().forEach(person -> {
            PositionPersonPO positionPersonPO = new PositionPersonPO();
            positionPersonPO.setPositionId(positionAddPO.getId());
            positionPersonPO.setPersonId(person.getId());
            relations.add(positionPersonPO);
        });
        positionPersonService.addPositionPerson(relations);
    }

    @Override
    public void deleteCorrelationPerson(String nodeName, JSONObject body) {
        if (body == null || body.getJSONArray("list") == null || body.getJSONArray("list").size() == 0) {
            return;
        }
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
        posWrapper.eq("old_id", nodeName);
        PositionAddPO positionAddPO = positionMapper.selectOne(posWrapper);
        if (positionAddPO == null) {
            throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
        }
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<PersonAddPO>();
        personWrapper.in("old_id", body.getJSONArray("list"));
        List<PersonAddPO> persons = personMapper.selectList(personWrapper);
        if (persons == null || persons.size() == 0) {
            return;
        }
        List<Long> personIds = new ArrayList<Long>();
        persons.stream().forEach(person -> personIds.add(person.getId()));
        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
        relationWrapper.eq("position_id", positionAddPO.getId());
        relationWrapper.in("person_id", personIds);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(relationWrapper);
        if (relations == null || relations.size() == 0) {
            return;
        }
        List<Long> relationIds = new ArrayList<Long>();
        relations.stream().forEach(relation -> {
            relationIds.add(relation.getId());
        });
        positionPersonService.batchDeleteByPersonId(relationIds);
    }

    @Override
    public void addCorNode(String nodeName, JSONObject body) {
        if (StringUtils.isBlank(nodeName) || body == null || body.size() == 0 || body.getJSONArray("list") == null || body.getJSONArray("list").size() == 0) {
            return;
        }
        JSONArray array = body.getJSONArray("list");
        List<String> names = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject node = array.getJSONObject(i);
            String name = node.getString("name");
            if (StringUtils.isBlank(name)) {
                continue;
            }
            names.add(name);
        }
        if (names.size() == 0) {
            return;
        }
        if (nodeName.startsWith(Constants.DEPARTMENT)) {
            QueryWrapper<DepartmentAddPO> departmentAddPOQueryWrapper = new QueryWrapper<>();
            departmentAddPOQueryWrapper.eq("valid", 1);
            departmentAddPOQueryWrapper.eq("old_id", nodeName);
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(departmentAddPOQueryWrapper);
            if (departmentAddPO == null) {
                return;
            }
            QueryWrapper<PositionAddPO> positionAddPOQueryWrapper = new QueryWrapper<>();
            positionAddPOQueryWrapper.eq("valid", 1);
            positionAddPOQueryWrapper.in("old_id", names);
            List<PositionAddPO>  positionAddPOS = positionMapper.selectList(positionAddPOQueryWrapper);
            if (positionAddPOS == null || positionAddPOS.size() == 0) {
                return;
            }
            positionAddPOS.stream().forEach(pos -> pos.setDepId(departmentAddPO.getId()));
            positionService.updateBatchById(positionAddPOS);
        } else if (nodeName.startsWith(Constants.POSITION)) {
            QueryWrapper<PositionAddPO> positionAddPOQueryWrapper = new QueryWrapper<>();
            positionAddPOQueryWrapper.eq("valid", 1);
            positionAddPOQueryWrapper.eq("old_id", nodeName);
            PositionAddPO positionAddPO = positionMapper.selectOne(positionAddPOQueryWrapper);
            if (positionAddPO == null) {
                return;
            }
            QueryWrapper<DepartmentAddPO> departmentAddPOQueryWrapper = new QueryWrapper<>();
            departmentAddPOQueryWrapper.eq("valid", 1);
            departmentAddPOQueryWrapper.eq("old_id", names.get(0));
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(departmentAddPOQueryWrapper);
            if (departmentAddPO == null) {
                return;
            }
            positionAddPO.setDepId(departmentAddPO.getId());
            positionMapper.updateById(positionAddPO);
        } else {
            return;
        }
    }

    @Override
    public JSONObject getOrgTree(String orgName, String path, String deep, String nodeName, String noBear, String orgType, String tenantId) {
        QueryWrapper<CompanyPO> companyPOQueryWrapper = new QueryWrapper<>();
        companyPOQueryWrapper.eq("old_id", orgName);
        CompanyPO companyPO = getOne(companyPOQueryWrapper);
        if (companyPO == null) {
            return new JSONObject();
        }
        if (StringUtils.isBlank(nodeName) || orgName.equals(nodeName)) {
            JSONObject companyJson = new JSONObject();
            companyToJson(companyJson, companyPO);
            if (StringUtils.isBlank(orgType)) {
                getDepartmentChildren(companyJson, null, noBear, deep, companyPO, tenantId);
                getPositionChildren(companyJson, null, noBear, deep, companyPO, tenantId);
            } else if (Constants.DEPARTMENT.equals(orgType)) {
                getDepartmentChildren(companyJson, null, noBear, deep, companyPO, tenantId);
            } else {
                getPositionChildren(companyJson, null, noBear, deep, companyPO, tenantId);
            }

            return companyJson;
        }
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();
        posWrapper.eq("old_id", nodeName);
        posWrapper.eq("valid", 1);
        PositionAddPO positionAddPO = positionMapper.selectOne(posWrapper);
        if (positionAddPO != null) {
            JSONObject nodeJson = new JSONObject();
            positionToJsonNoParent(nodeJson, positionAddPO, companyPO);
            getPositionChildren(nodeJson, positionAddPO.getId(), noBear, deep, companyPO, tenantId);
            return nodeJson;
        } else {
            QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<>();
            deptWrapper.eq("old_id", nodeName);
            deptWrapper.eq("valid", 1);
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(deptWrapper);
            if (departmentAddPO == null) {
                return new JSONObject();
            }
            JSONObject nodeJson = new JSONObject();
            departmentToJsonNoParent(nodeJson, departmentAddPO, companyPO);
            getDepartmentChildren(nodeJson, departmentAddPO.getId(), noBear, deep, companyPO, tenantId);
            return nodeJson;
        }
    }

    private void companyToJson(JSONObject companyJson, CompanyPO companyPO) {
        companyJson.put("parent", "supos_organization_default");
        companyJson.put("sequenceNumber", companyPO.getSort());
        companyJson.put("showName", companyPO.getFullName());
        companyJson.put("code", companyPO.getCode());
        companyJson.put("correlationPersons", new JSONArray());
        companyJson.put("root", companyPO.getOldId());
        companyJson.put("name", companyPO.getOldId());
        companyJson.put("orgType", Constants.COMPANY);
        companyJson.put("children", new JSONArray());
    }

    private void departmentToJson(JSONObject deptJson, DepartmentAddPO departmentAddPO, JSONObject parentJson) {
        deptJson.put("parent", parentJson.getString("name"));
        deptJson.put("sequenceNumber", departmentAddPO.getSort());
        deptJson.put("showName", departmentAddPO.getName());
        deptJson.put("code", departmentAddPO.getCode());
        deptJson.put("root", parentJson.getString("root"));
        deptJson.put("name", departmentAddPO.getOldId());
        deptJson.put("orgType", Constants.DEPARTMENT);
        deptJson.put("children", new JSONArray());
    }

    private void positionToJson(JSONObject posJson, PositionAddPO positionAddPO, JSONObject parentJson) {
        posJson.put("parent", parentJson.getString("name"));
        posJson.put("sequenceNumber", positionAddPO.getSort());
        posJson.put("showName", positionAddPO.getName());
        posJson.put("code", positionAddPO.getCode());
        posJson.put("root", parentJson.getString("root"));
        posJson.put("name", positionAddPO.getOldId());
        posJson.put("orgType", Constants.POSITION);
        posJson.put("children", new JSONArray());
    }

    private void positionToJsonNoParent(JSONObject posJson, PositionAddPO positionAddPO, CompanyPO companyPO) {
        if (positionAddPO.getParentId() == null) {
            posJson.put("parent", companyPO.getOldId());
        } else {
            PositionAddPO parentPos = positionMapper.selectById(positionAddPO.getParentId());
            if (parentPos != null) {
                posJson.put("parent", parentPos.getOldId());
            } else {
                posJson.put("parent", companyPO.getOldId());
            }
        }

        posJson.put("sequenceNumber", positionAddPO.getSort());
        posJson.put("showName", positionAddPO.getName());
        posJson.put("code", positionAddPO.getCode());
        posJson.put("root", companyPO.getOldId());
        posJson.put("name", positionAddPO.getOldId());
        posJson.put("orgType", Constants.POSITION);
        posJson.put("children", new JSONArray());
    }

    private void departmentToJsonNoParent(JSONObject deptJson, DepartmentAddPO departmentAddPO, CompanyPO companyPO) {
        if (departmentAddPO.getParentId() == null) {
            deptJson.put("parent", companyPO.getOldId());
        } else {
            DepartmentAddPO parentDept = departmentMapper.selectById(departmentAddPO.getParentId());
            if (parentDept != null) {
                deptJson.put("parent", parentDept.getOldId());
            } else {
                deptJson.put("parent", companyPO.getOldId());
            }
        }
        deptJson.put("sequenceNumber", departmentAddPO.getSort());
        deptJson.put("showName", departmentAddPO.getName());
        deptJson.put("code", departmentAddPO.getCode());
        deptJson.put("root", companyPO.getOldId());
        deptJson.put("name", departmentAddPO.getOldId());
        deptJson.put("orgType", Constants.DEPARTMENT);
        deptJson.put("children", new JSONArray());
    }
    private void personToJson(JSONObject personJson, PersonAddPO personAddPO) {
        personJson.put("id", personAddPO.getId());
        personJson.put("code", personAddPO.getCode());
        personJson.put("showName", personAddPO.getName());
        personJson.put("staffCode", personAddPO.getCode());
        if (StringUtils.isNotBlank(personAddPO.getGender())) {
            personJson.put("gender", personAddPO.getGender().equals(Constants.GENDER_FEMALE)? 0 : 1);
            personJson.put("sex", personAddPO.getGender().equals(Constants.GENDER_FEMALE)? 0 : 1);
        }
        personJson.put("timeZone", "(UTC+08:00) Beijing, Chongqing, Hong Kong, Urumqi, Taipei");
        personJson.put("userName", "");
        personJson.put("phone", personAddPO.getPhone());
        personJson.put("name", personAddPO.getOldId());
        personJson.put("staffName", personAddPO.getName());
        personJson.put("correlationUser", "");
        personJson.put("email", personAddPO.getEmail());
        if (StringUtils.isNotBlank(personAddPO.getStatus())) {
            personJson.put("status", personAddPO.getStatus().equals(Constants.ON_WORK_CODE)? 0 : 1);
        }

    }

    private void getDepartmentChildren(JSONObject parentJson, Long parentId, String noBear, String deep, CompanyPO companyPO, String tenantId) {
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("company_id", companyPO.getId());
        if (parentId == null) {
            queryWrapper.isNull("parent_id");
        } else {
            queryWrapper.eq("parent_id", parentId);
        }
        queryWrapper.eq("valid", 1);
        List<DepartmentAddPO> departmentAddPOS = departmentMapper.selectList(queryWrapper);
        if (departmentAddPOS != null && departmentAddPOS.size() > 0) {
            JSONArray children = parentJson.getJSONArray("children");
            departmentAddPOS.stream().forEach(departmentAddPO -> {
                JSONArray correlationPersons = new JSONArray();
                JSONObject departmentJson = new JSONObject();
                departmentToJson(departmentJson, departmentAddPO, parentJson);
                children.add(departmentJson);
                QueryWrapper<PositionAddPO> relPosWrapper = new QueryWrapper<>();
                relPosWrapper.eq("dep_id", departmentAddPO.getId());
                relPosWrapper.eq("valid", 1);
                List<PositionAddPO> positions = positionMapper.selectList(relPosWrapper);
                departmentJson.put("correlationPersons", correlationPersons);
                if (positions != null && positions.size() > 0) {
                    List<Long> posIds = new ArrayList<>();
                    positions.stream().forEach(pos -> posIds.add(pos.getId()));
                    QueryWrapper<PositionPersonPO> posPersonWrapper = new QueryWrapper<>();
                    posPersonWrapper.in("position_id", posIds);
                    posPersonWrapper.eq("valid", 1);
                    List<PositionPersonPO> personRels = positionPersonMapper.selectList(posPersonWrapper);
                    if (personRels != null && personRels.size() > 0) {
                        List<Long> personIds = new ArrayList<>();
                        personRels.stream().forEach(personRel -> personIds.add(personRel.getPersonId()));
                        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
                        personWrapper.in("id", personIds);
                        personWrapper.eq("valid", 1);
                        List<PersonAddPO> persons = personMapper.selectList(personWrapper);
                        List<Long> personUserIds = new ArrayList<>();
                        if (persons != null && persons.size() > 0) {
                            persons.stream().forEach(person -> {
                                JSONObject personJson = new JSONObject();
                                personToJson(personJson, person);
                                correlationPersons.add(personJson);
                                personUserIds.add(person.getId());
                            });
                            /*StringBuilder url = new StringBuilder("http://auth-service:8080/api/auth/users/person/list?");
                            for (String staffCode : personCodes) {
                                url.append("staffCode=" + staffCode + "&");
                            }*/
                            StringBuilder personIdSb = new StringBuilder();
                            for (Long staffId : personUserIds) {
                                personIdSb.append(staffId).append(",");
                            }
                            personIdSb.deleteCharAt(personIdSb.length() - 1);

                            Map<Long, UserDetailDTO> userResult = organizationAdapter.getUsersDetailByPerson(personIdSb.toString());
                            if (userResult != null && userResult.size() > 0) {
                                for (int i = 0; i < correlationPersons.size(); i++) {
                                    JSONObject json = correlationPersons.getJSONObject(i);
                                    UserDetailDTO userDetailDTO = userResult.get(json.getLong("id"));
                                    if (userDetailDTO != null) {
                                        json.put("correlationUser", userDetailDTO.getUserName());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if (StringUtils.isBlank(deep) || !"1".equals(deep)) {
                    getDepartmentChildren(departmentJson, departmentAddPO.getId(), noBear, deep, companyPO, tenantId);
                }
            });
        }
    }

    private void getPositionChildren(JSONObject parentJson, Long parentId, String noBear, String deep, CompanyPO companyPO, String tenantId) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("company_id", companyPO.getId());
        if (parentId == null) {
            queryWrapper.isNull("parent_id");
        } else {
            queryWrapper.eq("parent_id", parentId);
        }
        queryWrapper.eq("valid", 1);
        List<PositionAddPO> positionAddPOS = positionMapper.selectList(queryWrapper);
        if (positionAddPOS != null && positionAddPOS.size() > 0)  {
            JSONArray children = parentJson.getJSONArray("children");
            positionAddPOS.stream().forEach(positionAddPO -> {
                JSONArray correlationPersons = new JSONArray();
                JSONObject positionJson = new JSONObject();
                positionToJson(positionJson, positionAddPO, parentJson);
                children.add(positionJson);

                positionJson.put("correlationPersons", correlationPersons);
                QueryWrapper<PositionPersonPO> posPersonWrapper = new QueryWrapper<>();
                posPersonWrapper.in("id", positionAddPO.getId());
                posPersonWrapper.eq("valid", 1);
                List<PositionPersonPO> personRels = positionPersonMapper.selectList(posPersonWrapper);
                if (personRels != null && personRels.size() > 0) {
                    List<Long> personIds = new ArrayList<>();
                    personRels.stream().forEach(personRel -> personIds.add(personRel.getPersonId()));
                    QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
                    personWrapper.in("id", personIds);
                    personWrapper.eq("valid", 1);
                    List<PersonAddPO> persons = personMapper.selectList(personWrapper);
                    List<Long> personUserIds = new ArrayList<>();
                    if (persons != null && persons.size() > 0) {
                        persons.stream().forEach(person -> {
                            JSONObject personJson = new JSONObject();
                            personToJson(personJson, person);
                            correlationPersons.add(personJson);
                            personUserIds.add(person.getId());
                        });
                        StringBuilder personIdSb = new StringBuilder();
                        for (Long staffId : personUserIds) {
                            personIdSb.append(staffId).append(",");
                        }
                        personIdSb.deleteCharAt(personIdSb.length() - 1);

                        Map<Long, UserDetailDTO> userResult = organizationAdapter.getUsersDetailByPerson(personIdSb.toString());
                        if (userResult != null && userResult.size() > 0) {
                            for (int i = 0; i < correlationPersons.size(); i++) {
                                JSONObject json = correlationPersons.getJSONObject(i);
                                UserDetailDTO userDetailDTO = userResult.get(json.getLong("id"));
                                if (userDetailDTO != null) {
                                    json.put("correlationUser", userDetailDTO.getUserName());
                                    break;
                                }
                            }
                        }
                    }
                }
                if (StringUtils.isBlank(deep) || !"1".equals(deep)) {
                    getPositionChildren(positionJson, positionAddPO.getId(), noBear, deep, companyPO, tenantId);
                }
            });
        }
    }

    @Override
    public JSONObject getPersonChose(String nodeName, String curNodeName, String type, Boolean isAll, String keywords, Integer page, Integer per_page, String noBear, String tenantId) {
        if (page == null || page == 0) {
            page = 1;
        }
        if (per_page == null || per_page == 0) {
            per_page = 20;
        }
        if (isAll != null && isAll) {
            per_page = 10000;
        }
        JSONObject result = new JSONObject();

        JSONObject pagination = new JSONObject();
        JSONArray personArray = new JSONArray();
        pagination.put("total", 0);
        pagination.put("current", 1);
        pagination.put("pageSize", 20);
        result.put("pagination", pagination);
        result.put("list", personArray);
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();
        posWrapper.eq("old_id", curNodeName);
        posWrapper.eq("valid", 1);
        PositionAddPO positionAddPO = positionMapper.selectOne(posWrapper);

        List<PositionAddPO> positions = null;
        if (positionAddPO == null) {
            QueryWrapper<DepartmentAddPO> departmentWrapper = new QueryWrapper<>();
            departmentWrapper.eq("old_id", curNodeName);
            departmentWrapper.eq("valid", 1);
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(departmentWrapper);
            if (departmentAddPO == null) {
                return result;
            }
            List<Long> deptIds = new ArrayList<>();
            deptIds.add(departmentAddPO.getId());
            if (StringUtils.isNotBlank(keywords)) {
                QueryWrapper<DepartmentAddPO> subDeptWrapper = new QueryWrapper<>();
                subDeptWrapper.likeRight("full_path", departmentAddPO.getFullPath() + "/");
                subDeptWrapper.eq("valid", 1);
                List<DepartmentAddPO> depts = departmentMapper.selectList(subDeptWrapper);
                if (depts != null && depts.size() > 0) {
                    depts.stream().forEach(dept -> deptIds.add(dept.getId()));
                }
            }
            QueryWrapper<PositionAddPO> allPosWrapper = new QueryWrapper<>();
            allPosWrapper.in("dep_id", deptIds);
            allPosWrapper.eq("valid", 1);
            positions = positionMapper.selectList(allPosWrapper);
            if (positions == null || positions.size() == 0) {
                return result;
            }
        } else {
            if (StringUtils.isNotBlank(keywords)) {
                QueryWrapper<PositionAddPO> subPosWrapper = new QueryWrapper<>();
                subPosWrapper.likeRight("full_path", positionAddPO.getFullPath() + "/");
                subPosWrapper.eq("valid", 1);
                positions = positionMapper.selectList(subPosWrapper);
            }
            if (positions == null) {
                positions = new ArrayList<>();
            }
            positions.add(positionAddPO);
        }


        List<Long> posIds = new ArrayList<>();
        positions.stream().forEach(pos -> posIds.add(pos.getId()));
        QueryWrapper<PositionPersonPO> personRelWrapper = new QueryWrapper<>();
        personRelWrapper.eq("valid", 1);
        personRelWrapper.in("position_id", posIds);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(personRelWrapper);
        if (relations == null || relations.size() == 0) {
            return result;
        }
        List<Long> personIds = new ArrayList<>();
        relations.stream().forEach(relation -> personIds.add(relation.getPersonId()));
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
        personWrapper.in("id", personIds);
        personWrapper.eq("valid", 1);
        if (StringUtils.isNotBlank(keywords)) {
            personWrapper.and(item -> item.like("code", keywords).or().like("name", keywords));
        }

        Integer count = personMapper.selectCount(personWrapper);
        Page<PersonAddPO> pageInfo = new Page<PersonAddPO>(page, per_page, count);
        personMapper.selectPage(pageInfo, personWrapper);
        pagination.put("total", count);
        if (pageInfo.getRecords() == null || pageInfo.getRecords().size() == 0) {
            return result;
        }
        QueryWrapper<PositionAddPO> nodePosWrapper = new QueryWrapper<>();
        nodePosWrapper.eq("valid", 1);
        nodePosWrapper.eq("old_id", nodeName);
        PositionAddPO nodePosition = positionMapper.selectOne(nodePosWrapper);
        List<PositionAddPO> nodePositions = new ArrayList<>();
        if (nodePosition == null) {
            QueryWrapper<DepartmentAddPO> nodeDeptWrapper = new QueryWrapper<>();
            nodeDeptWrapper.eq("valid", 1);
            nodeDeptWrapper.eq("old_id", nodeName);
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(nodeDeptWrapper);
            if (departmentAddPO != null) {
                QueryWrapper<PositionAddPO> deptPosWrapper = new QueryWrapper<>();
                deptPosWrapper.eq("valid", 1);
                deptPosWrapper.eq("dep_id", departmentAddPO.getId());
                nodePositions = positionMapper.selectList(deptPosWrapper);
            }
        } else {
            nodePositions.add(nodePosition);
        }
        List<String> nodePersonCodes = new ArrayList<>();
        List<Long> nodePosids = new ArrayList<>();
        List<Long> nodePersonIds = new ArrayList<>();
        if (nodePositions != null && nodePositions.size() > 0) {
            nodePositions.stream().forEach(node -> nodePosids.add(node.getId()));
            QueryWrapper<PositionPersonPO> nodeRelWrapper = new QueryWrapper<>();
            nodeRelWrapper.eq("valid", 1);
            nodeRelWrapper.in("position_id", nodePosids);
            List<PositionPersonPO> nodeRels = positionPersonMapper.selectList(nodeRelWrapper);
            if (nodeRels != null) {
                nodeRels.stream().forEach(nodeRel -> nodePersonIds.add(nodeRel.getPersonId()));
            }
        }
        List<Long> personIdsForUser = new ArrayList<>();
        for (PersonAddPO personAddPO : pageInfo.getRecords()) {
            JSONObject personJson = personPOToJson(personAddPO, type);
            if (nodePersonIds.contains(personAddPO.getId())) {
                personJson.put("selection", true);
            }
            personIdsForUser.add(personAddPO.getId());
            personArray.add(personJson);
        }

        StringBuilder personIdsSb = new StringBuilder();
        for (Long personId : personIdsForUser) {
            personIdsSb.append(personId).append(",");
        }
        personIdsSb.deleteCharAt(personIdsSb.length() - 1);
        Map<Long, UserDetailDTO> userResult = organizationAdapter.getUsersDetailByPerson(personIdsSb.toString());
        if (userResult == null || userResult.size() == 0) {
            return result;
        }

        for (int i = 0; i < personArray.size(); i++) {
            JSONObject json = personArray.getJSONObject(i);
            UserDetailDTO userDetailDTO = userResult.get(json.getLong("id"));
            if (userDetailDTO == null) {
                continue;
            }
            json.put("userName", userDetailDTO.getUserName());
        }
        return result;
    }
    private JSONObject personPOToJson(PersonAddPO personAddPO, String type) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", personAddPO.getId());
        jsonObject.put("showName", personAddPO.getName());
        jsonObject.put("code", personAddPO.getCode());
        jsonObject.put("selection", false);
        jsonObject.put("name", personAddPO.getOldId());
        jsonObject.put("userName", "");
        jsonObject.put("organizations", new JSONArray());
        QueryWrapper<PositionPersonPO> relWrapper = new QueryWrapper<>();
        relWrapper.eq("valid", 1);
        relWrapper.eq("person_id", personAddPO.getId());
        List<PositionPersonPO> rels = positionPersonMapper.selectList(relWrapper);
        if (rels == null || rels.size() == 0) {
            return jsonObject;
        }
        List<Long> posIds = new ArrayList<>();
        rels.stream().forEach(rel -> posIds.add(rel.getPositionId()));
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();
        posWrapper.eq("valid", 1);
        posWrapper.in("id", posIds);
        List<PositionAddPO> positions = positionMapper.selectList(posWrapper);
        if (positions == null || positions.size() == 0) {
            return jsonObject;
        }
        JSONArray orgs = new JSONArray();
        if (Constants.DEPARTMENT.equals(type)) {
            List<Long> deptIds = new ArrayList<>();
            positions.stream().forEach(pos -> deptIds.add(pos.getDepId()));
            QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<>();
            deptWrapper.eq("valid", 1);
            deptWrapper.in("id", deptIds);
            List<DepartmentAddPO> depts = departmentMapper.selectList(deptWrapper);
            if (depts == null || depts.size() == 0) {
                return jsonObject;
            }

            depts.stream().forEach(dept -> orgs.add(dept.getFullPath()));
        } else {
            positions.stream().forEach(pos -> orgs.add(pos.getFullPath()));
        }
        jsonObject.put("organizations", orgs);
        return jsonObject;
    }

    @Override
    public JSONObject queryOrgDetailByCode(String code, String type) {
        JSONObject result = new JSONObject();
        if (Constants.DEPARTMENT.equals(type)) {
            QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("code", code);
            queryWrapper.eq("valid", 1);
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(queryWrapper);
            if (departmentAddPO == null) {
                return result;
            }
            CompanyPO companyPO = getById(departmentAddPO.getCompanyId());
            departmentToJsonNoParent(result, departmentAddPO, companyPO);
        } else if (Constants.POSITION.equals(type)) {
            QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("code", code);
            queryWrapper.eq("valid", 1);
            PositionAddPO positionAddPO = positionMapper.selectOne(queryWrapper);
            if (positionAddPO == null) {
                return result;
            }
            CompanyPO companyPO = getById(positionAddPO.getCompanyId());
            positionToJsonNoParent(result, positionAddPO, companyPO);
        } else {
            QueryWrapper<CompanyPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("code", code);
            queryWrapper.eq("valid", 1);
            CompanyPO companyPO = getOne(queryWrapper);
            if (companyPO == null) {
                return result;
            }
            companyToJson(result, companyPO);
        }
        return result;
    }

    @Override
    public CompanyPO findCompanyByCode(String code) {
        QueryWrapper<CompanyPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CompanyPO::getValid, true).eq(CompanyPO::getCode, code);
        CompanyPO companyPO = getOne(queryWrapper);
        return companyPO;
    }

    @Override
    public List<Long> querySupCompaniesById(Long companyId) {

        CompanyPO companyPO = getById(companyId);
        if (companyPO == null || StringUtils.isBlank(companyPO.getLayRec())) {
            return null;
        }
        String layRec = companyPO.getLayRec();
        String[] recs = layRec.split("-");
        List<Long> list = new ArrayList<>();
        for (String rec : recs) {
            list.add(Long.valueOf(rec));
        }
        return list;
    }

    @Override
    public PageResult<CompanyPO> loadCompanies(Integer current, Integer pageSize, Long fromTime) {
        QueryWrapper<CompanyPO> companyWrapper = new QueryWrapper<>();

        if (fromTime != null) {
            companyWrapper.and(item -> {
                item.gt("modify_time", new Date(fromTime)).or().gt("create_time", new Date(fromTime));
            });
        } else {
            companyWrapper.eq("valid", true);
        }
        int count = count(companyWrapper);
        if (count == 0) {
            return new PageResult<>(null, count, pageSize, current);
        }
        Page<CompanyPO> pageInfo = new Page<CompanyPO>(current, pageSize, count);
        page(pageInfo, companyWrapper);
        PageResult<CompanyPO> pageResult = new PageResult<>(pageInfo.getRecords(), count, pageSize, current);
        return pageResult;
    }

    @Override
    public PageResult<PersonResultBO> queryPersonsByCompanyId(Long companyId, Integer current, Integer pageSize) {
        QueryWrapper<CompanyPersonPO> comRelWrapper = new QueryWrapper<>();
        comRelWrapper.lambda().eq(CompanyPersonPO::getCompanyId, companyId).eq(CompanyPersonPO::getValid, true);
        List<CompanyPersonPO> comRelList = companyPersonMapper.selectList(comRelWrapper);
        if (comRelList == null || comRelList.size() == 0) {
            return null;
        }
        List<Long> personIds = new ArrayList<>();
        comRelList.stream().forEach(comRel -> {
            personIds.add(comRel.getPersonId());
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
    public PageResult<CompanyBO> queryCompaniesPages(Integer current, Integer pageSize) {
        QueryWrapper<CompanyPO> companyWrapper = new QueryWrapper<>();
        companyWrapper.lambda().eq(CompanyPO::getValid, true);
        Integer count = count(companyWrapper);
        if (count == null || count == 0) {
            return null;
        }
        Page<CompanyPO> page = page(new Page(current, pageSize, count), companyWrapper);
        if (page == null || page.getRecords() == null || page.getRecords().size() == 0) {
            return null;
        }
        List<CompanyBO> boList = new ArrayList<>();
        page.getRecords().stream().forEach(companyPO -> {
            CompanyBO companyBO = new CompanyBO();
            BeanUtils.copyProperties(companyPO, companyBO);
            boList.add(companyBO);
        });

        return new PageResult<>(boList, count, pageSize, current);
    }

    @Override
    public PageResult<CompanyDetailInfoBO> getCompanies(String modifyTime, Integer current, Integer pageSize) {
        String dbType = dataId.getDataId();
        Integer count = companyMapper.getCompanyCount(modifyTime, dbType);
        if (count == null || count == 0) {
            return new PageResult<>(new ArrayList<>(), 0, pageSize, current);
        }

        List<CompanyDetailInfoPO> companyDetailInfoPOS = companyMapper.getCompanies(modifyTime, current, pageSize, dbType);
        if (companyDetailInfoPOS == null || companyDetailInfoPOS.size() == 0) {
            return new PageResult<>(new ArrayList<>(), 0, pageSize, current);
        }
        List<Long> companyIds = new ArrayList<>();
        for (CompanyDetailInfoPO companyDetailInfoPO : companyDetailInfoPOS) {
            companyIds.add(companyDetailInfoPO.getId());
        }
        QueryWrapper<CompanyTagPO> companyTagPOQueryWrapper = new QueryWrapper<>();
        companyTagPOQueryWrapper.lambda().in(CompanyTagPO::getCompanyId, companyIds);
        List<CompanyTagPO> tags = companyTagMapper.selectList(companyTagPOQueryWrapper);
        Map<Long, List<String>> companyIdToTagMap = new HashMap<>();
        if (tags != null && tags.size() > 0) {
            for (CompanyTagPO companyTagPO : tags) {
                if (companyIdToTagMap.get(companyTagPO.getCompanyId()) == null) {
                    companyIdToTagMap.put(companyTagPO.getCompanyId(), new ArrayList<String>());
                }
                List<String> comTags = companyIdToTagMap.get(companyTagPO.getCompanyId());
                comTags.add(companyTagPO.getName());
            }
        }
        List<CompanyDetailInfoBO> companyDetailInfoBOS = new ArrayList<>();
        for (CompanyDetailInfoPO companyDetailInfoPO : companyDetailInfoPOS) {
            // 时间格式转换
            if (null != companyDetailInfoPO.getModifyTime()) {
                String formatTime = responseFormatTime(companyDetailInfoPO.getModifyTime());
                companyDetailInfoPO.setModifyTime(formatTime);
            }
            CompanyDetailInfoBO companyDetailInfoBO = new CompanyDetailInfoBO();
            BeanUtils.copyProperties(companyDetailInfoPO, companyDetailInfoBO);
            companyDetailInfoBO.setTags(companyIdToTagMap.get(companyDetailInfoPO.getId()));
            companyDetailInfoBOS.add(companyDetailInfoBO);
        }
        return new PageResult<>(companyDetailInfoBOS, count, pageSize, current);
    }

    @Override
    public Result<CompanyDetailInfoBO> getCompanyByCode(String companyCode) {
        CompanyDetailInfoPO companyPO = companyMapper.getCompanyByCode(companyCode);
        if (null == companyPO) {
            throw new BizHttpStatusException(OrganizationErrorEnum.COMPANY_PARAM_ID_NECESSARY, 400);
        }
        // 时间格式转换
        if (null != companyPO.getModifyTime()) {
            String formatTime = responseFormatTime(companyPO.getModifyTime());
            companyPO.setModifyTime(formatTime);
        }

        QueryWrapper<CompanyTagPO> companyTagPOQueryWrapper = new QueryWrapper<>();
        companyTagPOQueryWrapper.lambda().eq(CompanyTagPO::getCompanyId, companyPO.getId());
        List<CompanyTagPO> tags = companyTagMapper.selectList(companyTagPOQueryWrapper);
        Map<Long, List<String>> companyIdToTagMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(tags)) {
            for (CompanyTagPO companyTagPO : tags) {
                if (companyIdToTagMap.get(companyTagPO.getCompanyId()) == null) {
                    companyIdToTagMap.put(companyTagPO.getCompanyId(), new ArrayList<String>());
                }
                List<String> comTags = companyIdToTagMap.get(companyTagPO.getCompanyId());
                comTags.add(companyTagPO.getName());
            }
        }
        CompanyDetailInfoBO companyDetailInfoBO = new CompanyDetailInfoBO();
        BeanUtils.copyProperties(companyPO, companyDetailInfoBO);
        companyDetailInfoBO.setTags(companyIdToTagMap.get(companyPO.getId()));

        return new Result<>(companyDetailInfoBO);
    }

    @Override
    public PageResult<CompanyDetailInfoBO> getSubCompaniesByCode(String companyCode, String keyword, Boolean isMultistage, Integer current, Integer pageSize) {
        String dbType = this.dataId.getDataId();
        CompanyDetailInfoPO initCompanyPO = companyMapper.getCompanyByCode(companyCode);
        if (ObjectUtils.isEmpty(initCompanyPO)) {
            throw new OrganizationException(DepartmentErrorEnum.DEPARTMENT_COMPANYID_NOT_EXISTS);
        }
        if (!ObjectUtils.isEmpty(keyword)) {
            keyword = "%" + keyword + "%";
        }

        List<CompanyDetailInfoPO> subCompaniesByCode = companyMapper.getSubCompaniesByCode(initCompanyPO.getFullPath() + "/%", keyword, Integer.parseInt(initCompanyPO.getLayNo()), isMultistage, current, pageSize, dbType);
        Integer total = companyMapper.getSubCompaniesTotal(initCompanyPO.getFullPath() + "/%", keyword, Integer.parseInt(initCompanyPO.getLayNo()), isMultistage);
        for (int i = 0; i < subCompaniesByCode.size(); i++) {
            CompanyDetailInfoPO companyDetailInfoPO = subCompaniesByCode.get(i);
            // 时间格式转换
            if (null != companyDetailInfoPO.getModifyTime()) {
                String formatTime = OrgBaseUtils.responseFormatTime(companyDetailInfoPO.getModifyTime());
                companyDetailInfoPO.setModifyTime(formatTime);
            }

            //标签
            List<String> tags = companyTagService.getCompanyTagById(companyDetailInfoPO.getId());
            companyDetailInfoPO.setTags(tags);
        }

        List<CompanyDetailInfoBO> result = Lists.newArrayList();
        for (CompanyDetailInfoPO companyDetailInfoPO : subCompaniesByCode) {
            CompanyDetailInfoBO companyDetailInfoBO = new CompanyDetailInfoBO();
            BeanUtils.copyProperties(companyDetailInfoPO, companyDetailInfoBO);
            result.add(companyDetailInfoBO);
        }
        return new PageResult<>(result, total, pageSize, current);
    }

    @Override
    public PageResult<PersonDetailBO> getCompanyUsers(String companyId, String keyword, Boolean onlyUser, Integer current, Integer pageSize) {
        keyword = ObjectUtils.isEmpty(keyword) ? "" : "%" + keyword + "%";
        String dbType = this.dataId.getDataId();
        //根据公司id和keyword查询人员用户信息
        List<CompanyPersonPO> companyPersonPOS = personMapper.queryPersonOfCompany(companyId, keyword, onlyUser,current, pageSize, dbType);
        Integer total = personMapper.totalPersonOfCompany(companyId, onlyUser,keyword);
        List<Long> personIds = new ArrayList<>();
        if (!ObjectUtils.isEmpty(companyPersonPOS)) {
            personIds = companyPersonPOS.stream().map(CompanyPersonPO::getPersonId).collect(Collectors.toList());
        } else {
            return new PageResult<>(new ArrayList<>(), total, pageSize, current);
        }

        List<PersonDetailBO> personDetailBOS = new ArrayList<>();
        List<PersonAddPO> personAddPOS = personMapper.selectList(new QueryWrapper<PersonAddPO>().in("id", personIds));
        for (PersonAddPO personAddPO : personAddPOS) {
            //人员信息
            PersonDetailBO personDetailBO = new PersonDetailBO();
            BeanUtils.copyProperties(personAddPO, personDetailBO);
            personDetailBOS.add(personDetailBO);

            Long personId = personDetailBO.getId();

            //岗位信息
            List<MainPositionBO> mainPositionBOS = new ArrayList<>();
            personDetailBO.setPositionFullPath(mainPositionBOS);
            personDetailBO.setPosition(mainPositionBOS);

            QueryWrapper<PositionPersonPO> positionPersonWrapper = new QueryWrapper<>();
            positionPersonWrapper.select("position_id");
            positionPersonWrapper.eq("person_id", personId);
            positionPersonWrapper.eq("valid", 1);
            List<PositionPersonPO> positionPersonPOS = positionPersonMapper.selectList(positionPersonWrapper);
            List<Long> positionIds = positionPersonPOS.stream().map(PositionPersonPO::getPositionId).collect(Collectors.toList());

            QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<>();
            positionWrapper.in("id", positionIds);
            positionWrapper.in("valid", 1);
            List<PositionAddPO> positionAddPOS = positionMapper.selectList(positionWrapper);

            for (PositionAddPO positionAddPO : positionAddPOS) {
                MainPositionBO mainPositionBO = new MainPositionBO();
                BeanUtils.copyProperties(positionAddPO, mainPositionBO);
                if (personDetailBO.getMainPosition().equals(positionAddPO.getId())) {
                    mainPositionBO.setMainPosition(true);
                }
                mainPositionBOS.add(mainPositionBO);
            }

            //部门信息
            List<RelationDepartmentBO> relationDepartmentBOS = new ArrayList<>();
            personDetailBO.setDepartmentFullPath(relationDepartmentBOS);
            personDetailBO.setDepartment(relationDepartmentBOS);

            QueryWrapper<DepartmentPersonPO> departmentPersonWrapper = new QueryWrapper<>();
            departmentPersonWrapper.select("dept_id");
            departmentPersonWrapper.eq("person_id", personId);
            departmentPersonWrapper.eq("valid", 1);
            List<DepartmentPersonPO> departmentPersonPOS = departmentPersonMapper.selectList(departmentPersonWrapper);
            List<Long> departmentIds = departmentPersonPOS.stream().map(DepartmentPersonPO::getDeptId).collect(Collectors.toList());

            QueryWrapper<DepartmentAddPO> departmentWrapper = new QueryWrapper<>();
            departmentWrapper.in("id", departmentIds);
            departmentWrapper.eq("valid", 1);
            List<DepartmentAddPO> departmentAddPOS = departmentMapper.selectList(departmentWrapper);

            for (DepartmentAddPO departmentAddPO : departmentAddPOS) {
                RelationDepartmentBO relationDepartmentBO = new RelationDepartmentBO();
                BeanUtils.copyProperties(departmentAddPO, relationDepartmentBO);
                relationDepartmentBOS.add(relationDepartmentBO);
            }
        }

        return new PageResult<>(personDetailBOS, total, pageSize, current);
    }
}
