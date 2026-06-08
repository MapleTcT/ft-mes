package com.supcon.supfusion.auth.openapi.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.auth.common.constants.UserTypeEnum;
import com.supcon.supfusion.auth.common.utils.BijectionUtils;
import com.supcon.supfusion.auth.manager.PersonServiceAdapter;
import com.supcon.supfusion.auth.manager.RbacServiceAdapter;
import com.supcon.supfusion.auth.manager.SystemCodeServiceAdapter;
import com.supcon.supfusion.auth.openapi.suposvo.SuposPageResult;
import com.supcon.supfusion.auth.openapi.suposvo.SuposPagination;
import com.supcon.supfusion.auth.openapi.vo.*;
import com.supcon.supfusion.auth.service.UserRoleService;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.bo.UnBinderUserthridIdentityBos;
import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.auth.service.bo.UserRoleBO;
import com.supcon.supfusion.auth.service.cache.UserCache;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author lifangyuan
 */
@Slf4j
@RestController
@OpenApi(path = "/open-api/auth/v2")
@Validated
@Api(tags = "用户OpenApi", description = "用户OpenApi接口文档说明", hidden = true)
public class OpenUserController extends BaseController {

    @Resource
    private UserService userService;

    @Resource
    private PersonServiceAdapter personServiceAdapter;

    @Resource
    private RbacServiceAdapter rbacServiceAdapter;

    @Resource
    private UserRoleService userRoleService;

    @Resource
    private UserCache userCache;

    private static Pattern pattern = Pattern.compile("GMT[+-][0-9]{4}");

    private static Pattern userNamePattern = Pattern.compile("^\\w+$");

    private static Pattern numberPattern = Pattern.compile("^[0-9]*[1-9][0-9]*$");

    @Resource
    private SystemCodeServiceAdapter systemCodeServiceAdapter;


    @PostMapping("/users")
    public void addUser(@RequestBody AddUserVO addUserVO, HttpServletResponse response) {
        boolean verify = verify(addUserVO, response);
        if (!verify) {
            return;
        }
        UserBO userBO = new UserBO();
        userBO.setUserName(addUserVO.getUsername());
        userBO.setPassword(addUserVO.getPassword());
        userBO.setTimeZone(addUserVO.getTimeZone());
        userBO.setDescription(addUserVO.getUserDesc());
        userBO.setPersonCode(addUserVO.getPersonCode());
        userBO.setCompanyCode(addUserVO.getCompanyCode());
        userBO.setUserType(UserTypeEnum.COMMON_USER.getCode());
        userBO.setId(IDGenerator.newInstance().generate().longValue());
        if (addUserVO.getRoleNameList() != null) {
            List<UserRoleBO> userRoleBOS = addUserVO.getRoleNameList().stream().map(t -> {
                UserRoleBO userRoleBO = new UserRoleBO();
                userRoleBO.setRoleCode(t);
                return userRoleBO;
            }).collect(Collectors.toList());
            userBO.setRoles(userRoleBOS);
        }
        userService.creatOpenUser(userBO, response);
    }


