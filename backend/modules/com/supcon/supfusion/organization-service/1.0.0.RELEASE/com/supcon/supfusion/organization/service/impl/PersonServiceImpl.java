package com.supcon.supfusion.organization.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.auth.api.UserApiService;
import com.supcon.supfusion.auth.api.dto.PersonRoleDTO;
import com.supcon.supfusion.auth.api.dto.UserDetailDTO;
import com.supcon.supfusion.auth.api.dto.UserPersonNames;
import com.supcon.supfusion.framework.cloud.common.exception.BizHttpStatusException;
import com.supcon.supfusion.framework.cloud.common.message.Message;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.DepartmentErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationException;
import com.supcon.supfusion.organization.common.exception.PositionErrorEnum;
import com.supcon.supfusion.organization.common.exception.PositionException;
import com.supcon.supfusion.organization.common.kafka.OrganizationMessage;
import com.supcon.supfusion.organization.common.utils.DistinctUtils;
import com.supcon.supfusion.organization.common.utils.ExcelUtils;
import com.supcon.supfusion.organization.common.utils.FileToolUtils;
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
import com.supcon.supfusion.organization.dao.po.company.CompanyDetailInfoPO;
import com.supcon.supfusion.organization.dao.po.company.CompanyPO;
import com.supcon.supfusion.organization.dao.po.company.CompanyPersonPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentAddPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentPersonPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentSynchronizationInfoPO;
import com.supcon.supfusion.organization.dao.po.excel.ExcelPO;
import com.supcon.supfusion.organization.dao.po.person.*;
import com.supcon.supfusion.organization.dao.po.position.PositionAddPO;
import com.supcon.supfusion.organization.dao.po.position.PositionPersonPO;
import com.supcon.supfusion.organization.dao.po.position.PositionRolePO;
import com.supcon.supfusion.organization.dao.po.position.PositionSynchronizationInfoPO;
import com.supcon.supfusion.organization.manager.OrganizationAdapter;
import com.supcon.supfusion.organization.service.BaseServiceService;
import com.supcon.supfusion.organization.service.CompanyPersonService;
import com.supcon.supfusion.organization.service.DepartmentPersonService;
import com.supcon.supfusion.organization.service.DepartmentService;
import com.supcon.supfusion.organization.service.ExcelManageService;
import com.supcon.supfusion.organization.service.GroupPersonService;
import com.supcon.supfusion.organization.service.OrgMnecodeService;
import com.supcon.supfusion.organization.service.PersonService;
import com.supcon.supfusion.organization.service.PositionPersonService;
import com.supcon.supfusion.organization.service.PositionService;
import com.supcon.supfusion.organization.service.bo.baseService.BaseInfoBO;
import com.supcon.supfusion.organization.service.bo.baseService.CompanyBaseServiceBO;
import com.supcon.supfusion.organization.service.bo.baseService.DepartmentBaseServiceBO;
import com.supcon.supfusion.organization.service.bo.baseService.PersonBaseServiceBO;
import com.supcon.supfusion.organization.service.bo.baseService.PositionBaseServiceBO;
import com.supcon.supfusion.organization.service.bo.baseService.StaffDetailInfoBO;
import com.supcon.supfusion.organization.service.bo.company.CompanyBO;
import com.supcon.supfusion.organization.service.bo.company.CompanyResultBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentDetailBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentResultBO;
import com.supcon.supfusion.organization.service.bo.excel.PositionPersonExcelBO;
import com.supcon.supfusion.organization.service.bo.kafka.PersonDeleteMessageBO;
import com.supcon.supfusion.organization.service.bo.kafka.PersonMessageBO;
import com.supcon.supfusion.organization.service.bo.person.MainPositionBO;
import com.supcon.supfusion.organization.service.bo.person.MainPositionBaseBO;
import com.supcon.supfusion.organization.service.bo.person.PersonAddOpenBO;
import com.supcon.supfusion.organization.service.bo.person.PersonBO;
import com.supcon.supfusion.organization.service.bo.person.PersonBaseInfoBO;
import com.supcon.supfusion.organization.service.bo.person.PersonBulkOperateOpenBO;
import com.supcon.supfusion.organization.service.bo.person.PersonCompanyBaseBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDepartmentBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDepartmentBaseBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import com.supcon.supfusion.organization.service.bo.person.PersonFlowSimpleBO;
import com.supcon.supfusion.organization.service.bo.person.PersonLeaderBO;
import com.supcon.supfusion.organization.service.bo.person.PersonLoginInfoBO;
import com.supcon.supfusion.organization.service.bo.person.PersonOffPositionBO;
import com.supcon.supfusion.organization.service.bo.person.PersonPositionBO;
import com.supcon.supfusion.organization.service.bo.person.PersonPositionTransferBO;
import com.supcon.supfusion.organization.service.bo.person.PersonResultBO;
import com.supcon.supfusion.organization.service.bo.person.PersonSynchronizationInfoBO;
import com.supcon.supfusion.organization.service.bo.person.PersonTransferBO;
import com.supcon.supfusion.organization.service.bo.person.PersonUpdateOpenBO;
import com.supcon.supfusion.organization.service.bo.person.PersonUpdatePageBO;
import com.supcon.supfusion.organization.service.bo.person.PersonUserBO;
import com.supcon.supfusion.organization.service.bo.person.RelationDepartmentBO;
import com.supcon.supfusion.organization.service.bo.person.SystemCodeBO;
import com.supcon.supfusion.organization.service.bo.person.UserBO;
import com.supcon.supfusion.organization.service.bo.position.PositionDetailBO;
import com.supcon.supfusion.organization.service.bo.position.PositionResultBO;
import com.supcon.supfusion.organization.service.common.MultiParamSql;
import com.supcon.supfusion.rbac.api.dto.RoleUserDTO;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.bag.TreeBag;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import sun.misc.BASE64Encoder;

import javax.annotation.Resource;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.supcon.supfusion.organization.common.utils.OrgBaseUtils.responseFormatTime;

/**
 * 人员服务处理实现类
 * @author
 * @date 20-6-3 下午14:48
 */
@Slf4j
@Service
public class PersonServiceImpl extends ServiceImpl<PersonMapper, PersonAddPO> implements PersonService {

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private PositionPersonMapper positionPersonMapper;

    @Autowired
    private PositionPersonService positionPersonService;

    @Resource
    private GroupPersonService groupPersonService;

    @Autowired
    private OrganizationAdapter organizationAdapter;

    @Autowired
    private PositionService positionService;

    @Autowired
    private ExcelMapper excelMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private OrganizationManagerMapper organizationManagerMapper;

    @Autowired
    private PositionRoleMapper positionRoleMapper;

    @Autowired
    private ExcelManageService excelManageService;

    @Autowired
    private DepartmentPersonMapper departmentPersonMapper;

    @Autowired
    private CompanyPersonMapper companyPersonMapper;

    @Autowired
    private BaseServiceService baseServiceService;


    @Autowired
    private CompanyPersonService companyPersonService;

    @Autowired
    private DepartmentPersonService departmentPersonService;

    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;
    @Autowired
    private OrgMnecodeService orgMnecodeService;

    @Autowired
    private PersonMapper personMapper;

    @Autowired
    private UserApiService userApiService;

    @Autowired
    private OrganizationMessage organizationMessage;

    private static final String PERSON_TOPIC = "supOS_person_event";

    @Autowired
    private Environment env;

    //@Value("${file.path}")
    //private String picFilePath;

    /**
     * 人员新增方法实现
     * @param personAddPo

     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPerson(PersonAddPO personAddPo, String tenantId) {

        addPersonWithoutKafka(personAddPo, tenantId);

        if (!personAddPo.getCreateUser()) {
            //kafka
            List<PersonAddPO> personAddPOList = new ArrayList<>();
            personAddPOList.add(personAddPo);
            publishCreateOrUpdateMessage(personAddPOList, tenantId, "CREATE");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPersonAndUser(PersonAddPO personAddPo, String userName, String password, String userDescription, List<Long> roles, String tenantId) {
        PositionAddPO positionAddPo = null;
        if (personAddPo.getMainPosition() != null) {
            positionAddPo = positionMapper.selectById(personAddPo.getMainPosition());
            if (positionAddPo == null) {
                throw new OrganizationException(OrganizationErrorEnum.PERSON_POSITION_ID_NOT_EXISTS);
            }
        } else {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_POSITION_ID_NOT_EXISTS);
        }
        addPerson(personAddPo, tenantId);
        if (personAddPo.getCreateUser()) {
            organizationAdapter.createUser(userName, password, userDescription, positionAddPo.getCompanyId(), roles, personAddPo.getId(), 0, null, personAddPo.getCode(), personAddPo.getName());
            //根据用户名获取用户id
            Long userId = organizationAdapter.getUserIdByUserName(userName);

            //保存用户信息
            PersonUserBO personUserBO = new PersonUserBO();
            personUserBO.setPersonId(personAddPo.getId());
            personUserBO.setUserId(userId);
            personUserBO.setUserName(userName);
            this.saveOrUpdateUserByPersonId(personUserBO);

            List<Long> personIds = new ArrayList<>();
            personIds.add(personAddPo.getId());
            List<PersonRoleDTO> posRoles = generatePersonRoleByPositionId(personIds);
            if (posRoles == null || posRoles.size() == 0) {
                return;
            }
            Boolean flag = organizationAdapter.informRoleChange(posRoles);
            if (!flag) {
                throw new OrganizationException(OrganizationErrorEnum.ROLE_CHANGE_ERROR);
            }
            //kafka
            List<PersonAddPO> personAddPOList = new ArrayList<>();
            personAddPOList.add(personAddPo);
            publishCreateOrUpdateMessage(personAddPOList, tenantId, "CREATE");
        }

    }


    /**
     * 人员修改方法实现
     * @param personAddPo
     * @param tenantId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePerson(PersonAddPO personAddPo, String tenantId) {
        updatePersonWithoutKafka(personAddPo, tenantId);

        //kafka
        List<PersonAddPO> personAddPOList = new ArrayList<>();
        personAddPOList.add(personAddPo);
        publishCreateOrUpdateMessage(personAddPOList, tenantId, "UPDATE");
    }

    /**
     * 批量删除人员
     * @param id
     * @param tenantId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePerson(Long[] id, String tenantId) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
        queryWrapper.in("id", id);
        List<PersonAddPO> list = list(queryWrapper);
        if (list == null || list.size() == 0) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Long curId : id) {
            sb.append(curId).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        /*Map<Long, UserDetailDTO> users = organizationAdapter.getUsersDetailByPerson(sb.toString());
        if (users != null && users.size() > 0) {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_BINDING_USER_DELETE_ERROR);
        }*/
        UpdateWrapper<PersonAddPO> updateWrapper = new UpdateWrapper<PersonAddPO>();
        updateWrapper.in("id", id);
        updateWrapper.set("valid", 0);
        update(updateWrapper);
        for (Long curid : id) {
            groupPersonService.deleteByPersonId(curid);
            positionPersonService.deleteByPersonId(curid);
            deletePersonRelation(curid);
        }
        orgMnecodeService.deleteOrgMnecodeByOrgId(Arrays.asList(id), Constants.PERSON);
        Boolean flag = organizationAdapter.deleteUser(id);
        if (!flag) {
            throw new OrganizationException(OrganizationErrorEnum.USER_DELETE_ERROR);
        }
        //删除人员表绑定的用户
        this.deleteUserByPersonIds(Arrays.asList(id));

        //kafka
        publishDeletePersonMessage(list, tenantId);
    }

    private void deletePersonRelation(Long personId) {
        departmentPersonService.deletePerson(personId);
        companyPersonService.deletePerson(personId);
        /*QueryWrapper<DepartmentPersonPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("person_id", personId);
        queryWrapper.eq("valid", 1);
        List<DepartmentPersonPO> depts = departmentPersonMapper.selectList(queryWrapper);
        if (depts != null && depts.size() > 0) {
            depts.stream().forEach(dept -> {
                dept.setValid(false);
                departmentPersonMapper.updateById(dept);
            });
        }
        QueryWrapper<CompanyPersonPO> comWrapper = new QueryWrapper<>();
        comWrapper.eq("person_id", personId);
        comWrapper.eq("valid", 1);
        List<CompanyPersonPO> comps = companyPersonMapper.selectList(comWrapper);
        if (comps != null && comps.size() > 0) {
            comps.stream().forEach(com -> {
                com.setValid(false);
                companyPersonMapper.updateById(com);
            });
        }*/

    }

    /**
     * 分页查询人员列表
     * @param personIds
     * @param current
     * @param pageSize
     * @param conditionQuery
     * @return
     */
    @Override
    public PageResult<PersonDetailBO> queryPersonsById(List<Long> personIds, Integer current, Integer pageSize, PersonDetailBO conditionQuery) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
        // queryWrapper.in("id", personIds);
        queryWrapper.eq("valid", 1);
        queryWrapper.and(item -> {
            for (int i = 0; i < personIds.size(); i++) {
                item.or().eq("id", personIds.get(i));
            }
        });

        handleConditionQuery(conditionQuery, queryWrapper);

        Integer count = count(queryWrapper);
        queryWrapper.orderByDesc("create_time");
        Page<PersonAddPO> pageInfo = new Page<PersonAddPO>(current, pageSize, count);
        page(pageInfo, queryWrapper);
        List<PersonDetailBO> personList = new ArrayList<PersonDetailBO>();
        if (pageInfo.getRecords() == null) {
            return new PageResult<PersonDetailBO>(personList, count, pageSize, current);
        }
        pageInfo.getRecords().stream().forEach(item -> {
            PersonDetailBO personDetailBO = new PersonDetailBO();
            BeanUtils.copyProperties(item, personDetailBO);
            // added by xhf on 2021-02-20
            // directLeaderName
            Long directLeaderId = item.getDirectLeaderId();
            if (null != directLeaderId){
                QueryWrapper<PersonAddPO> queryPersonWrapper = new QueryWrapper<PersonAddPO>();
                queryPersonWrapper.eq("valid", 1);
                queryPersonWrapper.eq("id", directLeaderId);
                personDetailBO.setDirectLeaderName(getOne(queryPersonWrapper).getName());
            }
            // grandLeaderName
            Long grandLeaderId = item.getGrandLeaderId();
            if (null != grandLeaderId){
                QueryWrapper<PersonAddPO> queryPersonWrapper = new QueryWrapper<PersonAddPO>();
                queryPersonWrapper.eq("valid", 1);
                queryPersonWrapper.eq("id", grandLeaderId);
                personDetailBO.setGrandLeaderName(getOne(queryPersonWrapper).getName());
            }
            personList.add(personDetailBO);
        });
        return new PageResult<PersonDetailBO>(personList, count, pageSize, current);
    }


    /**
     * 根据公司id查询人员
     * @param companyId
     * @param keyword
     * @param current
     * @param pageSize
     * @param conditionQuery
     * @return
     */
    @Override
    public PageResult<PersonDetailBO> queryPersonsByCompanyId(Long companyId, List<Long> positionIds, String keyword, Integer current, Integer pageSize, PersonDetailBO conditionQuery) {

        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<PersonAddPO>();
        //personWrapper.eq("company_id", companyId);
        personWrapper.eq("valid", 1);
        List<PersonDetailBO> personList = new ArrayList<PersonDetailBO>();
        if (positionIds != null) {
            List<Long> personIds = positionPersonService.queryPersonIdByPositionIds(positionIds);
            if (personIds == null || personIds.size() == 0) {
                return new PageResult<PersonDetailBO>(personList, 0, pageSize, current);
            }
            personWrapper.and(item -> {
                for (int i = 0; i < personIds.size(); i++) {
                    item.or().eq("id", personIds.get(i));
                }
            });
            //personWrapper.in("id", personIds);
        }  else {
            QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<PositionAddPO>();
            positionWrapper.eq("company_id", companyId);
            positionWrapper.eq("valid", 1);
            positionWrapper.eq("sys_flag", 0);
            List<PositionAddPO> positions = positionMapper.selectList(positionWrapper);
            if (positions == null || positions.size() == 0) {
                return new PageResult<PersonDetailBO>(personList, 0, pageSize, current);
            }
            List<Long> posIds = new ArrayList<Long>();
            positions.stream().forEach(position -> posIds.add(position.getId()));
            List<Long> personIds = positionPersonService.queryPersonIdByPositionIds(posIds);
            if (personIds == null || personIds.size() == 0) {
                return new PageResult<PersonDetailBO>(personList, 0, pageSize, current);
            }
            personWrapper.and(item -> {
                for (int i = 0; i < personIds.size(); i++) {
                    item.or().eq("id", personIds.get(i));
                }
            });
        }

        if (keyword != null && !"".equals(keyword.trim())) {
            String key = dbStringUtil.getString(keyword);
            //获取数据库类型
            String dbType = dataId.getDataId();
            //使用queryWrapper形式
            //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
            if ("oracle".equals(dbType)){
                //personWrapper.apply("name like {0} escape '\\'", "%" + key + "%");

                personWrapper.and(item -> item.apply("name like {0} escape '\\'", "%" + key + "%")
                        .or().apply("code like {0} escape '\\'", "%" + key + "%")
                        .or().apply("phone like {0} escape '\\'", "%" + key + "%"));
            }else{
                personWrapper.and(item -> item.like("name", key).or().like("code", key).or().like("phone", key));
            }

            //personWrapper.and(item -> item.like("name", keyword).or().like("code", keyword).or().like("phone", keyword));
        }

        handleConditionQuery(conditionQuery, personWrapper);

        Integer count = count(personWrapper);
        personWrapper.orderByDesc("create_time");
        Page<PersonAddPO> pageInfo = new Page<PersonAddPO>(current, pageSize, count);
        page(pageInfo, personWrapper);

        if (pageInfo.getRecords() == null) {
            return new PageResult<PersonDetailBO>(personList, count, pageSize, current);
        }
        pageInfo.getRecords().stream().forEach(item -> {
            PersonDetailBO personDetailBO = new PersonDetailBO();
            BeanUtils.copyProperties(item, personDetailBO);
            personList.add(personDetailBO);
        });
        return new PageResult<PersonDetailBO>(personList, count, pageSize, current);
    }

    @Override
    public PageResult<PersonDetailBO> queryPersonsByCompanyIdBetter(Long companyId, List<Long> positionIds, String keyword, Integer current, Integer pageSize, PersonDetailBO conditionQuery) {

        List<PersonDetailBO> personList = new ArrayList<PersonDetailBO>();
        List<String> codeList = new ArrayList<>();
        List<String> nameList = new ArrayList<>();
        List<String> descriptionList = new ArrayList<>();
        List<String> emailList = new ArrayList<>();
        List<String> phoneList = new ArrayList<>();
        List<String> genderList = new ArrayList<>();
        List<String> statusList = new ArrayList<>();
        handleConditionQueryBetter(conditionQuery, codeList, nameList, descriptionList, emailList, phoneList, genderList, statusList);
        String key = null;
        if (keyword != null && !"".equals(keyword.trim())) {
            key = dbStringUtil.getString(keyword);
        }
        Integer allNum = pageSize;
        if (pageSize == -1) {
            allNum = 20000;
        }
        String dbType = dataId.getDataId();
        List<PersonAndLeaderPO> personAddPOList = null;
        Integer count = 0;
        if (positionIds != null) {
            if (positionIds.size() == 0) {
                return new PageResult<PersonDetailBO>(personList, count, pageSize, current);
            }
            count = personMapper.queryPeronsByPositionIdsAndConditionCount(positionIds, key, codeList, nameList, descriptionList, emailList, phoneList, genderList, statusList, dbType);
            if (count == null || count == 0) {
                return new PageResult<PersonDetailBO>(personList, count, pageSize, current);
            }
            personAddPOList = personMapper.queryPeronsByPositionIdsAndCondition(positionIds, key, current, allNum, dbType, codeList, nameList, descriptionList, emailList, phoneList, genderList, statusList);
        } else {
            count = personMapper.queryPeronsByCompanyIdAndConditionCount(companyId, key, codeList, nameList, descriptionList, emailList, phoneList, genderList, statusList, dbType);
            if (count == null || count == 0) {
                return new PageResult<PersonDetailBO>(personList, count, pageSize, current);
            }
            personAddPOList = personMapper.queryPeronsByCompanyIdAndCondition(companyId, key, current, allNum, dbType, codeList, nameList, descriptionList, emailList, phoneList, genderList, statusList);
        }
        if (personAddPOList == null) {
            return new PageResult<PersonDetailBO>(personList, 0, pageSize, current);
        }
        personAddPOList.stream().forEach(item -> {
            PersonDetailBO personDetailBO = new PersonDetailBO();
            BeanUtils.copyProperties(item, personDetailBO);
            personList.add(personDetailBO);
        });
        return new PageResult<PersonDetailBO>(personList, count, pageSize, current);
    }


    /**
     * 优化后的查询人员和组织信息列表
     * @param companyId
     * @param positionIds
     * @param keyword
     * @param current
     * @param pageSize
     * @param conditionQuery
     * @param currentPositionId
     * @return
     */
    @Override
    public PageResult<PersonDetailBO> queryPersonsAndOrgDetailByCompanyIdBetter(Long companyId, List<Long> positionIds, String keyword, Integer current, Integer pageSize, PersonDetailBO conditionQuery, Long currentPositionId) {

        List<PersonDetailBO> personList = new ArrayList<PersonDetailBO>();
        List<String> codeList = new ArrayList<>();
        List<String> nameList = new ArrayList<>();
        List<String> descriptionList = new ArrayList<>();
        List<String> emailList = new ArrayList<>();
        List<String> phoneList = new ArrayList<>();
        List<String> genderList = new ArrayList<>();
        List<String> statusList = new ArrayList<>();
        handleConditionQueryBetter(conditionQuery, codeList, nameList, descriptionList, emailList, phoneList, genderList, statusList);
        String key = null;
        if (keyword != null && !"".equals(keyword.trim())) {
            key = dbStringUtil.getString(keyword);
        }
        Integer allNum = pageSize;
        if (pageSize == -1) {
            allNum = 20000;
        }
        String dbType = dataId.getDataId();
        List<PersonDetailBetterPO> personAddPOList = null;
        Integer count = 0;
        if (positionIds != null) {
            if (positionIds.size() == 0) {
                return new PageResult<PersonDetailBO>(personList, count, pageSize, current);
            }
            count = personMapper.queryPeronsByPositionIdsAndConditionCount(positionIds, key, codeList, nameList, descriptionList, emailList, phoneList, genderList, statusList, dbType);
            if (count == null || count == 0) {
                return new PageResult<PersonDetailBO>(personList, count, pageSize, current);
            }
            personAddPOList = personMapper.queryPeronsOrgDetailByPositionIdsAndCondition(positionIds, key, current, allNum, dbType, codeList, nameList, descriptionList, emailList, phoneList, genderList, statusList);
        } else {
            count = personMapper.queryPeronsByCompanyIdAndConditionCount(companyId, key, codeList, nameList, descriptionList, emailList, phoneList, genderList, statusList, dbType);
            if (count == null || count == 0) {
                return new PageResult<PersonDetailBO>(personList, count, pageSize, current);
            }
            personAddPOList = personMapper.queryPeronsOrgDetailByCompanyIdAndCondition(companyId, key, current, allNum, dbType, codeList, nameList, descriptionList, emailList, phoneList, genderList, statusList);
        }
        if (personAddPOList == null) {
            return new PageResult<PersonDetailBO>(personList, 0, pageSize, current);
        }

        CompanyPO company = companyMapper.selectById(companyId);

        List<SystemCodeResultDTO> genderCodeList = organizationAdapter.querySystemCodesByEntityCode(Constants.GENDER_ENTITYCODE);
        List<SystemCodeResultDTO> statusCodeList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_STATUS_ENTITYCODE);
        List<SystemCodeResultDTO> titleCodeList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_TITLE_ENTITYCODE);
        List<SystemCodeResultDTO> educationCodeList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_EDUCATION_ENTITYCODE);
        Map<String, String> genderMap = new HashMap<>();
        Map<String, String> statusMap = new HashMap<>();
        Map<String, String> titleMap = new HashMap<>();
        Map<String, String> educationMap = new HashMap<>();
        if (genderCodeList != null) {
            genderCodeList.stream().forEach(genderSysCode -> {
                genderMap.put(genderSysCode.getEntityCode() + "/" + genderSysCode.getCode(), genderSysCode.getDisplayName());
            });
        }
        if (statusCodeList != null) {
            statusCodeList.stream().forEach(statusSysCode -> {
                statusMap.put(statusSysCode.getEntityCode() + "/" + statusSysCode.getCode(), statusSysCode.getDisplayName());
            });
        }
        if (titleCodeList != null) {
            titleCodeList.stream().forEach(titleSysCode -> {
                titleMap.put(titleSysCode.getEntityCode() + "/" + titleSysCode.getCode(), titleSysCode.getDisplayName());
            });
        }
        if (educationCodeList != null) {
            educationCodeList.stream().forEach(educationSysCode -> {
                educationMap.put(educationSysCode.getEntityCode() + "/" + educationSysCode.getCode(), educationSysCode.getDisplayName());
            });
        }
        Map<Long, PersonDetailBO> personIdToPersonDetailMap = new HashMap<>(personAddPOList.size());
        List<PersonDetailBO> resultPersons = new ArrayList<>(personAddPOList.size());

        personAddPOList.stream().forEach(personWithOrgDetailBO -> {
            if (StringUtils.isNotBlank(genderMap.get(personWithOrgDetailBO.getGender()))) {
                personWithOrgDetailBO.setGender(genderMap.get(personWithOrgDetailBO.getGender()));
            }
            if (StringUtils.isNotBlank(statusMap.get(personWithOrgDetailBO.getStatus()))) {
                personWithOrgDetailBO.setStatus(statusMap.get(personWithOrgDetailBO.getStatus()));
            }
            if (StringUtils.isNotBlank(titleMap.get(personWithOrgDetailBO.getTitle()))) {
                personWithOrgDetailBO.setTitle(titleMap.get(personWithOrgDetailBO.getTitle()));
            }
            if (StringUtils.isNotBlank(educationMap.get(personWithOrgDetailBO.getEducation()))) {
                personWithOrgDetailBO.setEducation(educationMap.get(personWithOrgDetailBO.getEducation()));
            }

            PersonDetailBO currentPersonDetail = null;
            if (personIdToPersonDetailMap.get(personWithOrgDetailBO.getId()) != null) {
                currentPersonDetail = personIdToPersonDetailMap.get(personWithOrgDetailBO.getId());
            } else {
                currentPersonDetail = new PersonDetailBO();
                BeanUtils.copyProperties(personWithOrgDetailBO, currentPersonDetail);
                personIdToPersonDetailMap.put(currentPersonDetail.getId(), currentPersonDetail);
                resultPersons.add(currentPersonDetail);
            }

            List<MainPositionBO> positionPathList = currentPersonDetail.getPositionFullPath();
            if (positionPathList == null) {
                positionPathList = new ArrayList<MainPositionBO>();
                currentPersonDetail.setPositionFullPath(positionPathList);
            }
            List<MainPositionBO> positionList = currentPersonDetail.getPosition();
            if (currentPersonDetail.getPosition() == null) {
                positionList = new ArrayList<MainPositionBO>();
                currentPersonDetail.setPosition(positionList);
            }



            MainPositionBO mainPositionBO = new MainPositionBO();
            mainPositionBO .setCompanyId(personWithOrgDetailBO.getCompanyId());
            mainPositionBO.setId(personWithOrgDetailBO.getPosId());
            mainPositionBO.setCode(personWithOrgDetailBO.getPosCode());
            mainPositionBO.setName(personWithOrgDetailBO.getPosName());
            mainPositionBO.setDepId(personWithOrgDetailBO.getPosDeptId());
            mainPositionBO.setLayRec(personWithOrgDetailBO.getPosLayRec());
            mainPositionBO.setParentId(personWithOrgDetailBO.getPosParentId());
            mainPositionBO.setFullPath(OrgBaseUtils.splitCompanyFullPath(personWithOrgDetailBO.getPosFullPath(), personWithOrgDetailBO.getComShortName(), personWithOrgDetailBO.getComFullPath()));

            if (personWithOrgDetailBO.getMainPosition().equals(personWithOrgDetailBO.getPosId())) {
                mainPositionBO.setMainPosition(true);
                positionPathList.add(0, mainPositionBO);
                positionList.add(0, mainPositionBO);
            } else {
                positionPathList.add(mainPositionBO);
                positionList.add(mainPositionBO);
            }
            List<RelationDepartmentBO> deptFullPaths = currentPersonDetail.getDepartmentFullPath();
            if (deptFullPaths == null) {
                deptFullPaths = new ArrayList<>();
                currentPersonDetail.setDepartmentFullPath(deptFullPaths);
            }

            List<RelationDepartmentBO> departmentList = currentPersonDetail.getDepartment();
            if (departmentList == null) {
                departmentList = new ArrayList<>();
                currentPersonDetail.setDepartment(departmentList);
            }

            RelationDepartmentBO relationDepartmentBO = new RelationDepartmentBO();
            relationDepartmentBO.setCompanyId(personWithOrgDetailBO.getCompanyId());
            relationDepartmentBO.setId(personWithOrgDetailBO.getDeptId());
            relationDepartmentBO.setCode(personWithOrgDetailBO.getDeptCode());
            relationDepartmentBO.setName(personWithOrgDetailBO.getDeptName());
            relationDepartmentBO.setParentId(personWithOrgDetailBO.getDeptParentId());
            relationDepartmentBO.setLayRec(personWithOrgDetailBO.getDeptLayRec());
            relationDepartmentBO.setFullPath(OrgBaseUtils.splitCompanyFullPath(personWithOrgDetailBO.getDeptFullPath(), personWithOrgDetailBO.getComShortName(), personWithOrgDetailBO.getComFullPath()));

            if (currentPositionId != null && currentPositionId.equals(personWithOrgDetailBO.getPosId())) {
                relationDepartmentBO.setCurrentPosition(true);
            }
            deptFullPaths.add(relationDepartmentBO);
            departmentList.add(relationDepartmentBO);
            currentPersonDetail.setDepartmentFullPath(deptFullPaths);
            currentPersonDetail.setDepartment(departmentList);
        });
        return new PageResult<PersonDetailBO>(resultPersons, count, pageSize, current);
    }

    private void handleConditionQueryBetter(PersonDetailBO conditionQuery, List<String> codeList, List<String> nameList, List<String> descriptionList, List<String> emailList,
                                            List<String> phoneList, List<String> genderList, List<String> statusList) {
        if (StringUtils.isNotBlank(conditionQuery.getCode())) {
            String[] codes = conditionQuery.getCode().split(",");

            for (String code : codes) {
                String key = dbStringUtil.getString(code);
                //获取数据库类型
                String dbType = dataId.getDataId();
                //使用queryWrapper形式
                //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
                codeList.add(key);
            }
        }
        if (StringUtils.isNotBlank(conditionQuery.getName())) {
            String[] names = conditionQuery.getName().split(",");
            for (String name : names) {
                String key = dbStringUtil.getString(name);
                //获取数据库类型
                String dbType = dataId.getDataId();
                //使用queryWrapper形式
                //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
                nameList.add(key);
            }
        }
        if (StringUtils.isNotBlank(conditionQuery.getDescription())) {
            String[] descriptions = conditionQuery.getDescription().split(",");
            for (String desc : descriptions) {
                String key = dbStringUtil.getString(desc);
                //获取数据库类型
                String dbType = dataId.getDataId();
                //使用queryWrapper形式
                //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
                descriptionList.add(key);
            }
        }
        if (StringUtils.isNotBlank(conditionQuery.getEmail())) {
            String[] emails = conditionQuery.getEmail().split(",");
            for (String email : emails) {
                String key = dbStringUtil.getString(email);
                //获取数据库类型
                String dbType = dataId.getDataId();
                //使用queryWrapper形式
                //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
                emailList.add(key);
            }
        }
        if (StringUtils.isNotBlank(conditionQuery.getPhone())) {
            String[] phones = conditionQuery.getPhone().split(",");
            for (String phone : phones) {
                String key = dbStringUtil.getString(phone);
                //获取数据库类型
                String dbType = dataId.getDataId();
                //使用queryWrapper形式
                //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
                phoneList.add(key);
            }
        }

        if (StringUtils.isNotBlank(conditionQuery.getGender())) {
            String[] genders = conditionQuery.getGender().split(",");
            for (String genderCode : genders) {
                genderList.add(Constants.GENDER_ENTITYCODE + "/" + genderCode);
            }
        }
        if (StringUtils.isNotBlank(conditionQuery.getStatus())) {
            String[] statuses = conditionQuery.getStatus().split(",");
            for (String statusCode : statuses) {
                statusList.add(Constants.PERSON_STATUS_ENTITYCODE + "/" + statusCode);
            }
        }
    }
    private void handleConditionQuery(PersonDetailBO conditionQuery, QueryWrapper<PersonAddPO> queryWrapper) {
        if (StringUtils.isNotBlank(conditionQuery.getCode())) {
            String[] codes = conditionQuery.getCode().split(",");

            queryWrapper.and(condition -> {
                for (String code : codes) {
                    String key = dbStringUtil.getString(code);
                    //获取数据库类型
                    String dbType = dataId.getDataId();
                    //使用queryWrapper形式
                    //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
                    if ("oracle".equals(dbType)){
                        //personWrapper.apply("name like {0} escape '\\'", "%" + key + "%");
                        condition.or().apply("code like {0} escape '\\'", "%" + key + "%");
                    }else{
                        condition.or().like("code", key);
                    }
                }
            });
        }
        if (StringUtils.isNotBlank(conditionQuery.getName())) {
            String[] names = conditionQuery.getName().split(",");
            queryWrapper.and(condition -> {
                for (String name : names) {
                    String key = dbStringUtil.getString(name);
                    //获取数据库类型
                    String dbType = dataId.getDataId();
                    //使用queryWrapper形式
                    //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
                    if ("oracle".equals(dbType)){
                        //personWrapper.apply("name like {0} escape '\\'", "%" + key + "%");
                        condition.or().apply("name like {0} escape '\\'", "%" + key + "%");
                    }else{
                        condition.or().like("name", key);
                    }
                }
            });

        }
        if (StringUtils.isNotBlank(conditionQuery.getDescription())) {
            String[] descriptions = conditionQuery.getDescription().split(",");
            queryWrapper.and(condition -> {
                for (String desc : descriptions) {
                    String key = dbStringUtil.getString(desc);
                    //获取数据库类型
                    String dbType = dataId.getDataId();
                    //使用queryWrapper形式
                    //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
                    if ("oracle".equals(dbType)){
                        //personWrapper.apply("name like {0} escape '\\'", "%" + key + "%");
                        condition.or().apply("description like {0} escape '\\'", "%" + key + "%");
                    }else{
                        condition.or().like("description", key);
                    }
                }
            });
        }
        if (StringUtils.isNotBlank(conditionQuery.getEmail())) {
            String[] emails = conditionQuery.getEmail().split(",");
            queryWrapper.and(condition -> {
                for (String email : emails) {
                    String key = dbStringUtil.getString(email);
                    //获取数据库类型
                    String dbType = dataId.getDataId();
                    //使用queryWrapper形式
                    //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
                    if ("oracle".equals(dbType)){
                        //personWrapper.apply("name like {0} escape '\\'", "%" + key + "%");
                        condition.or().apply("email like {0} escape '\\'", "%" + key + "%");
                    }else{
                        condition.or().like("email", key);
                    }
                }
            });
        }
        if (StringUtils.isNotBlank(conditionQuery.getPhone())) {
            String[] phones = conditionQuery.getPhone().split(",");
            queryWrapper.and(condition -> {
                for (String phone : phones) {
                    String key = dbStringUtil.getString(phone);
                    //获取数据库类型
                    String dbType = dataId.getDataId();
                    //使用queryWrapper形式
                    //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
                    if ("oracle".equals(dbType)){
                        //personWrapper.apply("name like {0} escape '\\'", "%" + key + "%");
                        condition.or().apply("phone like {0} escape '\\'", "%" + key + "%");
                    }else{
                        condition.or().like("phone", key);
                    }
                }
            });
        }

        if (StringUtils.isNotBlank(conditionQuery.getGender())) {
            String[] genders = conditionQuery.getGender().split(",");
            List<String> genderList = new ArrayList<>(genders.length);
            for (String genderCode : genders) {
                genderList.add(Constants.GENDER_ENTITYCODE + "/" + genderCode);
            }
            queryWrapper.in("gender", genderList);
        }
        if (StringUtils.isNotBlank(conditionQuery.getStatus())) {
            String[] statuses = conditionQuery.getStatus().split(",");
            List<String> statusList = new ArrayList<>(statuses.length);
            for (String statusCode : statuses) {
                statusList.add(Constants.PERSON_STATUS_ENTITYCODE + "/" + statusCode);
            }
            queryWrapper.in("status", statusList);
        } else {
            queryWrapper.eq("status", Constants.ON_WORK_CODE);
        }
    }
    /**
     * 根据组id查询人员
     * @param groudId
     * @param keyword
     * @param current
     * @param pageSize
     * @return
     */
    @Override
    public PageResult<PersonDetailBO> queryPersonsByGroupId(Long groudId, String keyword, Integer current, Integer pageSize) {
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<PersonAddPO>();

        List<PersonDetailBO> personList = new ArrayList<PersonDetailBO>();

        List<Long> personIds = groupPersonService.queryPersonIdByGroupId(groudId);

        personWrapper.in("id", personIds);

        if (keyword != null && !"".equals(keyword.trim())) {
            //personWrapper.and(item -> item.like("name", keyword).or().like("code", keyword).or().like("phone", keyword));
            String key = dbStringUtil.getString(keyword);
            //获取数据库类型
            String dbType = dataId.getDataId();
            //使用queryWrapper形式
            //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
            if ("oracle".equals(dbType)){
                //personWrapper.apply("name like {0} escape '\\'", "%" + key + "%");

                personWrapper.and(item -> item.apply("name like {0} escape '\\'", "%" + key + "%")
                        .or().apply("code like {0} escape '\\'", "%" + key + "%")
                        .or().apply("phone like {0} escape '\\'", "%" + key + "%"));
            }else{
                personWrapper.and(item -> item.like("name", key).or().like("code", key).or().like("phone", key));
            }

        }
        Integer count = count(personWrapper);
        Page<PersonAddPO> pageInfo = new Page<PersonAddPO>(current, pageSize, count);
        page(pageInfo, personWrapper);

        if (pageInfo.getRecords() == null) {
            return new PageResult<PersonDetailBO>(personList, count, pageSize, current);
        }
        pageInfo.getRecords().stream().forEach(item -> {
            PersonDetailBO personDetailBO = new PersonDetailBO();
            BeanUtils.copyProperties(item, personDetailBO);
            personList.add(personDetailBO);
        });
        return new PageResult<PersonDetailBO>(personList, count, pageSize, current);
    }
    /**
     * 根据人员id查询当前的详情,来加载修改页面
     * @param personId
     * @return
     */
    @Override
    public PersonUpdatePageBO queryDetailByPersonId(Long personId) {
        PersonAddPO personAddPO = getById(personId);
        PersonUpdatePageBO personUpdatePageBO = new PersonUpdatePageBO();
        if (personAddPO == null) {
            return personUpdatePageBO;
        }
        BeanUtils.copyProperties(personAddPO, personUpdatePageBO);

        BaseInfoBO directLeader = new BaseInfoBO();
        BaseInfoBO grandLeader = new BaseInfoBO();
        personUpdatePageBO.setDirectLeader(directLeader);
        personUpdatePageBO.setGrandLeader(grandLeader);
        if (personAddPO.getDirectLeaderId() != null) {
            PersonAddPO directP = getById(personAddPO.getDirectLeaderId());
            if (directP != null) {
                directLeader.setId(directP.getId());
                directLeader.setName(directP.getName());
                directLeader.setCode(directP.getCode());
            }
        }
        if (personAddPO.getGrandLeaderId() != null) {
            PersonAddPO grandP = getById(personAddPO.getGrandLeaderId());
            if (grandP != null) {
                grandLeader.setId(grandP.getId());
                grandLeader.setName(grandP.getName());
                grandLeader.setCode(grandP.getCode());
            }
        }

        //查询人员关联的所有岗位
        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
        relationWrapper.eq("person_id", personId);
        relationWrapper.eq("valid", 1);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(relationWrapper);
        if (relations != null) {
            List<PersonPositionBO> personPositionBOS = new ArrayList<PersonPositionBO>();
            relations.stream().forEach(relation -> {
                PersonPositionBO personPositionBO = new PersonPositionBO();
                personPositionBO.setId(relation.getPositionId());
                PositionAddPO positionAddPO = positionMapper.selectById(relation.getPositionId());
                personPositionBO.setName(positionAddPO.getName());
                personUpdatePageBO.setMainPosition(personAddPO.getMainPosition());
                personPositionBOS.add(personPositionBO);
            });
            personUpdatePageBO.setPositions(personPositionBOS);
        }

        personUpdatePageBO.setGender(personAddPO.getGender());
        personUpdatePageBO.setStatus(personAddPO.getStatus());
        personUpdatePageBO.setClassifedLevel(personAddPO.getClassifiedLevel());

        return personUpdatePageBO;
    }

    /**
     * 岗位调入
     * @param personPositionTransferBO
     * @param tenantId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferPosition(PersonPositionTransferBO personPositionTransferBO, String tenantId) {
        List<PersonTransferBO> persons = personPositionTransferBO.getPersons();
        if (persons == null || persons.size() == 0) {
            return;
        }

        QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<PositionAddPO>();
        PositionAddPO position = positionMapper.selectById(personPositionTransferBO.getPositionId());
        if (position == null) {
            throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
        }
        List<PersonAddPO> updatePersonList = new ArrayList<>();
        List<PositionPersonPO> relations = new ArrayList<PositionPersonPO>();
        List<Long> personIds = new ArrayList<>();
        persons.stream().forEach(person -> {
            if (person.getId() != null) {
                personIds.add(person.getId());
                PersonAddPO personAddPO = getById(person.getId());
                updatePersonList.add(personAddPO);
                if (personAddPO != null) {
                    PositionPersonPO positionPersonPO = new PositionPersonPO();
                    positionPersonPO.setPersonId(personAddPO.getId());
                    positionPersonPO.setPositionId(personPositionTransferBO.getPositionId());
                    positionPersonPO.setWorkTime(person.getWorkTime());
                    relations.add(positionPersonPO);
                }
                if (person.getMainPosition() && !personPositionTransferBO.getPositionId().equals(personAddPO.getMainPosition())) {
                    personAddPO.setMainPosition(personPositionTransferBO.getPositionId());
                    updateById(personAddPO);
                }
            }
        });
        positionPersonService.batchSaveOrUpdate(relations);

        List<PersonRoleDTO> roles = generatePersonRoleByPositionId(personIds);
        if (roles != null && roles.size() > 0) {
            Boolean flag = organizationAdapter.informRoleChange(roles);
            if (!flag) {
                throw new OrganizationException(OrganizationErrorEnum.ROLE_CHANGE_ERROR);
            }
        }

        //kafka
        publishCreateOrUpdateMessage(updatePersonList, tenantId, "UPDATE");
    }

    /**
     * 根据id查询人员详情
     * @param personIds
     * @return
     */
    @Override
    public List<PersonDetailBO> queryPersonsById(Long[] personIds) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
        queryWrapper.in("id", personIds);
        List<PersonAddPO> list = list(queryWrapper);
        List<PersonDetailBO> results = new ArrayList<PersonDetailBO>();
        if (list == null) {
            return results;
        }
        list.stream().forEach(person -> {
            PersonDetailBO personDetailBO = new PersonDetailBO();
            BeanUtils.copyProperties(person, personDetailBO);
            results.add(personDetailBO);
        });
        return results;
    }

    @Override
    public List<PersonDetailBO> queryPersonsByCodes(List<String> codes) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
        queryWrapper.in("code", codes);
        queryWrapper.eq("valid", true);
        List<PersonAddPO> list = list(queryWrapper);
        List<PersonDetailBO> results = new ArrayList<PersonDetailBO>();
        if (list == null) {
            return results;
        }
        list.stream().forEach(person -> {
            PersonDetailBO personDetailBO = new PersonDetailBO();
            BeanUtils.copyProperties(person, personDetailBO);
            results.add(personDetailBO);
        });
        return results;
    }

    /**
     * 下载人员模板
     * @param file
     */
    @Override
    public void downlowdExcelTemplate(File file) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        ExcelUtils.createExplainSheet(ExcelUtils.getPersonTemplateExplainComments(), workbook);
        Sheet personSheet = workbook.createSheet(Constants.PERSON_DATA_SHEETNAME);

        String[] genders = {""};
        List<SystemCodeResultDTO> genderList = organizationAdapter.querySystemCodesByEntityCode(Constants.GENDER_ENTITYCODE);
        if (genderList != null && genderList.size() > 0) {
            genders = new String[genderList.size()];
            for (int i = 0; i < genderList.size(); i++) {
                genders[i] = genderList.get(i).getDisplayName();
            }
        }
        CellRangeAddressList regions = new CellRangeAddressList(1, 1000, 2, 2);
        DataValidationHelper helper = personSheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(genders);
        DataValidation dataValidation = helper.createValidation(constraint, regions);

        String[] statuses = {""};
        List<SystemCodeResultDTO> statusList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_STATUS_ENTITYCODE);
        if (statusList != null && statusList.size() > 0) {
            statuses = new String[statusList.size()];
            for (int i = 0; i < statusList.size(); i++) {
                statuses[i] = statusList.get(i).getDisplayName();
            }
        }
        CellRangeAddressList statusRegions = new CellRangeAddressList(1, 1000, 3, 3);
        DataValidationHelper statusHelper = personSheet.getDataValidationHelper();
        DataValidationConstraint statusConstraint = statusHelper.createExplicitListConstraint(statuses);
        DataValidation statusDataValidation = statusHelper.createValidation(statusConstraint, statusRegions);

        //职称
        String[] titles = {""};
        List<SystemCodeResultDTO> titleList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_TITLE_ENTITYCODE);
        if (titleList != null && titleList.size() > 0) {
            titles = new String[titleList.size()];
            for (int i = 0; i < titleList.size(); i++) {
                titles[i] = titleList.get(i).getDisplayName();
            }
        }
        CellRangeAddressList titleRegions = new CellRangeAddressList(1, 1000, 12, 12);
        DataValidationHelper titleHelper = personSheet.getDataValidationHelper();
        DataValidationConstraint titleConstraint = titleHelper.createExplicitListConstraint(titles);
        DataValidation titleDataValidation = titleHelper.createValidation(titleConstraint, titleRegions);

        //学历
        String[] educations = {""};
        List<SystemCodeResultDTO> educationList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_EDUCATION_ENTITYCODE);
        if (educationList != null && educationList.size() > 0) {
            educations = new String[educationList.size()];
            for (int i = 0; i < educationList.size(); i++) {
                educations[i] = educationList.get(i).getDisplayName();
            }
        }
        CellRangeAddressList educationRegions = new CellRangeAddressList(1, 1000, 14, 14);
        DataValidationHelper educationHelper = personSheet.getDataValidationHelper();
        DataValidationConstraint educationConstraint = educationHelper.createExplicitListConstraint(educations);
        DataValidation educationDataValidation = educationHelper.createValidation(educationConstraint, educationRegions);

        //涉密等级
