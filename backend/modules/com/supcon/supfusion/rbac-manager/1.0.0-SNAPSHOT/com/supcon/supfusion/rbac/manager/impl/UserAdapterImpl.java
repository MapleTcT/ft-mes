package com.supcon.supfusion.rbac.manager.impl;

import com.supcon.supfusion.auth.api.UserApiService;
import com.supcon.supfusion.auth.api.dto.UserDetailDTO;
import com.supcon.supfusion.auth.api.dto.UserOrgDetailDTO;
import com.supcon.supfusion.auth.api.dto.UserRoleDTO;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.rbac.common.exception.PermissionErrorEnum;
import com.supcon.supfusion.rbac.common.exception.PermissionException;
import com.supcon.supfusion.rbac.manager.IUserAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserAdapterImpl implements IUserAdapter {

    @Autowired
    private UserApiService userApiService;

    @Override
    public Map<Long, UserDetailDTO> getUserIdByPersonId(String personIds) {
        return userApiService.getUsersDetailByPerson(personIds);
    }

    @Override
    public List<Long> getCompanyIdByUserName(String userName) {
        List<Long> companyIds = new ArrayList<>();
        try {
            Result<UserOrgDetailDTO> orgDetailByName = userApiService.getUserOrgDetailByName(userName);
            List<UserOrgDetailDTO.Company> companies = orgDetailByName.getData().getCompanies();
            if (!ObjectUtils.isEmpty(companies)) {
                companyIds = companies.stream().map(UserOrgDetailDTO.Company::getCompanyId).collect(Collectors.toList());
            }
        }catch (Exception e){
            throw new PermissionException(PermissionErrorEnum.PERMISSION_FRESH_ERROR);
        }
        return companyIds;
    }

    @Override
    public List<Long> getCompanyIdByUserId(Long userId) {
        Result<UserOrgDetailDTO> orgDetailById = userApiService.getUsersDetailById(userId);
        List<UserOrgDetailDTO.Company> companies = orgDetailById.getData().getCompanies();
        return companies.stream().map(UserOrgDetailDTO.Company::getCompanyId).collect(Collectors.toList());
    }

    @Override
    public Result<Boolean> bindRole(Long roleId, List<Long> userIds,boolean add) {
        UserRoleDTO userRoleDTO = new UserRoleDTO();
        userRoleDTO.setRoleId(roleId);
        if (add){
            userRoleDTO.setAddUserIds(userIds);
        }else{
            userRoleDTO.setDeleteUserIds(userIds);
        }
        return userApiService.bindRole(userRoleDTO);
    }


}
