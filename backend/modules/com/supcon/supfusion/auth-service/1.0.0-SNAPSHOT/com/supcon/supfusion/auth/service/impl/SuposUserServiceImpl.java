package com.supcon.supfusion.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.utils.BCryptUtil;
import com.supcon.supfusion.auth.dao.mapper.UserMapper;
import com.supcon.supfusion.auth.dao.po.UserPO;
import com.supcon.supfusion.auth.dao.po.UserRolePO;
import com.supcon.supfusion.auth.manager.KeycliandAdminClient;
import com.supcon.supfusion.auth.manager.PersonServiceAdapter;
import com.supcon.supfusion.auth.manager.RbacServiceAdapter;
import com.supcon.supfusion.auth.manager.bo.LoginConfigBO;
import com.supcon.supfusion.auth.service.PasswordService;
import com.supcon.supfusion.auth.service.SuposUserService;
import com.supcon.supfusion.auth.service.UserRoleService;
import com.supcon.supfusion.auth.service.bo.RoleDetailBO;
import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.auth.service.bo.UserRoleBO;
import com.supcon.supfusion.auth.service.cache.UserCache;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import com.supcon.supfusion.organization.api.dto.PersonUpdateDTO;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import com.supcon.supfusion.rbac.api.dto.RoleFRDTO;
import com.supcon.supfusion.rbac.api.dto.RoleResourceDTO;
import com.supcon.supfusion.rbac.api.dto.RoleUserFRDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.supcon.supfusion.auth.common.constants.Constants.ROLE_USER;

@Service
@Slf4j
public class SuposUserServiceImpl extends ServiceImpl<UserMapper, UserPO> implements SuposUserService {

    private static String bigSmall = "(?=.*[a-z])(?=.*[A-Z])";

    private static String number = "(?=.*[0-9])";

    private static String special = "(?=.*[!@#$%^&*();'?.,])";

    private static String all = "[A-Za-z0-9!@#$%^&*();'?.,]";

    @Resource
    private KeycliandAdminClient keycliandAdminClient;

    @Resource
    private UserRoleService userRoleService;


    @Resource
    private RbacServiceAdapter rbacServiceAdapter;

    @Resource
    private PersonServiceAdapter personServiceAdapter;

    @Autowired
    private PasswordService passwordService;

    @Resource
    private UserCache userCache;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void creatOpenUser(UserBO bo, String email, HttpServletResponse response) {
        if (!verifyOpenPassword(bo, response)) {
            return;
        }
        UserPO userPO = new UserPO();
        BeanUtils.copyProperties(bo, userPO);
        Result<Long> result = personServiceAdapter.addVirtualPerson(bo.getUserName(), 1000L);
        Long personId = result.getData();
        Map<Long, PersonDTO> personDTOMap = personServiceAdapter.queryPersonsById(new Long[]{personId});
        PersonDTO personDTO = personDTOMap.get(personId);
        userPO.setPersonId(personId);
        userPO.setPersonCode(personDTO.getCode());
        userPO.setPersonName(personDTO.getName());
        if (StringUtils.isNotEmpty(email)) {
            PersonUpdateDTO personUpdateDTO = new PersonUpdateDTO();
            personUpdateDTO.setId(personId);
            personUpdateDTO.setEmail(email);
            personServiceAdapter.updatePerson(personUpdateDTO);
        }
        if (bo.getRoles() != null) {
            List<String> codes = bo.getRoles().stream().map(UserRoleBO::getRoleCode).collect(Collectors.toList());
            List<RoleDTO> roleByCodes = rbacServiceAdapter.findRoleByCodes(codes);
            if (roleByCodes.isEmpty()) {
                StringBuilder str = new StringBuilder();
                codes.forEach(t -> {
                    str.append(t).append(",");
                });
                String remind = str.substring(0, str.length() - 1);
                errorResponse(response, 400, 100106013, remind + "对应角色不存在");
                return;
            } else if (roleByCodes.size() != codes.size()) {
                Set<String> roleCodes = roleByCodes.stream().map(RoleDTO::getCode).collect(Collectors.toSet());
                StringBuilder str = new StringBuilder();
                codes.forEach(t -> {
                    if (roleCodes.add(t)) {
                        str.append(t).append(",");
                    }
                });
                String remind = str.substring(0, str.length() - 1);
                errorResponse(response, 400, 100106013, remind + "对应角色不存在");
                return;
            }
            List<UserRoleBO> roles = roleByCodes.stream().map(t -> {
                UserRoleBO userRoleBO = new UserRoleBO();
                userRoleBO.setUserId(bo.getId());
                userRoleBO.setRoleType(ROLE_USER);
                userRoleBO.setRoleId(t.getId());
                return userRoleBO;
            }).collect(Collectors.toList());
            userRoleService.batchInsert(roles);
            UserBO userBO = new UserBO();
            BeanUtils.copyProperties(userPO, userPO);
            addUserRolesToRbac(userBO, roles);
        }
        this.save(userPO);
    }