/*        String[] classifiedLevels = {};
        List<SystemCodeResultDTO> classifiedLevelList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_CLASSIFIED_LEVEL_ENTITYCODE);
        if (classifiedLevelList != null && classifiedLevelList.size() > 0) {
            classifiedLevels = new String[classifiedLevelList.size()];
            for (int i = 0; i < classifiedLevelList.size(); i++) {
                classifiedLevels[i] = classifiedLevelList.get(i).getDisplayName();
            }
        }
        CellRangeAddressList classifiedLevelsRegions = new CellRangeAddressList(1, 1000, 17, 17);
        DataValidationHelper classifiedLevelHelper = personSheet.getDataValidationHelper();
        DataValidationConstraint classifiedLevelConstraint = classifiedLevelHelper.createExplicitListConstraint(classifiedLevels);
        DataValidation classifiedLevelDataValidation = classifiedLevelHelper.createValidation(classifiedLevelConstraint, classifiedLevelsRegions);*/

        //处理Excel兼容性问题
        if (dataValidation instanceof XSSFDataValidation) {
            //数据校验
            dataValidation.setSuppressDropDownArrow(true);
            dataValidation.setShowErrorBox(true);
        } else {
            dataValidation.setSuppressDropDownArrow(false);
        }
        //处理Excel兼容性问题
        if (statusDataValidation instanceof XSSFDataValidation) {
            //数据校验
            statusDataValidation.setSuppressDropDownArrow(true);
            statusDataValidation.setShowErrorBox(true);
        } else {
            statusDataValidation.setSuppressDropDownArrow(false);
        }
        if (titleDataValidation instanceof XSSFDataValidation) {
            //数据校验
            titleDataValidation.setSuppressDropDownArrow(true);
            titleDataValidation.setShowErrorBox(true);
        } else {
            titleDataValidation.setSuppressDropDownArrow(false);
        }
        if (educationDataValidation instanceof XSSFDataValidation) {
            //数据校验
            educationDataValidation.setSuppressDropDownArrow(true);
            educationDataValidation.setShowErrorBox(true);
        } else {
            educationDataValidation.setSuppressDropDownArrow(false);
        }
/*        if (classifiedLevelDataValidation instanceof XSSFDataValidation) {
            //数据校验
            classifiedLevelDataValidation.setSuppressDropDownArrow(true);
            classifiedLevelDataValidation.setShowErrorBox(true);
        } else {
            classifiedLevelDataValidation.setSuppressDropDownArrow(false);
        }*/
        personSheet.addValidationData(dataValidation);
        personSheet.addValidationData(statusDataValidation);
        personSheet.addValidationData(titleDataValidation);
        personSheet.addValidationData(educationDataValidation);
        //personSheet.addValidationData(classifiedLevelDataValidation);

        for (int i = 0; i < ExcelUtils.PERSON_IMPORT_TEMPLATE.size(); i++) {
            personSheet.setColumnWidth(i, ExcelUtils.COLUMN_LENGTH);
        }
        ExcelUtils.createHeadComments(personSheet, ExcelUtils.PERSON_IMPORT_TEMPLATE);
        //setComment(personSheet, PERSON_COMMENT, language);
        OutputStream outputStream = new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.flush();
    }

    /**
     * 人员导入Excel
     * @param workbook
     * @param fileName
     * @param tenantId
     * @param timeZone
     * @Param taskId 导入任务id
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importExcel(XSSFWorkbook workbook, Long taskId, Long companyId, String fileName, String tenantId, String timeZone) {

        ExcelPO excelPO = excelMapper.selectById(taskId);
        if (excelPO == null) {
            return;
        }
        excelPO.setStatus(1);
        //excelMapper.updateById(excelPO);
        excelManageService.excuteExcelState(excelPO);


        //人员操作
        List<PersonAddPO> persons = new ArrayList<PersonAddPO>();
        //人员关联岗位
        List<PositionPersonExcelBO> relations = new ArrayList<PositionPersonExcelBO>();
        try {
            Sheet sheet = workbook.getSheetAt(1);
            Row titleRow = sheet.getRow(0);

            HashMap<String, Integer> titleMap = new HashMap<String, Integer>();
            //校验标题头
            Boolean titleFlag = ExcelUtils.checkImportExcelTitle(ExcelUtils.PERSON_IMPORT_TEMPLATE, titleRow, titleMap);

            if (!titleFlag) {
                excelPO.setStatus(3);
                excelPO.setErrorMessage(OrganizationErrorEnum.EXCEL_IMPORT_TITLE_ERROR.getMessage());
                excelManageService.excuteExcelState(excelPO);
                return;
            }

            //错误
            CellStyle errorCellStyle = ExcelUtils.createImportErrorCellStyle(workbook);

            Drawing drawing = sheet.createDrawingPatriarch();

            String originFileName = fileName.substring(fileName.indexOf("-") + 1);
            if (!preCheckPersonPosition(sheet, titleMap, excelPO, drawing, errorCellStyle)) {

                String errorFileName = ExcelUtils.createErrorExcelFile(workbook, originFileName, taskId);

                excelPO.setErrorFile(errorFileName);
                excelPO.setStatus(3);
                excelManageService.excuteExcelState(excelPO);
                return;
            }


            Iterator<Row> rowIt = sheet.rowIterator();
            rowIt.next();
            Boolean importFlag = true;

            List<SystemCodeResultDTO> genderList = organizationAdapter.querySystemCodesByEntityCode(Constants.GENDER_ENTITYCODE);
            List<String> genderNames = new ArrayList<>();
            Map<String, String> genderMap = new HashMap<>();
            genderList.stream().forEach(code -> {
                genderNames.add(code.getDisplayName());
                genderMap.put(code.getDisplayName(), code.getCode());
            });

            List<SystemCodeResultDTO> statusList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_STATUS_ENTITYCODE);
            List<String> statusNames = new ArrayList<>();
            Map<String, String> statusMap = new HashMap<>();
            statusList.stream().forEach(code -> {
                statusNames.add(code.getDisplayName());
                statusMap.put(code.getDisplayName(), code.getCode());
            });

            List<SystemCodeResultDTO> titleList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_TITLE_ENTITYCODE);
            List<String> titleNames = new ArrayList<>();
            Map<String, String> personTitleMap = new HashMap<>();
            titleList.stream().forEach(code -> {
                titleNames.add(code.getDisplayName());
                personTitleMap.put(code.getDisplayName(), code.getCode());
            });

            List<SystemCodeResultDTO> educationList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_EDUCATION_ENTITYCODE);
            List<String> educationNames = new ArrayList<>();
            Map<String, String> eduMap = new HashMap<>();
            educationList.stream().forEach(code -> {
                educationNames.add(code.getDisplayName());
                eduMap.put(code.getDisplayName(), code.getCode());
            });

            while (rowIt.hasNext()) {
                PersonAddPO personAddPO = new PersonAddPO();
                PositionPersonExcelBO positionPersonExcelBO = new PositionPersonExcelBO();
                Boolean checkFlag = checkPersonData(rowIt.next(), titleMap, drawing, errorCellStyle, personAddPO, positionPersonExcelBO, companyId, genderNames, statusNames, titleNames, educationNames, genderMap, statusMap, personTitleMap, eduMap, timeZone);
                if (!checkFlag) {
                    importFlag = checkFlag;
                }
                if (importFlag) {
                    //personAddPO.setCompanyId(companyId);
                    persons.add(personAddPO);
                    relations.add(positionPersonExcelBO);
                }
            }
            if (!importFlag) {
                persons.clear();
                String errorFileName = ExcelUtils.createErrorExcelFile(workbook, originFileName, taskId);

                excelPO.setErrorFile(errorFileName);
                excelPO.setStatus(3);
                excelManageService.excuteExcelState(excelPO);
                return;
            }
            //正式导入数据
            excelBatchAddPerson(persons, relations, excelPO, tenantId);
        } catch (Exception e) {
            log.error("人员导入异常:" + e);
            excelPO.setStatus(3);
            excelPO.setErrorMessage(OrganizationErrorEnum.EXCEL_IMPORT_PROCESS_ERROR.getMessage());
            excelManageService.excuteExcelState(excelPO);
        }

    }

    /**
     * 批量入库Excel中的人员和人员岗位关系
     * @param persons
     * @param relations
     * @param excelPO
     * @param tenantId
     */
    @Transactional(rollbackFor = Exception.class)
    public void excelBatchAddPerson(List<PersonAddPO> persons, List<PositionPersonExcelBO> relations, ExcelPO excelPO, String tenantId) {

        List<PersonAddPO> createPersonList = new ArrayList<>();
        List<PersonAddPO> updatePersontList = new ArrayList<>();

        //批量新增或修改人员
        persons.stream().forEach(person -> {
            QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
            queryWrapper.eq("code", person.getCode());
            queryWrapper.eq("valid", 1);
            int count = count(queryWrapper);
            if (count == 0) {
                addPersonWithoutKafka(person, tenantId);
                createPersonList.add(person);
            } else {
                PersonAddPO curPerson = getOne(queryWrapper);
                person.setId(curPerson.getId());
                updatePersonWithoutKafka(person, tenantId);
                updatePersontList.add(person);
                QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
                relationWrapper.eq("position_id", person.getMainPosition());
                relationWrapper.eq("person_id", person.getId());
                relationWrapper.eq("valid", 1);
                int relationCount = positionPersonMapper.selectCount(relationWrapper);
                if (relationCount == 0) {
                    PositionPersonPO positionPersonPO = new PositionPersonPO();
                    positionPersonPO.setPositionId(person.getMainPosition());
                    positionPersonPO.setPersonId(person.getId());
                    positionPersonMapper.insert(positionPersonPO);
                }
            }
        });
        relations.stream().forEach(relation -> {
            QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
            queryWrapper.eq("code", relation.getPersonCode());
            queryWrapper.eq("valid", 1);
            PersonAddPO curPerson = getOne(queryWrapper);

            QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
            relationWrapper.eq("position_id", relation.getPositionId());
            relationWrapper.eq("person_id", curPerson.getId());
            relationWrapper.eq("valid", 1);
            int relationCount = positionPersonMapper.selectCount(relationWrapper);
            if (relationCount == 0) {
                PositionPersonPO positionPersonPO = new PositionPersonPO();
                positionPersonPO.setPositionId(relation.getPositionId());
                positionPersonPO.setPersonId(curPerson.getId());
                positionPersonMapper.insert(positionPersonPO);
            }
        });


        publishCreateOrUpdateMessage(createPersonList, tenantId, "CREATE");
        publishCreateOrUpdateMessage(updatePersontList, tenantId, "UPDATE");
        excelPO.setStatus(2);
        excelPO.setErrorMessage(Constants.EXCEL_IMPORT_SUCESS);
        excelManageService.excuteExcelState(excelPO);
    }

    private Boolean preCheckPersonPosition(Sheet sheet, Map<String, Integer> titleMap, ExcelPO excelPO, Drawing drawing, CellStyle cellStyle) {
        Boolean flag = true;
        //编码对应的主岗位,是否有不同的?
        Map<String, String> personMain = new HashMap<String, String>();
        //如果主岗没问题,看关联的岗位有没有重复
        //Map<String, String> personPos = new HashMap<String, String>();
        List<String> personRelPos = new ArrayList<>();

        //身份证号唯一性校验
        Set<String> idNumberList = new HashSet<>();

        //每个人必须有一条是主岗名称和所属岗位名称相同
        List<String> checkMainAndRelation = new ArrayList<>();
        Iterator<Row> rowIt = sheet.rowIterator();
        rowIt.next();
        //处理人员编码对应的主岗位personCode_mainPositionCode_mainPositionName不能有所不同
        while (rowIt.hasNext()) {
            Row row = rowIt.next();
            Cell idNumberCell = row.getCell(titleMap.get("idNumber"));
            if (idNumberCell != null && StringUtils.isNotBlank(getRightTypeCell(idNumberCell).toString())) {
                if (!Pattern.matches(ExcelUtils.ID_NUMBER_PATTERN, getRightTypeCell(idNumberCell).toString()) || getRightTypeCell(idNumberCell).toString().length() > 200) {
                    ExcelUtils.removeComment(idNumberCell);
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                    comment.setString(new XSSFRichTextString(Constants.PERSON_ID_NUMBER_ERROR));
                    idNumberCell.setCellComment(comment);
                    idNumberCell.setCellStyle(cellStyle);
                    flag = false;
                } else if (idNumberList.contains(getRightTypeCell(idNumberCell).toString())) {
                    ExcelUtils.removeComment(idNumberCell);
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                    comment.setString(new XSSFRichTextString(OrganizationErrorEnum.ID_NUMBER_EXISTS.getMessage()));
                    idNumberCell.setCellComment(comment);
                    idNumberCell.setCellStyle(cellStyle);
                    flag = false;
                } else {
                    QueryWrapper<PersonAddPO> idNumberWrapper = new QueryWrapper<>();
                    idNumberWrapper.lambda().eq(PersonAddPO::getIdNumber, getRightTypeCell(idNumberCell).toString())
                            .eq(PersonAddPO::getValid, true);
                    int idNumberCount = count(idNumberWrapper);
                    if (idNumberCount > 0) {
                        ExcelUtils.removeComment(idNumberCell);
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                        comment.setString(new XSSFRichTextString(OrganizationErrorEnum.ID_NUMBER_EXISTS.getMessage()));
                        idNumberCell.setCellComment(comment);
                        idNumberCell.setCellStyle(cellStyle);
                        flag = false;
                    }
                }
                idNumberList.add(getRightTypeCell(idNumberCell).toString());
            }

            Cell personCodeCell = row.getCell(titleMap.get("code"));
            Cell mainPositionCodeCell = row.getCell(titleMap.get("mainPositionCode"));
            Cell mainPositionNameCell = row.getCell(titleMap.get("mainPositionName"));
            StringBuilder sb = new StringBuilder();
            if (personCodeCell == null) {
                personCodeCell = row.createCell(titleMap.get("code"), CellType.STRING);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                comment.setString(new XSSFRichTextString(Constants.PERSON_CODE_LENGTH_ERROR));
                personCodeCell.setCellComment(comment);
                personCodeCell.setCellStyle(cellStyle);
                flag = false;
            } else if (StringUtils.isBlank(getRightTypeCell(personCodeCell).toString())) {
                ExcelUtils.removeComment(personCodeCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                comment.setString(new XSSFRichTextString(Constants.PERSON_CODE_LENGTH_ERROR));

                personCodeCell.setCellComment(comment);
                personCodeCell.setCellStyle(cellStyle);

                flag = false;
            } else {
                sb.append(getRightTypeCell(personCodeCell).toString()).append("-");
                if (mainPositionCodeCell == null || StringUtils.isBlank(getRightTypeCell(mainPositionCodeCell).toString())) {
                    sb.append("orgMainCode").append("-");
                } else {
                    sb.append(getRightTypeCell(mainPositionCodeCell).toString()).append("-");
                }
                if (mainPositionNameCell == null) {
                    mainPositionNameCell = row.createCell(titleMap.get("mainPositionName"), CellType.STRING);
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                    comment.setString(new XSSFRichTextString(Constants.PERSON_MAIN_POSITION_NAME_NECESSARY_ERROR));
                    mainPositionNameCell.setCellComment(comment);
                    mainPositionNameCell.setCellStyle(cellStyle);
                    flag = false;
                } else if (StringUtils.isBlank(getRightTypeCell(mainPositionNameCell).toString())) {
                    ExcelUtils.removeComment(mainPositionNameCell);
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                    comment.setString(new XSSFRichTextString(Constants.PERSON_MAIN_POSITION_NAME_NECESSARY_ERROR));

                    mainPositionNameCell.setCellComment(comment);
                    mainPositionNameCell.setCellStyle(cellStyle);
                    flag = false;
                } else {
                    String personCode = getRightTypeCell(personCodeCell).toString();
                    sb.append(getRightTypeCell(mainPositionNameCell).toString());
                    if (personMain.containsKey(personCode) && !personMain.get(personCode).equals(sb.toString())) {
                        ExcelUtils.removeComment(personCodeCell);
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                        comment.setString(new XSSFRichTextString(Constants.EXCEL_PERSON_CODE_MAIN_POSITION_DIFF));

                        personCodeCell.setCellComment(comment);
                        personCodeCell.setCellStyle(cellStyle);
                        flag = false;
                    } else {
                        personMain.put(getRightTypeCell(personCodeCell).toString(), sb.toString());
                    }
                }
            }
        }
        if (!flag) {
            return flag;
        }
        Iterator<Row> rowRelIt = sheet.rowIterator();
        rowRelIt.next();
        //处理人员编码对应的所属岗位位personCode_mainPositionCode_mainPositionName不能有所不同
        while (rowRelIt.hasNext()) {
            Row row = rowRelIt.next();
            Cell personCodeCell = row.getCell(titleMap.get("code"));
            Cell positionCodeCell = row.getCell(titleMap.get("positionCode"));
            Cell positionNameCell = row.getCell(titleMap.get("positionName"));
            Cell mainPositionNameCell = row.getCell(titleMap.get("mainPositionName"));
            StringBuilder sb = new StringBuilder();
            if (personCodeCell == null) {
                personCodeCell = row.createCell(titleMap.get("code"), CellType.STRING);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                comment.setString(new XSSFRichTextString(Constants.PERSON_CODE_LENGTH_ERROR));
                personCodeCell.setCellComment(comment);
                personCodeCell.setCellStyle(cellStyle);
                flag = false;
            } else if (StringUtils.isBlank(getRightTypeCell(personCodeCell).toString())) {
                ExcelUtils.removeComment(personCodeCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                comment.setString(new XSSFRichTextString(Constants.PERSON_CODE_LENGTH_ERROR));

                personCodeCell.setCellComment(comment);
                personCodeCell.setCellStyle(cellStyle);

                flag = false;
            } else {
                sb.append(getRightTypeCell(personCodeCell).toString()).append("-");
                if (positionCodeCell == null || StringUtils.isBlank(getRightTypeCell(positionCodeCell).toString())) {
                    sb.append("orgRelCode").append("-");
                } else {
                    sb.append(getRightTypeCell(positionCodeCell).toString()).append("-");
                }
                if (positionNameCell == null) {
                    positionNameCell = row.createCell(titleMap.get("positionName"), CellType.STRING);
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                    comment.setString(new XSSFRichTextString(Constants.PERSON_REL_POSITION_NAME_NECESSARY_ERROR));
                    positionNameCell.setCellComment(comment);
                    positionNameCell.setCellStyle(cellStyle);
                    flag = false;
                } else if (StringUtils.isBlank(getRightTypeCell(positionNameCell).toString())) {
                    ExcelUtils.removeComment(positionNameCell);
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                    comment.setString(new XSSFRichTextString(Constants.PERSON_REL_POSITION_NAME_NECESSARY_ERROR));

                    positionNameCell.setCellComment(comment);
                    positionNameCell.setCellStyle(cellStyle);
                    flag = false;
                } else {
                    String personCode = getRightTypeCell(personCodeCell).toString();
                    sb.append(getRightTypeCell(positionNameCell).toString());
                    if (personRelPos.contains(sb.toString())) {
                        ExcelUtils.removeComment(personCodeCell);
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                        comment.setString(new XSSFRichTextString(Constants.EXCEL_PERSON_CODE_REL_POSITION_DUP));

                        personCodeCell.setCellComment(comment);
                        personCodeCell.setCellStyle(cellStyle);
                        flag = false;
                    } else {
                        personRelPos.add(sb.toString());
                        StringBuilder marSb = new StringBuilder();
                        marSb.append(personCode).append("-").append(getRightTypeCell(mainPositionNameCell).toString()).append("-").append(getRightTypeCell(positionNameCell).toString());
                        checkMainAndRelation.add(marSb.toString());
                    }
                }
            }
        }
        if (!flag) {
            return flag;
        }
        Iterator<Row> checkRelIt = sheet.rowIterator();
        checkRelIt.next();
        //处理人员编码对应的所属岗位位personCode_mainPositionCode_mainPositionName不能有所不同
        while (checkRelIt.hasNext()) {
            Row row = checkRelIt.next();
            Cell personCodeCell = row.getCell(titleMap.get("code"));
            //Cell positionCodeCell = row.getCell(titleMap.get("positionCode"));
            //Cell positionNameCell = row.getCell(titleMap.get("positionName"));
            Cell mainPositionNameCell = row.getCell(titleMap.get("mainPositionName"));
            StringBuilder marSb = new StringBuilder();
            marSb.append(getRightTypeCell(personCodeCell).toString()).append("-").append(getRightTypeCell(mainPositionNameCell).toString()).append("-").append(getRightTypeCell(mainPositionNameCell).toString());
            if (!checkMainAndRelation.contains(marSb.toString())) {
                ExcelUtils.removeComment(mainPositionNameCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
                comment.setString(new XSSFRichTextString(Constants.PERSON_MAIN_POSITION_NO_RELATION_POSITION));

                mainPositionNameCell.setCellComment(comment);
                mainPositionNameCell.setCellStyle(cellStyle);
                flag = false;
            }
        }
        return flag;
    }


    /**
     * 校验每行数据
     * @param row
     * @param titleMap
     * @param drawing
     * @param cellStyle
     * @param genderNames
     * @param statusNames
     * @param titleNames
     * @param educationNames
     * @param genderMap
     * @param statusMap
     * @param personTitleMap
     * @param eduMap
     * @param timeZone
     * @return
     */
    private Boolean checkPersonData(Row row, Map<String, Integer> titleMap, Drawing drawing, CellStyle cellStyle, PersonAddPO personAddPO, PositionPersonExcelBO positionPersonExcelBO, Long companyId, List<String> genderNames, List<String> statusNames, List<String> titleNames, List<String> educationNames, Map<String, String> genderMap, Map<String, String> statusMap, Map<String, String> personTitleMap, Map<String, String> eduMap, String timeZone) throws ParseException {

        Boolean flag = true;
        int rowY = row.getRowNum();

        //校验人员编号
        Cell codeCell = row.getCell(titleMap.get("code"));
        if (codeCell != null && (StringUtils.isBlank(getRightTypeCell(codeCell).toString()) || getRightTypeCell(codeCell).toString().length() > 50)) {
            int codeX = codeCell.getColumnIndex();
            ExcelUtils.removeComment(codeCell);
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(codeX, rowY, codeX, rowY, codeX, rowY, codeX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_CODE_LENGTH_ERROR));

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
        }  else if (codeCell == null) {
            codeCell = row.createCell(titleMap.get("code"), CellType.STRING);
            int codeX = codeCell.getColumnIndex();
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(codeX, rowY, codeX, rowY, codeX, rowY, codeX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_CODE_LENGTH_ERROR));
            codeCell.setCellComment(comment);
            codeCell.setCellStyle(cellStyle);
            flag = false;
        }

        //人员姓名校验
        Cell nameCell = row.getCell(titleMap.get("name"));
        if (nameCell != null && (StringUtils.isBlank(getRightTypeCell(nameCell).toString()) || getRightTypeCell(nameCell).toString().length() > 200)) {
            int nameX = nameCell.getColumnIndex();
            ExcelUtils.removeComment(nameCell);
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(nameX, rowY, nameX, rowY, nameX, rowY, nameX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_NAME_LENGTH_ERROR));

            nameCell.setCellComment(comment);
            nameCell.setCellStyle(cellStyle);

            flag = false;
        } else if (nameCell == null) {
            nameCell = row.createCell(titleMap.get("name"), CellType.STRING);
            int nameX = nameCell.getColumnIndex();
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(nameX, rowY, nameX, rowY, nameX, rowY, nameX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_NAME_LENGTH_ERROR));
            nameCell.setCellComment(comment);
            nameCell.setCellStyle(cellStyle);
            flag = false;
        }

        //人员性别校验
        Cell genderCell = row.getCell(titleMap.get("gender"));
        if (genderCell != null && (StringUtils.isBlank(getRightTypeCell(genderCell).toString()) || !genderNames.contains(getRightTypeCell(genderCell).toString()))) {
            int genderX = genderCell.getColumnIndex();
            ExcelUtils.removeComment(genderCell);
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(genderX, rowY, genderX, rowY, genderX, rowY, genderX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_GENDER_ERROR));

            genderCell.setCellComment(comment);
            genderCell.setCellStyle(cellStyle);

            flag = false;
        } else if (genderCell == null) {
            genderCell = row.createCell(titleMap.get("gender"), CellType.STRING);
            int genderX = genderCell.getColumnIndex();
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(genderX, rowY, genderX, rowY, genderX, rowY, genderX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_GENDER_ERROR));
            genderCell.setCellComment(comment);
            genderCell.setCellStyle(cellStyle);
            flag = false;
        }

        //职称校验
        Cell titleCell = row.getCell(titleMap.get("title"));
        if (titleCell != null && (StringUtils.isNotBlank(getRightTypeCell(titleCell).toString())
                && !titleNames.contains(getRightTypeCell(titleCell).toString()))) {
            int titleX = titleCell.getColumnIndex();
            ExcelUtils.removeComment(titleCell);
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(titleX, rowY, titleX, rowY, titleX, rowY, titleX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_TITLE_ERROR));

            titleCell.setCellComment(comment);
            titleCell.setCellStyle(cellStyle);
            flag = false;
        }
        //学历校验
        Cell educationCell = row.getCell(titleMap.get("education"));
        if (educationCell != null && (StringUtils.isNotBlank(getRightTypeCell(educationCell).toString())
                && !educationNames.contains(getRightTypeCell(educationCell).toString()))) {
            int educationX = educationCell.getColumnIndex();
            ExcelUtils.removeComment(educationCell);
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(educationX, rowY, educationX, rowY, educationX, rowY, educationX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_EDUCATION_ERROR));

            educationCell.setCellComment(comment);
            educationCell.setCellStyle(cellStyle);
            flag = false;
        }
        //学历校验