    @ApiOperation(value = "查询用户列表")
    @GetMapping("/users")
    public SuposPageResult<UserDetailVO> getUsers(@ApiParam(value = "用户名模糊查询关键词") @RequestParam(name = "keyword", required = false) String keyword,
                                                  @ApiParam(value = "公司编码", required = true) @RequestParam(name = "companyCode", required = false) String companyCode,
                                                  @ApiParam(value = "当前页码", defaultValue = "1") @RequestParam(name = "pageIndex", required = false) String pageIndex,
                                                  @ApiParam(value = "每页条数", defaultValue = "20") @RequestParam(name = "pageSize", defaultValue = "20", required = false) Integer pageSize,
                                                  @ApiParam(value = "角色编码") @RequestParam(name = "roleCode", required = false) String roleCode,
                                                  @RequestParam(name = "modifyTime", required = false) String modifyTime,
                                                  HttpServletResponse response) {

        boolean verify = verify(companyCode, pageIndex, pageSize, response);
        if (!verify) {
            return null;
        }
        Page<UserBO> page = new Page<>(Integer.valueOf(pageIndex), pageSize);
        Page<UserBO> result = userService.openGetUsers(page, keyword, companyCode, roleCode, modifyTime, response);
        if (result == null) {
            return null;
        }
        List<UserDetailVO> collect = result.getRecords().stream().map(t -> {
            UserDetailVO userDetailVO = new UserDetailVO();
            userDetailVO.setUsername(t.getUserName());
            userDetailVO.setAccountType(t.getUserType());
            userDetailVO.setLockStatus(t.getHasLock() ? 1 : 0);
            userDetailVO.setUserDesc(t.getDescription());
            userDetailVO.setCreateTime(t.getCreateTime());
            userDetailVO.setModifyTime(t.getModifyTime());
            if (t.getPersonId() != null) {
                Map<Long, PersonDTO> map = personServiceAdapter.queryPersonsById(new Long[]{t.getPersonId()});
                PersonDTO personDTO = map.get(t.getPersonId());
                if (personDTO != null) {
                    userDetailVO.setPersonCode(personDTO.getCode());
                    userDetailVO.setPersonName(personDTO.getName());
                }
            }
            List<UserRoleBO> roles = userRoleService.getRole(t.getId());
            if (roles != null && !roles.isEmpty()) {
                List<Long> list = roles.stream().map(UserRoleBO::getRoleId).collect(Collectors.toList());
                List<RoleDTO> roleDTOS = rbacServiceAdapter.findRoleByIds(list);
                if (roleDTOS != null && !roleDTOS.isEmpty()) {
                    List<UserDetailVO.Role> roleList = roleDTOS.stream().map(roleDTO -> {
                        UserDetailVO.Role role = new UserDetailVO.Role();
                        role.setShowName(roleDTO.getName());
                        role.setName(roleDTO.getCode());
                        return role;
                    }).collect(Collectors.toList());
                    userDetailVO.setUserRoleList(roleList);
                }
            }
            return userDetailVO;
        }).collect(Collectors.toList());
        SuposPageResult<UserDetailVO> userDetailVOS = new SuposPageResult<>();
        userDetailVOS.setList(collect);
        SuposPagination suposPagination = new SuposPagination();
        suposPagination.setTotal(result.getTotal());
        suposPagination.setPageSize(result.getSize());
        suposPagination.setPageIndex(result.getCurrent());
        userDetailVOS.setPagination(suposPagination);
        return userDetailVOS;
    }

    @ApiOperation(value = "根据用户名查询用户详情")
    @GetMapping(value = "/users/{userName}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public UserDetailVO getUserInfo(@ApiParam(value = "用户名", required = true) @PathVariable("userName") String userName, HttpServletResponse response) {
        UserDetailVO userDetailVO = new UserDetailVO();
        UserBO userBO = userService.findByUserName(userName);
        if (StringUtils.isEmpty(userBO.getUserName())) {
            errorResponse(response, 400, 100106014, "用户不存在");
            return null;
        }
        userDetailVO.setUsername(userBO.getUserName());
        userDetailVO.setAccountType(userBO.getUserType());
        userDetailVO.setLockStatus(userBO.getHasLock() ? 1 : 0);
        userDetailVO.setUserDesc(userBO.getDescription());
        userDetailVO.setCreateTime(userBO.getCreateTime());
        userDetailVO.setModifyTime(userBO.getModifyTime());
//        userDetailVO.setFaceUrl(userBO.getFaceUrl());
        if (userBO.getPersonId() != null) {
            Map<Long, PersonDTO> map = personServiceAdapter.queryPersonsById(new Long[]{userBO.getPersonId()});
            PersonDTO personDTO = map.get(userBO.getPersonId());
            if (personDTO != null) {
                userDetailVO.setPersonCode(personDTO.getCode());
                userDetailVO.setPersonName(personDTO.getName());
//                userDetailVO.setEmail(personDTO.getEmail());
//                userDetailVO.setMobile(personDTO.getPhone());
            }
        }
        List<UserRoleBO> roles = userRoleService.getRole(userBO.getId());
        if (roles != null && !roles.isEmpty()) {
            List<Long> list = roles.stream().map(UserRoleBO::getRoleId).collect(Collectors.toList());
            List<RoleDTO> roleDTOS = rbacServiceAdapter.findRoleByIds(list);
            if (roleDTOS != null && !roleDTOS.isEmpty()) {
                List<UserDetailVO.Role> roleList = roleDTOS.stream().map(t -> {
                    UserDetailVO.Role role = new UserDetailVO.Role();
                    role.setShowName(t.getName());
                    role.setName(t.getCode());
                    return role;
                }).collect(Collectors.toList());
                userDetailVO.setUserRoleList(roleList);
            }
        }
        return userDetailVO;
    }

