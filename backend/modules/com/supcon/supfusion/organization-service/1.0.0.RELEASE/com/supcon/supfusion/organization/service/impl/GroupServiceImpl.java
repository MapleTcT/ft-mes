package com.supcon.supfusion.organization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.*;
import com.supcon.supfusion.organization.dao.mapper.group.GroupMapper;
import com.supcon.supfusion.organization.dao.mapper.group.GroupPersonMapper;
import com.supcon.supfusion.organization.dao.mapper.person.OrganizationManagerMapper;
import com.supcon.supfusion.organization.dao.mapper.person.PersonMapper;
import com.supcon.supfusion.organization.dao.po.group.GroupPO;
import com.supcon.supfusion.organization.dao.po.group.GroupPersonPO;
import com.supcon.supfusion.organization.dao.po.person.OrganizationManagerPO;
import com.supcon.supfusion.organization.dao.po.person.PersonAddPO;
import com.supcon.supfusion.organization.service.GroupService;
import com.supcon.supfusion.organization.service.PersonService;
import com.supcon.supfusion.organization.service.bo.group.GroupBO;
import com.supcon.supfusion.organization.service.bo.group.GroupPersonBO;
import com.supcon.supfusion.organization.service.bo.person.OrganizationManagerBO;
import com.supcon.supfusion.organization.service.GroupPersonService;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 组服务处理实现类
 *
 * @author lifangyuan
 */