/*        Cell classifiedLevelCell = row.getCell(titleMap.get("classifiedLevel"));
        if (classifiedLevelCell != null && (StringUtils.isBlank(getRightTypeCell(classifiedLevelCell).toString())
                || (!"一般涉密".equals(getRightTypeCell(classifiedLevelCell).toString()) && !"重要涉密".equals(getRightTypeCell(classifiedLevelCell).toString()) && !"核心涉密".equals(getRightTypeCell(classifiedLevelCell).toString())))) {
            int classifiedLevelX = classifiedLevelCell.getColumnIndex();
            ExcelUtils.removeComment(classifiedLevelCell);
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(classifiedLevelX, rowY, classifiedLevelX, rowY, classifiedLevelX, rowY, classifiedLevelX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_CLASSIFIEDLEVEL_ERROR));

            classifiedLevelCell.setCellComment(comment);
            classifiedLevelCell.setCellStyle(cellStyle);
        }*/
        //校验主岗编码
        Cell mainPositionCodeCell = row.getCell(titleMap.get("mainPositionCode"));


        //人员状态校验
        Cell statusCell = row.getCell(titleMap.get("status"));
        if (statusCell != null && (StringUtils.isBlank(getRightTypeCell(statusCell).toString()) || !statusNames.contains(getRightTypeCell(statusCell).toString()))) {
            int statusX = statusCell.getColumnIndex();
            ExcelUtils.removeComment(statusCell);
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(statusX, rowY, statusX, rowY, statusX, rowY, statusX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_STATUS_ERROR));

            statusCell.setCellComment(comment);
            statusCell.setCellStyle(cellStyle);

            flag = false;
        } else if (statusCell == null) {
            statusCell = row.createCell(titleMap.get("status"), CellType.STRING);
            int statusX = statusCell.getColumnIndex();
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(statusX, rowY, statusX, rowY, statusX, rowY, statusX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_STATUS_ERROR));
            statusCell.setCellComment(comment);
            statusCell.setCellStyle(cellStyle);
            flag = false;
        }

        //手机号校验
        Cell phoneCell = row.getCell(titleMap.get("phone"));
        if (phoneCell != null && StringUtils.isNotBlank(getRightTypeCell(phoneCell).toString()) && !Pattern.matches(ExcelUtils.PHONE_PATTERN, getRightTypeCell(phoneCell).toString())) {
            int phoneX = phoneCell.getColumnIndex();
            ExcelUtils.removeComment(phoneCell);
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(phoneX, rowY, phoneX, rowY, phoneX, rowY, phoneX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_PHONE_ERROR));

            phoneCell.setCellComment(comment);
            phoneCell.setCellStyle(cellStyle);

            flag = false;
        }

        //邮箱地址校验
        Cell emailCell = row.getCell(titleMap.get("email"));
        if (emailCell != null && StringUtils.isNotBlank(getRightTypeCell(emailCell).toString()) && !Pattern.matches(ExcelUtils.EMAIL_PATTERN, getRightTypeCell(emailCell).toString())) {
            int emailX = emailCell.getColumnIndex();
            ExcelUtils.removeComment(emailCell);
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(emailX, rowY, emailX, rowY, emailX, rowY, emailX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_EMAIL_ERROR));

            emailCell.setCellComment(comment);
            emailCell.setCellStyle(cellStyle);

            flag = false;
        }

        //描述校验
        Cell descCell = row.getCell(titleMap.get("description"));
        if (descCell != null && StringUtils.isNotBlank(getRightTypeCell(descCell).toString()) && getRightTypeCell(descCell).toString().length() > 500) {
            int descX = descCell.getColumnIndex();
            ExcelUtils.removeComment(descCell);
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(descX, rowY, descX, rowY, descX, rowY, descX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_DESCRIPTION_LENGTH_ERROR));

            descCell.setCellComment(comment);
            descCell.setCellStyle(cellStyle);

            flag = false;
        }

        //主岗位id
        Long mainPositionId = null;

        //主岗名称校验
        Cell mainPositionNameCell = row.getCell(titleMap.get("mainPositionName"));

        if ((mainPositionCodeCell != null && StringUtils.isBlank(getRightTypeCell(mainPositionCodeCell).toString())) || mainPositionCodeCell == null) {

            if (mainPositionNameCell != null && StringUtils.isNotBlank(getRightTypeCell(mainPositionNameCell).toString())) {
                int mainNameX = mainPositionNameCell.getColumnIndex();
                QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
                queryWrapper.eq("company_id", companyId);
                queryWrapper.eq("name", getRightTypeCell(mainPositionNameCell).toString());
                queryWrapper.eq("valid", 1);
                int count = positionMapper.selectCount(queryWrapper);
                if (count == 0) {
                    ExcelUtils.removeComment(mainPositionNameCell);
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(mainNameX, rowY, mainNameX, rowY, mainNameX, rowY, mainNameX, rowY));
                    comment.setString(new XSSFRichTextString(Constants.POSITION_THIS_NAME_NOT_EXISTS));

                    mainPositionNameCell.setCellComment(comment);
                    mainPositionNameCell.setCellStyle(cellStyle);

                    flag = false;
                } else if (count > 1) {
                    if (mainPositionCodeCell == null) {
                        mainPositionCodeCell = row.createCell(titleMap.get("mainPositionCode"), CellType.STRING);
                        int mainCodeX = mainPositionCodeCell.getColumnIndex();
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(mainCodeX, rowY, mainCodeX, rowY, mainCodeX, rowY, mainCodeX, rowY));
                        comment.setString(new XSSFRichTextString(Constants.PERSON_MAIN_POSITION_CODE_NECESSARY_ERROR));
                        mainPositionCodeCell.setCellComment(comment);
                        mainPositionCodeCell.setCellStyle(cellStyle);
                        flag = false;
                    } else {
                        int mainCodeX = mainPositionCodeCell.getColumnIndex();
                        ExcelUtils.removeComment(mainPositionCodeCell);
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(mainCodeX, rowY, mainCodeX, rowY, mainCodeX, rowY, mainCodeX, rowY));
                        comment.setString(new XSSFRichTextString(Constants.PERSON_MAIN_POSITION_CODE_NECESSARY_ERROR));

                        mainPositionCodeCell.setCellComment(comment);
                        mainPositionCodeCell.setCellStyle(cellStyle);

                        flag = false;
                    }

                } else {
                    PositionAddPO positionAddPO = positionMapper.selectOne(queryWrapper);
                    mainPositionId = positionAddPO.getId();
                }
            }
        } else if (mainPositionCodeCell != null) {
            QueryWrapper<PositionAddPO> queryCodeWrapper = new QueryWrapper<PositionAddPO>();
            queryCodeWrapper.eq("company_id", companyId);
            queryCodeWrapper.eq("code", getRightTypeCell(mainPositionCodeCell).toString());
            queryCodeWrapper.eq("valid", 1);
            int codeCount = positionMapper.selectCount(queryCodeWrapper);
            if (codeCount == 0) {
                int mainCodeX = mainPositionCodeCell.getColumnIndex();
                ExcelUtils.removeComment(mainPositionCodeCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(mainCodeX, rowY, mainCodeX, rowY, mainCodeX, rowY, mainCodeX, rowY));
                comment.setString(new XSSFRichTextString(Constants.POSITION_THIS_CODE_NOT_EXISTS));

                mainPositionCodeCell.setCellComment(comment);
                mainPositionCodeCell.setCellStyle(cellStyle);

                flag = false;
            } else {
                PositionAddPO positionAddPO = positionMapper.selectOne(queryCodeWrapper);
                mainPositionId = positionAddPO.getId();
            }
        }

        if (mainPositionNameCell != null && StringUtils.isBlank(getRightTypeCell(mainPositionNameCell).toString())) {
            int mainNameX = mainPositionNameCell.getColumnIndex();
            ExcelUtils.removeComment(mainPositionNameCell);
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(mainNameX, rowY, mainNameX, rowY, mainNameX, rowY, mainNameX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_MAIN_POSITION_NAME_NECESSARY_ERROR));

            mainPositionNameCell.setCellComment(comment);
            mainPositionNameCell.setCellStyle(cellStyle);

            flag = false;
        }  else if (mainPositionNameCell == null) {
            mainPositionNameCell = row.createCell(titleMap.get("mainPositionName"), CellType.STRING);
            int mainNameX = mainPositionNameCell.getColumnIndex();
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(mainNameX, rowY, mainNameX, rowY, mainNameX, rowY, mainNameX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_MAIN_POSITION_NAME_NECESSARY_ERROR));
            mainPositionNameCell.setCellComment(comment);
            mainPositionNameCell.setCellStyle(cellStyle);
            flag = false;
        }

        Cell relationPositionCodeCell = row.getCell(titleMap.get("positionCode"));

        //所属岗位名称校验
        Cell relationPositionNameCell = row.getCell(titleMap.get("positionName"));

        if ((relationPositionCodeCell != null && StringUtils.isBlank(getRightTypeCell(relationPositionCodeCell).toString())) || relationPositionCodeCell == null) {
            if (relationPositionNameCell != null && StringUtils.isNotBlank(getRightTypeCell(relationPositionNameCell).toString())) {
                int relNameX = relationPositionNameCell.getColumnIndex();
                QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
                queryWrapper.eq("name", getRightTypeCell(relationPositionNameCell).toString());
                queryWrapper.eq("valid", 1);
                queryWrapper.eq("company_id", companyId);
                int count = positionMapper.selectCount(queryWrapper);
                if (count == 0) {
                    ExcelUtils.removeComment(relationPositionNameCell);
                    Comment comment = drawing.createCellComment(new XSSFClientAnchor(relNameX, rowY, relNameX, rowY, relNameX, rowY, relNameX, rowY));
                    comment.setString(new XSSFRichTextString(Constants.POSITION_THIS_NAME_NOT_EXISTS));

                    relationPositionNameCell.setCellComment(comment);
                    relationPositionNameCell.setCellStyle(cellStyle);

                    flag = false;
                } else if (count > 1) {

                    if (relationPositionCodeCell == null || StringUtils.isBlank(getRightTypeCell(relationPositionCodeCell).toString())) {
                        relationPositionCodeCell = row.createCell(titleMap.get("positionCode"), CellType.STRING);
                        int relCodeX = relationPositionCodeCell.getColumnIndex();
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(relCodeX, rowY, relCodeX, rowY, relCodeX, rowY, relCodeX, rowY));
                        comment.setString(new XSSFRichTextString(Constants.PERSON_REL_POSITION_CODE_NECESSARY_ERROR));
                        relationPositionCodeCell.setCellComment(comment);
                        relationPositionCodeCell.setCellStyle(cellStyle);
                        flag = false;
                    } else {
                        int relCodeX = relationPositionCodeCell.getColumnIndex();
                        ExcelUtils.removeComment(relationPositionCodeCell);
                        Comment comment = drawing.createCellComment(new XSSFClientAnchor(relCodeX, rowY, relCodeX, rowY, relCodeX, rowY, relCodeX, rowY));
                        comment.setString(new XSSFRichTextString(Constants.POSITION_THIS_CODE_NOT_EXISTS));

                        relationPositionCodeCell.setCellComment(comment);
                        relationPositionCodeCell.setCellStyle(cellStyle);

                        flag = false;
                    }
                } else {
                    PositionAddPO positionAddPO = positionMapper.selectOne(queryWrapper);
                    positionPersonExcelBO.setPositionId(positionAddPO.getId());
                }
            }
        } else {
            QueryWrapper<PositionAddPO> queryCodeWrapper = new QueryWrapper<PositionAddPO>();
            queryCodeWrapper.eq("code", getRightTypeCell(relationPositionCodeCell).toString());
            queryCodeWrapper.eq("valid", 1);
            queryCodeWrapper.eq("company_id", companyId);
            int codeCount = positionMapper.selectCount(queryCodeWrapper);
            if (codeCount == 0) {
                int relCodeX = relationPositionCodeCell.getColumnIndex();
                ExcelUtils.removeComment(relationPositionCodeCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(relCodeX, rowY, relCodeX, rowY, relCodeX, rowY, relCodeX, rowY));
                comment.setString(new XSSFRichTextString(Constants.POSITION_THIS_CODE_NOT_EXISTS));

                relationPositionCodeCell.setCellComment(comment);
                relationPositionCodeCell.setCellStyle(cellStyle);

                flag = false;
            } else {
                PositionAddPO positionAddPO = positionMapper.selectOne(queryCodeWrapper);
                positionPersonExcelBO.setPositionId(positionAddPO.getId());
            }
        }

        if (relationPositionNameCell != null && StringUtils.isBlank(getRightTypeCell(relationPositionNameCell).toString())) {
            int relNameX = relationPositionNameCell.getColumnIndex();
            ExcelUtils.removeComment(relationPositionNameCell);
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(relNameX, rowY, relNameX, rowY, relNameX, rowY, relNameX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_MAIN_POSITION_NAME_NECESSARY_ERROR));

            relationPositionNameCell.setCellComment(comment);
            relationPositionNameCell.setCellStyle(cellStyle);

            flag = false;
        } else if (relationPositionNameCell == null) {
            relationPositionNameCell = row.createCell(titleMap.get("positionName"), CellType.STRING);
            int relNameX = relationPositionNameCell.getColumnIndex();
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(relNameX, rowY, relNameX, rowY, relNameX, rowY, relNameX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_MAIN_POSITION_NAME_NECESSARY_ERROR));
            relationPositionNameCell.setCellComment(comment);
            relationPositionNameCell.setCellStyle(cellStyle);
            flag = false;
        }

        Cell entryDateCell = row.getCell(titleMap.get("entryDate"));
        String entryTime = "";
        if (entryDateCell != null && StringUtils.isNotBlank(getRightTypeCell(entryDateCell).toString())) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            try {
                format.parse(getRightTypeCell(entryDateCell).toString());
            } catch (ParseException e) {
                log.error(e.getMessage());
                int entryX = entryDateCell.getColumnIndex();
                ExcelUtils.removeComment(entryDateCell);
                Comment comment = drawing.createCellComment(new XSSFClientAnchor(entryX, rowY, entryX, rowY, entryX, rowY, entryX, rowY));
                comment.setString(new XSSFRichTextString(OrganizationErrorEnum.DATE_FORMAT_ERROR.getMessage()));

                entryDateCell.setCellComment(comment);
                entryDateCell.setCellStyle(cellStyle);
                flag = false;
            }
        }

        Cell qualificationCell = row.getCell(titleMap.get("qualification"));
        if (qualificationCell != null && StringUtils.isNotBlank(getRightTypeCell(qualificationCell).toString()) && getRightTypeCell(qualificationCell).toString().length() > 200) {
            int qualificationX = qualificationCell.getColumnIndex();
            ExcelUtils.removeComment(qualificationCell);
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(qualificationX, rowY, qualificationX, rowY, qualificationX, rowY, qualificationX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_QUALIFICATION_LENHTH_ERROR));

            qualificationCell.setCellComment(comment);
            qualificationCell.setCellStyle(cellStyle);

            flag = false;
        }
        Cell majorCell = row.getCell(titleMap.get("major"));
        if (majorCell != null && StringUtils.isNotBlank(getRightTypeCell(majorCell).toString()) && getRightTypeCell(majorCell).toString().length() > 200) {
            int majorX = majorCell.getColumnIndex();
            ExcelUtils.removeComment(majorCell);
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(majorX, rowY, majorX, rowY, majorX, rowY, majorX, rowY));
            comment.setString(new XSSFRichTextString(Constants.PERSON_MAJOR_LENHTH_ERROR));

            majorCell.setCellComment(comment);
            majorCell.setCellStyle(cellStyle);

            flag = false;
        }
        if (!flag) {
            return flag;
        }

        positionPersonExcelBO.setPersonCode(getRightTypeCell(codeCell).toString());

        personAddPO.setCode(getRightTypeCell(codeCell).toString());
        personAddPO.setName(getRightTypeCell(nameCell).toString());

        personAddPO.setMainPosition(mainPositionId);

        personAddPO.setGender(Constants.GENDER_ENTITYCODE + "/" + genderMap.get(getRightTypeCell(genderCell).toString()));
/*        if ("男".equals(getRightTypeCell(genderCell).toString())) {
            personAddPO.setGender(Constants.GENDER_MALE);
        } else {
            personAddPO.setGender(Constants.GENDER_FEMALE);
        }*/
        personAddPO.setStatus(Constants.PERSON_STATUS_ENTITYCODE + "/" + statusMap.get(getRightTypeCell(statusCell).toString()));
