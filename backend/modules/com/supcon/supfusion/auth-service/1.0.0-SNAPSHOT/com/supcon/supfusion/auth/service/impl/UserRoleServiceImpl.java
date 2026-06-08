package com.supcon.supfusion.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.auth.dao.mapper.UserRoleMapper;
import com.supcon.supfusion.auth.dao.po.UserPO;
import com.supcon.supfusion.auth.dao.po.UserRolePO;
import com.supcon.supfusion.auth.manager.PersonServiceAdapter;
import com.supcon.supfusion.auth.manager.RbacServiceAdapter;
import com.supcon.supfusion.auth.service.UserRoleService;
import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.auth.service.bo.UserRoleBO;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import com.supcon.supfusion.rbac.api.dto.RoleFRDTO;
import com.supcon.supfusion.rbac.api.dto.RoleUserFRDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author lifangyuan
 */
@Service
@Slf4j
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRolePO> implements UserRoleService {

    @Resource
    private RbacServiceAdapter rbacServiceAdapter;

    @Resource
    private PersonServiceAdapter personServiceAdapter;

    @Override
    public void batchInsert(List<UserRoleBO> userRoleBOS) {
        List<UserRolePO> collect = userRoleBOS.stream().map(t -> {
            UserRolePO userRolePO = new UserRolePO();
            BeanUtils.copyProperties(t, userRolePO);
            userRolePO.setId(IDGenerator.newInstance().generate().longValue());
            return userRolePO;
        }).collect(Collectors.toList());
        saveBatch(collect);
    }

    @Override
    public List<UserRoleBO> getRole(Long userId) {
        List<UserRolePO> list = this.list(Wrappers.lambdaQuery(UserRolePO.class).eq(UserRolePO::getUserId, userId));
        return list.stream().map(t -> {
            UserRoleBO userRoleBO = new UserRoleBO();
            BeanUtils.copyProperties(t, userRoleBO);
            return userRoleBO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserBO> selectUserRole(Long roleId) {
        List<UserPO> userPOS = this.baseMapper.selectUserRole(roleId);
        return userPOS.stream().map(t -> {
            UserBO userBO = new UserBO();
            BeanUtils.copyProperties(t, userBO);
            return userBO;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateRoles(List<UserRoleBO> userRoleBOS,UserBO userBO) {
     this.baseMapper.delete(Wrappers.lambdaUpdate(UserRolePO.class).eq(UserRolePO::getUserId,userBO.getId()));
        List<UserRolePO> collect = userRoleBOS.stream().map(t -> {
            UserRolePO userRolePO = new UserRolePO();
            BeanUtils.copyProperties(t, userRolePO);
            userRolePO.setId(IDGenerator.newInstance().generate().longValue());
            return userRolePO;
        }).collect(Collectors.toList());
        saveBatch(collect);
        addUserRolesToRbac(userBO,userRoleBOS);
    }
    /**
     * 将用户和角色的关联发送给rbac
     */
    void addUserRolesToRbac(UserBO userBo, List<UserRoleBO> role) {
        List<RoleUserFRDTO> roleUserFRDTOS = new ArrayList<>();
        RoleUserFRDTO roleUserAddBatchDTO = new RoleUserFRDTO();
        roleUserAddBatchDTO.setUserId(userBo.getId());
        roleUserAddBatchDTO.setUserName(userBo.getUserName());
        roleUserAddBatchDTO.setCid(userBo.getCompanyId());
        PersonDTO person = getPersonDTO(userBo.getPersonId());
        if (null == userBo.getPersonCode() && null != person) {
            roleUserAddBatchDTO.setPersonCode(person.getCode());
        } else {
            roleUserAddBatchDTO.setPersonCode(userBo.getPersonCode());
        }
        if (null == userBo.getPersonName() && null != person) {
            roleUserAddBatchDTO.setPersonName(person.getName());
        } else {
            roleUserAddBatchDTO.setPersonName(userBo.getPersonName());
        }
        if (role != null && !role.isEmpty()) {
            List<RoleFRDTO> roleFRDTOS = new ArrayList<>();
            role.stream().forEach(t -> {
                RoleFRDTO roleFRDTO = new RoleFRDTO();
                roleFRDTO.setId(t.getRoleId());
                roleFRDTO.setFromPosition(t.getRoleType());
                roleFRDTOS.add(roleFRDTO);
            });
            roleUserAddBatchDTO.setRoles(roleFRDTOS);
        } else {
            roleUserAddBatchDTO.setRoles(new ArrayList<>());
        }
        roleUserFRDTOS.add(roleUserAddBatchDTO);
        rbacServiceAdapter.batchSaveOneUserFR(roleUserFRDTOS);
    }
    /**
     * 根据personId 获取PersonDTO
     */
    public PersonDTO getPersonDTO(Long peronId) {
        if (null != peronId) {
            Long[] personIds = new Long[1];
            personIds[0] = peronId;
            PersonDTO personDTO = personServiceAdapter.queryPersonsById(personIds).get(peronId);
            return personDTO;
        }
        return null;
    }

}