@Slf4j
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupPO> implements GroupService {

    @Resource
    private OrganizationManagerMapper organizationManagerMapper;

    @Resource
    private PersonMapper personMapper;

    @Resource
    private GroupPersonService groupPersonService;

    @Resource
    private GroupPersonMapper groupPersonMapper;

    @Resource
    private PersonService personService;

    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;

    @Override
    public void addGroup(GroupBO groupBO) {
        GroupPO groupPO = new GroupPO();
        BeanUtils.copyProperties(groupBO, groupPO);
        QueryWrapper<GroupPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", groupBO.getCode());
        queryWrapper.eq("valid", 1);
        int count = count(queryWrapper);
        if (count > 0) {
            throw new GroupException(GroupErrorEnum.GROUP_THIS_CODE_EXISTS);
        }
        super.save(groupPO);
    }

    @Override
    public void updateGroup(GroupBO groupBO) {
        GroupPO groupPO = new GroupPO();
        BeanUtils.copyProperties(groupBO, groupPO);
        super.updateById(groupPO);
    }

    @Override
    public void deleteGroupById(Long groupId) {
        super.removeById(groupId);
    }

    @Override
    public GroupBO getGroupInfoById(Long groupId) {
        GroupBO groupBO = new GroupBO();
        GroupPO groupPO = super.getById(groupId);
        if (groupPO != null) {
            BeanUtils.copyProperties(groupPO, groupBO);
        }
        QueryWrapper<OrganizationManagerPO> managerWrapper = new QueryWrapper<OrganizationManagerPO>();
        managerWrapper.eq("org_id", groupId);
        managerWrapper.eq("manager_type", Constants.GROUP);
        List<OrganizationManagerPO> managers = organizationManagerMapper.selectList(managerWrapper);
        List<OrganizationManagerBO> list = new ArrayList<OrganizationManagerBO>();
        managers.stream().forEach(manager -> {
            OrganizationManagerBO organizationManagerBO = new OrganizationManagerBO();
            BeanUtils.copyProperties(manager, organizationManagerBO);
            list.add(organizationManagerBO);
        });
        groupBO.setManagers(list);
        return groupBO;
    }

    @Override
    public Page<GroupBO> queryPageList(GroupBO groupBO, Page<GroupPO> page) {

        LambdaQueryWrapper<GroupPO> lambda = new QueryWrapper<GroupPO>().lambda();
        lambda.eq(GroupPO::getCompanyId, groupBO.getCompanyId());
        if (!StringUtils.isEmpty(groupBO.getName())) {
            lambda.like(GroupPO::getName, groupBO.getName());

            String key = dbStringUtil.getString(groupBO.getName());
            //获取数据库类型
            String dbType = dataId.getDataId();
            //使用queryWrapper形式
            //oracle的处理方式与mysql、sqlserver、mariadb不同，需要特殊处理
            if ("oracle".equals(dbType)){
                lambda.apply("name like {0} escape '\\'", "%" + key + "%");
            }else{
                lambda.like(GroupPO::getName, key);
            }


        }
        Page<GroupPO> pages = super.page(page, lambda);
        List<GroupBO> collect = pages.getRecords().stream().map(t -> {
            GroupBO temp = new GroupBO();
            BeanUtils.copyProperties(t, temp);
            return temp;
        }).collect(Collectors.toList());
        Page<GroupBO> result = new Page<>(pages.getCurrent(), pages.getSize(), pages.getTotal());
        result.setRecords(collect);
        return result;
    }

    /**
     * 新增岗位人员关系
     *
     * @param groupPersonBO
     */
    @Override
    public void addGroupPerson(GroupPersonBO groupPersonBO) {
        List<Long> persons = groupPersonBO.getPersons();
        QueryWrapper<PersonAddPO> personWrapper = new QueryWrapper<PersonAddPO>();
        personWrapper.in("id", persons);
        List<GroupPersonPO> relations = new ArrayList<>();
        persons.forEach(personId -> {
            GroupPersonPO groupPersonPO = new GroupPersonPO();
            groupPersonPO.setPersonId(personId);
            groupPersonPO.setId(IDGenerator.newInstance().generate().longValue());
            groupPersonPO.setGroupId(groupPersonBO.getGroupId());
            relations.add(groupPersonPO);
        });
        groupPersonService.batchSaveOrUpdate(relations);
    }

    /**
     * 删除组人员关联关系
     *
     * @param groupId   组id
     * @param personIds 人员id
     */
    @Override
    public void deleteRelations(Long groupId, Long[] personIds) {
        Arrays.stream(personIds).forEach(personId -> {
            GroupPersonPO groupPersonPO = groupPersonMapper.selectOne(new QueryWrapper<GroupPersonPO>().lambda()
                    .eq(GroupPersonPO::getGroupId, groupId)
                    .eq(GroupPersonPO::getPersonId, personId)
                    .eq(GroupPersonPO::getValid, true));
            if (groupPersonPO != null) {
                groupPersonPO.setValid(false);
                groupPersonMapper.updateById(groupPersonPO);
            }
        });
    }

    @Override
    public PageResult<PersonDetailBO> queryGroupPersons(Long groupId, String keyword, Integer current, Integer pageSize) {
        PageResult<PersonDetailBO> pageResult=null;
        if(StringUtils.isEmpty(keyword)){
            List<Long> personIds = groupPersonService.queryPersonIdByGroupId(groupId);
            PersonDetailBO conditionQuery = new PersonDetailBO();
            if(personIds!=null&&!personIds.isEmpty()){
                pageResult = personService.queryPersonsById(personIds, current, pageSize, conditionQuery);
            }
        }else {
           pageResult = personService.queryPersonsByGroupId(groupId, keyword, current, pageSize);
        }
        if(pageResult!=null){
            List<PersonDetailBO> persons = (List<PersonDetailBO>) pageResult.getList();
            persons.forEach(this::transferSystemCode);
        }
        return pageResult;
    }

    private void transferSystemCode(PersonDetailBO person) {
        if (person.getGender() != null) {
            if (person.getGender().equals("male")) {
                person.setGender("男");
            } else {
                person.setGender("女");
            }
        }
        if (person.getStatus() != null) {
            if (person.getStatus().equals("onWork")) {
                person.setStatus("在职");
            } else {
                person.setStatus("离职");
            }
        }
        if (person.getClassifiedLevel() != null) {
            if (person.getClassifiedLevel().equals("unclassified")) {
                person.setClassifiedLevel("非密");
            } else if (person.getClassifiedLevel().equals("generalClassified")) {
                person.setClassifiedLevel("普通加密");
            } else if (person.getClassifiedLevel().equals("importantClassified")) {
                person.setClassifiedLevel("重要加密");
            } else {
                person.setClassifiedLevel("核心加密");
            }
        }
    }
}
