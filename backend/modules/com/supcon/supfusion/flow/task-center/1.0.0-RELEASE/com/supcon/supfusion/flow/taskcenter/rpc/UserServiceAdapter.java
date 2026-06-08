/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.rpc;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.supcon.supfusion.auth.api.UserApiService;
import com.supcon.supfusion.auth.api.dto.UserDetailDTO;
import com.supcon.supfusion.auth.api.dto.UserFlowInfoDTO;
import com.supcon.supfusion.auth.api.dto.UserOrgDetailDTO;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.taskcenter.component.RedisUtils;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiReference;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zhuangmh
 * @date: 2020年9月29日 上午9:43:18
 */
@Service
@Slf4j
public class UserServiceAdapter {

    @ServiceApiReference
    private UserApiService userApiService;
    @Autowired
    private RedisUtils redisUtils;
    /**
     * 查询角色成员
     * @param roleId
     * @return
     */
    public Collection<PersonDetailDTO> queryRoleMember(String roleId) {
        Result<List<UserFlowInfoDTO>> result = userApiService.getUsersByRoleId(Long.parseLong(roleId));
        List<PersonDetailDTO> persons = new LinkedList<>();
        if (result.getData() != null) {
            for (UserFlowInfoDTO user : result.getData()) {
                if (user.getPersonId() != null) {
                    PersonDetailDTO person = new PersonDetailDTO();
                    person.setId(user.getPersonId());
                    person.setUserId(user.getId());
                    person.setUserName(user.getUserName());
                    persons.add(person);
                }
            }
        }
        return persons;
    }
    
    /**
     * 获取用户名, 用户名缓存10分钟, 避免频繁调用影响性能
     * @param userId
     * @return
     */
    public String getUserName(Long userId) {
        String cachedUserName = redisUtils.getStringValue(userId.toString());
        if (StringUtils.isNotEmpty(cachedUserName)) {
            return cachedUserName;
        }
        UserOrgDetailDTO user = getUserById(userId);
        if (user != null) {
            redisUtils.setStringValue(userId.toString(), user.getUserName(), 10 * 60 * 1000);
            return user.getUserName();
        }
        return "";
    }
    
    /**
     * 查询用户详情
     * @param userId
     * @return
     */
    public UserOrgDetailDTO getUserById(Long userId) {
        try {
            Result<UserOrgDetailDTO> userDetail = userApiService.getUsersDetailById(userId);
            if (userDetail != null && userDetail.getData() != null) {
                return userDetail.getData();
            }
        } catch (Exception e) {
            log.error("获取用户详情异常,用户ID:{}", userId, e);
        }
        return null;
    }
    
    /**
     * 根据名称查询用户
     * @param username
     * @return
     */
    public UserOrgDetailDTO getUserByName(String username) {
        try {
            Result<UserOrgDetailDTO> userDetail = userApiService.getUsersDetailByName(username, null);
            if (userDetail != null && userDetail.getData() != null) {
                return userDetail.getData();
            }
        } catch (Exception e) {
            log.error("获取用户详情异常,用户名: {}", username, e);
        }
        return null;
    }
    
    public Map<Long, UserDetailDTO> batchQueryUserById(Collection<String> ids) {
        String idStr = String.join(Constants.SPLIT_COMMA, ids);
        return userApiService.getUsersDetail(idStr);
    }

    public Map<String, UserDetailDTO> batchQueryUserByName(Collection<String> names) {
        String[] nameArray = names.toArray(new String[names.size()]);
        Result<Map<String, UserDetailDTO>> result = userApiService.getBatchUsersDetailByName(nameArray);
        return result.getData();
    }
    
    /**
     * 查询人员姓名
     * @param userId
     * @return
     */
    public String getPersonName(Long userId) {
        try {
            String cachedUserName = redisUtils.getStringValue("person_" + userId);
            if (StringUtils.isNotEmpty(cachedUserName)) {
                return cachedUserName;
            }
            UserOrgDetailDTO user = getUserById(userId);
            if (user != null && user.getPersonName() != null) {
                redisUtils.setStringValue("person_" + userId, user.getPersonName());
                return user.getPersonName();
            }
        } catch (Exception e) {
            log.error("根据用户ID获取人员名称异常, 用户ID: {}", userId, e);
        }
        return "";
    }
    
}
