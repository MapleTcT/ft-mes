package com.supcon.supfusion.auth.manager.Impl;

import com.supcon.supfusion.auth.manager.RbacServiceAdapter;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.rbac.api.IRoleApiService;
import com.supcon.supfusion.rbac.api.IRoleUserApiService;
import com.supcon.supfusion.rbac.api.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RbacServiceAdapterImpl implements RbacServiceAdapter {
    @Resource
    private IRoleApiService roleApiService;
    @Resource
    private IRoleUserApiService roleUserApiService;

    @Override
    public Map<Long, String> findBatchName(String ids) {
        return roleApiService.findBatchName(ids);
    }

    @Override
    public List<RoleDTO> findRoleByCodes(List<String> codes) {
        return roleApiService.findRoleByCodes(codes);
    }

    @Override
    public RoleDTO createAdminRole(AdminRoleDTO adminRoleDTO) {
        return roleApiService.createAdminRole(adminRoleDTO);
    }


    @Override
    public void batchUpdateOneUser(RoleUserAddBatchDTO roleUserAddBatchDTO) {
        roleUserApiService.updateRoleUser(roleUserAddBatchDTO);
    }

    @Override
    public List<RoleDTO> findRoleByIds(List<Long> ids) {
        return roleApiService.findRoleByIds(ids);
    }

    @Override
    public RoleDTO bindCompanyAdminUser(AdminRoleDTO adminRoleDTO) {
        try {
            return roleApiService.bindCompanyAdminUser(adminRoleDTO);
        }catch (Exception e){
            log.error("error is",e);
        }
        return null;
    }

    @Override
    public void deleteRolesByUserIds(List<Long> userIds) {
        roleUserApiService.deleteByUserId(userIds);
    }

    @Override
    public void batchSaveOneUserFR(List<RoleUserFRDTO> roleUserFRDTOS) {
        roleUserApiService.batchSaveOneUserFR(roleUserFRDTOS);
    }

    @Override
    public PageResult<RoleDTO> getRolesByCid(Long cid, String keyword, Integer current, Integer pageSize) {
        return roleApiService.getRolesByCid(cid, keyword, current, pageSize);
    }

    @Override
    public void addRole(Long cid, String code, String name, String description) {
        CreateRoleDTO createRoleDTO = new CreateRoleDTO();
        createRoleDTO.setCid(cid);
        createRoleDTO.setCode(code);
        createRoleDTO.setName(name);
        createRoleDTO.setDescription(description);
        roleApiService.createRole(createRoleDTO);
    }

    @Override
    public void updateRole(String code, String name, String description) {
        UpdateRoleDTO updateRoleDTO = new UpdateRoleDTO();
        updateRoleDTO.setCode(code);
        updateRoleDTO.setDescription(description);
        updateRoleDTO.setShowName(name);
        roleApiService.updateRole(updateRoleDTO);
    }

    @Override
    public void deleteRoles(List<String> codes) {
        roleApiService.deleteRolesByCodes(codes);
    }

    @Override
    public RoleResourceDTO getRoleDetail(String code) {
        Result<RoleResourceDTO> roleResources = roleApiService.getRoleResources(code);
        return roleResources.getData();
    }
}