    @PutMapping("/users/{userName}")
    public void updateUserInfo(@PathVariable("userName") String userName, @RequestBody UpdateUserVO updateUserVO, HttpServletResponse response) {
        boolean verify = verify(updateUserVO, response);
        if (!verify) {
            return;
        }
        UserBO userBO = userService.findByUserName(userName);
        if (StringUtils.isEmpty(userBO.getUserName())) {
            errorResponse(response, 400, 100106014, "用户不存在");
            return;
        }
        UserBO updateBo = new UserBO();
        updateBo.setDescription(updateUserVO.getUserDesc());
        updateBo.setTimeZone(updateUserVO.getTimeZone());
        updateBo.setId(userBO.getId());
        if (updateUserVO.getLockStatus() != null) {
            updateBo.setHasLock(updateUserVO.getLockStatus() == 0 ? false : true);
        }
        if (StringUtils.isNotEmpty(updateUserVO.getPersonCode())) {
            ListResult<PersonDetailDTO> person = personServiceAdapter.queryPersonByCodes(Arrays.asList(updateUserVO.getPersonCode()));
            if (person.getList().isEmpty()) {
                errorResponse(response, 400, 100106012, updateUserVO.getPersonCode() + "对应人员不存在");
                return;
            } else {
                Optional<PersonDetailDTO> first = person.getList().stream().findFirst();
                if (first.isPresent()) {
                    updateBo.setPersonId(first.get().getId());
                    updateBo.setPersonName(first.get().getName());
                    updateBo.setPersonCode(first.get().getCode());
                }
            }
        }
        userService.updateUser(updateBo, null);
    }

    @DeleteMapping("/users")
    public void getUserInfo(@RequestParam(name = "usernames", required = false) List<String> usernames, HttpServletResponse response) {
        userService.batchDeletByNames(usernames, response);
    }


    @PostMapping("/users/{username}/role")
    public void bindUserRole(@PathVariable("username") String userName, @RequestBody UpdateUserRoleVO updateUserRoleVO, HttpServletResponse response) {
        UserBO userBO = userService.findByUserName(userName);
        if (StringUtils.isEmpty(userBO.getUserName())) {
            errorResponse(response, 400, 100106014, "用户不存在");
            return;
        }
        userService.openUpdateUser(userBO, updateUserRoleVO.getRoleCodes() == null ? new ArrayList<>() : updateUserRoleVO.getRoleCodes(), new ArrayList<>(), response);
    }