/*        if ("在职".equals(getRightTypeCell(statusCell).toString())) {
            personAddPO.setStatus(Constants.ON_WORK_CODE);
        } else {
            personAddPO.setStatus(Constants.OFF_WORK_CODE);
        }*/
        if (titleCell != null && StringUtils.isNotBlank(getRightTypeCell(titleCell).toString())) {
            personAddPO.setTitle(Constants.PERSON_TITLE_ENTITYCODE + "/" + personTitleMap.get(getRightTypeCell(titleCell).toString()));
        }
        if (educationCell != null && StringUtils.isNotBlank(getRightTypeCell(educationCell).toString())) {
            personAddPO.setEducation(Constants.PERSON_EDUCATION_ENTITYCODE + "/" + eduMap.get(getRightTypeCell(educationCell).toString()));
        }

        if (phoneCell != null && StringUtils.isNotBlank(getRightTypeCell(phoneCell).toString())) {
            personAddPO.setPhone(getRightTypeCell(phoneCell).toString());
        }

        if (emailCell != null && StringUtils.isNotBlank(getRightTypeCell(emailCell).toString())) {
            personAddPO.setEmail(getRightTypeCell(emailCell).toString());
        }
        if (descCell != null && StringUtils.isNotBlank(getRightTypeCell(descCell).toString())) {
            personAddPO.setDescription(getRightTypeCell(descCell).toString());
        }

        Cell idNumberCell = row.getCell(titleMap.get("idNumber"));
        if (idNumberCell != null && StringUtils.isNotBlank(getRightTypeCell(idNumberCell).toString())) {
            personAddPO.setIdNumber(getRightTypeCell(idNumberCell).toString());
        }

        if (StringUtils.isNotBlank(entryTime)) {
            personAddPO.setEntryDate(entryTime);
        }

        if (qualificationCell != null && StringUtils.isNotBlank(getRightTypeCell(qualificationCell).toString())) {
            personAddPO.setQualification(getRightTypeCell(qualificationCell).toString());
        }

        if (majorCell != null && StringUtils.isNotBlank(getRightTypeCell(majorCell).toString())) {
            personAddPO.setMajor(getRightTypeCell(majorCell).toString());
        }
        if (entryDateCell != null && StringUtils.isNotBlank(getRightTypeCell(entryDateCell).toString())) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date entryDate = format.parse(getRightTypeCell(entryDateCell).toString());
            String dateYmd = format.format(entryDate);
            personAddPO.setEntryDate(dateYmd);
        }
        return true;
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
     * 根据人员id查询所有关联的公司
     * @param personId
     * @return
     */
    @Override
    public List<CompanyBO> queryCompanIdByPersonIds(Long personId) {
        QueryWrapper<PositionPersonPO> queryWrapper = new QueryWrapper<PositionPersonPO>();
        queryWrapper.eq("person_id", personId);
        queryWrapper.eq("valid", 1);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(queryWrapper);
        List<Long> positionIds = new ArrayList<Long>();
        List<CompanyBO> companies = new ArrayList<CompanyBO>();
        if (relations != null && relations.size() > 0) {
            relations.stream().forEach(relation -> positionIds.add(relation.getPositionId()));
        } else {
            return companies;
        }
        List<Long> companyIds = new ArrayList<Long>();

        if (positionIds.size() > 0) {
            QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<PositionAddPO>();
            positionWrapper.in("id", positionIds);
            List<PositionAddPO> positions = positionMapper.selectList(positionWrapper);
            if (positions == null || positions.size() == 0) {
                return companies;
            }
            positions.stream().forEach(position -> {
                if (!companyIds.contains(position.getCompanyId())) {
                    companyIds.add(position.getCompanyId());
                }
            });
        }
        QueryWrapper<CompanyPO> companyWrapper = new QueryWrapper<CompanyPO>();
        if (companyIds.size() > 0) {
            companyWrapper.in("id", companyIds);
            List<CompanyPO> list = companyMapper.selectList(companyWrapper);
            list.stream().forEach(company -> {
                CompanyBO companyBO = new CompanyBO();
                BeanUtils.copyProperties(company, companyBO);
                companies.add(companyBO);
            });
        }

        return companies;
    }

    /**
     * 导出人员数据
     * @param ids 导出所选
     * @param all 是否导出所有
     * @param excelPOId 导出任务id
     * @param companyId 公司id
     * @param conditionQuery
     * @param keyword
     * @param orgId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void exportExcelData(List<Long> ids, Boolean all, Long excelPOId, Long companyId, PersonDetailBO conditionQuery, String keyword, Long orgId) {
        ExcelPO excelPO = excelMapper.selectById(excelPOId);
        excelPO.setStatus(1);
        excelManageService.excuteExcelState(excelPO);
        XSSFWorkbook workbook = new XSSFWorkbook();
        ExcelUtils.createExplainSheet(ExcelUtils.getPersonTemplateExplainComments(), workbook);
        Sheet sheet = workbook.createSheet(Constants.PERSON_DATA_SHEETNAME);
        ExcelUtils.createHeadComments(sheet, ExcelUtils.PERSON_IMPORT_TEMPLATE);
/*        Row titleRow = sheet.createRow(0);
        for (int i = 0; i < ExcelUtils.getPersonTemplateTiles().size(); i++) {
            titleRow.createCell(i, CellType.STRING).setCellValue(ExcelUtils.getPersonTemplateTiles().get(i));
        }*/
        List<PersonAddPO> persons = new ArrayList<PersonAddPO>();
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
        queryWrapper.eq("valid", 1);
        if (!all) {
//            queryWrapper.in("id", ids);
//            persons = list(queryWrapper);
            persons = MultiParamSql.forPerson(ids, queryWrapper, "id", personMapper);
            if (persons.size() > 0) {
                persons.stream().forEach(person -> {
                    transferSystemCode(person);
                });
            }
        }  else {
            PageResult<PersonDetailBO> pageResult = null;
            if (orgId != null && companyId != null && companyId.equals(orgId)) {
                pageResult = positionService.queryPositionPersonsBetter(companyId, orgId, keyword, 1, -1, conditionQuery, false);
            } else {
                PositionAddPO pos = positionMapper.selectById(orgId);
                DepartmentAddPO dep = departmentMapper.selectById(orgId);

                if (pos != null) {
                    pageResult = positionService.queryPositionPersonsBetter(companyId, orgId, keyword, 1, -1, conditionQuery, false);
                } else if (dep != null) {
                    pageResult = departmentService.queryDepartmentPersonsBetter(companyId, orgId, keyword, 1, -1, conditionQuery, false);
                } else {
                    try {
                        String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.PERSON_FILE, excelPOId);
                        excelPO.setFileName(filePath);
                        excelPO.setStatus(2);
                        return;
                    } catch (IOException e) {
                        log.warn("人员导出失败", e.getCause());
                        excelPO.setStatus(3);
                        return;
                    } finally {
                        excelManageService.excuteExcelState(excelPO);
                    }

                }
            }

            log.info("person export size====:{}", pageResult.getList().size());
            if (pageResult == null || pageResult.getList() == null || pageResult.getList().size() == 0) {
                try {
                    String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.PERSON_FILE, excelPOId);
                    excelPO.setFileName(filePath);
                    excelPO.setStatus(2);
                    return;
                } catch (IOException e) {
                    log.warn(e.getMessage(), e.getCause());
                    excelPO.setStatus(3);
                    return;
                } finally {
                    excelManageService.excuteExcelState(excelPO);
                }
            }

            List<PersonDetailBO> personDetails = (List<PersonDetailBO>) pageResult.getList();
            for (PersonDetailBO personDetailBO : personDetails) {
                PersonAddPO personAddPO = new PersonAddPO();
                BeanUtils.copyProperties(personDetailBO, personAddPO);
                persons.add(personAddPO);
            }
        }

        if (persons == null || persons.size() == 0) {
            try {
                String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.PERSON_FILE, excelPOId);
                excelPO.setFileName(filePath);
                excelPO.setStatus(2);
                return;
            } catch (IOException e) {
                log.warn("人员导出失败", e.getCause());
                excelPO.setStatus(3);
                return;
            } finally {
                excelManageService.excuteExcelState(excelPO);
            }
        }
        /*List<Long> personIds = new ArrayList<Long>();
        persons.stream().forEach(person -> personIds.add(person.getId()));
        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
        relationWrapper.in("person_id", personIds);
        relationWrapper.eq("valid", 1);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(relationWrapper);*/

        List<Long> personIds = new ArrayList<>(200);
        List<PositionPersonPO> relations = new ArrayList<>(persons.size());
        for (int i = 0; i < persons.size(); i++) {
            personIds.add(persons.get(i).getId());
            if (personIds.size() >=100 || i >= (persons.size() - 1)) {
                QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
                relationWrapper.in("person_id", personIds);
                relationWrapper.eq("valid", 1);
                List<PositionPersonPO> tmpRelList = positionPersonMapper.selectList(relationWrapper);
                if (tmpRelList != null && tmpRelList.size() > 0) {
                    relations.addAll(tmpRelList);
                }
                personIds.clear();
            }
        }


        if (relations == null || relations.size() == 0) {
            try {
                for (int i = 0; i < persons.size(); i++) {
                    Row curRow = sheet.createRow(i + 1);
                    PersonAddPO person = persons.get(i);
                    createCell(curRow, person);
                }
                String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.PERSON_FILE, excelPOId);
                excelPO.setFileName(filePath);
                excelPO.setStatus(2);
                return;
            } catch (IOException e) {
                log.warn("人员导出失败", e.getCause());
                excelPO.setStatus(3);
                return;
            } finally {
                excelManageService.excuteExcelState(excelPO);
            }
        }

        /*List<Long> positionIds = new ArrayList<Long>();
        relations.stream().forEach(relation -> positionIds.add(relation.getPositionId()));
        QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<PositionAddPO>();
        positionWrapper.in("id", positionIds);
        positionWrapper.eq("valid", 1);
        List<PositionAddPO> positions = positionMapper.selectList(positionWrapper);*/

        List<Long> positionIds = new ArrayList<Long>(200);
        List<PositionAddPO> positions = new ArrayList<>(relations.size());
        for (int i = 0; i < relations.size(); i++) {
            positionIds.add(relations.get(i).getPositionId());
            if (positionIds.size() > 100 || i >= (relations.size() - 1)) {
                QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<PositionAddPO>();
                positionWrapper.in("id", positionIds);
                positionWrapper.eq("valid", 1);
                List<PositionAddPO> tmppositions = positionMapper.selectList(positionWrapper);
                if (tmppositions != null && tmppositions.size() > 0) {
                    positions.addAll(tmppositions);
                }
                positionIds.clear();
            }
        }

        if (positions == null || positions.size() == 0) {
            try {
                for (int i = 0; i < persons.size(); i++) {
                    Row curRow = sheet.createRow(i + 1);
                    PersonAddPO person = persons.get(i);
                    createCell(curRow, person);
                }
                String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.PERSON_FILE, excelPOId);
                excelPO.setFileName(filePath);
                excelPO.setStatus(2);
                return;
            } catch (IOException e) {
                log.warn("人员导出失败", e.getCause());
                excelPO.setStatus(3);
                return;
            } finally {
                excelManageService.excuteExcelState(excelPO);
            }
        }
        int count = 0;
        for (int i = 0; i < persons.size(); i++) {
            PersonAddPO person = persons.get(i);

            for (int j = 0; j < relations.size(); j++) {
                PositionPersonPO relation = relations.get(j);
                if (person.getId().equals(relation.getPersonId())) {
                    Row curRow = sheet.createRow(++count);
                    createCell(curRow, person);
                    for (int p = 0; p < positions.size(); p++) {
                        PositionAddPO position = positions.get(p);
                        if (relation.getPositionId().equals(position.getId())) {
                            curRow.createCell(6, CellType.STRING).setCellValue(position.getCode());
                            curRow.createCell(7, CellType.STRING).setCellValue(position.getName());
                        }
                    }
                }
            }
        }
        try {
            String filePath = ExcelUtils.createExportFile(workbook, ExcelUtils.PERSON_FILE, excelPOId);
            excelPO.setFileName(filePath);
            excelPO.setStatus(2);
        } catch (IOException e) {
            log.warn("人员导出失败", e.getCause());
            excelPO.setStatus(3);
            return;
        } finally {
            excelManageService.excuteExcelState(excelPO);
        }
    }

    private void transferSystemCode(PersonAddPO person) {
        if (person.getGender() != null) {
            String genderName = organizationAdapter.getSystemCodeName(person.getGender());
            person.setGender(genderName);
        }
        if (person.getStatus() != null) {
            String statusName = organizationAdapter.getSystemCodeName(person.getStatus());
            person.setStatus(statusName);
        }
        if (person.getTitle() != null) {
            String titleName = organizationAdapter.getSystemCodeName(person.getTitle());
            person.setTitle(titleName);
        }
        if (person.getEducation() != null) {
            String eduName = organizationAdapter.getSystemCodeName(person.getEducation());
            person.setEducation(eduName);
        }
    }
    private void createCell(Row curRow, PersonAddPO person) {

        PositionAddPO mainPosition = positionMapper.selectById(person.getMainPosition());
        curRow.createCell(4, CellType.STRING).setCellValue(mainPosition.getCode());
        curRow.createCell(5, CellType.STRING).setCellValue(mainPosition.getName());
        curRow.createCell(1, CellType.STRING).setCellValue(person.getCode());
        curRow.createCell(0, CellType.STRING).setCellValue(person.getName());
        curRow.createCell(2, CellType.STRING).setCellValue(person.getGender());
        curRow.createCell(3, CellType.STRING).setCellValue(person.getStatus());
/*        if (person.getGender().equals(Constants.GENDER_FEMALE) || person.getGender().equals("女")) {
            curRow.createCell(2, CellType.STRING).setCellValue("女");
        } else {
            curRow.createCell(2, CellType.STRING).setCellValue("男");
        }
        if (person.getStatus().equals(Constants.ON_WORK_CODE) || person.getStatus().equals("在职")) {
            curRow.createCell(4, CellType.STRING).setCellValue("在职");
        } else {
            curRow.createCell(4, CellType.STRING).setCellValue("离职");
        }*/
        curRow.createCell(6, CellType.STRING).setCellValue(mainPosition.getCode());
        curRow.createCell(7, CellType.STRING).setCellValue(mainPosition.getName());
        if (StringUtils.isNotBlank(person.getPhone())) {
            curRow.createCell(8, CellType.STRING).setCellValue(person.getPhone());
        }
        if (StringUtils.isNotBlank(person.getEmail())) {
            curRow.createCell(9, CellType.STRING).setCellValue(person.getEmail());
        }
        if (StringUtils.isNotBlank(person.getDescription())) {
            curRow.createCell(10, CellType.STRING).setCellValue(person.getDescription());
        }
        if (StringUtils.isNotBlank(person.getEntryDate())) {
            curRow.createCell(11, CellType.STRING).setCellValue(person.getEntryDate());
        }
        if (StringUtils.isNotBlank(person.getTitle())) {
            curRow.createCell(12, CellType.STRING).setCellValue(person.getTitle());
        }
        if (StringUtils.isNotBlank(person.getQualification())) {
            curRow.createCell(13, CellType.STRING).setCellValue(person.getQualification());
        }
        if (StringUtils.isNotBlank(person.getEducation())) {
            curRow.createCell(14, CellType.STRING).setCellValue(person.getEducation());
        }
        if (StringUtils.isNotBlank(person.getMajor())) {
            curRow.createCell(15, CellType.STRING).setCellValue(person.getMajor());
        }
        if (StringUtils.isNotBlank(person.getIdNumber())) {
            curRow.createCell(16, CellType.STRING).setCellValue(person.getIdNumber());
        }
        if (StringUtils.isNotBlank(person.getClassifiedLevel())) {
            curRow.createCell(17, CellType.STRING).setCellValue(person.getClassifiedLevel());
        }
    }

    @Override
    public List<PersonDTO> queryPersonByNotification(List<String> roleCodes, List<String> positionCodes, List<String> departmentCodes, List<String> personCodes) {

        List<PersonDTO> personDTOS = new ArrayList<PersonDTO>();
        //根据角色id查人
        if (roleCodes != null && roleCodes.size() > 0) {
            List<RoleUserDTO> roleUsers = organizationAdapter.findeRoleUserByRoleCodes(roleCodes);
            if (roleUsers != null && roleUsers.size() > 0) {
                //StringBuilder userSb = new StringBuilder();

                //roleUsers.stream().forEach(roleUser -> userSb.append(roleUser.getUserId()).append(","));
                //userSb.deleteCharAt(userSb.length() - 1);

                Map<Long, UserDetailDTO> users = new HashMap<>();
                Integer countIndex = 0;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < roleUsers.size(); i++) {
                    countIndex++;
                    RoleUserDTO roleUserDTO = roleUsers.get(i);
                    sb.append(roleUserDTO.getUserId()).append(",");
                    if (countIndex == roleUsers.size() || countIndex == 50) {
                        countIndex = 0;
                        sb.deleteCharAt(sb.length() - 1);
                        Map<Long, UserDetailDTO> curUsers = organizationAdapter.getUsersDetail(sb.toString());
                        if (curUsers != null && curUsers.size() > 0) {
                            users.putAll(curUsers);
                        }
                        sb = new StringBuilder();
                    }
                }

                //Map<Long, UserDetailDTO> users = organizationAdapter.getUsersDetail(userSb.toString());
                Map<Long, String> personIdToUserName = new HashedMap<Long, String>(users.size());
                if (users != null && users.size() > 0) {
                    Set<Map.Entry<Long, UserDetailDTO>> userSet = users.entrySet();
                    Iterator<Map.Entry<Long, UserDetailDTO>> userIt = userSet.iterator();
                    List<Long> perIds = new ArrayList<Long>();
                    while (userIt.hasNext()) {
                        Map.Entry<Long, UserDetailDTO> userMap = userIt.next();
                        if (userMap != null && userMap.getValue() != null && userMap.getValue().getPersonId() != null) {
                            perIds.add(userMap.getValue().getPersonId());
                            personIdToUserName.put(userMap.getValue().getPersonId(), userMap.getValue().getUserName());
                        }
                    }
                    if (perIds.size() > 0) {
                        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
//                        queryWrapper.in("id", perIds);
                        queryWrapper.eq("valid", 1);
                        queryWrapper.eq("status", Constants.ON_WORK_CODE);
//                        List<PersonAddPO> persons = list(queryWrapper);
                        List<PersonAddPO> persons = MultiParamSql.forPerson(perIds, queryWrapper, "id", personMapper);
                        if (persons != null && persons.size() > 0) {
                            persons.stream().forEach(person -> {
                                PersonDTO personDTO = new PersonDTO();
                                BeanUtils.copyProperties(person, personDTO);
                                personDTO.setUserName(personIdToUserName.get(person.getId()));
                                personDTOS.add(personDTO);
                            });
                        }
                    }

                }


            }
        }
        //根据岗位Id查人
        if (positionCodes != null && positionCodes.size() > 0) {
            QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
            posWrapper.in("code", positionCodes);
            posWrapper.eq("valid", 1);
            List<PositionAddPO> positions = positionMapper.selectList(posWrapper);
            List<Long> positionIds = new ArrayList<Long>();
            if (positions != null && positions.size() > 0) {
                positions.stream().forEach(position -> positionIds.add(position.getId()));
                QueryWrapper<PositionPersonPO> queryWrapper = new QueryWrapper<PositionPersonPO>();
                queryWrapper.in("position_id", positionIds);
                queryWrapper.eq("valid", 1);
                List<PositionPersonPO> relations = positionPersonMapper.selectList(queryWrapper);
                if (relations != null && relations.size() > 0) {
                    List<Long> perIds = new ArrayList<Long>();
                    relations.stream().forEach(relation -> perIds.add(relation.getPersonId()));
                    QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<PersonAddPO>();
//                    personWrapper.in("id", perIds);
                    personWrapper.eq("valid", 1);
                    personWrapper.eq("status", Constants.ON_WORK_CODE);
//                    List<PersonAddPO> persons = list(personWrapper);
                    List<PersonAddPO> persons = MultiParamSql.forPerson(perIds, personWrapper, "id", personMapper);
                    if (persons != null && persons.size() > 0) {
                        /*StringBuilder personIdSb = new StringBuilder();
                        perIds.stream().forEach(perId -> personIdSb.append(perId).append(","));
                        personIdSb.deleteCharAt(personIdSb.length() - 1);
                        Map<Long, UserDetailDTO> personMap = organizationAdapter.getUsersDetailByPerson(personIdSb.toString());*/

                        Map<Long, UserDetailDTO> personMap = new HashMap<>();
                        Integer countIndex = 0;
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < perIds.size(); i++) {
                            countIndex++;
                            Long personId = perIds.get(i);
                            sb.append(personId).append(",");
                            if (countIndex == perIds.size() || countIndex == 50) {
                                countIndex = 0;
                                sb.deleteCharAt(sb.length() - 1);
                                Map<Long, UserDetailDTO> curUsers = organizationAdapter.getUsersDetailByPerson(sb.toString());
                                if (curUsers != null && curUsers.size() > 0) {
                                    personMap.putAll(curUsers);
                                }
                                sb = new StringBuilder();
                            }
                        }
                        persons.stream().forEach(person -> {
                            PersonDTO personDTO = new PersonDTO();
                            BeanUtils.copyProperties(person, personDTO);
                            if (personMap != null && personMap.get(person.getId()) != null) {
                                personDTO.setUserName(personMap.get(person.getId()).getUserName());
                            }

                            personDTOS.add(personDTO);
                        });
                    }
                }
            }
        }
        //根据部门Id查人
        if (departmentCodes != null && departmentCodes.size() > 0) {
            QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<DepartmentAddPO>();
            deptWrapper.in("code", departmentCodes);
            deptWrapper.eq("valid", 1);
            List<DepartmentAddPO> departments = departmentMapper.selectList(deptWrapper);
            List<Long> departmentIds = new ArrayList<Long>();
            if (departments != null && departments.size() > 0) {
                departments.stream().forEach(department -> departmentIds.add(department.getId()));
                QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<PositionAddPO>();
                queryWrapper.in("dep_id", departmentIds);
                queryWrapper.eq("valid", 1);
                List<PositionAddPO> positions = positionMapper.selectList(queryWrapper);
                if (positions != null && positions.size() > 0) {
                    List<Long> posIds = new ArrayList<Long>();
                    positions.stream().forEach(position -> posIds.add(position.getId()));
                    QueryWrapper<PositionPersonPO> posWrapper = new QueryWrapper<PositionPersonPO>();
                    posWrapper.in("position_id", posIds);
                    posWrapper.eq("valid", 1);
                    List<PositionPersonPO> relations = positionPersonMapper.selectList(posWrapper);
                    if (relations != null && relations.size() > 0) {
                        List<Long> perIds = new ArrayList<Long>();
                        relations.stream().forEach(relation -> perIds.add(relation.getPersonId()));
                        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<PersonAddPO>();
//                        personWrapper.in("id", perIds);
                        personWrapper.eq("valid", 1);
                        personWrapper.eq("status", Constants.ON_WORK_CODE);
//                        List<PersonAddPO> persons = list(personWrapper);
                        List<PersonAddPO> persons = MultiParamSql.forPerson(perIds, personWrapper, "id", personMapper);
                        if (persons != null && persons.size() > 0) {
                            /*StringBuilder personIdSb = new StringBuilder();
                            perIds.stream().forEach(perId -> personIdSb.append(perId).append(","));
                            personIdSb.deleteCharAt(personIdSb.length() - 1);
                            Map<Long, UserDetailDTO> personMap = organizationAdapter.getUsersDetailByPerson(personIdSb.toString());*/
                            Map<Long, UserDetailDTO> personMap = new HashMap<>();
                            Integer countIndex = 0;
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < perIds.size(); i++) {
                                countIndex++;
                                Long personId = perIds.get(i);
                                sb.append(personId).append(",");
                                if (countIndex == perIds.size() || countIndex == 50) {
                                    countIndex = 0;
                                    sb.deleteCharAt(sb.length() - 1);
                                    Map<Long, UserDetailDTO> curUsers = organizationAdapter.getUsersDetailByPerson(sb.toString());
                                    if (curUsers != null && curUsers.size() > 0) {
                                        personMap.putAll(curUsers);
                                    }
                                    sb = new StringBuilder();
                                }
                            }
                            persons.stream().forEach(person -> {
                                PersonDTO personDTO = new PersonDTO();
                                BeanUtils.copyProperties(person, personDTO);
                                if (personMap.get(person.getId()) != null) {
                                    personDTO.setUserName(personMap.get(person.getId()).getUserName());
                                }
                                personDTOS.add(personDTO);
                            });
                        }
                    }
                }
            }

        }

        //根据人员id
        if (personCodes != null && personCodes.size() > 0) {
            QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
            queryWrapper.in("code", personCodes);
            queryWrapper.eq("valid", 1);
            queryWrapper.eq("status", Constants.ON_WORK_CODE);
            List<PersonAddPO> persons = list(queryWrapper);
            if (persons != null && persons.size() > 0) {
                List<Long> personIds = new ArrayList<Long>();
                persons.stream().forEach(person -> personIds.add(person.getId()));
                /*StringBuilder personIdSb = new StringBuilder();
                personIds.stream().forEach(perId -> personIdSb.append(perId).append(","));
                personIdSb.deleteCharAt(personIdSb.length() - 1);
                Map<Long, UserDetailDTO> personMap = organizationAdapter.getUsersDetailByPerson(personIdSb.toString());*/
                Map<Long, UserDetailDTO> personMap = new HashMap<>();
                Integer countIndex = 0;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < personIds.size(); i++) {
                    countIndex++;
                    Long personId = personIds.get(i);
                    sb.append(personId).append(",");
                    if (countIndex == personIds.size() || countIndex == 50) {
                        countIndex = 0;
                        sb.deleteCharAt(sb.length() - 1);
                        Map<Long, UserDetailDTO> curUsers = organizationAdapter.getUsersDetailByPerson(sb.toString());
                        if (curUsers != null && curUsers.size() > 0) {
                            personMap.putAll(curUsers);
                        }
                        sb = new StringBuilder();
                    }
                }
                persons.stream().forEach(person -> {
                    PersonDTO personDTO = new PersonDTO();
                    BeanUtils.copyProperties(person, personDTO);
                    if (personMap.get(person.getId()) != null) {
                        personDTO.setUserName(personMap.get(person.getId()).getUserName());
                    }
                    personDTOS.add(personDTO);
                });
            }
        }
        return personDTOS;
    }

    @Override
    public List<PersonBO> queryAllPersons() {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
        queryWrapper.eq("valid", 1);
        List<PersonAddPO> list = list(queryWrapper);
        List<PersonBO> personDTOS = new ArrayList<PersonBO>();
        if (list != null && list.size() > 0) {
            list.stream().forEach(personAddPO -> {
                PersonBO personBO = new PersonBO();
                BeanUtils.copyProperties(personAddPO, personBO);
                personDTOS.add(personBO);
            });
        }
        return personDTOS;
    }

    @Override
    public List<CompanyBO> queryCompanIdByPersonCode(String personCode) {
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<PersonAddPO>();
        personWrapper.eq("code", personCode);
        personWrapper.eq("valid", 1);
        PersonAddPO person = getOne(personWrapper);
        List<CompanyBO> companies = new ArrayList<CompanyBO>();
        if (person == null) {
            return companies;
        }
        QueryWrapper<PositionPersonPO> queryWrapper = new QueryWrapper<PositionPersonPO>();
        queryWrapper.eq("person_id", person.getId());
        queryWrapper.eq("valid", 1);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(queryWrapper);
        List<Long> positionIds = new ArrayList<Long>();

        if (relations != null && relations.size() > 0) {
            relations.stream().forEach(relation -> positionIds.add(relation.getPositionId()));
        } else {
            return companies;
        }
        List<Long> companyIds = new ArrayList<Long>();

        if (positionIds.size() > 0) {
            QueryWrapper<PositionAddPO> positionWrapper = new QueryWrapper<PositionAddPO>();
            positionWrapper.in("id", positionIds);
            List<PositionAddPO> positions = positionMapper.selectList(positionWrapper);
            if (positions == null || positions.size() == 0) {
                return companies;
            }
            positions.stream().forEach(position -> {
                if (!companyIds.contains(position.getCompanyId())) {
                    companyIds.add(position.getCompanyId());
                }
            });
        }
        QueryWrapper<CompanyPO> companyWrapper = new QueryWrapper<CompanyPO>();
        if (companyIds.size() > 0) {
            companyWrapper.in("id", companyIds);
            List<CompanyPO> list = companyMapper.selectList(companyWrapper);
            list.stream().forEach(company -> {
                CompanyBO companyBO = new CompanyBO();
                BeanUtils.copyProperties(company, companyBO);
                companies.add(companyBO);
            });
        }

        return companies;
    }

    @Override
    public List<PersonDetailBO> queryPersonInfoByIds(List<Long> ids) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
