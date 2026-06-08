package com.supcon.supfusion.auth.openapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.auth.manager.PersonServiceAdapter;
import com.supcon.supfusion.auth.manager.RbacServiceAdapter;
import com.supcon.supfusion.auth.openapi.suposvo.*;
import com.supcon.supfusion.auth.service.SuposUserService;
import com.supcon.supfusion.auth.service.UserRoleService;
import com.supcon.supfusion.auth.service.bo.RoleDetailBO;
import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.auth.service.bo.UserRoleBO;
import com.supcon.supfusion.auth.service.cache.UserCache;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestController
@OpenApi(path = "/open-api/supos/auth/v2")
@Validated
public class SuposOpenUserController {

    @Resource
    private SuposUserService suposUserService;

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

    private static Pattern emailPattern = Pattern.compile("^[0-9a-z]+\\w*@([0-9a-z]+\\.)+[0-9a-z]+$");


    @PostMapping("/user")
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
        userBO.setCompanyId(1000L);
        userBO.setUserType(addUserVO.getAccountType());
        userBO.setId(IDGenerator.newInstance().generate().longValue());
        if (addUserVO.getRoleNameList() != null) {
            List<UserRoleBO> userRoleBOS = addUserVO.getRoleNameList().stream().map(t -> {
                UserRoleBO userRoleBO = new UserRoleBO();
                userRoleBO.setRoleCode(t);
                return userRoleBO;
            }).collect(Collectors.toList());
            userBO.setRoles(userRoleBOS);
        }
        suposUserService.creatOpenUser(userBO, addUserVO.getEmail(), response);
    }

    @GetMapping("/users")
    public SuposPageResult<UserDetailVO> getUsers(@RequestParam(name = "keyword", required = false) String keyword, @RequestParam(name = "pageIndex", defaultValue = "1", required = false) Integer pageIndex, @RequestParam(name = "pageSize", defaultValue = "20", required = false) Integer pageSize, HttpServletResponse response) {
        if (pageIndex < 1) {
            errorResponse(response, 400, 200003, "pageIndex从1开始");
            return null;
        }
        if (pageSize > 100) {
            errorResponse(response, 400, 200003, "pageSize最大100");
            return null;
        }
        if (pageSize < 0) {
            errorResponse(response, 400, 200003, "pageSize最小1");
            return null;
        }
        Page<UserBO> page = new Page<>(pageIndex, pageSize);
        Page<UserBO> userBOPage = suposUserService.openGetUsers(page, keyword, response);
        List<UserDetailVO> result = userBOPage.getRecords().stream().map(t -> {
            UserDetailVO userDetailVO = new UserDetailVO();
            userDetailVO.setUserDesc(t.getDescription());
            userDetailVO.setUsername(t.getUserName());
            if (t.getPersonId() != null) {
                Map<Long, PersonDTO> map = personServiceAdapter.queryPersonsById(new Long[]{t.getPersonId()});
                PersonDTO personDTO = map.get(t.getPersonId());
                if (personDTO != null) {
                    userDetailVO.setEmail(personDTO.getEmail());
                }
            }
            List<UserRoleBO> roles = userRoleService.getRole(t.getId());
            if (roles != null && !roles.isEmpty()) {
                List<Long> list = roles.stream().map(UserRoleBO::getRoleId).collect(Collectors.toList());
                List<RoleDTO> roleDTOS = rbacServiceAdapter.findRoleByIds(list);
                if (roleDTOS != null && !roleDTOS.isEmpty()) {
                    List<UserDetailVO.Role> roleList = roleDTOS.stream().map(roleDTO -> {
                        UserDetailVO.Role role = new UserDetailVO.Role();
                        role.setName(roleDTO.getCode());
                        role.setShowName(roleDTO.getName());
                        role.setDescription(roleDTO.getDescription());
                        role.setCreateTime(roleDTO.getCreateTime());
                        role.setModifyTime(roleDTO.getModifyTime());
                        role.setCreateUsername(roleDTO.getCreator());
                        role.setModifyUsername(roleDTO.getModifier());
                        return role;
                    }).collect(Collectors.toList());
                    userDetailVO.setUserRoleList(roleList);
                }
            }
            return userDetailVO;
        }).collect(Collectors.toList());
        SuposPagination suposPagination = new SuposPagination();
        suposPagination.setPageSize(userBOPage.getSize());
        suposPagination.setTotal(userBOPage.getTotal());
        suposPagination.setPageIndex(userBOPage.getCurrent());
        SuposPageResult<UserDetailVO> resultPage = new SuposPageResult<>();
        resultPage.setPagination(suposPagination);
        resultPage.setList(result);
        return resultPage;
    }

    @GetMapping(value = "/users/{username}")
    public UserDetailVO getUserInfo(@PathVariable("username") String userName, HttpServletResponse response) {
        UserBO user = suposUserService.getUserInfo(userName, response);
        if (user != null) {
            UserDetailVO userDetailVO = new UserDetailVO();
            userDetailVO.setUserDesc(user.getDescription());
            userDetailVO.setUsername(user.getUserName());
            if (user.getPersonId() != null) {
                Map<Long, PersonDTO> map = personServiceAdapter.queryPersonsById(new Long[]{user.getPersonId()});
                PersonDTO personDTO = map.get(user.getPersonId());
                if (personDTO != null) {
                    userDetailVO.setEmail(personDTO.getEmail());
                }
            }
            List<UserRoleBO> roles = userRoleService.getRole(user.getId());
            if (roles != null && !roles.isEmpty()) {
                List<Long> list = roles.stream().map(UserRoleBO::getRoleId).collect(Collectors.toList());
                List<RoleDTO> roleDTOS = rbacServiceAdapter.findRoleByIds(list);
                if (roleDTOS != null && !roleDTOS.isEmpty()) {
                    List<UserDetailVO.Role> roleList = roleDTOS.stream().map(roleDTO -> {
                        UserDetailVO.Role role = new UserDetailVO.Role();
                        role.setName(roleDTO.getCode());
                        role.setShowName(roleDTO.getName());
                        role.setDescription(roleDTO.getDescription());
                        role.setCreateTime(roleDTO.getCreateTime());
                        role.setModifyTime(roleDTO.getModifyTime());
                        role.setCreateUsername(roleDTO.getCreator());
                        role.setModifyUsername(roleDTO.getModifier());
                        return role;
                    }).collect(Collectors.toList());
                    userDetailVO.setUserRoleList(roleList);
                }
            }
            return userDetailVO;
        } else {
            return new UserDetailVO();
        }
    }

    @PutMapping(value = "/user/{username}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void updateUserInfo(@PathVariable("username") String username, @RequestBody UpdateUserVO updateUserVO, HttpServletResponse response) {
        if (!verify(updateUserVO, response)) {
            return;
        }
        suposUserService.updateUserInfo(username, updateUserVO.getTimeZone(), updateUserVO.getUserDesc(), updateUserVO.getEmail(), updateUserVO.getRoleNameList(), response);
    }


    @PostMapping(value = "/delete/users", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void deleteUsers(@RequestBody DeleteUsersVO deleteUsersVO, HttpServletResponse response) {
        suposUserService.deleteUsers(deleteUsersVO.getList());
    }

    @GetMapping(value = "/users/queryUsersByName/{name}")
    public ListResult<UserDetailVO> getUsersByRoleCode(@PathVariable("name") String roleCode, HttpServletResponse response) {
        List<UserBO> userBOS = suposUserService.openGetUsersByRoleCode(roleCode, response);
        List<UserDetailVO> result = userBOS.stream().map(t -> {
            UserDetailVO userDetailVO = new UserDetailVO();
            userDetailVO.setUserDesc(t.getDescription());
            userDetailVO.setUsername(t.getUserName());
            if (t.getPersonId() != null) {
                Map<Long, PersonDTO> map = personServiceAdapter.queryPersonsById(new Long[]{t.getPersonId()});
                PersonDTO personDTO = map.get(t.getPersonId());
                if (personDTO != null) {
                    userDetailVO.setEmail(personDTO.getEmail());
                }
            }
            List<UserRoleBO> roles = userRoleService.getRole(t.getId());
            if (roles != null && !roles.isEmpty()) {
                List<Long> list = roles.stream().map(UserRoleBO::getRoleId).collect(Collectors.toList());
                List<RoleDTO> roleDTOS = rbacServiceAdapter.findRoleByIds(list);
                if (roleDTOS != null && !roleDTOS.isEmpty()) {
                    List<UserDetailVO.Role> roleList = roleDTOS.stream().map(roleDTO -> {
                        UserDetailVO.Role role = new UserDetailVO.Role();
                        role.setName(roleDTO.getCode());
                        role.setShowName(roleDTO.getName());
                        role.setDescription(roleDTO.getDescription());
                        role.setCreateTime(roleDTO.getCreateTime());
                        role.setModifyTime(roleDTO.getModifyTime());
                        role.setCreateUsername(roleDTO.getCreator());
                        role.setModifyUsername(roleDTO.getModifier());
                        return role;
                    }).collect(Collectors.toList());
                    userDetailVO.setUserRoleList(roleList);
                }
            }
            return userDetailVO;
        }).collect(Collectors.toList());
        return new ListResult<>(result);
    }

    @GetMapping("/roles")
    public SuposPageResult<RoleDetailVO> getRoles(@RequestParam(name = "keyword", required = false) String keyword, @RequestParam(name = "pageIndex", defaultValue = "1", required = false) Integer pageIndex, @RequestParam(name = "pageSize", defaultValue = "20", required = false) Integer pageSize, HttpServletResponse response) {
        if (pageIndex < 1) {
            errorResponse(response, 400, 200003, "pageIndex从1开始");
            return null;
        }
        if (pageSize > 100) {
            errorResponse(response, 400, 200003, "pageSize最大100");
            return null;
        }
        if (pageSize < 0) {
            errorResponse(response, 400, 200003, "pageSize最小1");
            return null;
        }
        Page<RoleDetailBO> roles = suposUserService.getRoles(pageIndex, pageSize, keyword, response);
        SuposPagination suposPagination = new SuposPagination();
        suposPagination.setPageSize(roles.getSize());
        suposPagination.setTotal(roles.getTotal());
        suposPagination.setPageIndex(roles.getCurrent());
        SuposPageResult<RoleDetailVO> suposPageResult = new SuposPageResult<>();
        suposPageResult.setPagination(suposPagination);
        List<RoleDetailVO> result = roles.getRecords().stream().map(t -> {
            RoleDetailVO roleDetailVO = new RoleDetailVO();
            BeanUtils.copyProperties(t, roleDetailVO);
            return roleDetailVO;
        }).collect(Collectors.toList());
        suposPageResult.setList(result);
        return suposPageResult;
    }

    @PostMapping("/roles")
    public void addRole(@RequestBody AddRoleVO addRoleVO, HttpServletResponse response) {
        suposUserService.addRole(1000L, addRoleVO.getName(), addRoleVO.getShowName(), addRoleVO.getDescription());
    }

    @PutMapping("/roles/{name}")
    public void updateRole(@PathVariable("name") String code, @RequestBody UpdateRoleVO updateRoleVO, HttpServletResponse response) {
        suposUserService.update(code, updateRoleVO.getShowName(), updateRoleVO.getDescription());
    }

    @GetMapping("/roles/{name}")
    public RoleDetailVO getRoleDetail(@PathVariable("name") String name, HttpServletResponse response) {
        RoleDetailVO roleDetailVO = new RoleDetailVO();
        RoleDetailBO roleDetail = suposUserService.getRoleDetail(name);
        if (roleDetail == null) {
            errorResponse(response, 400, 200005, "未找到对应数据");
            return null;
        }
        BeanUtils.copyProperties(roleDetail, roleDetailVO);
        return roleDetailVO;
    }

    @PostMapping("/delete/roles")
    public void deleteRoles(@RequestBody DeleteRolesVO vo, HttpServletResponse response) {
        suposUserService.deleteRoles(vo.getList());
    }


    private boolean verify(Object object, HttpServletResponse response) {
        if (object instanceof AddUserVO) {
            AddUserVO addUserVO = (AddUserVO) object;
            if (StringUtils.isEmpty(addUserVO.getUsername())) {
                errorResponse(response, 400, 200004, "用户名必填");
                return false;
            } else {
                if (addUserVO.getUsername().length() > 50) {
                    errorResponse(response, 400, 200003, "用户名最大50个字符");
                    return false;
                } else {
                    Matcher matcher = userNamePattern.matcher(addUserVO.getUsername());
                    if (!matcher.find()) {
                        errorResponse(response, 400, 200003, "用户名支持字母、数字或下划线及其组合");
                        return false;
                    }
                }
            }
            if (StringUtils.isEmpty(addUserVO.getPassword())) {
                errorResponse(response, 400, 200004, "密码必填");
                return false;
            }
            if (!StringUtils.isEmpty(addUserVO.getTimeZone())) {
                Matcher matcher = pattern.matcher(addUserVO.getTimeZone());
                if (!matcher.find()) {
                    errorResponse(response, 400, 200003, "时区格式错误");
                    return false;
                }
            }
            if (StringUtils.isNotEmpty(addUserVO.getUserDesc())) {
                if (addUserVO.getUserDesc().length() > 255) {
                    errorResponse(response, 400, 200003, "用户描述最长255个字符");
                    return false;
                }
            }

            if (addUserVO.getAccountType() != 0) {
                errorResponse(response, 400, 200003, "用户类型只能为0");
                return false;
            }

            if (StringUtils.isNotEmpty(addUserVO.getEmail())) {
                Matcher matcher = emailPattern.matcher(addUserVO.getEmail());
                if (!matcher.find()) {
                    errorResponse(response, 400, 200003, "邮箱格式错误");
                    return false;
                }
            }
        }
        if (object instanceof UpdateUserVO) {
            UpdateUserVO updateUserVO = (UpdateUserVO) object;
            if (StringUtils.isNotEmpty(updateUserVO.getTimeZone())) {
                Matcher matcher = pattern.matcher(updateUserVO.getTimeZone());
                if (!matcher.find()) {
                    errorResponse(response, 400, 200003, "时区格式错误");
                    return false;
                }
            }
            if (StringUtils.isNotEmpty(updateUserVO.getEmail())) {
                Matcher matcher = emailPattern.matcher(updateUserVO.getEmail());
                if (!matcher.find()) {
                    errorResponse(response, 400, 200003, "邮箱格式错误");
                    return false;
                }
            }
            if (StringUtils.isNotEmpty(updateUserVO.getUserDesc())) {
                if (updateUserVO.getUserDesc().length() > 255) {
                    errorResponse(response, 400, 200003, "用户描述最长255个字符");
                    return false;
                }
            }
        }
        return true;
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
}