    @PutMapping("/users/{username}/role")
    public void unbindUserRole(@PathVariable("username") String userName, @RequestBody UpdateUserRoleVO updateUserRoleVO, HttpServletResponse response) {
        UserBO userBO = userService.findByUserName(userName);
        if (StringUtils.isEmpty(userBO.getUserName())) {
            errorResponse(response, 400, 100106014, "用户不存在");
            return;
        }
        userService.openUpdateUser(userBO, new ArrayList<>(), updateUserRoleVO.getRoleCodes() == null ? new ArrayList<>() : updateUserRoleVO.getRoleCodes(), response);
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

    private boolean verify(Object object, HttpServletResponse response) {
        if (object instanceof AddUserVO) {
            AddUserVO addUserVO = (AddUserVO) object;
            if (StringUtils.isEmpty(addUserVO.getUsername())) {
                errorResponse(response, 400, 100106500, "用户名必填");
                return false;
            } else {
                if (addUserVO.getUsername().length() > 50) {
                    errorResponse(response, 400, 100106500, "用户名最大50个字符");
                    return false;
                } else {
                    Matcher matcher = userNamePattern.matcher(addUserVO.getUsername());
                    if (!matcher.find()) {
                        errorResponse(response, 400, 100106500, "用户名支持字母、数字或下划线及其组合");
                        return false;
                    }
                }
            }
            if (StringUtils.isEmpty(addUserVO.getPassword())) {
                errorResponse(response, 400, 100106500, "密码必填");
                return false;
            }
            if (StringUtils.isEmpty(addUserVO.getPersonCode())) {
                errorResponse(response, 400, 100106500, "人员编码必填");
                return false;
            }
            if (StringUtils.isEmpty(addUserVO.getCompanyCode())) {
                errorResponse(response, 400, 100106500, "公司编码必填");
                return false;
            }
            if (StringUtils.isEmpty(addUserVO.getTimeZone())) {
                errorResponse(response, 400, 100106500, "时区必填");
                return false;
            } else {
                Matcher matcher = pattern.matcher(addUserVO.getTimeZone());
                if (!matcher.find()) {
                    errorResponse(response, 400, 100106500, "时区格式错误");
                    return false;
                }
            }
            if (StringUtils.isNotEmpty(addUserVO.getUserDesc())) {
                if (addUserVO.getUserDesc().length() > 255) {
                    errorResponse(response, 400, 100106500, "用户描述最长255个字符");
                    return false;
                }
            }
            if (addUserVO.getAccountType() == null) {
                errorResponse(response, 400, 100106500, "用户类型必填");
                return false;
            } else {
                if (addUserVO.getAccountType() != 0) {
                    errorResponse(response, 400, 100106500, "用户类型只能为0");
                    return false;
                }
            }
        }
        if (object instanceof UpdateUserVO) {
            UpdateUserVO updateUserVO = (UpdateUserVO) object;
            if (StringUtils.isNotEmpty(updateUserVO.getTimeZone())) {
                Matcher matcher = pattern.matcher(updateUserVO.getTimeZone());
                if (!matcher.find()) {
                    errorResponse(response, 400, 100106500, "时区格式错误");
                    return false;
                }
            }
            if (StringUtils.isNotEmpty(updateUserVO.getUserDesc())) {
                if (updateUserVO.getUserDesc().length() > 255) {
                    errorResponse(response, 400, 100106500, "用户描述最长255个字符");
                    return false;
                }
            }
            if (updateUserVO.getLockStatus() != null) {
                if (updateUserVO.getLockStatus() != 0 && updateUserVO.getLockStatus() != 1) {
                    errorResponse(response, 400, 100106500, "lockStatus值为0或者为1");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean verify(String companyCode, String current, Integer pageSize, HttpServletResponse response) {
        if (StringUtils.isEmpty(companyCode)) {
            errorResponse(response, 400, 100106500, "公司编码必填");
            return false;
        }
        if (StringUtils.isEmpty(current)) {
            errorResponse(response, 400, 100106500, "pageIndex必填");
            return false;
        } else {
            Matcher matcher = numberPattern.matcher(current);
            if (!matcher.find()) {
                errorResponse(response, 400, 100106500, "pageIndex至少从1开始");
                return false;
            }
        }
        if (pageSize > 500) {
            errorResponse(response, 400, 100106500, "pageSize超过最大值500");
            return false;
        } else if (pageSize < 1) {
            errorResponse(response, 400, 100106500, "pageSize小于最小值1");
            return false;
        }
        return true;
    }

    @GetMapping(value = "/users/{username}/persons", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "查询用户关联人员")
    public PersonDetailVO getPersonInfo(@ApiParam(value = "用户名", required = true) @PathVariable("username") String userName, HttpServletResponse response) {
        UserBO userBO = userService.findByUserName(userName);
        if (StringUtils.isEmpty(userBO.getUserName())) {
            errorResponse(response, 400, 100106014, "用户不存在");
            return null;
        }
        PersonDetailDTO personDetailDTO = new PersonDetailDTO();
        if (userBO.getPersonId() != null) {
            ListResult<PersonDetailDTO> personDetailDTOList = personServiceAdapter.queryPersonDetailByIds(Arrays.asList(userBO.getPersonId()));
            Collection<PersonDetailDTO> list = personDetailDTOList.getList();
            personDetailDTO = list.iterator().next();
        }
        PersonDetailVO personDetailVO = new PersonDetailVO();
        BeanUtils.copyProperties(personDetailDTO, personDetailVO);

        //性别
        SystemCodeVO genderVO = new SystemCodeVO();
        SystemCodeResultDTO gender = systemCodeServiceAdapter.getSystemCodeByCode(personDetailDTO.getGender());
        genderVO.setCode(gender.getCode());
        genderVO.setName(gender.getDisplayName());
        personDetailVO.setGender(genderVO);

        //状态
        SystemCodeVO statusVO = new SystemCodeVO();
        SystemCodeResultDTO status = systemCodeServiceAdapter.getSystemCodeByCode(personDetailDTO.getStatus());
        statusVO.setCode(status.getCode());
        statusVO.setName(status.getDisplayName());
        personDetailVO.setStatus(statusVO);

        return personDetailVO;
    }


    @ApiOperation("重置系统默认管理员密码")
    @PutMapping("/users/admin/password")
    public JSONObject resetAdminPassword(@RequestHeader(required = true, name = "Authorization") String authorization) {
        String newPasswd = userService.resetAdminPassword(authorization);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("password",newPasswd);
        return jsonObject;
    }

    @ApiOperation("解绑角色")
    @DeleteMapping("users/{username}/third/identity")
    public void unBindUserThridIdentitys(@PathVariable String username,@RequestParam(name = "identityIds", required = false) List<String> identityIds){
        userService.unBindUserThridIdentitys(username,identityIds);
    }

}