//        queryWrapper.in("id", ids);
//        List<PersonAddPO> list = list(queryWrapper);
        List<PersonAddPO> list = MultiParamSql.forPerson(ids, queryWrapper, "id", personMapper);
        if (list == null || list.size() == 0) {
            return null;
        }
        List<PersonDetailBO> bos = new ArrayList<PersonDetailBO>();
        list.stream().forEach(po -> {
            PersonDetailBO personDetailBO = new PersonDetailBO();
            BeanUtils.copyProperties(po, personDetailBO);
            bos.add(personDetailBO);
        });
        return bos;
    }

    @Override
    public List<PersonPositionBO> queryPersonPosition(Long id, Long companyId) {
        PersonAddPO personAddPO = getById(id);
        if (personAddPO == null) {
            return null;
        }
        QueryWrapper<PositionPersonPO> personWrapper = new QueryWrapper<>();
        personWrapper.eq("person_id", id);
        personWrapper.eq("valid", 1);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(personWrapper);
        if (relations == null || relations.size() == 0) {
            return null;
        }
        List<Long> posIds = new ArrayList<>();
        relations.stream().forEach(relation -> posIds.add(relation.getPositionId()));
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();
        posWrapper.in("id", posIds);
        posWrapper.eq("valid", 1);
        if (companyId != null) {
            posWrapper.eq("company_id", companyId);
        }
        List<PositionAddPO> poses = positionMapper.selectList(posWrapper);
        if (poses == null || poses.size() == 0) {
            return null;
        }
        List<PersonPositionBO> results = new ArrayList<>();
        poses.stream().forEach(pos -> {
            PersonPositionBO personPositionBO = new PersonPositionBO();
            personPositionBO.setId(pos.getId());
            personPositionBO.setName(pos.getName());
            personPositionBO.setCode(pos.getCode());

            QueryWrapper<DepartmentAddPO> deptWrapper = new QueryWrapper<>();
            deptWrapper.eq("id", pos.getDepId());
            deptWrapper.eq("valid", 1);
            DepartmentAddPO departmentAddPO = departmentMapper.selectOne(deptWrapper);
            if (departmentAddPO != null) {
                personPositionBO.setDeptName(departmentAddPO.getName());
            }
            personPositionBO.setFullPath(pos.getFullPath());
            if (personAddPO.getMainPosition().equals(pos.getId())) {
                personPositionBO.setMainPosition(true);
                results.add(0, personPositionBO);
            } else {
                results.add(personPositionBO);
            }
        });
        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offPosition(PersonOffPositionBO personOffPositionBO, String tenantId) {
        PersonAddPO personAddPO = getById(personOffPositionBO.getId());
        if (personAddPO == null) {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NOT_EXISTS);
        }


        List<Long> offPosIds = personOffPositionBO.getPositionIds();
        if (personOffPositionBO.getMainPositionId() != null && offPosIds.contains(personOffPositionBO.getMainPositionId())) {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_RELATION_POSITION_MUST_NOT_MAIN_POSITION);
        }
        if (personOffPositionBO.getMainPositionId() == null && offPosIds.contains(personAddPO.getMainPosition())) {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_RELATION_POSITION_MUST_NOT_MAIN_POSITION);
        }
        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<>();
        relationWrapper.eq("person_id", personAddPO.getId());
        relationWrapper.eq("valid", 1);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(relationWrapper);
        List<PositionPersonPO> updateRels = new ArrayList<>();
        if (relations == null || relations.size() == 0) {
            return;
        }
        Iterator<PositionPersonPO> relIt = relations.iterator();
        Boolean hasMain = false;
        if (personOffPositionBO.getMainPositionId() == null) {
            hasMain = true;
        }
        while (relIt.hasNext()) {
            PositionPersonPO rel = relIt.next();
            if (personOffPositionBO.getMainPositionId() != null && personOffPositionBO.getMainPositionId().equals(rel.getPositionId())) {
                hasMain = true;
            }
            if (offPosIds.contains(rel.getPositionId())) {
                rel.setValid(false);
                rel.setOffTime(personOffPositionBO.getOffTime());
                updateRels.add(rel);
                relIt.remove();
            }
        }
        if (!hasMain) {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_MAIN_POSITION_NOT_EXISTS);
        }
        if (relations.size() == 0) {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_RELATION_POSITION_LESS_THAN_ONE);
        }
        //positionPersonService.batchSaveOrUpdate(updateRels);
        offAllOrg(updateRels, personOffPositionBO.getId());
        if (personOffPositionBO.getMainPositionId() != null && !personAddPO.getMainPosition().equals(personOffPositionBO.getMainPositionId())) {
            personAddPO.setMainPosition(personOffPositionBO.getMainPositionId());
            updateById(personAddPO);
        }
        List<Long> personIds = new ArrayList<>();
        personIds.add(personOffPositionBO.getId());
        List<PersonRoleDTO> roles = generatePersonRoleByPositionId(personIds);
        if (roles != null && roles.size() >0) {
            Boolean flag = organizationAdapter.informRoleChange(roles);
            if (!flag) {
                throw new OrganizationException(OrganizationErrorEnum.ROLE_CHANGE_ERROR);
            }
        }

        //kafka
        List<PersonAddPO> personAddPOList = new ArrayList<>();
        personAddPOList.add(personAddPO);
        publishCreateOrUpdateMessage(personAddPOList, tenantId, "UPDATE");

    }

    private void offAllOrg(List<PositionPersonPO> updateRels, Long personId) {

        positionPersonService.batchDeleteRelations(updateRels);

        List<Long> posIds = new ArrayList<>();
        updateRels.stream().forEach(rel -> posIds.add(rel.getPositionId()));
        departmentPersonService.offDepartmentByPersonId(posIds, personId);
        companyPersonService.offDepartmentByPersonId(posIds, personId);
    }

    @Override
    public List<DepartmentDetailBO> queryPersonsDepartmentsByPersonIds(List<Long> ids) {
        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
        relationWrapper.eq("valid", 1);
        relationWrapper.in("person_id", ids);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(relationWrapper);
        if (relations == null || relations.size() == 0) {
            return null;
        }
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
        personWrapper.eq("valid", 1);
        personWrapper.in("id", ids);
        List<PersonAddPO> persons = list(personWrapper);
        if (persons == null || persons.size() == 0) {
            return null;
        }
        List<Long> posIds = new ArrayList<>();
        relations.stream().forEach(rel -> posIds.add(rel.getPositionId()));
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();
        posWrapper.eq("valid", 1);
        posWrapper.in("id", posIds);
        List<PositionAddPO> poses = positionMapper.selectList(posWrapper);
        if (poses == null || poses.size() == 0) {
            return null;
        }
        List<Long> deptIds = new ArrayList<>();
        poses.stream().forEach(pos -> deptIds.add(pos.getDepId()));
        QueryWrapper<DepartmentAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("valid", 1);
        queryWrapper.in("id", deptIds);
        List<DepartmentAddPO> depts = departmentMapper.selectList(queryWrapper);
        if (depts == null || depts.size() == 0) {
            return null;
        }
        List<DepartmentDetailBO> results = new ArrayList<>();
        depts.stream().forEach(dept -> {
            DepartmentDetailBO departmentDetailBO = new DepartmentDetailBO();
            BeanUtils.copyProperties(dept, departmentDetailBO);
            results.add(departmentDetailBO);
        });
        return results;
    }

    @Override
    public List<PositionDetailBO> queryPersonsPositionsByPersonIds(List<Long> ids) {
        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
        relationWrapper.eq("valid", 1);
        relationWrapper.in("person_id", ids);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(relationWrapper);
        if (relations == null || relations.size() == 0) {
            return null;
        }
        List<Long> posIds = new ArrayList<>();
        relations.stream().forEach(rel -> posIds.add(rel.getPositionId()));
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();
        posWrapper.eq("valid", 1);
        posWrapper.in("id", posIds);
        List<PositionAddPO> poses = positionMapper.selectList(posWrapper);
        if (poses == null || poses.size() == 0) {
            return null;
        }
        List<PositionDetailBO> results = new ArrayList<>();
        poses.stream().forEach(pos -> {
            PositionDetailBO positionDetailBO = new PositionDetailBO();
            BeanUtils.copyProperties(pos, positionDetailBO);
            results.add(positionDetailBO);
        });
        return results;
    }

    /**
     * @param code 人员编号
     * @param tenantId
     */
    @Override
    @Transactional
    public void deletePersonByCode(String code, String tenantId) {
        // TODO 删除关联用户
        PersonAddPO person = queryPersonByCode(code);
        if (person == null) {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NOT_EXISTS);
        }
        UpdateWrapper<PersonAddPO> updateWrapper = new UpdateWrapper<PersonAddPO>();
        updateWrapper.eq("code", code);
        updateWrapper.set("valid", 0);
        update(updateWrapper);
        groupPersonService.deleteByPersonId(person.getId());
        positionPersonService.deleteByPersonId(person.getId());

        //kafka
        List<PersonAddPO> personAddPOList = new ArrayList<>();
        personAddPOList.add(person);
        publishDeletePersonMessage(personAddPOList, tenantId);

    }

    private PersonAddPO queryPersonByCode(String code) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
        queryWrapper.eq("code", code);
        queryWrapper.eq("valid", 1);
        return getOne(queryWrapper);
    }

    private List<PersonAddPO> queryPersonByCode(List<String> codes) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
        queryWrapper.in("code", codes);
        queryWrapper.eq("valid", 1);
        return list(queryWrapper);
    }

    /**
     * 批量删除人员
     * @param codes
     * @param tenantId
     */
    @Override
    public void batchDeletePersonByCode(List<String> codes, String tenantId) {
        List<PersonAddPO> persons = queryPersonByCode(codes);
        if (persons == null || persons.size() == 0) {
            return;
        }
        Long[] personIds = new Long[persons.size()];
        for (int i = 0; i < persons.size(); i++) {
            personIds[i] = persons.get(i).getId();
        }
        deletePerson(personIds, tenantId);
        /*UpdateWrapper<PersonAddPO> updateWrapper = new UpdateWrapper<PersonAddPO>();
        updateWrapper.in("code", codes);
        updateWrapper.set("valid", 0);
        update(updateWrapper);
        List<Long> personIds = persons.stream().map(PersonAddPO::getId).collect(Collectors.toList());
        if (!personIds.isEmpty()) {
            groupPersonService.batchDeleteByPersonId(personIds);
            positionPersonService.batchDeleteByPersonId(personIds);
        }

        publishDeletePersonMessage(persons, tenantId);*/
    }

    @Override
    public JSONObject getCurrentLoginInfo(Long id, String includes) {
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
        personWrapper.eq("valid", 1);
        personWrapper.eq("id", id);
        PersonAddPO personAddPO = getOne(personWrapper);
        if (personAddPO == null) {
            return new JSONObject();
        }
        JSONObject result = new JSONObject();

        PersonBaseServiceBO personBaseServiceBO = new PersonBaseServiceBO();
        BeanUtils.copyProperties(personAddPO, personBaseServiceBO);
        JSONObject personJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(personBaseServiceBO), includes, Constants.PERSON);
        result.put("staff", personJson);

        Long mainPositionId = personAddPO.getMainPosition();
        PositionAddPO mainPosition = positionMapper.selectById(mainPositionId);
        if (mainPosition == null) {
            result.put("company", new JSONObject());
            result.put("department", new JSONObject());
            result.put("mainPosition", new JSONObject());
            return result;
        }
        PositionBaseServiceBO positionBaseServicePO = new PositionBaseServiceBO();
        BeanUtils.copyProperties(mainPosition, positionBaseServicePO);
        JSONObject positionJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(positionBaseServicePO), includes, Constants.POSITION);
        result.put("mainPosition", positionJson);

        Long deptId = mainPosition.getDepId();
        DepartmentAddPO mainPositonDept = departmentMapper.selectById(deptId);
        if (mainPositonDept == null) {
            result.put("department", new JSONObject());
        } else {
            DepartmentBaseServiceBO departmentBaseServiceBO = new DepartmentBaseServiceBO();
            BeanUtils.copyProperties(mainPositonDept, departmentBaseServiceBO);
            JSONObject deptJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(departmentBaseServiceBO), includes, Constants.DEPARTMENT);

            QueryWrapper<OrganizationManagerPO> managerPOQueryWrapper = new QueryWrapper<>();
            managerPOQueryWrapper.eq("org_Id", mainPositonDept.getId());
            List<OrganizationManagerPO> managerRelations = organizationManagerMapper.selectList(managerPOQueryWrapper);
            if (managerRelations != null && managerRelations.size() > 0) {
                List<Long> personIds = new ArrayList<>();
                managerRelations.stream().forEach(rel -> personIds.add(rel.getManagerId()));
                QueryWrapper<PersonAddPO> managerWrapper = new QueryWrapper<>();
                managerWrapper.in("id", personIds);
                managerWrapper.eq("valid", 1);
                List<PersonAddPO> persons = list(managerWrapper);
                if (persons == null || persons.size() == 0) {
                    deptJson.put("manager", new JSONObject());
                } else {
                    PersonAddPO person = persons.get(0);
                    PersonBaseServiceBO managerBaseServiceBO = new PersonBaseServiceBO();
                    BeanUtils.copyProperties(person, managerBaseServiceBO);
                    JSONObject managerJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(managerBaseServiceBO), includes, Constants.PERSON);
                    deptJson.put("manager", managerJson);
                }
            } else {
                deptJson.put("manager", new JSONObject());
            }

            result.put("department", deptJson);
        }

        Long companyId = mainPosition.getCompanyId();
        CompanyPO companyPO = companyMapper.selectById(companyId);
        if (companyPO == null) {
            result.put("company", new JSONObject());
            return result;
        }
        CompanyBaseServiceBO companyBaseServicePO = new CompanyBaseServiceBO();
        BeanUtils.copyProperties(companyPO, companyBaseServicePO);
        JSONObject companyJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(companyBaseServicePO), includes, Constants.COMPANY);
        result.put("company", companyJson);
        return result;
    }

    @Override
    public StaffDetailInfoBO getStaff(Long id, String includes) {
        StaffDetailInfoBO staffDetailInfoBO = new StaffDetailInfoBO();
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
        personWrapper.eq("valid", 1);
        personWrapper.eq("id", id);
        PersonAddPO personAddPO = getOne(personWrapper);
        if (personAddPO == null) {
            return staffDetailInfoBO;
        }
        JSONObject result = new JSONObject();


        PersonBaseServiceBO personBaseServiceBO = new PersonBaseServiceBO();
        BeanUtils.copyProperties(personAddPO, personBaseServiceBO);
        JSONObject personJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(personBaseServiceBO), includes, Constants.PERSON);
        staffDetailInfoBO.setStaff(personJson.toJavaObject(PersonBaseServiceBO.class));        ;


        Long mainPositionId = personAddPO.getMainPosition();
        PositionAddPO mainPosition = positionMapper.selectById(mainPositionId);
        if (mainPosition == null) {
            return staffDetailInfoBO;
        }
        PositionBaseServiceBO positionBaseServicePO = new PositionBaseServiceBO();
        BeanUtils.copyProperties(mainPosition, positionBaseServicePO);
        JSONObject positionJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(positionBaseServicePO), includes, Constants.POSITION);
        result.put("mainPosition", positionJson);
        staffDetailInfoBO.setMainPosition(positionJson.toJavaObject(PositionBaseServiceBO.class));

        Long deptId = mainPosition.getDepId();
        DepartmentAddPO mainPositonDept = departmentMapper.selectById(deptId);
        if (mainPositonDept == null) {
            result.put("department", new JSONObject());
        } else {
            DepartmentBaseServiceBO departmentBaseServiceBO = new DepartmentBaseServiceBO();
            BeanUtils.copyProperties(mainPositonDept, departmentBaseServiceBO);
            JSONObject deptJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(departmentBaseServiceBO), includes, Constants.DEPARTMENT);
            staffDetailInfoBO.setDepartment(deptJson.toJavaObject(DepartmentBaseServiceBO.class));
        }

        Long companyId = mainPosition.getCompanyId();
        CompanyPO companyPO = companyMapper.selectById(companyId);
        if (companyPO == null) {
            result.put("company", new JSONObject());
            return staffDetailInfoBO;
        }
        CompanyBaseServiceBO companyBaseServicePO = new CompanyBaseServiceBO();
        BeanUtils.copyProperties(companyPO, companyBaseServicePO);
        JSONObject companyJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(companyBaseServicePO), includes, Constants.COMPANY);
        staffDetailInfoBO.setCompany(companyJson.toJavaObject(CompanyBaseServiceBO.class));
        return staffDetailInfoBO;
    }

    @Override
    public JSONObject getStaffById(Long id, String includes) {
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
        personWrapper.eq("valid", 1);
        personWrapper.eq("id", id);
        PersonAddPO personAddPO = getOne(personWrapper);
        if (personAddPO == null) {
            return new JSONObject();
        }
        PersonBaseServiceBO personBaseServiceBO = new PersonBaseServiceBO();
        BeanUtils.copyProperties(personAddPO, personBaseServiceBO);
        JSONObject personJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(personBaseServiceBO), includes, Constants.PERSON);

