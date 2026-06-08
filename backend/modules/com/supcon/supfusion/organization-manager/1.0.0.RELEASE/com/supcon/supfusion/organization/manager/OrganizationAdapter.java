package com.supcon.supfusion.organization.manager;

import com.supcon.supfusion.auth.api.UserApiService;
import com.supcon.supfusion.auth.api.dto.*;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiReference;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.rbac.api.IMenuInfoApiService;
import com.supcon.supfusion.rbac.api.IRoleApiService;
import com.supcon.supfusion.rbac.api.IRoleUserApiService;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import com.supcon.supfusion.rbac.api.dto.RoleUserDTO;
import com.supcon.supfusion.systemcode.api.SystemCodeApiService;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Setter
@Getter
@Component
public class OrganizationAdapter {

    @ServiceApiReference
    private UserApiService userApiService;

    @ServiceApiReference
    private IRoleApiService iRoleApiService;

    @ServiceApiReference
    private IRoleUserApiService iRoleUserApiService;

    @ServiceApiReference
    private IMenuInfoApiService iMenuInfoApiService;
    @ServiceApiReference
    private SystemCodeApiService systemCodeApiService;
    /**
     * 创建用户
     * @param userName 用户名
     * @param password 密码
     * @param companyId 公司
     * @param roleIds 角色id
     * @param companyCode
     */
    public Result<String> createUser(String userName, String password, String description, Long companyId, List<Long> roleIds, Long personId, Integer userType, String companyCode, String personCode, String personName) {
        BatchInsertDTO batchInsertDTO = new BatchInsertDTO();
        UserAddDTO userAddDTO = new UserAddDTO();
        userAddDTO.setUserName(userName);
        userAddDTO.setPassword(password);
        userAddDTO.setCompanyId(companyId);
        userAddDTO.setRoleIds(roleIds);
        userAddDTO.setDescription(description);
        userAddDTO.setPersonId(personId);
        userAddDTO.setUserType(userType);
        userAddDTO.setCompanyCode(companyCode);
        userAddDTO.setPersonCode(personCode);
        userAddDTO.setPersonName(personName);
        List<UserAddDTO> users = new ArrayList<UserAddDTO>();
        users.add(userAddDTO);
        batchInsertDTO.setUsers(users);
        Result<String> result = userApiService.createUser(batchInsertDTO);
        return result;
    }

    /**
     * 获取用户信息
     * @param personId
     * @return
     */
    public UserDetailDTO getUserDetailByPerson(Long personId) {
        Map<Long, UserDetailDTO> users = userApiService.getUsersDetailByPerson(personId.toString());
        return users.get(personId);
    }

    public List<RoleDTO> findRoleByIds(List<Long> roleIds) {
        return iRoleApiService.findRoleByIds(roleIds);
    }

    public List<RoleDTO> findRoleByCodes(List<String> codes) {
        return iRoleApiService.findRoleByCodes(codes);
    }

    public List<RoleUserDTO> findeRoleUserByRoleIds(List<Long> roleIds) {
        return iRoleUserApiService.findUserByRoleId(roleIds);
    }

    public List<RoleUserDTO> findeRoleUserByRoleCodes(List<String> roleCodes) {
        return iRoleUserApiService.findUserByRoleCode(roleCodes);
    }

    public Map<Long, UserDetailDTO> getUsersDetail(String userIds) {
        return userApiService.getUsersDetail(userIds);
    }

    public Map<Long, UserDetailDTO> getUsersDetailByPerson(String personIds) {
        return userApiService.getUsersDetailByPerson(personIds);
    }

    public UserOrgDetailDTO getUsersDetailByName(@RequestParam("userName") String var1, Long companyId) {
        return userApiService.getUsersDetailByName(var1, companyId).getData();
    }

    public List<UserDetailDTO> queryCompanyUsers(Long companyId) {
        ListResult<UserDetailDTO> result =  userApiService.companyAdminUser(companyId);
        if (result == null) {
            return null;
        }
        return (List<UserDetailDTO>) result.getList();
    }

    /**
     * 查询系统编码值
     * @param code 系统编码sys_gender/male
     * @return
     */
    public String getSystemCodeName(String code) {
        if (StringUtils.isEmpty(code)) {
            return "";
        }
        String[] systemCode = code.split("/");
        if (systemCode.length < 2) {
            return "";
        }
        Result<SystemCodeResultDTO> result = systemCodeApiService.queryValueByCode(systemCode[0], systemCode[1]);
        if (result == null || result.getData() == null) {
            return "";
        }
        SystemCodeResultDTO systemCodeResultDTO = result.getData();
        if (!StringUtils.isEmpty(systemCodeResultDTO.getDisplayName())) {
            return systemCodeResultDTO.getDisplayName();
        }
        return "";
    }

    public Boolean deleteCompanyUser(Long companyId) {
        Result<Boolean> result = userApiService.deleteCompanyUser(companyId);
        if (result == null || !result.getData()) {
            return false;
        }
        return result.getData();
    }

    public Boolean deleteRbac(Long companyId) {
        Result<Boolean> result = iMenuInfoApiService.deleteCompanyRef(companyId);
        if (result == null || !result.getData()) {
            return false;
        }
        return result.getData();
    }

    public Map<Long, UserDetailDTO> queryUsersByPersonIds(List<Long> personIds, String keyword) {
        UserQueryDTO userQueryDTO = new UserQueryDTO();
        userQueryDTO.setPersonIds(personIds);
        userQueryDTO.setKeyword(keyword);
        Map<Long, UserDetailDTO> userDetailDTOMap = userApiService.queryUsersDetail(userQueryDTO);
        return  userDetailDTOMap;
    }

    public Boolean informRoleChange(List<PersonRoleDTO> personRoleList) {
        Result<Boolean> result = userApiService.changeRole(personRoleList);
        if (result == null) {
            return false;
        }
        return result.getData();
    }

    public Boolean deleteUser(Long[] personIds) {
        Result<Boolean> flag = userApiService.deleteCompanyUser(personIds);
        if (flag == null) {
            return false;
        }
        return flag.getData();
    }

    /**
     * 根据entityCode查询所有系统编码
     * @param entityCode
     * @return
     */
    public List<SystemCodeResultDTO> querySystemCodesByEntityCode (String entityCode) {
        PageResult<SystemCodeResultDTO> pageResult = systemCodeApiService.queryValueList(entityCode, "", 1, 100);
        List<SystemCodeResultDTO> systemCodes = new ArrayList<>();
        if (pageResult != null && pageResult.getList() != null) {
            for (SystemCodeResultDTO systemCodeResultDTO : pageResult.getList()) {
                systemCodes.add(systemCodeResultDTO);
            }
        }
        return systemCodes;
    }

    public Long getUserIdByUserName(String userName) {
        Result<Long> result = userApiService.getIdByUserName(userName);
        return result.getData();
    }

    public List<UserDetailDTO> getAllPersonsUsers() {
        ListResult<UserDetailDTO> result = userApiService.getAllPersonsUsers();
        return (List<UserDetailDTO>) result.getList();
    }

    public void updateUserPersonName(List<UserPersonNames> userPersonNameList) {
        userApiService.changeUserPersonName(userPersonNameList);
    }
}
