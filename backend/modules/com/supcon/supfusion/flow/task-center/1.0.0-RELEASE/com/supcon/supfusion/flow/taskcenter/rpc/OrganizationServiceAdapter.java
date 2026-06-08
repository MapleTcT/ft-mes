/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.rpc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.supcon.supfusion.flow.common.dto.CandidateGroupDTO;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.exception.EmptyExecutorException;
import com.supcon.supfusion.flow.taskcenter.component.RedisUtils;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiReference;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.CompanyResultDTO;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;
import com.supcon.supfusion.organization.api.dto.PersonLeaderDTO;
import com.supcon.supfusion.organization.api.dto.PositionDetailDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zhuangmh
 * @date: 2020年8月13日 上午10:24:19
 */
@Slf4j
@Service
public class OrganizationServiceAdapter {
    
    @ServiceApiReference
    private PersonApiService orgService;
    @Autowired
    private UserServiceAdapter userServiceAdapter;
    @Autowired
    private RedisUtils redisUtils;
    /**
     * 查询部门成员
     * @param departmentId
     * @return
     */
    public Collection<PersonDetailDTO> queryDepartMember(String departmentId) {
        ListResult<PersonDetailDTO> result = orgService.queryPersonsByDepartmentId(Long.parseLong(departmentId));
        List<PersonDetailDTO> persons = new ArrayList<>();
        if (result != null && result.getList() != null) {
            for (PersonDetailDTO person : result.getList()) {
                if (person.getUserId() != null) {
                    persons.add(person);
                }
            }
        }
        return persons;
    }
    
    /**
     * 查询岗位成员
     * @param positionId
     * @return
     */
    public Collection<PersonDetailDTO> queryPositionMember(String positionId) {
        ListResult<PersonDetailDTO> result = orgService.queryPersonsByPositionId(Long.parseLong(positionId));
        List<PersonDetailDTO> persons = new ArrayList<>();
        if (result != null && result.getList() != null) {
            for (PersonDetailDTO person : result.getList()) {
                if (person.getUserId() != null) {
                    persons.add(person);
                }
            }
        }
        return persons;
    }
    
    /**
     * 获取公司名称并缓存10分钟, 避免频繁调用影响性能
     * @param companyId
     * @return
     */
    public String getCompanyName(Long companyId) {
        try {
            String cachedCompany = redisUtils.getStringValue(companyId.toString());
            if (StringUtils.isNotEmpty(cachedCompany)) {
                return cachedCompany;
            }
            Result<CompanyResultDTO> result = orgService.findCompany(companyId);
            CompanyResultDTO company = result.getData();
            redisUtils.setStringValue(companyId.toString(), company.getShortName(), 10 * 60 * 1000);
            return company.getShortName();
        } catch (Exception e) {
            // ignore e
            log.error("获取公司名称异常,公司ID: {}", companyId);
        }
        return "";
    }
    
    /**
     * 验证所有的组是否都为空
     * @param candidateGroups
     * @return 
     */
    public void validateEmptyMemberTeam(Set<CandidateGroupDTO> candidateGroups) {
        for (CandidateGroupDTO candidateGroup : candidateGroups) {
            if (StringUtils.isNotEmpty(candidateGroup.getRole())) {
                Collection<PersonDetailDTO> members = userServiceAdapter.queryRoleMember(candidateGroup.getRole());
                if (members.isEmpty()) {
                    throw new EmptyExecutorException(FlowErrorEnum.GROUP_MEMBER_NOT_EMPTY_ERROR, "角色");
                }
            }
            if (StringUtils.isNotEmpty(candidateGroup.getPosition())) {
                Collection<PersonDetailDTO> members = queryPositionMember(candidateGroup.getPosition());
                if (members.isEmpty()) {
                    throw new EmptyExecutorException(FlowErrorEnum.GROUP_MEMBER_NOT_EMPTY_ERROR, "岗位");
                }
            }
            if (StringUtils.isNotEmpty(candidateGroup.getDepart())) {
                Collection<PersonDetailDTO> members = queryDepartMember(candidateGroup.getDepart());
                if (members.isEmpty()) {
                    throw new EmptyExecutorException(FlowErrorEnum.GROUP_MEMBER_NOT_EMPTY_ERROR, "部门");
                }
            }
        }
    }
    /**
     * 批量查询人员信息
     * @param personIds
     * @return
     */
    public List<PersonDetailDTO> queryPerson(List<String> personIds) {
        List<Long> ids = personIds.stream().map(Long::parseLong).collect(Collectors.toList());
        Map<Long, PersonDetailDTO> personMap = orgService.queryPersonsByIds(ids);
        List<PersonDetailDTO> persons = new LinkedList<>();
        for (Map.Entry<Long, PersonDetailDTO> map : personMap.entrySet()) {
            persons.add(map.getValue());
        }
        return persons;
    }
    
    /**
     * 批量ID查询人员信息
     * @param ids
     * @return
     */
    public PersonDetailDTO getPerson(Long id) {
        List<Long> ids = Collections.singletonList(id);
        try {
            Map<Long, PersonDetailDTO> personMap = orgService.queryPersonsByIds(ids);
            return personMap.get(id);
        } catch (Exception e) {
            log.error("获取人员异常, 人员ID: {}", id, e);
        }
        return null;
    }
    /**
     * 查询人员所在岗位
     * @param personId
     * @return
     */
    public Collection<PositionDetailDTO> getPosition(Long personId) {
        ListResult<PositionDetailDTO> result = orgService.queryPersonPositionsByPersonId(personId);
        return result != null ? result.getList() : new ArrayList<>(1);
    }
    
    /**
     * 验证是否上下级岗位关系
     * @param subordinateId 下级人员ID
     * @param superiorId 上级人员ID
     * @param companyId 公司ID
     * @return
     */
    public boolean validateSuperiorSubordinate(Long subordinateId, Long superiorId, Long companyId) {
        Result<Boolean> result = orgService.checkPersonSupAndSub(superiorId, subordinateId, companyId);
        if (result == null || result.getData() == null) {
            return false;
        }
        return result.getData().booleanValue();
    }
    
    /**
     * 验证是否上下级岗位关系
     * @param subordinateId 上级岗位ID
     * @param superiorId 下级岗位ID
     * @param companyId 公司ID
     * @return
     */
    public boolean validatePositionSuperiorSubordinate(Long superiorId, Long subordinateId) {
        Result<Boolean> result = orgService.checkPositionSupAndSub(superiorId, subordinateId);
        if (result == null || result.getData() == null) {
            return false;
        }
        return result.getData().booleanValue();
    }

    /**
     * 获取直属领导
     * @param personId
     * @return
     */
    public PersonDetailDTO getDirectLeader(Long personId) {
        PersonLeaderDTO personLeader = orgService.getPersonLeader(personId);
        PersonDTO directLeader = personLeader.getDirectLeader();
        if (directLeader != null) {
            PersonDetailDTO person = new PersonDetailDTO();
            person.setId(directLeader.getId());
            person.setUserId(directLeader.getUserId());
            return person;
        }
        return null;
    }
    
    /**
     * 获取隔级领导
     * @param personId
     * @return
     */
    public PersonDetailDTO getSuperiorLeader(Long personId) {
        PersonLeaderDTO personLeader = orgService.getPersonLeader(personId);
        PersonDTO superiorLeader = personLeader.getGrandLeader();
        if (superiorLeader != null) {
            PersonDetailDTO person = new PersonDetailDTO();
            person.setId(superiorLeader.getId());
            person.setUserId(superiorLeader.getUserId());
            return person;
        }
        return null;
    }
}