/*        Long mainPositionId = personAddPO.getMainPosition();
        PositionAddPO mainPosition = positionMapper.selectById(mainPositionId);
        if (mainPosition == null) {
            personJson.put("mainPosition", new JSONObject());
            return personJson;
        }
        PositionBaseServiceBO positionBaseServicePO = new PositionBaseServiceBO();
        BeanUtils.copyProperties(mainPosition, positionBaseServicePO);
        JSONObject positionJson = baseServiceService.transferToJSON((JSONObject) JSON.toJSON(positionBaseServicePO), includes, Constants.POSITION);
        personJson.put("mainPosition", positionJson);*/
        return personJson;
    }

    @Override
    public List<PositionDetailBO> queryPersonPositionsByPersonId(Long id) {
        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
        relationWrapper.eq("valid", 1);
        relationWrapper.eq("person_id", id);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(relationWrapper);
        if (relations == null || relations.size() == 0) {
            return null;
        }
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
        personWrapper.eq("valid", 1);
        personWrapper.eq("id", id);
        PersonAddPO personAddPO = getOne(personWrapper);
        if (personAddPO == null) {
            return null;
        }
        List<Long> posIds = new ArrayList<>();
        relations.stream().forEach(rel -> posIds.add(rel.getPositionId()));
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();
        posWrapper.eq("valid", 1);
        posWrapper.in("id", posIds);
        List<PositionAddPO> poses = positionMapper.selectList(posWrapper);
        if (poses == null || poses.size() == 0) {
            return null;
        }
        List<PositionDetailBO> results = new ArrayList<>();
        poses.stream().forEach(pos -> {

            PositionDetailBO positionDetailBO = new PositionDetailBO();
            BeanUtils.copyProperties(pos, positionDetailBO);
            if (pos.getId().equals(personAddPO.getMainPosition())) {
                positionDetailBO.setMainPosition(true);
            }
            results.add(positionDetailBO);
        });
        return results;
    }

    //======old version======
    @Override
    public JSONObject queryPersonList(String keywords, Boolean hasAccount, Boolean isAll, Boolean includeOrgs, Integer page, Integer per_page, String noBear, String tenantId) {
        JSONObject result = new JSONObject();
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
        queryWrapper.eq("valid", 1);
        if (StringUtils.isNotBlank(keywords)) {
            queryWrapper.and(item -> item.like("name", keywords).or().like("code", keywords).or().like("description", keywords));
        }
        int count = count(queryWrapper);
        JSONObject pagination = new JSONObject();
        pagination.put("current", page);
        pagination.put("pageSize", per_page);
        pagination.put("total", count);
        result.put("pagination", pagination);
        if(count == 0) {
            result.put("list", new JSONArray());
            return result;
        }
        Page<PersonAddPO> pageInfo = new Page<PersonAddPO>(page, per_page, count);
        page(pageInfo, queryWrapper);
        List<PersonAddPO> persons = pageInfo.getRecords();
        if (persons == null) {
            result.put("list", new JSONArray());
            return result;
        }
        JSONArray array = new JSONArray();
        List<Long> personIds = new ArrayList<Long>();
        List<Long> personIdsForUser = new ArrayList<Long>();
        persons.stream().forEach(person -> {
            personIds.add(person.getId());
            personIdsForUser.add(person.getId());
            JSONObject json = new JSONObject();
            json.put("id", person.getId());
            json.put("code", person.getCode());
            json.put("showName", person.getName());
            if ("female".equals(person.getGender())) {
                json.put("gender", 0);
            } else {
                json.put("gender", 1);
            }
            json.put("name", person.getOldId());
            json.put("description", person.getDescription());
            if ("onWork".equals(person.getStatus())) {
                json.put("status", 0);
            } else {
                json.put("status", 1);
            }
            json.put("organizations", new JSONArray());
            json.put("account", new JSONObject());
            json.put("phone", person.getPhone());
            json.put("email", person.getEmail());
            array.add(json);
        });
        result.put("list", array);
        if (includeOrgs) {

            QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
            relationWrapper.in("person_id", personIds);
            relationWrapper.eq("valid", 1);
            List<PositionPersonPO> relations = positionPersonMapper.selectList(relationWrapper);
            if (relations == null || relations.size() == 0) {
                return result;
            }
            List<Long> positionIds = new ArrayList<Long>();
            relations.stream().forEach(relation -> positionIds.add(relation.getPositionId()));
            QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
            posWrapper.in("id", positionIds);
            posWrapper.eq("valid", 1);
            List<PositionAddPO> positions = positionMapper.selectList(posWrapper);
            if (positions != null && positions.size() > 0) {
                for (int i = 0; i < array.size(); i++) {
                    JSONObject personJson = array.getJSONObject(i);
                    for (int r = 0; r < relations.size(); r++) {
                        PositionPersonPO positionPersonPO = relations.get(r);
                        if (personJson.getLong("id").equals(positionPersonPO.getPersonId())) {
                            for (int p = 0; p < positions.size(); p++) {
                                PositionAddPO positionAddPO = positions.get(p);
                                if (positionAddPO.getId().equals(positionPersonPO.getPositionId())) {
                                    JSONArray organizations = personJson.getJSONArray("organizations");
                                    JSONObject orgJson = new JSONObject();
                                    orgJson.put("fullPath", positionAddPO.getFullPath());
                                    orgJson.put("orgType", "Position");
                                    orgJson.put("code", positionAddPO.getOldId());
                                    orgJson.put("showName", positionAddPO.getName());
                                    orgJson.put("name", positionAddPO.getOldId());
                                    organizations.add(orgJson);
                                    DepartmentAddPO departmentAddPO = departmentMapper.selectById(positionAddPO.getDepId());
                                    if (departmentAddPO != null) {
                                        JSONObject deptJson = new JSONObject();
                                        deptJson.put("fullPath", departmentAddPO.getFullPath());
                                        deptJson.put("orgType", Constants.DEPARTMENT);
                                        deptJson.put("code", departmentAddPO.getOldId());
                                        deptJson.put("showName", departmentAddPO.getName());
                                        deptJson.put("name", departmentAddPO.getOldId());
                                        organizations.add(deptJson);
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
        if (hasAccount) {
            StringBuilder personIdSb = new StringBuilder();

            for (Long personId : personIdsForUser) {
                personIdSb.append(personId).append(",");
            }
            personIdSb.deleteCharAt(personIdSb.length() - 1);
            Map<Long, UserDetailDTO> userResult = organizationAdapter.getUsersDetailByPerson(personIdSb.toString());
            if (userResult == null || userResult.size() == 0) {
                return result;
            }

            for (int i = 0; i < array.size(); i++) {
                JSONObject json = array.getJSONObject(i);
                UserDetailDTO userDetailDTO = userResult.get(json.getLong("id"));
                if (userDetailDTO == null) {
                    continue;
                }
                json.getJSONObject("account").put("userName", userDetailDTO.getUserName());
                json.getJSONObject("account").put("email", json.getString("email"));
                json.getJSONObject("account").put("phone", json.getString("phone"));
                json.put("avatarUrl", "");
            }
        }

        return result;
    }

    @Override
    public JSONObject queryPersonDetail(String code, String noBear, String tenantId) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
        queryWrapper.eq("code", code);
        queryWrapper.eq("valid", 1);
        PersonAddPO person = getOne(queryWrapper);
        if (person == null) {
            throw new OrganizationException(404, Constants.PERSON_NOT_EXISTS, 404);
        }
        JSONObject json = new JSONObject();
        json.put("id", person.getId());
        json.put("code", person.getCode());
        json.put("showName", person.getName());
        if ("female".equals(person.getGender())) {
            json.put("gender", 0);
        } else {
            json.put("gender", 1);
        }
        json.put("name", person.getOldId());
        json.put("description", person.getDescription());
        if ("onWork".equals(person.getStatus())) {
            json.put("status", 0);
        } else {
            json.put("status", 1);
        }
        json.put("organizations", new JSONArray());
        json.put("account", new JSONObject());
        json.put("phone", person.getPhone());
        json.put("email", person.getEmail());
        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
        relationWrapper.eq("person_id", person.getId());
        relationWrapper.eq("valid", 1);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(relationWrapper);
        if (relations == null || relations.size() == 0) {
            return json;
        }
        List<Long> positionIds = new ArrayList<Long>();
        relations.stream().forEach(relation -> positionIds.add(relation.getPositionId()));
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<PositionAddPO>();
        posWrapper.in("id", positionIds);
        posWrapper.eq("valid", 1);
        List<PositionAddPO> positions = positionMapper.selectList(posWrapper);
        if (positions == null) {
            return json;
        }
        for (int r = 0; r < relations.size(); r++) {
            PositionPersonPO positionPersonPO = relations.get(r);
            if (person.getId().equals(positionPersonPO.getPersonId())) {
                for (int p = 0; p < positions.size(); p++) {
                    PositionAddPO positionAddPO = positions.get(p);
                    if (positionAddPO.getId().equals(positionPersonPO.getPositionId())) {
                        JSONArray organizations = json.getJSONArray("organizations");
                        JSONObject orgJson = new JSONObject();
                        orgJson.put("fullPath", positionAddPO.getFullPath());
                        orgJson.put("orgType", "Position");
                        orgJson.put("code", positionAddPO.getOldId());
                        orgJson.put("showName", positionAddPO.getName());
                        orgJson.put("name", positionAddPO.getOldId());
                        organizations.add(orgJson);
                        DepartmentAddPO departmentAddPO = departmentMapper.selectById(positionAddPO.getDepId());
                        if (departmentAddPO != null) {
                            JSONObject deptJson = new JSONObject();
                            deptJson.put("fullPath", departmentAddPO.getFullPath());
                            deptJson.put("orgType", Constants.DEPARTMENT);
                            deptJson.put("code", departmentAddPO.getOldId());
                            deptJson.put("showName", departmentAddPO.getName());
                            deptJson.put("name", departmentAddPO.getOldId());
                            organizations.add(deptJson);
                        }
                    }
                }
            }
        }
        StringBuilder personIdsSb = new StringBuilder();
        personIdsSb.append(person.getId());
        Map<Long, UserDetailDTO> userResult = organizationAdapter.getUsersDetailByPerson(personIdsSb.toString());
        if (userResult == null || userResult.size() == 0) {
            return json;
        }
        UserDetailDTO userDetailDTO = userResult.get(person.getId());
        json.getJSONObject("account").put("userName", userDetailDTO.getUserName());
        json.getJSONObject("account").put("email", json.getString("email"));
        json.getJSONObject("account").put("phone", json.getString("phone"));
        json.put("avatarUrl", "");

        return json;
    }

    /**
     * 人员修改方法实现
     * @param personAddPo
     */
    @Override
    public boolean updatePersonByCode(PersonAddPO personAddPo) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
        queryWrapper.eq("code", personAddPo.getCode());
        return update(personAddPo, queryWrapper);
    }

    @Override
    public List<Long> queryRoleIdByPersonId(Long personId) {
        QueryWrapper<PositionPersonPO> relWrapper = new QueryWrapper<>();
        relWrapper.eq("person_id", personId);
        relWrapper.eq("valid", 1);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(relWrapper);
        if (relations == null || relations.size() == 0) {
            return null;
        }
        QueryWrapper<PositionRolePO> roleWrapper = new QueryWrapper<>();
        relations.stream().forEach(rel -> {
            roleWrapper.or().eq("position_id", rel.getPositionId());
        });
        List<PositionRolePO> roles = positionRoleMapper.selectList(roleWrapper);
        if (roles == null || roles.size() == 0) {
            return null;
        }
        List<Long> roleIds = new ArrayList<>();
        roles.stream().forEach(role -> roleIds.add(role.getRoleId()));
        return roleIds;
    }

    @Override
    public Set<Long> queryPersonsByCompanyId(Long id) {
        QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("company_id", id);
        queryWrapper.eq("valid", 1);
        List<PositionAddPO> poses = positionMapper.selectList(queryWrapper);
        if (poses == null || poses.size() == 0) {
            return null;
        }
        QueryWrapper<PositionPersonPO> relWrapper = new QueryWrapper<>();
        relWrapper.eq("valid", 1);
        relWrapper.and(item -> {
            poses.stream().forEach(pos -> {
                item.or().eq("position_id", pos.getId());
            });
        });
        List<PositionPersonPO> rels = positionPersonMapper.selectList(relWrapper);
        if (rels == null || rels.size() == 0) {
            return null;
        }
        Set<Long> perIds = new HashSet<>();
        rels.stream().forEach(rel -> perIds.add(rel.getPersonId()));
        return perIds;
    }

    private List<PersonRoleDTO> generatePersonRoleByPositionId(List<Long> personIds) {
        Map<Long, PersonRoleDTO> perIdToRole = new HashMap<>();
        List<PersonRoleDTO> result = new ArrayList<>();

        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<>();
        relationWrapper.eq("valid", 1);
        relationWrapper.and(item -> {
            personIds.stream().forEach(personId -> {
                item.or().eq("person_id", personId);
                PersonRoleDTO personRoleDTO = new PersonRoleDTO();
                personRoleDTO.setPersonId(personId);
                perIdToRole.put(personId, personRoleDTO);
                result.add(personRoleDTO);
            });
        });
        List<PositionPersonPO> relations = positionPersonMapper.selectList(relationWrapper);
        if (relations == null || relations.size() == 0) {
            return result;
        }
        QueryWrapper<PositionRolePO> roleWrapper = new QueryWrapper<>();
        roleWrapper.and(item -> {
            relations.stream().forEach(rel -> {
                item.or().eq("position_id", rel.getPositionId());
            });
        });
        List<PositionRolePO> roles = positionRoleMapper.selectList(roleWrapper);
        if (roles == null || roles.size() == 0) {
            return result;
        }
        Map<Long, Set<Long>> posIdToRoleIds = new HashMap<>();
        roles.stream().forEach(positionRolePO -> {
            if (!posIdToRoleIds.containsKey(positionRolePO.getPositionId())) {
                Set<Long> roleSet = new HashSet<>();
                roleSet.add(positionRolePO.getRoleId());
                posIdToRoleIds.put(positionRolePO.getPositionId(), roleSet);
            } else {
                posIdToRoleIds.get(positionRolePO.getPositionId()).add(positionRolePO.getRoleId());
            }
        });

        for (PositionPersonPO positionPersonPO : relations) {
            if (!perIdToRole.containsKey(positionPersonPO.getPersonId())) {
                PersonRoleDTO personRoleDTO = new PersonRoleDTO();
                personRoleDTO.setPersonId(positionPersonPO.getPersonId());
                perIdToRole.put(positionPersonPO.getPersonId(), personRoleDTO);
                result.add(personRoleDTO);
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
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addVirtualPerson(String userName, Long positionId, String tenantId) {
        PersonAddPO personAddPO = new PersonAddPO();
        personAddPO.setCode(userName);
        personAddPO.setName(userName);
        personAddPO.setMainPosition(positionId);
        personAddPO.setStatus(Constants.ON_WORK_CODE);
        personAddPO.setSysFlag(true);
        addPerson(personAddPO, tenantId);
        return personAddPO.getId();
    }

    @Override
    public List<PersonDetailBO> queryPersonsByPositionId(Long positionId) {
        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<>();
        relationWrapper.lambda().eq(PositionPersonPO::getPositionId, positionId).eq(PositionPersonPO::getValid, true);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(relationWrapper);
        if (relations == null || relations.size() == 0) {
            return null;
        }
        List<Long> personIds = new ArrayList<>();
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
        personWrapper.eq("valid", true);
        personWrapper.and(item -> {
            for (PositionPersonPO rel : relations) {
                item.or().eq("id", rel.getPersonId());
                personIds.add(rel.getPersonId());
            }
        });
        List<PersonAddPO> persons = list(personWrapper);

        if (persons == null || persons.size() == 0) {
            return null;
        }
        Map<Long, UserDetailDTO> userMap = organizationAdapter.queryUsersByPersonIds(personIds, "");

        List<PersonDetailBO> result = new ArrayList<>();
        persons.stream().forEach(person -> {
            PersonDetailBO personDetailBO = new PersonDetailBO();
            BeanUtils.copyProperties(person, personDetailBO);
            result.add(personDetailBO);
            if (userMap != null && userMap.get(person.getId()) != null) {
                personDetailBO.setUserId(userMap.get(person.getId()).getId());
            }
        });

        return result;
    }

    @Override
    public List<PersonDetailBO> queryPersonsByDepartmentId(Long departmentId) {
        QueryWrapper<DepartmentPersonPO> relationWrapper = new QueryWrapper<>();
        relationWrapper.lambda().eq(DepartmentPersonPO::getDeptId, departmentId).eq(DepartmentPersonPO::getValid, true);
        List<DepartmentPersonPO> relations = departmentPersonMapper.selectList(relationWrapper);
        if (relations == null || relations.size() == 0) {
            return null;
        }
        List<Long> personIds = new ArrayList<>();
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
        personWrapper.eq("valid", true);
        personWrapper.and(item -> {
            for (DepartmentPersonPO rel : relations) {
                item.or().eq("id", rel.getPersonId());
                personIds.add(rel.getPersonId());
            }
        });
        List<PersonAddPO> persons = list(personWrapper);

        if (persons == null || persons.size() == 0) {
            return null;
        }
        Map<Long, UserDetailDTO> userMap = organizationAdapter.queryUsersByPersonIds(personIds, "");

        List<PersonDetailBO> result = new ArrayList<>();
        persons.stream().forEach(person -> {
            PersonDetailBO personDetailBO = new PersonDetailBO();
            BeanUtils.copyProperties(person, personDetailBO);
            result.add(personDetailBO);
            if (userMap != null && userMap.get(person.getId()) != null) {
                personDetailBO.setUserId(userMap.get(person.getId()).getId());
            }
        });

        return result;
    }

    @Override
    public Boolean checkPersonSupAndSub(Long supPersonId, Long subPersonId, Long companyId) {
        QueryWrapper<CompanyPersonPO> supRelationWrapper = new QueryWrapper<>();
        supRelationWrapper.lambda().eq(CompanyPersonPO::getCompanyId, companyId).eq(CompanyPersonPO::getPersonId, supPersonId).eq(CompanyPersonPO::getValid, true);
        List<CompanyPersonPO> supRelations = companyPersonMapper.selectList(supRelationWrapper);
        if (supRelations == null || supRelations.size() == 0) {
            return false;
        }

        QueryWrapper<CompanyPersonPO> subRelationWrapper = new QueryWrapper<>();
        subRelationWrapper.lambda().eq(CompanyPersonPO::getCompanyId, companyId).eq(CompanyPersonPO::getPersonId, subPersonId).eq(CompanyPersonPO::getValid, true);
        List<CompanyPersonPO> subRelations = companyPersonMapper.selectList(subRelationWrapper);
        if (subRelations == null || subRelations.size() == 0) {
            return false;
        }

        Set<Long> subPositionIds = new HashSet<>();
        subRelations.stream().forEach(item -> {
            subPositionIds.add(item.getPositionId());
        });

        for (CompanyPersonPO supPos : supRelations) {
            PositionAddPO positionAddPO = positionMapper.selectById(supPos.getPositionId());
            QueryWrapper<PositionAddPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(PositionAddPO::getValid, true).likeRight(PositionAddPO::getFullPath, positionAddPO.getFullPath() + "/").in(PositionAddPO::getId, subPositionIds);
            int count = positionMapper.selectCount(queryWrapper);
            if (count > 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<PersonDetailBO> queryPersonsById(List<Long> personIds) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
//        queryWrapper.in("id", personIds);
        queryWrapper.eq("valid", true);
//        List<PersonAddPO> list = list(queryWrapper);
        List<PersonAddPO> list = MultiParamSql.forPerson(personIds, queryWrapper, "id", personMapper);
        List<PersonDetailBO> results = new ArrayList<PersonDetailBO>();
        if (list == null) {
            return results;
        }
        Map<Long, UserDetailDTO> userMap = organizationAdapter.queryUsersByPersonIds(personIds, "");

        list.stream().forEach(person -> {
            PersonDetailBO personDetailBO = new PersonDetailBO();
            BeanUtils.copyProperties(person, personDetailBO);
            if (userMap != null && userMap.get(person.getId()) != null) {
                personDetailBO.setUserId(userMap.get(person.getId()).getId());
            }
            results.add(personDetailBO);
        });
        return results;
    }

    @Override
    public PersonDepartmentBO queryMainDepartmentByPersonId(Long id) {
        QueryWrapper<PersonAddPO> personQuery = new QueryWrapper<>();
        personQuery.lambda().eq(PersonAddPO::getId, id).eq(PersonAddPO::getValid, true);
        PersonAddPO personAddPO = getOne(personQuery);
        if (personAddPO == null) {
            return null;
        }
        PositionAddPO mainPosition = positionMapper.selectById(personAddPO.getMainPosition());
        if (mainPosition == null) {
            return null;
        }
        DepartmentAddPO departmentAddPO = departmentMapper.selectById(mainPosition.getDepId());
        if (departmentAddPO == null) {
            return null;
        }
        DepartmentDetailBO mainDepartment = new DepartmentDetailBO();
        BeanUtils.copyProperties(departmentAddPO, mainDepartment);

        String rootDeptId = departmentAddPO.getLayRec().split("-")[0];
        DepartmentAddPO rootDept = departmentMapper.selectById(Long.valueOf(rootDeptId));
        if (rootDept == null) {
            return null;
        }
        DepartmentDetailBO rootDepartment = new DepartmentDetailBO();
        BeanUtils.copyProperties(rootDept, rootDepartment);
        PersonDepartmentBO personDepartmentBO = new PersonDepartmentBO();
        personDepartmentBO.setDepartment(mainDepartment);
        personDepartmentBO.setRootDepartment(rootDepartment);
        return personDepartmentBO;
    }

    @Override
    public PageResult<PersonDetailBO> loadPersons(Integer current, Integer pageSize, Long fromTime) {
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();

        if (fromTime != null) {
            personWrapper.and(item -> {
                item.gt("modify_time", new Date(fromTime)).or().gt("create_time", new Date(fromTime));
            });
        } else {
            personWrapper.eq("valid", true);
        }
        personWrapper.eq("sys_flag", 0);
        int count = count(personWrapper);
        if (count == 0) {
            return new PageResult<>(null, count, pageSize, current);
        }
        Page<PersonAddPO> pageInfo = new Page<PersonAddPO>(current, pageSize, count);
        page(pageInfo, personWrapper);

        List<PersonDetailBO> personDetailList = new ArrayList<>();
        pageInfo.getRecords().stream().forEach(personPO -> {
            PersonDetailBO personDetailBO = new PersonDetailBO();
            BeanUtils.copyProperties(personPO, personDetailBO);
            Map<Long, UserDetailDTO> userMap = organizationAdapter.getUsersDetailByPerson(personPO.getId().toString());
            if (userMap != null && userMap.get(personPO.getId()) != null) {
                UserDetailDTO userDetailDTO = userMap.get(personPO.getId());
                personDetailBO.setUserId(userDetailDTO.getId());
                personDetailBO.setUserName(userDetailDTO.getUserName());
            }
            personDetailList.add(personDetailBO);
            QueryWrapper<PositionPersonPO> positionRelWrapper = new QueryWrapper<>();
            positionRelWrapper.lambda().eq(PositionPersonPO::getPersonId, personPO.getId()).eq(PositionPersonPO::getValid, true);
            List<PositionPersonPO> posRelList = positionPersonMapper.selectList(positionRelWrapper);
            if (posRelList != null && posRelList.size() > 0) {
                List<Long> posIds = new ArrayList<>();
                posRelList.stream().forEach(rel -> {
                    posIds.add(rel.getPositionId());
                });
                QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();
                posWrapper.lambda().in(PositionAddPO::getId, posIds).eq(PositionAddPO::getValid, true);
                List<PositionAddPO> poses = positionMapper.selectList(posWrapper);
                if (poses != null && poses.size() > 0) {
                    List<Long> depIds = new ArrayList<>();
                    List<MainPositionBO> positions = new ArrayList<>();
                    personDetailBO.setPosition(positions);
                    poses.stream().forEach(pos -> {
                        depIds.add(pos.getDepId());
                        MainPositionBO mainPositionBO = new MainPositionBO();
                        BeanUtils.copyProperties(pos, mainPositionBO);
                        if (mainPositionBO.getId().equals(personPO.getMainPosition())) {
                            mainPositionBO.setMainPosition(true);
                        }
                        positions.add(mainPositionBO);
                    });

                    QueryWrapper<DepartmentAddPO> depWrapper = new QueryWrapper<>();
                    depWrapper.lambda().in(DepartmentAddPO::getId, depIds).eq(DepartmentAddPO::getValid, true);
                    List<DepartmentAddPO> deps = departmentMapper.selectList(depWrapper);
                    if (deps != null && deps.size() > 0) {
                        List<RelationDepartmentBO> depts = new ArrayList<>();
                        personDetailBO.setDepartment(depts);
                        deps.stream().forEach(dep -> {
                            RelationDepartmentBO relationDepartmentBO = new RelationDepartmentBO();
                            BeanUtils.copyProperties(dep, relationDepartmentBO);
                            depts.add(relationDepartmentBO);
                        });
                    }
                }
            }

        });
        PageResult<PersonDetailBO> pageResult = new PageResult<>(personDetailList, count, pageSize, current);
        return pageResult;
    }

    /**
     * 根据人员编号查询人员信息
     * @param code
     * @return
     */
    @Override
    public PersonResultBO queryPersonByPersonCode(String code) {
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<>();
        personWrapper.lambda().eq(PersonAddPO::getCode, code).eq(PersonAddPO::getValid, true).eq(PersonAddPO::getSysFlag, 0);
        PersonAddPO personAddPO = getOne(personWrapper);
        if (personAddPO == null) {
            return null;
        }
        PersonResultBO personResultBO = new PersonResultBO();
        BeanUtils.copyProperties(personAddPO, personResultBO);
        Map<Long, UserDetailDTO> userMap = organizationAdapter.getUsersDetailByPerson(personAddPO.getId().toString());
        if (userMap != null) {
            UserDetailDTO userDetailDTO = userMap.get(personAddPO.getId());
            if (userDetailDTO != null) {
                UserBO userBO = new UserBO();
                userBO.setUsername(userDetailDTO.getUserName());
                personResultBO.setAccount(userBO);
            }
        }
        QueryWrapper<PositionPersonPO> posRelWrapper = new QueryWrapper<>();
        posRelWrapper.lambda().eq(PositionPersonPO::getPersonId, personAddPO.getId()).eq(PositionPersonPO::getValid, true);
        List<PositionPersonPO> posRelList = positionPersonMapper.selectList(posRelWrapper);
        if (posRelList == null || posRelList.size() == 0) {
            return personResultBO;
        }
        QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();
        posWrapper.eq("valid", true);
        posWrapper.and(item -> {
           for (PositionPersonPO positionPersonPO : posRelList) {
               item.or().eq("id", positionPersonPO.getPositionId());
           }
        });
        List<PositionAddPO> posList = positionMapper.selectList(posWrapper);
        if (posList == null || posList.size() == 0) {
            return personResultBO;
        }
        List<PositionResultBO> positions = new ArrayList<>();
        personResultBO.setPositions(positions);
        posList.stream().forEach(pos -> {
            CompanyPO companyPO = companyMapper.selectById(pos.getCompanyId());
            CompanyResultBO companyResultBO = new CompanyResultBO();
            BeanUtils.copyProperties(companyPO, companyResultBO);
            PositionResultBO positionResultBO = new PositionResultBO();
            BeanUtils.copyProperties(pos, positionResultBO);
            positionResultBO.setCompany(companyResultBO);
            positions.add(positionResultBO);
        });

        QueryWrapper<DepartmentPersonPO> depRelWrapper = new QueryWrapper<>();
        depRelWrapper.lambda().eq(DepartmentPersonPO::getPersonId, personAddPO.getId()).eq(DepartmentPersonPO::getValid, true);
        List<DepartmentPersonPO> depRelList = departmentPersonMapper.selectList(depRelWrapper);
        if (depRelList == null || depRelList.size() == 0) {
            return personResultBO;
        }
        QueryWrapper<DepartmentAddPO> depWrapper = new QueryWrapper<>();
        depWrapper.eq("valid", true);
        depWrapper.and(item -> {
            for (DepartmentPersonPO departmentPersonPO : depRelList) {
                item.or().eq("id", departmentPersonPO.getDeptId());
            }
        });
        List<DepartmentAddPO> depList = departmentMapper.selectList(depWrapper);
        if (depList == null || depList.size() == 0) {
            return personResultBO;
        }
        List<DepartmentResultBO> departments = new ArrayList<>();
        personResultBO.setDepartments(departments);
        depList.stream().forEach(dep -> {
            CompanyPO companyPO = companyMapper.selectById(dep.getCompanyId());
            CompanyResultBO companyResultBO = new CompanyResultBO();
            BeanUtils.copyProperties(companyPO, companyResultBO);
            DepartmentResultBO departmentResultBO = new DepartmentResultBO();
            BeanUtils.copyProperties(dep, departmentResultBO);
            departmentResultBO.setCompany(companyResultBO);
            departments.add(departmentResultBO);
        });
        return personResultBO;
    }

    @Override
    public PersonLeaderBO getPersonLeader(Long personId) {
        PersonAddPO personAddPO = getById(personId);
        if (personAddPO == null) {
            return null;
        }
        PersonLeaderBO personLeaderBO = new PersonLeaderBO();
        if (personAddPO.getDirectLeaderId() != null) {
            PersonAddPO directPerson = getById(personAddPO.getDirectLeaderId());
            if (directPerson != null) {
                PersonBO directBO = new PersonBO();
                BeanUtils.copyProperties(directPerson, directBO);
                Map<Long, UserDetailDTO> directUser = organizationAdapter.getUsersDetailByPerson(directPerson.getId().toString());
                if (directUser != null && directUser.get(directPerson.getId()) != null) {
                    directBO.setUserId(directUser.get(directPerson.getId()).getId());
                }
                personLeaderBO.setDirectLeader(directBO);
            }
        }
        if (personAddPO.getGrandLeaderId() != null) {
            PersonAddPO grandPerson = getById(personAddPO.getGrandLeaderId());
            if (grandPerson != null) {
                PersonBO grandBO = new PersonBO();
                BeanUtils.copyProperties(grandPerson, grandBO);
                Map<Long, UserDetailDTO> grandUser = organizationAdapter.getUsersDetailByPerson(grandPerson.getId().toString());
                if (grandUser != null && grandUser.get(grandPerson.getId()) != null) {
                    grandBO.setUserId(grandUser.get(grandPerson.getId()).getId());
                }
                personLeaderBO.setGrandLeader(grandBO);
            }
        }
        return personLeaderBO;
    }

    @Override
    public PageResult<PersonBaseInfoBO> getPersonsByDepartmentCode(String departmentCode, Integer current, Integer pageSize) {
        DepartmentSynchronizationInfoPO departmentPO = departmentMapper.getDepartmentByCode(departmentCode);
        if (null == departmentPO) {
            throw new BizHttpStatusException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS, 400);
        }
        Integer total = personMapper.getPersonsCountByDepartmentCode(departmentCode);
        if (total == 0) {
            return new PageResult<PersonBaseInfoBO>(new ArrayList<PersonBaseInfoBO>(), 0, pageSize, current);
        }
        String dbType = dataId.getDataId();

        List<PersonBaseInfoPO> persons = personMapper.getPersonsByDepartmentCode(departmentCode, current, pageSize, dbType);
        if (persons == null || persons.size() == 0) {
            return new PageResult<PersonBaseInfoBO>(new ArrayList<PersonBaseInfoBO>(), 0, pageSize, current);
        }
        Map<String, List<SystemCodeResultDTO>> entityCodeMap = new HashMap<>();

        List<PersonBaseInfoBO> personBaseInfoBOS = new ArrayList<>();
        for (PersonBaseInfoPO personBaseInfoPO : persons) {
            PersonBaseInfoBO personBaseInfoBO = new PersonBaseInfoBO();
            BeanUtils.copyProperties(personBaseInfoPO, personBaseInfoBO);
            personBaseInfoBOS.add(personBaseInfoBO);

            // 查询公司列表
            List<PersonSynchronizationCompanyPO> personCompanyPOS = personMapper.queryCompaniesByPersonId(personBaseInfoPO.getId());
            if (CollectionUtils.isNotEmpty(personCompanyPOS)) {
                List<PersonCompanyBaseBO> personCompanyBaseBOS = personCompanyPOS.stream().filter(DistinctUtils.distinctByKey(PersonSynchronizationCompanyPO::getCompanyCode)).map(personCompanyPO -> {
                    PersonCompanyBaseBO personCompanyBaseBO = new PersonCompanyBaseBO();
                    personCompanyBaseBO.setCode(personCompanyPO.getCompanyCode());
                    personCompanyBaseBO.setName(personCompanyPO.getCompanyName());
                    return personCompanyBaseBO;
                }).collect(Collectors.toList());
                personBaseInfoBO.setCompanies(personCompanyBaseBOS);
            }

            // 查询部门列表
            List<PersonSynchronizationDepartmentPO> personDepartmentPOS = personMapper.queryDepartmentsByPersonId(personBaseInfoPO.getId());
            if (CollectionUtils.isNotEmpty(personDepartmentPOS)) {
                List<PersonDepartmentBaseBO> personDepartmentBaseBOS = personDepartmentPOS.stream().filter(DistinctUtils.distinctByKey(PersonSynchronizationDepartmentPO::getDepartmentCode)).map(personDepartmentPO -> {
                    PersonDepartmentBaseBO personDepartmentBaseBO = new PersonDepartmentBaseBO();
                    personDepartmentBaseBO.setCode(personDepartmentPO.getDepartmentCode());
                    personDepartmentBaseBO.setName(personDepartmentPO.getDepartmentName());
                    return personDepartmentBaseBO;
                }).collect(Collectors.toList());
                personBaseInfoBO.setDepartments(personDepartmentBaseBOS);
            }

            personBaseInfoBO.setTitle(baseServiceService.findSystemCode(personBaseInfoPO.getTitle(), entityCodeMap));
            personBaseInfoBO.setEducation(baseServiceService.findSystemCode(personBaseInfoPO.getEducation(), entityCodeMap));
            personBaseInfoBO.setGender(baseServiceService.findSystemCode(personBaseInfoPO.getGenderCode(), entityCodeMap));
            personBaseInfoBO.setStatus(baseServiceService.findSystemCode(personBaseInfoPO.getStatusCode(), entityCodeMap));
            MainPositionBaseBO mainPositionBaseBO = new MainPositionBaseBO();
            mainPositionBaseBO.setCode(personBaseInfoPO.getMainPositionCode());
            mainPositionBaseBO.setName(personBaseInfoPO.getMainPositionName());
            personBaseInfoBO.setMainPosition(mainPositionBaseBO);
        }
        return new PageResult(personBaseInfoBOS, total, pageSize, current);
    }


    @Override
    public PageResult<PersonBaseInfoBO> getPersonsByPositionCode(String positionCode, Integer current, Integer pageSize) {
        PositionSynchronizationInfoPO positionPO = positionMapper.getPositionByCode(positionCode);
        if (null == positionPO) {
            throw new BizHttpStatusException(PositionErrorEnum.POSITION_ID_NOT_EXISTS, 400);
        }
        Integer total = personMapper.getPersonsCountByPositionCode(positionCode);
        if (total == null || total == 0) {
            return new PageResult<PersonBaseInfoBO>(new ArrayList<PersonBaseInfoBO>(), 0, pageSize, current);
        }
        String dbType = dataId.getDataId();

        List<PersonBaseInfoPO> persons = personMapper.getPersonsByPositionCode(positionCode, current, pageSize, dbType);
        if (persons == null || persons.size() == 0) {
            return new PageResult<PersonBaseInfoBO>(new ArrayList<PersonBaseInfoBO>(), 0, pageSize, current);
        }
        Map<String, List<SystemCodeResultDTO>> entityCodeMap = new HashMap<>();

        List<PersonBaseInfoBO> personBaseInfoBOS = new ArrayList<>();
        for (PersonBaseInfoPO personBaseInfoPO : persons) {
            PersonBaseInfoBO personBaseInfoBO = new PersonBaseInfoBO();
            BeanUtils.copyProperties(personBaseInfoPO, personBaseInfoBO);
            personBaseInfoBOS.add(personBaseInfoBO);

            // 查询公司列表
            List<PersonSynchronizationCompanyPO> personCompanyPOS = personMapper.queryCompaniesByPersonId(personBaseInfoPO.getId());
            if (CollectionUtils.isNotEmpty(personCompanyPOS)) {
                List<PersonCompanyBaseBO> personCompanyBaseBOS = personCompanyPOS.stream().filter(DistinctUtils.distinctByKey(PersonSynchronizationCompanyPO::getCompanyCode)).map(personCompanyPO -> {
                    PersonCompanyBaseBO personCompanyBaseBO = new PersonCompanyBaseBO();
                    personCompanyBaseBO.setCode(personCompanyPO.getCompanyCode());
                    personCompanyBaseBO.setName(personCompanyPO.getCompanyName());
                    return personCompanyBaseBO;
                }).collect(Collectors.toList());
                personBaseInfoBO.setCompanies(personCompanyBaseBOS);
            }

            // 查询部门列表
            List<PersonSynchronizationDepartmentPO> personDepartmentPOS = personMapper.queryDepartmentsByPersonId(personBaseInfoPO.getId());
            if (CollectionUtils.isNotEmpty(personDepartmentPOS)) {
                List<PersonDepartmentBaseBO> personDepartmentBaseBOS = personDepartmentPOS.stream().filter(DistinctUtils.distinctByKey(PersonSynchronizationDepartmentPO::getDepartmentCode)).map(personDepartmentPO -> {
                    PersonDepartmentBaseBO personDepartmentBaseBO = new PersonDepartmentBaseBO();
                    personDepartmentBaseBO.setCode(personDepartmentPO.getDepartmentCode());
                    personDepartmentBaseBO.setName(personDepartmentPO.getDepartmentName());
                    return personDepartmentBaseBO;
                }).collect(Collectors.toList());
                personBaseInfoBO.setDepartments(personDepartmentBaseBOS);
            }

            personBaseInfoBO.setTitle(baseServiceService.findSystemCode(personBaseInfoPO.getTitle(), entityCodeMap));
            personBaseInfoBO.setEducation(baseServiceService.findSystemCode(personBaseInfoPO.getEducation(), entityCodeMap));
            personBaseInfoBO.setGender(baseServiceService.findSystemCode(personBaseInfoPO.getGenderCode(), entityCodeMap));
            personBaseInfoBO.setStatus(baseServiceService.findSystemCode(personBaseInfoPO.getStatusCode(), entityCodeMap));
            MainPositionBaseBO mainPositionBaseBO = new MainPositionBaseBO();
            mainPositionBaseBO.setCode(personBaseInfoPO.getMainPositionCode());
            mainPositionBaseBO.setName(personBaseInfoPO.getMainPositionName());
            personBaseInfoBO.setMainPosition(mainPositionBaseBO);
        }
        return new PageResult(personBaseInfoBOS, total, pageSize, current);
    }

    @Override
    public PageResult<PersonBaseInfoBO> getPersonsByCompanyCode(String companyCode, Integer current, Integer pageSize) {

        CompanyDetailInfoPO companyPO = companyMapper.getCompanyByCode(companyCode);
        if (null == companyPO) {
            throw new OrganizationException(OrganizationErrorEnum.COMPANY_PARAM_ID_NECESSARY);
        }

        Integer total = personMapper.getPersonsCountByCompanyCode(companyCode);
        if (total == 0) {
            return new PageResult<PersonBaseInfoBO>(new ArrayList<PersonBaseInfoBO>(), 0, pageSize, current);
        }
        String dbType = dataId.getDataId();

        List<PersonBaseInfoPO> persons = personMapper.getPersonsByCompanyCode(companyCode, current, pageSize, dbType);
        if (persons == null || persons.size() == 0) {
            return new PageResult<PersonBaseInfoBO>(new ArrayList<PersonBaseInfoBO>(), 0, pageSize, current);
        }
        Map<String, List<SystemCodeResultDTO>> entityCodeMap = new HashMap<>();

        List<PersonBaseInfoBO> personBaseInfoBOS = new ArrayList<>();
        for (PersonBaseInfoPO personBaseInfoPO : persons) {
            PersonBaseInfoBO personBaseInfoBO = new PersonBaseInfoBO();
            BeanUtils.copyProperties(personBaseInfoPO, personBaseInfoBO);
            personBaseInfoBOS.add(personBaseInfoBO);

            // 查询公司列表
            List<PersonSynchronizationCompanyPO> personCompanyPOS = personMapper.queryCompaniesByPersonId(personBaseInfoPO.getId());
            if (CollectionUtils.isNotEmpty(personCompanyPOS)) {
                List<PersonCompanyBaseBO> personCompanyBaseBOS = personCompanyPOS.stream().filter(DistinctUtils.distinctByKey(PersonSynchronizationCompanyPO::getCompanyCode)).map(personCompanyPO -> {
                    PersonCompanyBaseBO personCompanyBaseBO = new PersonCompanyBaseBO();
                    personCompanyBaseBO.setCode(personCompanyPO.getCompanyCode());
                    personCompanyBaseBO.setName(personCompanyPO.getCompanyName());
                    return personCompanyBaseBO;
                }).collect(Collectors.toList());
                personBaseInfoBO.setCompanies(personCompanyBaseBOS);
            }

            // 查询部门列表
            List<PersonSynchronizationDepartmentPO> personDepartmentPOS = personMapper.queryDepartmentsByPersonId(personBaseInfoPO.getId());
            if (CollectionUtils.isNotEmpty(personDepartmentPOS)) {
                List<PersonDepartmentBaseBO> personDepartmentBaseBOS = personDepartmentPOS.stream().filter(DistinctUtils.distinctByKey(PersonSynchronizationDepartmentPO::getDepartmentCode)).map(personDepartmentPO -> {
                    PersonDepartmentBaseBO personDepartmentBaseBO = new PersonDepartmentBaseBO();
                    personDepartmentBaseBO.setCode(personDepartmentPO.getDepartmentCode());
                    personDepartmentBaseBO.setName(personDepartmentPO.getDepartmentName());
                    return personDepartmentBaseBO;
                }).collect(Collectors.toList());
                personBaseInfoBO.setDepartments(personDepartmentBaseBOS);
            }

            personBaseInfoBO.setTitle(baseServiceService.findSystemCode(personBaseInfoPO.getTitle(), entityCodeMap));
            personBaseInfoBO.setEducation(baseServiceService.findSystemCode(personBaseInfoPO.getEducation(), entityCodeMap));
            personBaseInfoBO.setGender(baseServiceService.findSystemCode(personBaseInfoPO.getGenderCode(), entityCodeMap));
            personBaseInfoBO.setStatus(baseServiceService.findSystemCode(personBaseInfoPO.getStatusCode(), entityCodeMap));
            MainPositionBaseBO mainPositionBaseBO = new MainPositionBaseBO();
            mainPositionBaseBO.setCode(personBaseInfoPO.getMainPositionCode());
            mainPositionBaseBO.setName(personBaseInfoPO.getMainPositionName());
            personBaseInfoBO.setMainPosition(mainPositionBaseBO);
        }
        return new PageResult(personBaseInfoBOS, total, pageSize, current);
    }

    @Override
    public PageResult<PersonSynchronizationInfoBO> getPersons(String modifyTime, Integer current, Integer pageSize) {
        String dbType = dataId.getDataId();
        Integer total = personMapper.getPersonCount(modifyTime, dbType);
        if (total == null || total == 0) {
            return new PageResult<>(new ArrayList<>(), 0, pageSize, current);
        }


        List<PersonSynchronizationInfoPO> personSynchronizationInfoPOList = personMapper.getPersons(modifyTime, current, pageSize, dbType);
        if (personSynchronizationInfoPOList.size() == 0) {
            return new PageResult<>(new ArrayList<>(), 0, pageSize, current);
        }

        List<Long> personIds = new ArrayList<>();
        personSynchronizationInfoPOList.stream().forEach(personPO -> {
            personIds.add(personPO.getId());
        });

        List<PersonSynchronizationCompanyPO> personCompanyRelList = personMapper.queryCompaniesByPersonIds(personIds);
        // 通过人员ID和公司编码过滤重复数据
        List<PersonSynchronizationCompanyPO> personCompanyResultList = personCompanyRelList.stream()
                .collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(pc -> pc.getPersonId() + pc.getCompanyCode()))), ArrayList::new));
        Map<Long, List<PersonCompanyBaseBO>> personIdToCompanyMap = new HashMap<>();
        if (personCompanyResultList.size() > 0) {
            for (PersonSynchronizationCompanyPO personSynchronizationCompanyPO : personCompanyResultList) {
                if (personIdToCompanyMap.get(personSynchronizationCompanyPO.getPersonId()) == null) {
                    personIdToCompanyMap.put(personSynchronizationCompanyPO.getPersonId(), new ArrayList<>());
                }
                List<PersonCompanyBaseBO> personCompanyBaseBOList = personIdToCompanyMap.get(personSynchronizationCompanyPO.getPersonId());
                PersonCompanyBaseBO personCompanyBaseBO = new PersonCompanyBaseBO();
                personCompanyBaseBO.setCode(personSynchronizationCompanyPO.getCompanyCode());
                personCompanyBaseBO.setName(personSynchronizationCompanyPO.getCompanyName());
                personCompanyBaseBOList.add(personCompanyBaseBO);
            }
        }

        List<PersonSynchronizationDepartmentPO> personDepartmentRelList = personMapper.queryDepartmentsByPersonIds(personIds);
        // 通过人员ID和部门编码过滤重复数据
        List<PersonSynchronizationDepartmentPO> personDepartmentResultList = personDepartmentRelList.stream()
                .collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(pd -> pd.getPersonId() + pd.getDepartmentCode()))), ArrayList::new));
        Map<Long, List<PersonDepartmentBaseBO>> personIdToDepartmentMap = new HashMap<>();
        if (personDepartmentResultList.size() > 0) {
            for (PersonSynchronizationDepartmentPO personSynchronizationDepartmentPO : personDepartmentResultList) {
                if (personIdToDepartmentMap.get(personSynchronizationDepartmentPO.getPersonId()) == null) {
                    personIdToDepartmentMap.put(personSynchronizationDepartmentPO.getPersonId(), new ArrayList<>());
                }
                List<PersonDepartmentBaseBO> personDepartmentBaseBOList = personIdToDepartmentMap.get(personSynchronizationDepartmentPO.getPersonId());
                PersonDepartmentBaseBO personDepartmentBaseBO = new PersonDepartmentBaseBO();
                personDepartmentBaseBO.setCode(personSynchronizationDepartmentPO.getDepartmentCode());
                personDepartmentBaseBO.setName(personSynchronizationDepartmentPO.getDepartmentName());
                personDepartmentBaseBOList.add(personDepartmentBaseBO);
            }
        }

        List<PersonSynchronizationPositionPO> personRelList = personMapper.getPositionsByPersonIds(personIds);
        Map<Long, List<MainPositionBaseBO>> personIdToPositionMap = new HashMap<>();
        if (personRelList.size() > 0) {
            for (PersonSynchronizationPositionPO personSynchronizationPositionPO : personRelList) {
                if (personIdToPositionMap.get(personSynchronizationPositionPO.getPersonId()) == null) {
                    personIdToPositionMap.put(personSynchronizationPositionPO.getPersonId(), new ArrayList<>());
                }
                List<MainPositionBaseBO> mainPositionBaseBOList = personIdToPositionMap.get(personSynchronizationPositionPO.getPersonId());
                MainPositionBaseBO mainPositionBaseBO = new MainPositionBaseBO();
                mainPositionBaseBO.setCode(personSynchronizationPositionPO.getPositionCode());
                mainPositionBaseBO.setName(personSynchronizationPositionPO.getPositionName());
                mainPositionBaseBOList.add(mainPositionBaseBO);
            }
        }

        Map<String, List<SystemCodeResultDTO>> entityCodeMap = new HashMap<>();

        List<PersonSynchronizationInfoBO> personSynchronizationInfoBOList = new ArrayList<>();
        for (PersonSynchronizationInfoPO personSynchronizationInfoPO : personSynchronizationInfoPOList) {
            PersonSynchronizationInfoBO personSynchronizationInfoBO = new PersonSynchronizationInfoBO();

            // 时间格式转换
            if (null != personSynchronizationInfoPO.getModifyTime()) {
                String formatTime = responseFormatTime(personSynchronizationInfoPO.getModifyTime());
                personSynchronizationInfoPO.setModifyTime(formatTime);
            }
            BeanUtils.copyProperties(personSynchronizationInfoPO, personSynchronizationInfoBO);
            personSynchronizationInfoBO.setTitle(baseServiceService.findSystemCode(personSynchronizationInfoPO.getTitle(), entityCodeMap));
            personSynchronizationInfoBO.setEducation(baseServiceService.findSystemCode(personSynchronizationInfoPO.getEducation(), entityCodeMap));
            personSynchronizationInfoBO.setGender(baseServiceService.findSystemCode(personSynchronizationInfoPO.getGenderCode(), entityCodeMap));
            personSynchronizationInfoBO.setStatus(baseServiceService.findSystemCode(personSynchronizationInfoPO.getStatusCode(), entityCodeMap));
            MainPositionBaseBO mainPositionBaseBO = new MainPositionBaseBO();
            mainPositionBaseBO.setCode(personSynchronizationInfoPO.getMainPositionCode());
            mainPositionBaseBO.setName(personSynchronizationInfoPO.getMainPositionName());
            personSynchronizationInfoBO.setMainPosition(mainPositionBaseBO);

            // TODO 后续更改获取用户信息的方式
            UserBO userBO = new UserBO();
            UserDetailDTO userDetailByPerson = organizationAdapter.getUserDetailByPerson(personSynchronizationInfoPO.getId());
            if (null != userDetailByPerson && !StringUtils.isEmpty(userDetailByPerson.getUserName())) {
                userBO.setUsername(userDetailByPerson.getUserName());
            }
            personSynchronizationInfoBO.setUser(userBO);

            if (personIdToPositionMap.get(personSynchronizationInfoPO.getId()) == null) {
                personSynchronizationInfoBO.setPositions(new ArrayList<>());
            } else {
                personSynchronizationInfoBO.setCompanies(personIdToCompanyMap.get(personSynchronizationInfoPO.getId()));
                personSynchronizationInfoBO.setDepartments(personIdToDepartmentMap.get(personSynchronizationInfoPO.getId()));
                personSynchronizationInfoBO.setPositions(personIdToPositionMap.get(personSynchronizationInfoPO.getId()));
            }

            personSynchronizationInfoBOList.add(personSynchronizationInfoBO);
        }
        return new PageResult<>(personSynchronizationInfoBOList, total, pageSize, current);
    }

    @Override
    public Result<PersonSynchronizationInfoBO> getPersonDetailByPersonCode(String personCode) {
        // 获取人员
        PersonSynchronizationInfoPO personPO = personMapper.getPersonByPersonCode(personCode);
        if (null == personPO) {
            throw new BizHttpStatusException(OrganizationErrorEnum.PERSON_ID_NOT_EXISTS, 400);
        }
        // 时间格式转换
        if (null != personPO.getModifyTime()) {
            String formatTime = responseFormatTime(personPO.getModifyTime());
            personPO.setModifyTime(formatTime);
        }

        PersonSynchronizationInfoBO personSycBO = new PersonSynchronizationInfoBO();
        BeanUtils.copyProperties(personPO, personSycBO);

        // TODO 后续更改获取用户信息的方式
        UserBO userBO = new UserBO();
        UserDetailDTO userDetailByPerson = organizationAdapter.getUserDetailByPerson(personPO.getId());
        if (null != userDetailByPerson && !StringUtils.isEmpty(userDetailByPerson.getUserName())) {
            userBO.setUsername(userDetailByPerson.getUserName());
        }
        personSycBO.setUser(userBO);

        // 查询公司列表
        List<PersonSynchronizationCompanyPO> personCompanyPOS = personMapper.queryCompaniesByPersonId(personPO.getId());
        if (CollectionUtils.isNotEmpty(personCompanyPOS)) {
            List<PersonCompanyBaseBO> personCompanyBaseBOS = personCompanyPOS.stream().filter(DistinctUtils.distinctByKey(PersonSynchronizationCompanyPO::getCompanyCode)).map(personCompanyPO -> {
                PersonCompanyBaseBO personCompanyBaseBO = new PersonCompanyBaseBO();
                personCompanyBaseBO.setCode(personCompanyPO.getCompanyCode());
                personCompanyBaseBO.setName(personCompanyPO.getCompanyName());
                return personCompanyBaseBO;
            }).collect(Collectors.toList());
            personSycBO.setCompanies(personCompanyBaseBOS);
        }

        // 查询部门列表
        List<PersonSynchronizationDepartmentPO> personDepartmentPOS = personMapper.queryDepartmentsByPersonId(personPO.getId());
        if (CollectionUtils.isNotEmpty(personDepartmentPOS)) {
            List<PersonDepartmentBaseBO> personDepartmentBaseBOS = personDepartmentPOS.stream().filter(DistinctUtils.distinctByKey(PersonSynchronizationDepartmentPO::getDepartmentCode)).map(personDepartmentPO -> {
                PersonDepartmentBaseBO personDepartmentBaseBO = new PersonDepartmentBaseBO();
                personDepartmentBaseBO.setCode(personDepartmentPO.getDepartmentCode());
                personDepartmentBaseBO.setName(personDepartmentPO.getDepartmentName());
                return personDepartmentBaseBO;
            }).collect(Collectors.toList());
            personSycBO.setDepartments(personDepartmentBaseBOS);
        }

        // 查询岗位列表
        List<PersonSynchronizationPositionPO> personPositionPOS = personMapper.getPositionByPersonId(personPO.getId());
        if (!CollectionUtils.isEmpty(personPositionPOS)) {
            List<MainPositionBaseBO> personPositionBOS = personPositionPOS.stream().map(personPositionPO -> {
                MainPositionBaseBO positionBaseBO = new MainPositionBaseBO();
                positionBaseBO.setCode(personPositionPO.getPositionCode());
                positionBaseBO.setName(personPositionPO.getPositionName());
                return positionBaseBO;
            }).collect(Collectors.toList());
            personSycBO.setPositions(personPositionBOS);
        }

        Map<String, List<SystemCodeResultDTO>> entityCodeMap = new HashMap<>();
        BeanUtils.copyProperties(personPO, personSycBO);

        personSycBO.setTitle(baseServiceService.findSystemCode(personPO.getTitle(), entityCodeMap));
        personSycBO.setEducation(baseServiceService.findSystemCode(personPO.getEducation(), entityCodeMap));
        personSycBO.setGender(baseServiceService.findSystemCode(personPO.getGenderCode(), entityCodeMap));
        personSycBO.setStatus(baseServiceService.findSystemCode(personPO.getStatusCode(), entityCodeMap));
        MainPositionBaseBO mainPositionBaseBO = new MainPositionBaseBO();
        mainPositionBaseBO.setCode(personPO.getMainPositionCode());
        mainPositionBaseBO.setName(personPO.getMainPositionName());
        personSycBO.setMainPosition(mainPositionBaseBO);

        return new Result<>(personSycBO);
    }

    @Override
    public void deletePersonById(Long personId) {
        PersonAddPO personAddPO = getById(personId);
        if (personAddPO == null) {
            return;
        }
        personAddPO.setValid(false);
        updateById(personAddPO);
        groupPersonService.deleteByPersonId(personId);
        positionPersonService.deleteByPersonId(personId);
        deletePersonRelation(personId);

        orgMnecodeService.deleteOrgMnecodeByOrgId(Arrays.asList(personId),Constants.PERSON);
    }

    @Override
    public List<PersonFlowSimpleBO> queryPersonIdByCodes(List<String> codes) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(PersonAddPO::getCode, codes).eq(PersonAddPO::getValid, true);
        List<PersonAddPO> personAddPOList = list(queryWrapper);
        if (personAddPOList == null) {
            return null;
        }
        List<PersonFlowSimpleBO> personFlowSimpleBOList = new ArrayList<>();
        personAddPOList.stream().forEach(personAddPO -> {
            PersonFlowSimpleBO personFlowSimpleBO = new PersonFlowSimpleBO();
            BeanUtils.copyProperties(personAddPO, personFlowSimpleBO);
            personFlowSimpleBOList.add(personFlowSimpleBO);
        });
        return personFlowSimpleBOList;
    }

    @Override
    public void saveOrUpdateUserByPersonId(PersonUserBO personUserBO) {
        personMapper.saveOrUpdateUserByPersonId(personUserBO.getPersonId(), personUserBO.getUserId(), personUserBO.getUserName());
    }

    @Override
    public void deleteUserByPersonIds(List<Long> personIds) {
        personMapper.deleteUserByPersonId(personIds);
    }

    @Override
    public PersonUpdatePageBO queryDetailByPersonCode(String code) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PersonAddPO::getCode, code).eq(PersonAddPO::getValid, true);
        PersonAddPO personAddPO = getOne(queryWrapper);
        PersonUpdatePageBO personUpdatePageBO = new PersonUpdatePageBO();
        if (personAddPO == null) {
            return personUpdatePageBO;
        }
        Long personId = personAddPO.getId();
        BeanUtils.copyProperties(personAddPO, personUpdatePageBO);

        BaseInfoBO directLeader = new BaseInfoBO();
        BaseInfoBO grandLeader = new BaseInfoBO();
        personUpdatePageBO.setDirectLeader(directLeader);
        personUpdatePageBO.setGrandLeader(grandLeader);
        if (personAddPO.getDirectLeaderId() != null) {
            PersonAddPO directP = getById(personAddPO.getDirectLeaderId());
            if (directP != null) {
                directLeader.setId(directP.getId());
                directLeader.setName(directP.getName());
                directLeader.setCode(directP.getCode());
            }
        }
        if (personAddPO.getGrandLeaderId() != null) {
            PersonAddPO grandP = getById(personAddPO.getGrandLeaderId());
            if (grandP != null) {
                grandLeader.setId(grandP.getId());
                grandLeader.setName(grandP.getName());
                grandLeader.setCode(grandP.getCode());
            }
        }

        //查询人员关联的所有岗位
        QueryWrapper<PositionPersonPO> relationWrapper = new QueryWrapper<PositionPersonPO>();
        relationWrapper.eq("person_id", personId);
        relationWrapper.eq("valid", 1);
        List<PositionPersonPO> relations = positionPersonMapper.selectList(relationWrapper);
        if (relations != null) {
            List<PersonPositionBO> personPositionBOS = new ArrayList<PersonPositionBO>();
            relations.stream().forEach(relation -> {
                PersonPositionBO personPositionBO = new PersonPositionBO();
                personPositionBO.setId(relation.getPositionId());
                PositionAddPO positionAddPO = positionMapper.selectById(relation.getPositionId());
                personPositionBO.setName(positionAddPO.getName());
                personUpdatePageBO.setMainPosition(personAddPO.getMainPosition());
                personPositionBOS.add(personPositionBO);
            });
            personUpdatePageBO.setPositions(personPositionBOS);
        }

        personUpdatePageBO.setGender(personAddPO.getGender());
        personUpdatePageBO.setStatus(personAddPO.getStatus());
        personUpdatePageBO.setClassifedLevel(personAddPO.getClassifiedLevel());

        return personUpdatePageBO;
    }

    @Override
    public Integer countPersonUser() {
        return personMapper.countPersonUser();
    }

    @Override
    public PersonBO getPersonByUserName(String userName) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        queryWrapper.eq("valid", 1);
        PersonAddPO personAddPO = getOne(queryWrapper);
        if (ObjectUtils.isEmpty(personAddPO)) {
            return null;
        }
        PersonBO personBO = new PersonBO();
        BeanUtils.copyProperties(personAddPO,personBO);
        return personBO;
    }

    @Override
    public List<Long> queryMultiCompanyPersonsByCompanyId(Long companyId) {

        List<PersonCompanyPO> list = personMapper.queryMultiCompanyPersonsByCompanyId(companyId);
        if (list == null || list.size() == 0) {
            return new ArrayList<>();
        }
        Map<Long, HashSet<Long>> personIdToCompanyIdsMap = new HashMap<>();
        list.stream().forEach(rel -> {
            HashSet<Long> companyIdSet = personIdToCompanyIdsMap.get(rel.getPersonId());
            if (companyIdSet == null) {
                companyIdSet = new HashSet<>();
                personIdToCompanyIdsMap.put(rel.getPersonId(), companyIdSet);
            }
            companyIdSet.add(rel.getCompanyId());
        });
        List<Long> result = new ArrayList<>();
        Iterator<Map.Entry<Long, HashSet<Long>>> it = personIdToCompanyIdsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, HashSet<Long>> entry = it.next();
            Long personId = entry.getKey();
            HashSet<Long> companySet = entry.getValue();
            if (companySet.size() > 1) {
                result.add(personId);
            }
        }
        return result;
    }

    /**
     * 新增或修改人员发送消息
     * @param personAddPOList
     * @param tenantId
     * @param event
     */
    private void publishCreateOrUpdateMessage(List<PersonAddPO> personAddPOList, String tenantId, String event) {

        if (personAddPOList == null || personAddPOList.size() == 0) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        List<PersonMessageBO> messageBody = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        personAddPOList.stream().forEach(personAddPO -> {
            sb.append(personAddPO.getId()).append(",");
        });
        /*sb.deleteCharAt(sb.length() - 1);
        Map<Long, UserDetailDTO> userMap = organizationAdapter.getUsersDetailByPerson(sb.toString());

        Map<Long, String> personIdToUserMap = new HashMap<>();
        if (userMap != null && userMap.size() > 0) {
            Iterator<Map.Entry<Long, UserDetailDTO>> it = userMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, UserDetailDTO> entry = it.next();
                Long pid = entry.getKey();
                UserDetailDTO user = entry.getValue();
                personIdToUserMap.put(pid, user.getUserName());
            }
        }*/
        List<SystemCodeResultDTO> genderList = organizationAdapter.querySystemCodesByEntityCode(Constants.GENDER_ENTITYCODE);
        List<SystemCodeResultDTO> statusList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_STATUS_ENTITYCODE);
        List<SystemCodeResultDTO> titleList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_TITLE_ENTITYCODE);
        List<SystemCodeResultDTO> educationList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_EDUCATION_ENTITYCODE);
        personAddPOList.stream().forEach(personAddPO -> {
            PersonMessageBO personMessageBO = new PersonMessageBO();
            personMessageBO.setRowVersion(0L);
            personMessageBO.setId(personAddPO.getId());
            personMessageBO.setCode(personAddPO.getCode());
            personMessageBO.setName(personAddPO.getName());
            personMessageBO.setPhone(personAddPO.getPhone());
            personMessageBO.setEmail(personAddPO.getEmail());
            personMessageBO.setDescription(personAddPO.getDescription());
            personMessageBO.setAvatarUrl(personAddPO.getAvatarUrl());
            personMessageBO.setSignPicUrl(personAddPO.getSignPicUrl());
            personMessageBO.setEntryDate(personAddPO.getEntryDate());
            personMessageBO.setQualification(personAddPO.getQualification());
            personMessageBO.setMajor(personAddPO.getMajor());
            personMessageBO.setIdNumber(personAddPO.getIdNumber());
            SystemCodeBO gender = new SystemCodeBO();

            if (genderList != null && genderList.size() > 0 && StringUtils.isNotBlank(personAddPO.getGender())) {
                for (SystemCodeResultDTO genderType : genderList) {
                    if (personAddPO.getGender().equals(genderType.getEntityCode() + "/" + genderType.getCode())) {
                        gender.setCode(genderType.getCode());
                        gender.setName(genderType.getDisplayName());
                    }
                }
            }
            personMessageBO.setGender(gender);

            SystemCodeBO status = new SystemCodeBO();

            if (statusList != null && statusList.size() > 0 && StringUtils.isNotBlank(personAddPO.getStatus())) {
                for (SystemCodeResultDTO statusType : statusList) {
                    if (personAddPO.getStatus().equals(statusType.getEntityCode() + "/" + statusType.getCode())) {
                        status.setCode(statusType.getCode());
                        status.setName(statusType.getDisplayName());
                    }
                }
            }
            personMessageBO.setStatus(status);

            SystemCodeBO title = new SystemCodeBO();
            if (titleList != null && titleList.size() > 0 && StringUtils.isNotBlank(personAddPO.getTitle())) {
                for (SystemCodeResultDTO titleType : titleList) {
                    if (personAddPO.getTitle().equals(titleType.getEntityCode() + "/" + titleType.getCode())) {
                        title.setCode(titleType.getCode());
                        title.setName(titleType.getDisplayName());
                    }
                }
            }
            personMessageBO.setTitle(title);

            SystemCodeBO education = new SystemCodeBO();
            if (educationList != null && educationList.size() > 0 && StringUtils.isNotBlank(personAddPO.getEducation())) {
                for (SystemCodeResultDTO educationType : educationList) {
                    if (personAddPO.getEducation().equals(educationType.getEntityCode() + "/" + educationType.getCode())) {
                        education.setCode(educationType.getCode());
                        education.setName(educationType.getDisplayName());
                    }
                }
            }
            personMessageBO.setEducation(education);

            UserBO user = new UserBO();
            user.setUsername(personAddPO.getUserName());
            personMessageBO.setUser(user);

            QueryWrapper<PositionPersonPO> posPersonWrapper = new QueryWrapper<>();
            posPersonWrapper.lambda().eq(PositionPersonPO::getPersonId, personAddPO.getId()).eq(PositionPersonPO::getValid, true);
            List<PositionPersonPO> positionPersonPOList = positionPersonMapper.selectList(posPersonWrapper);
            List<Long> positionIds = new ArrayList<>();
            if (positionPersonPOList != null) {
                positionPersonPOList.stream().forEach(rel -> positionIds.add(rel.getPositionId()));
            }
            QueryWrapper<PositionAddPO> posWrapper = new QueryWrapper<>();
            posWrapper.lambda().in(PositionAddPO::getId, positionIds).eq(PositionAddPO::getValid, true);
            List<PositionAddPO> positionAddPOList = positionMapper.selectList(posWrapper);

            List<MainPositionBaseBO> positions = new ArrayList<>();
            if (positionAddPOList != null) {
                positionAddPOList.stream().forEach(pos -> {
                    MainPositionBaseBO positionBaseBO = new MainPositionBaseBO();
                    positionBaseBO.setId(pos.getId());
                    positionBaseBO.setCode(pos.getCode());
                    positionBaseBO.setName(pos.getName());
                    if (pos.getId().equals(personAddPO.getMainPosition())) {
                        MainPositionBaseBO mainPositionBaseBO = new MainPositionBaseBO();
                        mainPositionBaseBO.setId(pos.getId());
                        mainPositionBaseBO.setCode(pos.getCode());
                        mainPositionBaseBO.setName(pos.getName());
                        personMessageBO.setMainPosition(mainPositionBaseBO);
                    }
                    positions.add(positionBaseBO);
                });
            }
            personMessageBO.setPositions(positions);

            messageBody.add(personMessageBO);
        });
        Map<String, Object> header = new HashMap<>();
        header.put("encode", "json");
        header.put("event", event);

        OrganizationMessage.Builder<PersonMessageBO> messageBuilder = new OrganizationMessage.Builder<>();
        Message message = messageBuilder
                .setSender("organization")
                .setTenantId(tenantId)
                .setCreateTime(sdf.format(new Date()))
                .setTopic(PERSON_TOPIC)
                .setHeader(header)
                .setBody(messageBody)
                .build();
        organizationMessage.publishMessage(message);
    }

    /**
     * 删除人员发送kafka
     * @param personAddPOList
     * @param tenantId
     */
    private void publishDeletePersonMessage(List<PersonAddPO> personAddPOList, String tenantId) {

        List<PersonDeleteMessageBO> messageBody = new ArrayList<>();
        personAddPOList.stream().forEach(personAddPO -> {
            PersonDeleteMessageBO personDeleteMessageBO = new PersonDeleteMessageBO();
            BeanUtils.copyProperties(personAddPO, personDeleteMessageBO);
            personDeleteMessageBO.setRowVersion(0L);
            messageBody.add(personDeleteMessageBO);
        });
        Map<String, Object> header = new HashMap<>();
        header.put("encode", "json");
        header.put("event", "DELETE");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        OrganizationMessage.Builder<PersonDeleteMessageBO> messageBuilder = new OrganizationMessage.Builder<>();
        Message message = messageBuilder.setSender("organization")
                .setTenantId(tenantId)
                .setCreateTime(sdf.format(new Date()))
                .setTopic(PERSON_TOPIC)
                .setHeader(header)
                .setBody(messageBody)
                .build();
        organizationMessage.publishMessage(message);
    }

    /**
     * 新增人员不发kafka
     * @param personAddPo
     * @param tenantId
     */
    @Transactional(rollbackFor = Exception.class)
    private void addPersonWithoutKafka(PersonAddPO personAddPo, String tenantId) {
        if (personAddPo == null) {
            return;
        }
        Long uid = IDGenerator.newInstance().generate().longValue();
        String oldId = "Person_" + uid;
        personAddPo.setId(uid);
        personAddPo.setOldId(oldId);

        PositionAddPO positionAddPo = null;
        if (personAddPo.getMainPosition() != null) {
            positionAddPo = positionMapper.selectById(personAddPo.getMainPosition());
            if (positionAddPo == null) {
                throw new OrganizationException(OrganizationErrorEnum.PERSON_POSITION_ID_NOT_EXISTS);
            }
        } else {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_POSITION_ID_NOT_EXISTS);
        }

        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
        queryWrapper.eq("code", personAddPo.getCode());
        queryWrapper.eq("valid", 1);
        int count = count(queryWrapper);
        if (count > 0) {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_CODE_EXISTS);
        }
        //身份证校验
        if (StringUtils.isNotBlank(personAddPo.getIdNumber())) {
            QueryWrapper<PersonAddPO> idNumberWrapper = new QueryWrapper<>();
            idNumberWrapper.lambda().eq(PersonAddPO::getIdNumber, personAddPo.getIdNumber())
                    .eq(PersonAddPO::getValid, true);
            int idNumberCount = count(idNumberWrapper);
            if (idNumberCount > 0) {
                throw new OrganizationException(OrganizationErrorEnum.ID_NUMBER_EXISTS);
            }
        }