    private void errorResponse(HttpServletResponse response, Integer statusCode, Integer code, String message) {
        try {
            response.setStatus(statusCode);
            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            HashMap<String, Object> map = new HashMap<>();
            map.put("message", message);
            map.put("code", code);
            ObjectMapper objectMapper = new ObjectMapper();
            String remind = objectMapper.writeValueAsString(map);
            response.getWriter().println(remind);
        } catch (Exception e) {

        }
    }

    @Override
    public Page<UserBO> openGetUsers(Page<UserBO> page, String keyword, HttpServletResponse response) {
        Page<UserPO> userPOPage = new Page<>();
        userPOPage.setCurrent(page.getCurrent());
        userPOPage.setSize(page.getSize());
        LambdaQueryWrapper<UserPO> wrapper = Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getCompanyId, 1000L).ne(UserPO::getUserType, 2);
        if (StringUtils.isNotEmpty(keyword)) {
            wrapper.like(UserPO::getDescription, keyword).or().like(UserPO::getUserName, keyword);
        }
        wrapper.orderByDesc(UserPO::getCreateTime);
        Page<UserPO> userPage = this.page(userPOPage, wrapper);
        List<UserBO> collect = userPage.getRecords().stream().map(t -> {
            UserBO userBO = new UserBO();
            BeanUtils.copyProperties(t, userBO);
            return userBO;
        }).collect(Collectors.toList());
        Page<UserBO> temp = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        temp.setRecords(collect);
        return temp;
    }

    @Override
    public UserBO getUserInfo(String userName, HttpServletResponse response) {
        UserPO one = this.getOne(Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getUserName, userName));
        if (one != null) {
            UserBO userBO = new UserBO();
            BeanUtils.copyProperties(one, userBO);
            return userBO;
        } else {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(String username, String timeZone, String userDesc, String email, List<String> roleNameList, HttpServletResponse response) {
        UserPO one = getOne(Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getUserName, username));
        if (one != null) {
            UserPO userPO = new UserPO();
            if (StringUtils.isNotEmpty(timeZone)) {
                userPO.setTimeZone(timeZone);
            }

            if (StringUtils.isNotEmpty(userDesc)) {
                userPO.setDescription(userDesc);
            }

            if (StringUtils.isNotEmpty(email)) {
                PersonUpdateDTO personUpdateDTO = new PersonUpdateDTO();
                personUpdateDTO.setId(one.getPersonId());
                personUpdateDTO.setEmail(email);
                personServiceAdapter.updatePerson(personUpdateDTO);
            }

            if (roleNameList != null && !roleNameList.isEmpty()) {
                userRoleService.remove(Wrappers.lambdaQuery(UserRolePO.class).eq(UserRolePO::getUserId, one.getId()));
                List<RoleDTO> roleDetails = rbacServiceAdapter.findRoleByCodes(roleNameList);
                List<UserRoleBO> collect = roleDetails.stream().map(t -> {
                    UserRoleBO userRoleBO = new UserRoleBO();
                    userRoleBO.setUserId(one.getId());
                    userRoleBO.setRoleId(t.getId());
                    userRoleBO.setRoleType(ROLE_USER);
                    return userRoleBO;
                }).collect(Collectors.toList());
                userRoleService.batchInsert(collect);
                UserBO userBO = new UserBO();
                BeanUtils.copyProperties(userPO, userBO);
                addUserRolesToRbac(userBO, collect);
            }
            this.update(userPO,Wrappers.lambdaUpdate(UserPO.class).eq(UserPO::getId,one.getId()));
            UserBO userBO = new UserBO();
            userBO.setId(one.getId());
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUsers(List<String> list) {
        if (!list.isEmpty()) {
            LambdaQueryWrapper<UserPO> wrapper = Wrappers.lambdaQuery(UserPO.class).in(UserPO::getUserName, list);
            List<UserPO> userPOS = this.list(wrapper);
            List<Long> collect = userPOS.stream().map(UserPO::getId).collect(Collectors.toList());
            this.remove(Wrappers.lambdaQuery(UserPO.class).in(UserPO::getUserName, list));
            userRoleService.remove(Wrappers.lambdaQuery(UserRolePO.class).in(UserRolePO::getUserId, collect));
        }
    }

    @Override
    public List<UserBO> openGetUsersByRoleCode(String roleCode, HttpServletResponse response) {
        List<RoleDTO> roles = rbacServiceAdapter.findRoleByCodes(Arrays.asList(roleCode));
        Long id = roles.get(0).getId();
        List<UserRolePO> list = userRoleService.list(Wrappers.lambdaQuery(UserRolePO.class).eq(UserRolePO::getRoleId, id));
        Set<Long> userIds = new HashSet<>();
        for (UserRolePO userRolePO : list) {
            userIds.add(userRolePO.getUserId());
        }
        List<UserPO> userPOS = this.list(Wrappers.lambdaQuery(UserPO.class).in(UserPO::getId, userIds).eq(UserPO::getCompanyId, 1000L));
        List<UserBO> userBOS = userPOS.stream().map(t -> {
            UserBO userBO = new UserBO();
            List<UserRoleBO> role = userRoleService.getRole(t.getId());
            List<Long> roleIds = role.stream().map(UserRoleBO::getRoleId).collect(Collectors.toList());
            List<RoleDTO> roleByIds = rbacServiceAdapter.findRoleByIds(roleIds);
            roleByIds.forEach(roleDTO -> {
                UserRoleBO userRoleBO = new UserRoleBO();
                userRoleBO.setRoleId(roleDTO.getId());
                userRoleBO.setRoleCode(roleDTO.getCode());
                userRoleBO.setRoleName(roleDTO.getName());
            });
            BeanUtils.copyProperties(t, userBO);
            return userBO;
        }).collect(Collectors.toList());
        return userBOS;
    }

    @Override
    public Page<RoleDetailBO> getRoles(Integer pageIndex, Integer pageSize, String keyword, HttpServletResponse response) {
        PageResult<RoleDTO> roles = rbacServiceAdapter.getRolesByCid(1000L, keyword, pageIndex, pageSize);
        List<RoleDetailBO> roleDetailBOS = roles.getList().stream().map(t -> {
            RoleDetailBO roleDetailBO = new RoleDetailBO();
            roleDetailBO.setShowName(t.getName());
            roleDetailBO.setName(t.getCode());
            roleDetailBO.setDescription(t.getDescription());
            roleDetailBO.setCreateTime(t.getCreateTime());
            roleDetailBO.setCreateUsername(t.getCreator());
            roleDetailBO.setModifyTime(t.getModifyTime());
            roleDetailBO.setModifyUsername(t.getModifyTime());
            return roleDetailBO;
        }).collect(Collectors.toList());
        Page<RoleDetailBO> page = new Page<>(roles.getPagination().getCurrent(), roles.getPagination().getPageSize(),roles.getPagination().getTotal());
        page.setRecords(roleDetailBOS);
        return page;
    }

    @Override
    public void addRole(Long cid, String code, String name, String description) {
        rbacServiceAdapter.addRole(cid, code, name, description);
    }

    @Override
    public void update(String code, String name, String description) {
        rbacServiceAdapter.updateRole(code, name, description);
    }

    @Override
    public void deleteRoles(List<String> codes) {
        rbacServiceAdapter.deleteRoles(codes);
    }

    @Override
    public RoleDetailBO getRoleDetail(String code) {
        RoleResourceDTO roleDetail = rbacServiceAdapter.getRoleDetail(code);
        if(roleDetail==null){
            return null;
        }
        RoleDetailBO roleDetailBO = new RoleDetailBO();
        BeanUtils.copyProperties(roleDetail, roleDetailBO);
        roleDetailBO.setShowName(roleDetail.getShowname());
        return roleDetailBO;
    }

    private Boolean verifyOpenPassword(UserBO bo, HttpServletResponse response) {
        StringBuilder str = new StringBuilder();
        StringBuilder remind = new StringBuilder();
        LoginConfigBO loginConfig = passwordService.getLoginConfig();
        String passwordRemind = null;
        if (Constants.COMBINATION_PWD_RULE.compareTo(loginConfig.getRuleType())==0) {
            if (loginConfig.getBigSmall() != null && loginConfig.getBigSmall()) {
                str.append(bigSmall);
                remind.append("大小写、");
            }
            if (loginConfig.getNumber() != null && loginConfig.getNumber()) {
                str.append(number);
                remind.append("数字、");
            }
            if (loginConfig.getSpecialChar() != null && loginConfig.getSpecialChar()) {
                str.append(special);
                remind.append("特殊字符（仅支持!@#$%^&*();'?.,)、");
            }
        } else {
            str.append(loginConfig.getRegularExpression());
        }
        if (loginConfig.getMin() != null && loginConfig.getMax() != null) {
            str.append(".{").append(loginConfig.getMin()).append(",").append(loginConfig.getMax()).append("}");
        }
        if (Constants.COMBINATION_PWD_RULE.compareTo(loginConfig.getRuleType())==0) {
            String composeStr = !StringUtils.isEmpty(remind.toString()) ? remind.substring(0, remind.length() - 1) : "大小写、数字、特殊字符（仅支持!@#$%^&*();'?.,)";
            passwordRemind = String.format(Constants.RULE_ERROR, composeStr, loginConfig.getMin(), loginConfig.getMax());
        } else {
            passwordRemind = loginConfig.getHint();
        }
        //        String substring = !org.springframework.util.StringUtils.isEmpty(remind.toString()) ? remind.substring(0, remind.length() - 1) : "大小写、数字、特殊字符 !@#$%^&*();'?.,,";
        Pattern compile = Pattern.compile("^" + str.toString() + "$");
        Matcher matcher = compile.matcher(bo.getPassword());
        if (!matcher.find()) {
            errorResponse(response, 400, 100106021, passwordRemind);
            return false;
        }
        bo.setPassword(BCryptUtil.encode(bo.getPassword()));
        return true;
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