/*        if (StringUtils.isNotBlank(personAddPo.getEntryDate())) {
            Date entryDate = null;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            try {
                entryDate = format.parse(personAddPo.getEntryDate());
                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                sdf1.setTimeZone(TimeZone.getTimeZone("UTC"));
                personAddPo.setEntryDate(sdf1.format(entryDate));
            } catch (ParseException e) {
                throw new OrganizationException(OrganizationErrorEnum.DATE_FORMAT_ERROR);
            }
        }*/
        handleImageFilePath(personAddPo, tenantId);
        save(personAddPo);
        PositionPersonPO positionPersonPO = new PositionPersonPO();
        positionPersonPO.setPositionId(personAddPo.getMainPosition());
        positionPersonPO.setPersonId(personAddPo.getId());
        positionPersonMapper.insert(positionPersonPO);

        DepartmentPersonPO departmentPersonPO = new DepartmentPersonPO();
        departmentPersonPO.setDeptId(positionAddPo.getDepId());
        departmentPersonPO.setPersonId(personAddPo.getId());
        departmentPersonPO.setPositionId(positionAddPo.getId());
        departmentPersonMapper.insert(departmentPersonPO);

        CompanyPersonPO companyPersonPO = new CompanyPersonPO();
        companyPersonPO.setCompanyId(positionAddPo.getCompanyId());
        companyPersonPO.setPersonId(personAddPo.getId());
        companyPersonPO.setPositionId(positionAddPo.getId());
        companyPersonMapper.insert(companyPersonPO);
        orgMnecodeService.addOrgMnecode(personAddPo.getId(), personAddPo.getName(), Constants.PERSON);
    }
    private void handleImageFilePath(PersonAddPO personAddPO, String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            tenantId = Constants.FILE_NO_TENANTID;
        }
        String picFilePath = env.getProperty("file.path") + "/" + tenantId;
        if (StringUtils.isNotBlank(personAddPO.getAvatarUrl())) {
            if (!personAddPO.getAvatarUrl().startsWith(picFilePath + "/avatar/" + personAddPO.getCode() + ".")) {
                String targetFilePath = picFilePath + "/avatar/" + personAddPO.getCode() + personAddPO.getAvatarUrl().substring(personAddPO.getAvatarUrl().lastIndexOf("."), personAddPO.getAvatarUrl().length());
                File file = new File(personAddPO.getAvatarUrl());
                File parentSourceFile = file.getParentFile();
                if (file.exists()) {
                    FileInputStream sourceStream = null;
                    FileOutputStream targetStream = null;
                    try {
                        sourceStream = new FileInputStream(file);
                        File targetFile = FileToolUtils.createFile(targetFilePath);
                        targetStream = new FileOutputStream(targetFile);
                        IOUtils.copy(sourceStream, targetStream);
                        personAddPO.setAvatarUrl(targetFilePath);
                    } catch (Exception e) {
                        throw new OrganizationException(OrganizationErrorEnum.FILE_NOT_EXISTS);
                    } finally {
                        if (sourceStream != null) {
                            try {
                                sourceStream.close();
                                file.delete();
                                parentSourceFile.delete();
                            } catch (IOException e) {
                                throw new OrganizationException(OrganizationErrorEnum.FILE_NOT_EXISTS);
                            }
                        }
                        if (targetStream != null) {
                            try {
                                targetStream.close();
                            } catch (IOException e) {
                                throw new OrganizationException(OrganizationErrorEnum.FILE_NOT_EXISTS);
                            }
                        }
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(personAddPO.getSignPicUrl())) {
            if (!personAddPO.getSignPicUrl().startsWith(picFilePath + "/sign/" + personAddPO.getCode() + ".")) {
                String targetFilePath = picFilePath + "/sign/" + personAddPO.getCode() + personAddPO.getSignPicUrl().substring(personAddPO.getSignPicUrl().lastIndexOf("."), personAddPO.getSignPicUrl().length());
                File file = new File(personAddPO.getSignPicUrl());
                File parentSourceFile = file.getParentFile();
                if (file.exists()) {
                    FileInputStream sourceStream = null;
                    FileOutputStream targetStream = null;
                    try {
                        sourceStream = new FileInputStream(file);
                        File targetFile = FileToolUtils.createFile(targetFilePath);
                        targetStream = new FileOutputStream(targetFile);
                        IOUtils.copy(sourceStream, targetStream);
                        personAddPO.setSignPicUrl(targetFilePath);
                    } catch (Exception e) {
                        throw new OrganizationException(OrganizationErrorEnum.FILE_NOT_EXISTS);
                    } finally {
                        if (sourceStream != null) {
                            try {
                                sourceStream.close();
                                file.delete();
                                parentSourceFile.delete();
                            } catch (IOException e) {
                                throw new OrganizationException(OrganizationErrorEnum.FILE_NOT_EXISTS);
                            }
                        }
                        if (targetStream != null) {
                            try {
                                targetStream.close();
                            } catch (IOException e) {
                                throw new OrganizationException(OrganizationErrorEnum.FILE_NOT_EXISTS);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 修改人员不发kafka
     * @param personAddPo
     * @param tenantId
     */
    @Transactional(rollbackFor = Exception.class)
    private void updatePersonWithoutKafka(PersonAddPO personAddPo, String tenantId) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<PersonAddPO>();
        queryWrapper.eq("id", personAddPo.getId());
        //int count = count(queryWrapper);
        PersonAddPO currentPerson = getOne(queryWrapper);
        if (currentPerson == null) {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NOT_EXISTS);
        }
        //身份证校验
        if (StringUtils.isNotBlank(personAddPo.getIdNumber())) {
            QueryWrapper<PersonAddPO> idNumberWrapper = new QueryWrapper<>();
            idNumberWrapper.lambda().eq(PersonAddPO::getIdNumber, personAddPo.getIdNumber())
                    .eq(PersonAddPO::getValid, true).ne(PersonAddPO::getId, personAddPo.getId());
            int idNumberCount = count(idNumberWrapper);
            if (idNumberCount > 0) {
                throw new OrganizationException(OrganizationErrorEnum.ID_NUMBER_EXISTS);
            }
        }
/*        if (StringUtils.isNotBlank(personAddPo.getEntryDate())) {
            Date entryDate = null;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            try {
                entryDate = format.parse(personAddPo.getEntryDate());
                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                sdf1.setTimeZone(TimeZone.getTimeZone("UTC"));
                personAddPo.setEntryDate(sdf1.format(entryDate));
            } catch (ParseException e) {
                throw new OrganizationException(OrganizationErrorEnum.DATE_FORMAT_ERROR);
            }
        }*/
        personAddPo.setCode(currentPerson.getCode());
        handleImageFilePath(personAddPo, tenantId);
        updateById(personAddPo);
        if (StringUtils.isNotBlank(personAddPo.getName())) {
            orgMnecodeService.deleteOrgMnecodeByOrgId(personAddPo.getId(), Constants.PERSON);
            orgMnecodeService.addOrgMnecode(personAddPo.getId(), personAddPo.getName(), Constants.PERSON);
        }
        if (StringUtils.isNotBlank(personAddPo.getStatus()) && personAddPo.getStatus().equals(Constants.OFF_WORK_CODE)) {
            Long[] personIds = {personAddPo.getId()};
            Boolean flag = organizationAdapter.deleteUser(personIds);
            if (!flag) {
                throw new OrganizationException(OrganizationErrorEnum.USER_DELETE_ERROR);
            }
            //删除人员表绑定的用户
            this.deleteUserByPersonIds(Arrays.asList(personAddPo.getId()));
        } else {
            if (StringUtils.isNotBlank(personAddPo.getName())) {
                List<UserPersonNames> userPersonNameList = new ArrayList<>();
                UserPersonNames userPersonNames = new UserPersonNames(personAddPo.getId(), personAddPo.getName());
                userPersonNameList.add(userPersonNames);
                organizationAdapter.updateUserPersonName(userPersonNameList);
            }
        }
    }
    @Override
    public PersonLoginInfoBO getHeadImgById(Long staffId) {
        //PersonAddPO personAddPO = personMapper.selectById(staffId);
        PersonLoginPO personAddPO = personMapper.queryLoginPersonById(staffId);
        if (personAddPO == null){
            return null;
            //throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NOT_EXISTS);
        }
        PersonLoginInfoBO personLoginInfoBO = new PersonLoginInfoBO();
        BeanUtils.copyProperties(personAddPO, personLoginInfoBO);
        return personLoginInfoBO;
    }

    @Override
    public Result fileUpload(MultipartFile uploadFile, String tenantId) throws IOException {
        File file = null;
        InputStream is = null;
        FileOutputStream fileOutputStream = null;
        String originalFilename = uploadFile.getOriginalFilename();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        try {
            String picFilePath = env.getProperty("file.path");
            if (StringUtils.isBlank(tenantId)) {
                tenantId = Constants.FILE_NO_TENANTID;
            }
            String filePath = picFilePath + "/" + tenantId + "/" + timeStamp + "/" + originalFilename;
            file = FileToolUtils.createFile(filePath);
            is = uploadFile.getInputStream();
            fileOutputStream = new FileOutputStream(file);
            IOUtils.copy(is, fileOutputStream);
            return new Result(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            //throw new OrganizationException(OrganizationErrorEnum.FILE_CREATE_ERROR);
        }finally {
            fileOutputStream.close();
            is.close();
        }
        return null;
    }

    @Override
    public Map<String, String> downloadFile(String[] filePaths) {
        Map<String, String> res = new HashMap<>();
        for (int i = 0; i < filePaths.length; i++){
            String filePath = filePaths[i];
            File file = new File(filePath);
            if (file.isFile() && file.exists()){
                log.info("获取文件路径filePath：" + filePath);
                InputStream in = null;
                BufferedInputStream inBuffer = null;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try{
                    in = new FileInputStream(file);
                    inBuffer = new BufferedInputStream(in);

                    IOUtils.copy(inBuffer, bos);
                    byte[] fileByte = bos.toByteArray();
                    // base64
                    BASE64Encoder encoder = new BASE64Encoder();
                    String imageData = encoder.encode(fileByte);
                    res.put(filePath, imageData);
                }catch (Exception e){
                    log.error("download:", e);
                }finally {
                    try {
                        bos.close();
                        inBuffer.close();
                        in.close();
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        }
        return res;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void bulkOperate(PersonBulkOperateOpenBO personBulkOperateOpenBO, String tenantId) {
        bulkCheckPersonParam(personBulkOperateOpenBO);
        bulkAddPersons(personBulkOperateOpenBO.getAddPersons(), tenantId);
        bulkUpdatePersons(personBulkOperateOpenBO.getUpdatePersons(), tenantId);
        bulkDeletePersons(personBulkOperateOpenBO.getDeletePersons(), tenantId);
    }

    private void bulkCheckPersonParam(PersonBulkOperateOpenBO personBulkOperateOpenBO) {
        //List<String> idNumberList = new ArrayList<>();
        Map<String, String> idNumberToCode = new HashMap<>();
        if (personBulkOperateOpenBO.getAddPersons() != null && personBulkOperateOpenBO.getAddPersons().size() > 0) {
            personBulkOperateOpenBO.getAddPersons().stream().forEach(addPerson -> {
                if (StringUtils.isBlank(addPerson.getCode()) || addPerson.getCode().length() > 50 || !Pattern.matches(Constants.CODE_REGEX, addPerson.getCode())) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_CODE_NECESSARY);
                }
                if (StringUtils.isBlank(addPerson.getName()) || addPerson.getName().length() > 200) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_NAME_NECESSARY);
                }
                if (StringUtils.isBlank(addPerson.getGender())) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_GENDER_NECESSARY);
                }
                if (StringUtils.isBlank(addPerson.getStatus())) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_STATUS_NECESSARY);
                }
                if (StringUtils.isBlank(addPerson.getMainPositionCode())) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_MAIN_POSITION_NECESSARY);
                }
                if (StringUtils.isNotBlank(addPerson.getDescription()) && addPerson.getDescription().length() > 500) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_DESCRIPTION_ERROR);
                }
                if (StringUtils.isNotBlank(addPerson.getIdNumber()) && (addPerson.getIdNumber().length() > 200 || !Pattern.matches(Constants.ID_NUMBER_REGEX, addPerson.getIdNumber()))) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NUMBER_ERROR);
                }
                if (StringUtils.isNotBlank(addPerson.getIdNumber()) && idNumberToCode.containsKey(addPerson.getIdNumber()) && idNumberToCode.get(addPerson.getIdNumber()).equals(addPerson.getCode())) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NUMBER_MULTI);
                } else if (StringUtils.isNotBlank(addPerson.getIdNumber())) {
                    idNumberToCode.put(addPerson.getIdNumber(), addPerson.getCode());
                }
            });
        }

        if (personBulkOperateOpenBO.getUpdatePersons() != null && personBulkOperateOpenBO.getUpdatePersons().size() > 0) {
            personBulkOperateOpenBO.getUpdatePersons().stream().forEach(updatePerson -> {
                if (StringUtils.isBlank(updatePerson.getCode()) || updatePerson.getCode().length() > 50 || !Pattern.matches(Constants.CODE_REGEX, updatePerson.getCode())) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_CODE_NECESSARY);
                }
                if (StringUtils.isBlank(updatePerson.getName()) || updatePerson.getName().length() > 200) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_NAME_NECESSARY);
                }
                if (StringUtils.isBlank(updatePerson.getGender())) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_GENDER_NECESSARY);
                }
                if (StringUtils.isBlank(updatePerson.getStatus())) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_STATUS_NECESSARY);
                }
                if (StringUtils.isBlank(updatePerson.getMainPositionCode())) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_MAIN_POSITION_NECESSARY);
                }
                if (StringUtils.isNotBlank(updatePerson.getDescription()) && updatePerson.getDescription().length() > 500) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_DESCRIPTION_ERROR);
                }
                if (StringUtils.isNotBlank(updatePerson.getIdNumber()) && (updatePerson.getIdNumber().length() > 200 || !Pattern.matches(Constants.ID_NUMBER_REGEX, updatePerson.getIdNumber()))) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NUMBER_ERROR);
                }
                if (StringUtils.isNotBlank(updatePerson.getIdNumber()) && idNumberToCode.containsKey(updatePerson.getIdNumber()) && idNumberToCode.get(updatePerson.getIdNumber()).equals(updatePerson.getCode())) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NUMBER_MULTI);
                } else if (StringUtils.isNotBlank(updatePerson.getIdNumber())) {
                    idNumberToCode.put(updatePerson.getIdNumber(), updatePerson.getCode());
                }
            });
        }
    }

    private void bulkAddPersons(List<PersonAddOpenBO> addPersons, String tenantId) {
        if (addPersons == null || addPersons.size() == 0) {
            return;
        }
        List<PersonAddPO> personAddPOList = new LinkedList<>();

        Set<String> mainPositionCodeSet = new HashSet<>();
        Set<String> personCodeSet = new HashSet<>();
        Set<String> directLeaderCodeSet = new HashSet<>();
        Set<String> grandLeaderCodeSet = new HashSet<>();

        Set<String> idNumberSet = new HashSet<>();

        for (PersonAddOpenBO personAddOpenBO : addPersons) {
            mainPositionCodeSet.add(personAddOpenBO.getMainPositionCode());
            if (personCodeSet.contains(personAddOpenBO.getCode())) {
                throw new OrganizationException(OrganizationErrorEnum.REQUEST_ADD_EXISTS_MULTI_PERSON_CODE);
            }
            personCodeSet.add(personAddOpenBO.getCode());
            if (StringUtils.isNotBlank(personAddOpenBO.getDirectLeaderCode())) {
                directLeaderCodeSet.add(personAddOpenBO.getDirectLeaderCode());
            }
            if (StringUtils.isNotBlank(personAddOpenBO.getGrandLeaderCode())) {
                grandLeaderCodeSet.add(personAddOpenBO.getGrandLeaderCode());
            }
            if (StringUtils.isNotBlank(personAddOpenBO.getIdNumber())) {
                idNumberSet.add(personAddOpenBO.getIdNumber());
            }
        }
        /**
         * 校验人员是否存在了
         */
        Set<String> tempPersonCodeSet = new HashSet<>(50);
        Iterator<String> personCodeIt = personCodeSet.iterator();
        while (personCodeIt.hasNext()) {
            tempPersonCodeSet.add(personCodeIt.next());
            if (tempPersonCodeSet.size() >= 50 || !personCodeIt.hasNext()) {
                QueryWrapper<PersonAddPO> personCountWrapper = new QueryWrapper<>();
                personCountWrapper.lambda().in(PersonAddPO::getCode, tempPersonCodeSet).eq(PersonAddPO::getValid, true);
                Integer personCount = count(personCountWrapper);
                if (personCount > 0) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_CODE_EXISTS);
                }
                tempPersonCodeSet.clear();
            }
        }

        /**
         * 身份证号
         */
        Set<String> tempIdNumberSet = new HashSet<>(50);
        Iterator<String> idNumberIt = idNumberSet.iterator();
        while (idNumberIt.hasNext()) {
            tempIdNumberSet.add(idNumberIt.next());
            if (tempIdNumberSet.size() >= 50 || !idNumberIt.hasNext()) {
                QueryWrapper<PersonAddPO> idNumberWrapper = new QueryWrapper<>();
                idNumberWrapper.lambda().in(PersonAddPO::getIdNumber, tempIdNumberSet).eq(PersonAddPO::getValid, true);
                Integer idNumberCount = count(idNumberWrapper);
                if (idNumberCount > 0) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NUMBER_MULTI);
                }
                tempIdNumberSet.clear();
            }
        }

        /**
         * 校验直属领导是否存在
         */
        Map<String, Long> directCodeToIdMap = new HashMap<>();
        if (directLeaderCodeSet.size() > 0) {
            QueryWrapper<PersonAddPO> directWrapper = new QueryWrapper<>();
            directWrapper.lambda().in(PersonAddPO::getCode, directLeaderCodeSet).eq(PersonAddPO::getValid, true);
            Integer directCount = count(directWrapper);
            if (directCount != directLeaderCodeSet.size()) {
                throw new OrganizationException(OrganizationErrorEnum.DIRECT_LEADER_CODE_NOT_EXISTS);
            }
            List<PersonAddPO> directLeaderList = list(directWrapper);
            for (PersonAddPO directLeader : directLeaderList) {
                directCodeToIdMap.put(directLeader.getCode(), directLeader.getId());
            }
        }

        /**
         * 校验隔级领导是否存在
         */
        Map<String, Long> grandCodeToIdMap = new HashMap<>();
        if (grandLeaderCodeSet.size() > 0) {
            QueryWrapper<PersonAddPO> grandWrapper = new QueryWrapper<>();
            grandWrapper.lambda().in(PersonAddPO::getCode, grandLeaderCodeSet).eq(PersonAddPO::getValid, true);
            Integer grandCount = count(grandWrapper);
            if (grandCount != grandLeaderCodeSet.size()) {
                throw new OrganizationException(OrganizationErrorEnum.GRAND_LEADER_CODE_NOT_EXISTS);
            }
            List<PersonAddPO> grandLeaderList = list(grandWrapper);
            for (PersonAddPO grandLeader : grandLeaderList) {
                grandCodeToIdMap.put(grandLeader.getCode(), grandLeader.getId());
            }
        }

        /**
         * 校验岗位是否存在
         */
        Map<String, PositionAddPO> mainPosCodeToPosPOMap = new HashMap<>();
        if (mainPositionCodeSet.size() > 0) {
            QueryWrapper<PositionAddPO> mainPosWrapper = new QueryWrapper<>();
            mainPosWrapper.lambda().in(PositionAddPO::getCode, mainPositionCodeSet).eq(PositionAddPO::getValid, true);
            Integer posCount = positionMapper.selectCount(mainPosWrapper);
            if (posCount != mainPositionCodeSet.size()) {
                throw new OrganizationException(OrganizationErrorEnum.PERSON_POSITION_ID_NOT_EXISTS);
            }
            List<PositionAddPO> positionAddPOList = positionMapper.selectList(mainPosWrapper);
            for (PositionAddPO positionAddPO : positionAddPOList) {
                mainPosCodeToPosPOMap.put(positionAddPO.getCode(), positionAddPO);
            }
        }

        List<PersonAddPO> insertPersons = new ArrayList<>(addPersons.size());
        List<PositionPersonPO> relPoses = new ArrayList<>(addPersons.size());
        List<DepartmentPersonPO> relDepts = new ArrayList<>(addPersons.size());
        List<CompanyPersonPO> relComs = new ArrayList<>(addPersons.size());

        List<SystemCodeResultDTO> genderSysCodeList = organizationAdapter.querySystemCodesByEntityCode(Constants.GENDER_ENTITYCODE);
        List<SystemCodeResultDTO> statusSysCodeList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_STATUS_ENTITYCODE);
        List<SystemCodeResultDTO> titleSysCodeList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_TITLE_ENTITYCODE);
        List<SystemCodeResultDTO> educationSysCodeList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_EDUCATION_ENTITYCODE);
        List<String> genderCodes = new ArrayList<>();
        List<String> statusCodes = new ArrayList<>();
        List<String> titleCodes = new ArrayList<>();
        List<String> educationCodes = new ArrayList<>();
        if (genderSysCodeList != null) {
            genderSysCodeList.stream().forEach(sysCode -> {
                genderCodes.add(sysCode.getCode());
            });
        }
        if (statusSysCodeList != null) {
            statusSysCodeList.stream().forEach(sysCode -> {
                statusCodes.add(sysCode.getCode());
            });
        }
        if (titleSysCodeList != null) {
            titleSysCodeList.stream().forEach(sysCode -> {
                titleCodes.add(sysCode.getCode());
            });
        }
        if (educationSysCodeList != null) {
            educationSysCodeList.stream().forEach(sysCode -> {
                educationCodes.add(sysCode.getCode());
            });
        }
        for (PersonAddOpenBO personAddOpenBO : addPersons) {
            PersonAddPO personAddPO = new PersonAddPO();
            BeanUtils.copyProperties(personAddOpenBO, personAddPO);
            if (StringUtils.isNotBlank(personAddPO.getGender())) {
                if (!genderCodes.contains(personAddOpenBO.getGender())) {
                    throw new OrganizationException(OrganizationErrorEnum.GENDER_SYS_CODE_ERROR);
                }
                personAddPO.setGender(Constants.GENDER_ENTITYCODE + "/" + personAddPO.getGender());
            }
            if (StringUtils.isNotBlank(personAddPO.getStatus())) {
                if (!statusCodes.contains(personAddOpenBO.getStatus())) {
                    throw new OrganizationException(OrganizationErrorEnum.STATUS_SYS_CODE_ERROR);
                }
                personAddPO.setStatus(Constants.PERSON_STATUS_ENTITYCODE + "/" + personAddPO.getStatus());
            }
            if (StringUtils.isNotBlank(personAddPO.getTitle())) {
                if (!titleCodes.contains(personAddOpenBO.getTitle())) {
                    throw new OrganizationException(OrganizationErrorEnum.TITLE_SYS_CODE_ERROR);
                }
                personAddPO.setTitle(Constants.PERSON_TITLE_ENTITYCODE + "/" + personAddPO.getTitle());
            }
            if (StringUtils.isNotBlank(personAddPO.getEducation())) {
                if (!educationCodes.contains(personAddOpenBO.getEducation())) {
                    throw new OrganizationException(OrganizationErrorEnum.EDUCATION_SYS_CODE_ERROR);
                }
                personAddPO.setEducation(Constants.PERSON_EDUCATION_ENTITYCODE + "/" + personAddPO.getEducation());
            }
            personAddPO.setId(IDGenerator.newInstance().generate().longValue());
            personAddPO.setOldId("Person_" + personAddPO.getId());
            if (StringUtils.isNotBlank(personAddOpenBO.getDirectLeaderCode())) {
                personAddPO.setDirectLeaderId(directCodeToIdMap.get(personAddOpenBO.getDirectLeaderCode()));
            }
            if (StringUtils.isNotBlank(personAddOpenBO.getGrandLeaderCode())) {
                personAddPO.setGrandLeaderId(grandCodeToIdMap.get(personAddOpenBO.getGrandLeaderCode()));
            }
            PositionAddPO mainPos = mainPosCodeToPosPOMap.get(personAddOpenBO.getMainPositionCode());
            personAddPO.setMainPosition(mainPos.getId());

            PositionPersonPO relPos = new PositionPersonPO();
            relPos.setPersonId(personAddPO.getId());
            relPos.setPositionId(personAddPO.getMainPosition());
            relPos.setValid(true);
            DepartmentPersonPO relDept = new DepartmentPersonPO();
            relDept.setPositionId(mainPos.getId());
            relDept.setValid(true);
            relDept.setDeptId(mainPos.getDepId());
            relDept.setPersonId(personAddPO.getId());
            CompanyPersonPO relCom = new CompanyPersonPO();
            relCom.setPositionId(mainPos.getId());
            relCom.setPersonId(personAddPO.getId());
            relCom.setCompanyId(mainPos.getCompanyId());
            relPoses.add(relPos);
            relDepts.add(relDept);
            relComs.add(relCom);
            insertPersons.add(personAddPO);
        }
        List<PersonAddPO> tmpInsertPersons = new ArrayList<>(80);
        List<PositionPersonPO> tmpInsertPoss = new ArrayList<>(80);
        List<DepartmentPersonPO> tmpInsertDepts = new ArrayList<>(80);
        List<CompanyPersonPO> tmpInsertComs = new ArrayList<>(80);
        for (int i = 0; i < addPersons.size(); i++) {
            tmpInsertPersons.add(insertPersons.get(i));
            tmpInsertPoss.add(relPoses.get(i));
            tmpInsertDepts.add(relDepts.get(i));
            tmpInsertComs.add(relComs.get(i));
            if (tmpInsertPersons.size() >= 50 || i == (addPersons.size() - 1)) {
                saveBatch(tmpInsertPersons);
                positionPersonService.saveBatchRel(tmpInsertPoss);
                departmentPersonService.saveBatchRel(tmpInsertDepts);
                companyPersonService.saveBatchRel(tmpInsertComs);
                tmpInsertPersons.clear();
                tmpInsertPoss.clear();
                tmpInsertDepts.clear();
                tmpInsertComs.clear();
            }
        }
        publishCreateOrUpdateMessage(insertPersons, tenantId, "CREATE");
    }

    private void bulkUpdatePersons(List<PersonUpdateOpenBO> updatePersons, String tenantId) {
        if (updatePersons == null || updatePersons.size() == 0) {
            return;
        }
        Set<String> mainPositionCodeSet = new HashSet<>();
        Set<String> personCodeSet = new HashSet<>();
        Set<String> directLeaderCodeSet = new HashSet<>();
        Set<String> grandLeaderCodeSet = new HashSet<>();


        for (PersonUpdateOpenBO personUpdateOpenBO : updatePersons) {
            mainPositionCodeSet.add(personUpdateOpenBO.getMainPositionCode());
            personCodeSet.add(personUpdateOpenBO.getCode());
            if (StringUtils.isNotBlank(personUpdateOpenBO.getDirectLeaderCode())) {
                directLeaderCodeSet.add(personUpdateOpenBO.getDirectLeaderCode());
            }
            if (StringUtils.isNotBlank(personUpdateOpenBO.getGrandLeaderCode())) {
                grandLeaderCodeSet.add(personUpdateOpenBO.getGrandLeaderCode());
            }
            if (StringUtils.isNotBlank(personUpdateOpenBO.getIdNumber())) {
                QueryWrapper<PersonAddPO> idNumberWrapper = new QueryWrapper<>();
                idNumberWrapper.lambda().eq(PersonAddPO::getIdNumber, personUpdateOpenBO.getIdNumber()).eq(PersonAddPO::getValid, true).ne(PersonAddPO::getCode, personUpdateOpenBO.getCode());
                Integer idNumberCount = count(idNumberWrapper);
                if (idNumberCount > 0) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NUMBER_MULTI);
                }
            }
        }

        Map<String, PersonAddPO> personCodeToPersonPOMap = new HashMap<>(personCodeSet.size());
        List<PersonAddPO> updatePersonPOS = new ArrayList<>();
        Iterator<String> personCodeIt = personCodeSet.iterator();
        Set<String> tmpPersonCdeSet = new HashSet<>(50);
        while (personCodeIt.hasNext()) {
            tmpPersonCdeSet.add(personCodeIt.next());
            if (tmpPersonCdeSet.size() == 50 || !personCodeIt.hasNext()) {
                QueryWrapper<PersonAddPO> countWrapper = new QueryWrapper<>();
                countWrapper.lambda().in(PersonAddPO::getCode, tmpPersonCdeSet).eq(PersonAddPO::getValid, true);
                Integer count = count(countWrapper);
                List<PersonAddPO> list = list(countWrapper);
                for (PersonAddPO po : list) {
                    personCodeToPersonPOMap.put(po.getCode(), po);
                    updatePersonPOS.add(po);
                }
                if (tmpPersonCdeSet.size() != count) {
                    throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NOT_EXISTS);
                }
                tmpPersonCdeSet.clear();
            }
        }


        /**
         * 校验直属领导是否存在
         */
        Map<String, Long> directCodeToIdMap = new HashMap<>();
        if (directLeaderCodeSet.size() > 0) {
            QueryWrapper<PersonAddPO> directWrapper = new QueryWrapper<>();
            directWrapper.lambda().in(PersonAddPO::getCode, directLeaderCodeSet).eq(PersonAddPO::getValid, true);
            Integer directCount = count(directWrapper);
            if (directCount != directLeaderCodeSet.size()) {
                throw new OrganizationException(OrganizationErrorEnum.DIRECT_LEADER_CODE_NOT_EXISTS);
            }
            List<PersonAddPO> directLeaderList = list(directWrapper);
            for (PersonAddPO directLeader : directLeaderList) {
                directCodeToIdMap.put(directLeader.getCode(), directLeader.getId());
            }
        }

        /**
         * 校验隔级领导是否存在
         */
        Map<String, Long> grandCodeToIdMap = new HashMap<>();
        if (grandLeaderCodeSet.size() > 0) {
            QueryWrapper<PersonAddPO> grandWrapper = new QueryWrapper<>();
            grandWrapper.lambda().in(PersonAddPO::getCode, grandLeaderCodeSet).eq(PersonAddPO::getValid, true);
            Integer grandCount = count(grandWrapper);
            if (grandCount != grandLeaderCodeSet.size()) {
                throw new OrganizationException(OrganizationErrorEnum.GRAND_LEADER_CODE_NOT_EXISTS);
            }
            List<PersonAddPO> grandLeaderList = list(grandWrapper);
            for (PersonAddPO grandLeader : grandLeaderList) {
                grandCodeToIdMap.put(grandLeader.getCode(), grandLeader.getId());
            }
        }

        /**
         * 校验岗位是否存在
         */
        Map<String, PositionAddPO> mainPosCodeToPosPOMap = new HashMap<>();
        if (mainPositionCodeSet.size() > 0) {
            QueryWrapper<PositionAddPO> mainPosWrapper = new QueryWrapper<>();
            mainPosWrapper.lambda().in(PositionAddPO::getCode, mainPositionCodeSet).eq(PositionAddPO::getValid, true);
            Integer posCount = positionMapper.selectCount(mainPosWrapper);
            if (posCount != mainPositionCodeSet.size()) {
                throw new OrganizationException(OrganizationErrorEnum.PERSON_POSITION_ID_NOT_EXISTS);
            }
            List<PositionAddPO> positionAddPOList = positionMapper.selectList(mainPosWrapper);
            for (PositionAddPO positionAddPO : positionAddPOList) {
                mainPosCodeToPosPOMap.put(positionAddPO.getCode(), positionAddPO);
            }
        }

        List<SystemCodeResultDTO> genderSysCodeList = organizationAdapter.querySystemCodesByEntityCode(Constants.GENDER_ENTITYCODE);
        List<SystemCodeResultDTO> statusSysCodeList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_STATUS_ENTITYCODE);
        List<SystemCodeResultDTO> titleSysCodeList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_TITLE_ENTITYCODE);
        List<SystemCodeResultDTO> educationSysCodeList = organizationAdapter.querySystemCodesByEntityCode(Constants.PERSON_EDUCATION_ENTITYCODE);
        List<String> genderCodes = new ArrayList<>();
        List<String> statusCodes = new ArrayList<>();
        List<String> titleCodes = new ArrayList<>();
        List<String> educationCodes = new ArrayList<>();
        if (genderSysCodeList != null) {
            genderSysCodeList.stream().forEach(sysCode -> {
                genderCodes.add(sysCode.getCode());
            });
        }
        if (statusSysCodeList != null) {
            statusSysCodeList.stream().forEach(sysCode -> {
                statusCodes.add(sysCode.getCode());
            });
        }
        if (titleSysCodeList != null) {
            titleSysCodeList.stream().forEach(sysCode -> {
                titleCodes.add(sysCode.getCode());
            });
        }
        if (educationSysCodeList != null) {
            educationSysCodeList.stream().forEach(sysCode -> {
                educationCodes.add(sysCode.getCode());
            });
        }
        List<UserPersonNames> userPersonNamesList = new ArrayList<>();

        List<PositionPersonPO> updatePosRel = new ArrayList<>();
        List<PositionPersonPO> deleteRelList = new ArrayList<>();
        List<DepartmentPersonPO> updatedeptRel = new ArrayList<>();
        List<CompanyPersonPO> updatecomRel = new ArrayList<>();
        for (int i = 0; i < updatePersons.size(); i++) {
            PersonAddPO personAddPO = personCodeToPersonPOMap.get(updatePersons.get(i).getCode());
            PersonUpdateOpenBO personUpdateOpenBO = updatePersons.get(i);
            if (StringUtils.isBlank(personUpdateOpenBO.getDirectLeaderCode())) {
                personAddPO.setDirectLeaderId(null);
            } else {
                Long direcLeadertId = directCodeToIdMap.get(personUpdateOpenBO.getDirectLeaderCode());
                personAddPO.setDirectLeaderId(direcLeadertId);
            }
            if (StringUtils.isBlank(personUpdateOpenBO.getGrandLeaderCode())) {
                personAddPO.setGrandLeaderId(null);
            } else {
                Long grandLeaderId = grandCodeToIdMap.get(personUpdateOpenBO.getGrandLeaderCode());
                personAddPO.setGrandLeaderId(grandLeaderId);
            }
            PositionAddPO newPositionPo = mainPosCodeToPosPOMap.get(personUpdateOpenBO.getMainPositionCode());
            if (!newPositionPo.getId().equals(personAddPO.getMainPosition())) {
                personAddPO.setMainPosition(newPositionPo.getId());
                PositionPersonPO positionPersonPO = new PositionPersonPO();
                positionPersonPO.setValid(true);
                positionPersonPO.setPositionId(newPositionPo.getId());
                positionPersonPO.setPersonId(personAddPO.getId());
                updatePosRel.add(positionPersonPO);
                PositionPersonPO deleteRel = new PositionPersonPO();
                deleteRel.setValid(false);
                deleteRel.setPersonId(personAddPO.getId());
                deleteRel.setPositionId(personAddPO.getMainPosition());
                deleteRelList.add(deleteRel);

                DepartmentPersonPO departmentPersonPO = new DepartmentPersonPO();
                departmentPersonPO.setValid(true);
                departmentPersonPO.setPersonId(personAddPO.getId());
                departmentPersonPO.setDeptId(newPositionPo.getDepId());
                departmentPersonPO.setPositionId(newPositionPo.getId());
                updatedeptRel.add(departmentPersonPO);

                CompanyPersonPO companyPersonPO = new CompanyPersonPO();
                companyPersonPO.setValid(true);
                companyPersonPO.setCompanyId(newPositionPo.getCompanyId());
                companyPersonPO.setPersonId(personAddPO.getId());
                companyPersonPO.setPositionId(newPositionPo.getId());

                updatecomRel.add(companyPersonPO);
            }

            if (StringUtils.isNotBlank(personUpdateOpenBO.getGender())) {
                if (!genderCodes.contains(personUpdateOpenBO.getGender())) {
                    throw new OrganizationException(OrganizationErrorEnum.GENDER_SYS_CODE_ERROR);
                }
                personAddPO.setGender(Constants.GENDER_ENTITYCODE + "/" + personUpdateOpenBO.getGender());
            }
            if (StringUtils.isNotBlank(personUpdateOpenBO.getStatus())) {
                if (!statusCodes.contains(personUpdateOpenBO.getStatus())) {
                    throw new OrganizationException(OrganizationErrorEnum.STATUS_SYS_CODE_ERROR);
                }
                personAddPO.setStatus(Constants.PERSON_STATUS_ENTITYCODE + "/" + personUpdateOpenBO.getStatus());
            }
            if (StringUtils.isNotBlank(personUpdateOpenBO.getTitle())) {
                if (!titleCodes.contains(personUpdateOpenBO.getTitle())) {
                    throw new OrganizationException(OrganizationErrorEnum.TITLE_SYS_CODE_ERROR);
                }
                personAddPO.setTitle(Constants.PERSON_TITLE_ENTITYCODE + "/" + personUpdateOpenBO.getTitle());
            }
            if (StringUtils.isNotBlank(personUpdateOpenBO.getEducation())) {
                if (!educationCodes.contains(personUpdateOpenBO.getEducation())) {
                    throw new OrganizationException(OrganizationErrorEnum.EDUCATION_SYS_CODE_ERROR);
                }
                personAddPO.setEducation(Constants.PERSON_EDUCATION_ENTITYCODE + "/" + personUpdateOpenBO.getEducation());
            }
            if (StringUtils.isNotBlank(personUpdateOpenBO.getName()) && !personUpdateOpenBO.getName().equals(personAddPO.getName())) {
                UserPersonNames userPersonNames = new UserPersonNames(personAddPO.getId(), personUpdateOpenBO.getName());
                userPersonNamesList.add(userPersonNames);
            }
            personAddPO.setName(personUpdateOpenBO.getName());
            personAddPO.setPhone(personUpdateOpenBO.getPhone());
            //personAddPO.setGender(personUpdateOpenBO.getGender());
            //personAddPO.setStatus(personUpdateOpenBO.getStatus());
            personAddPO.setEmail(personUpdateOpenBO.getEmail());
            personAddPO.setDescription(personUpdateOpenBO.getDescription());
            personAddPO.setAvatarUrl(personUpdateOpenBO.getAvatarUrl());
            personAddPO.setSignPicUrl(personUpdateOpenBO.getSignPicUrl());
            personAddPO.setEntryDate(personUpdateOpenBO.getEntryDate());
            //personAddPO.setTitle(personUpdateOpenBO.getTitle());
            personAddPO.setQualification(personUpdateOpenBO.getQualification());
            //personAddPO.setEducation(personUpdateOpenBO.getEducation());
            personAddPO.setMajor(personUpdateOpenBO.getMajor());
            personAddPO.setIdNumber(personUpdateOpenBO.getIdNumber());
        }

        //删除关系
        for (PositionPersonPO positionPersonPO : deleteRelList) {
            QueryWrapper<PositionPersonPO> updateWrapper = new QueryWrapper<>();
            updateWrapper.lambda().eq(PositionPersonPO::getPersonId, positionPersonPO.getPersonId()).
                    eq(PositionPersonPO::getPositionId, positionPersonPO.getPositionId());
            PositionPersonPO pos = positionPersonMapper.selectOne(updateWrapper);
            pos.setValid(false);
            positionPersonMapper.updateById(pos);

            QueryWrapper<DepartmentPersonPO> updateDeptWrapper = new QueryWrapper<>();
            updateDeptWrapper.lambda().eq(DepartmentPersonPO::getPersonId, positionPersonPO.getPersonId()).eq(DepartmentPersonPO::getPositionId, positionPersonPO.getPersonId());
            DepartmentPersonPO dept = departmentPersonMapper.selectOne(updateDeptWrapper);
            dept.setValid(false);
            departmentPersonMapper.updateById(dept);

            QueryWrapper<CompanyPersonPO> updateComWrapper = new QueryWrapper<>();
            updateComWrapper.lambda().eq(CompanyPersonPO::getPersonId, positionPersonPO.getPersonId()).eq(CompanyPersonPO::getPositionId, positionPersonPO.getPersonId());
            CompanyPersonPO com = companyPersonMapper.selectOne(updateComWrapper);
            com.setValid(false);
            companyPersonMapper.updateById(com);
        }
        positionPersonService.saveBatchRel(updatePosRel);
        departmentPersonService.saveBatchRel(updatedeptRel);
        companyPersonService.saveBatchRel(updatecomRel);
        updateBatchById(updatePersonPOS);
        organizationAdapter.updateUserPersonName(userPersonNamesList);
        publishCreateOrUpdateMessage(updatePersonPOS, tenantId, "UPDATE");
    }

    private void bulkDeletePersons(List<String> personCodes, String tenantId) {
        if (personCodes == null || personCodes.size() == 0) {
            return;
        }

        batchDeletePersonByCode(personCodes, tenantId);
    }

    @Override
    public void updateAvatarUrl(String personCode, String filePath, String tenantId) {
        QueryWrapper<PersonAddPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PersonAddPO::getCode, personCode).eq(PersonAddPO::getValid, true);
        PersonAddPO personAddPO = getOne(queryWrapper);
        if (personAddPO == null) {
            File file = new File(filePath);
            if (file.exists()) {
                File parentFile = file.getParentFile();
                parentFile.delete();
                file.delete();
            }
            throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NOT_EXISTS);
        }
        personAddPO.setAvatarUrl(filePath);
        handleImageFilePath(personAddPO, tenantId);
        updateById(personAddPO);
    }

    @Override
    public PersonAddPO queryPersonPOById(Long id) {
        PersonAddPO personAddPO = getById(id);
        return personAddPO;
    }
}
