package com.supcon.supfusion.auth.webapi.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.exception.UserErrorEnum;
import com.supcon.supfusion.auth.common.exception.UserException;
import com.supcon.supfusion.auth.common.useragent.UserAgent;
import com.supcon.supfusion.auth.common.useragent.UserAgentUtil;
import com.supcon.supfusion.auth.common.utils.BCryptUtil;
import com.supcon.supfusion.auth.common.utils.IpUtil;
import com.supcon.supfusion.auth.common.utils.UrlUtil;
import com.supcon.supfusion.auth.manager.PersonServiceAdapter;
import com.supcon.supfusion.auth.manager.RbacServiceAdapter;
import com.supcon.supfusion.auth.service.AuthLoginLogService;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.bo.LoginBO;
import com.supcon.supfusion.auth.service.bo.LoginResponseBO;
import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.auth.service.bo.UserRoleBO;
import com.supcon.supfusion.auth.service.bo.bap.BapUserInfoBO;
import com.supcon.supfusion.auth.service.cache.UserCache;
import com.supcon.supfusion.auth.webapi.result.UserInfoResult;
import com.supcon.supfusion.auth.webapi.result.UserSessinResult;
import com.supcon.supfusion.auth.webapi.vo.*;
import com.supcon.supfusion.auth.webapi.vo.bap.BapResultVO;
import com.supcon.supfusion.auth.webapi.vo.bap.BapUserInfoVO;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author lifangyuan
 */
@Slf4j
@RestController
@InternalApi(path = "/inter-api/auth")
@Api(value = "用户管理", tags = "用户管理")
public class UserController extends BaseController {

    private static final String pattern = "^(?![A-Za-z]+$)(?![A-Z0-9]+$)(?![a-z0-9]+$)(?![a-z\\W]+$)(?![A-Z\\W]+$)(?![0-9\\W]+$)[+a-zA-Z0-9\\W]{8,16}$";

    @Resource
    private UserService userService;
    @Resource
    private RbacServiceAdapter rbacServiceAdapter;
    @Resource
    private PersonServiceAdapter personServiceAdapter;
    @Resource
    private UserCache userCache;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private AuthLoginLogService loginLogService;


    @PostMapping("/v1/user")
    @ApiOperation(value = "创建用户", httpMethod = "POST")
    public void createUser(@Validated @RequestBody @ApiParam(name = "body", value = "json格式", required = true) UserAddVO vo) {
        UserBO userBO = new UserBO();
        BeanUtils.copyProperties(vo, userBO);
        userBO.setId(IDGenerator.newInstance().generate().longValue());
        List<UserRoleBO> userRoleBOS = buildRole(vo.getRole(), userBO);
        if (StringUtils.isEmpty(userBO.getTimeZone())) {
            userBO.setTimeZone(Constants.DEFAULT_TIME_ZONE);
        }
        userBO.setRoles(userRoleBOS);
        userBO.setCompanyId(UserContext.getUserContext().getCompanyId());
        userBO.setCompanyCode(UserContext.getUserContext().getCompanyCode());
        userService.creatUser(userBO, userRoleBOS);
    }

    private List<UserRoleBO> buildRole(List<RoleVO> roleVOS, UserBO userBO) {
        List<UserRoleBO> roleBOS = new ArrayList<>();
        Map<Long, UserRoleBO> map = new HashMap<>();
        if (roleVOS != null && !roleVOS.isEmpty()) {
            roleVOS.stream().filter(t -> !t.getType().equals(Constants.ROLE_ORG)).forEach(role -> {
                UserRoleBO userRoleBO = new UserRoleBO();
                userRoleBO.setUserId(userBO.getId());
                userRoleBO.setRoleId(role.getId());
                userRoleBO.setRoleType(Constants.ROLE_USER);
                userRoleBO.setRoleName(role.getName());
                map.put(role.getId(), userRoleBO);
                roleBOS.add(userRoleBO);
            });
        }

        if (userBO.getPersonId() != null) {
            ListResult<Long> orgRole = personServiceAdapter.queryRoleIdByPersonId(userBO.getPersonId());
            if (orgRole.getList() != null && !orgRole.getList().isEmpty()) {
                orgRole.getList().forEach(id -> {
                    UserRoleBO role = map.get(id);
                    if (role != null) {
                        role.setRoleType(Constants.ROLE_REPEATE);
                    } else {
                        UserRoleBO userRoleBO = new UserRoleBO();
                        userRoleBO.setUserId(userBO.getId());
                        userRoleBO.setRoleId(id);
                        userRoleBO.setRoleType(Constants.ROLE_ORG);
                        Map<Long, String> batchName = rbacServiceAdapter.findBatchName(String.valueOf(id));
                        userRoleBO.setRoleName(batchName.get(id));
                        roleBOS.add(userRoleBO);
                    }
                });
            }
        }
        return roleBOS;
    }


    @PutMapping("/v1/user")
    @ApiOperation(value = "修改用户", httpMethod = "PUT")
    public void updateUser(@Validated @RequestBody @ApiParam(name = "body", value = "json格式", required = true) UserUpdateVO vo) {
        UserBO userBO = new UserBO();
        BeanUtils.copyProperties(vo, userBO);
        List<UserRoleBO> userRoleBOS = buildRole(vo.getRole(), userBO);
        userService.updateUser(userBO, userRoleBOS);
    }

    @PutMapping("/v1/user/password")
    @ApiOperation(value = "修改密码", httpMethod = "PUT")
    public void updatePassword(@Validated @RequestBody @ApiParam(name = "body", value = "json格式", required = true) UserUpdateVO vo) {
        UserBO userBo = new UserBO();
        userBo.setId(vo.getId());
        userBo.setPassword(vo.getPassword());
        userBo.setLoginFirst(true);
        userService.updateUser(userBo, null);
    }

    @PutMapping("/v1/user/status")
    @ApiOperation(value = "修改锁定状态", httpMethod = "PUT")
    public void lockUser(@Validated @RequestBody @ApiParam(name = "body", value = "json格式", required = true) UserUpdateVO vo) {
        UserBO userBo = new UserBO();
        userBo.setId(vo.getId());
        userBo.setHasLock(vo.getLock());
        if (Optional.ofNullable(userBo.getHasLock()).orElse(false) && Objects.equals(userBo.getId(), UserContext.getUserContext().getUserId())) {
            throw new UserException(UserErrorEnum.CURRENT_USER_CANNOT_LOCK, UserContext.getUserContext().getUserName());
        }
        if (Optional.ofNullable(userBo.getHasLock()).orElse(false) && userBo.getId().compareTo(1L) == 0) {
            throw new UserException(UserErrorEnum.ADMIN_USER_CANNOT_LOCK, UserContext.getUserContext().getUserName());
        }
        userService.updateUser(userBo, null);
    }

    @DeleteMapping("/v1/user")
    @ApiOperation(value = "批量删除用户", httpMethod = "DELETE")
    public void batchDeleteUser(@Validated @RequestBody UserBatchDeletVO vo) {
        userService.batchDelet(vo.getIds());
    }

    @GetMapping("/v1/user/search/ref")
    @ApiOperation(value = "用户搜索", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keyword", value = "关键字搜索", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = true, dataType = "Integer", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量-默认10", required = true, dataType = "Integer", paramType = "query"),
    })
    public PageResult<UserDetailVO> searchUserRef(@RequestParam(value = "keyword", required = false) String keyword,
                                                  @RequestParam("current") @NotNull(message = "current must not null") Integer current, @RequestParam("pageSize") @NotNull(message = "pageSize must not null") Integer pageSize, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        return searchUser(keyword, current, pageSize, response);
    }

    @GetMapping("/v1/user/search")
    @ApiOperation(value = "用户搜索", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keyword", value = "关键字搜索", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = true, dataType = "Integer", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量-默认10", required = true, dataType = "Integer", paramType = "query"),
    })
    public PageResult<UserDetailVO> searchUser(@RequestParam(value = "keyword", required = false) String keyword,
                                               @RequestParam("current") @NotNull(message = "current must not null") Integer current, @RequestParam("pageSize") @NotNull(message = "pageSize must not null") Integer pageSize, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        Page<UserBO> search = userService.search(keyword, current, pageSize);
        List<UserDetailVO> userDetailVOS = search.getRecords().stream().map(t -> {
            UserDetailVO userDetailVO = new UserDetailVO();
            BeanUtils.copyProperties(t, userDetailVO);
            if (t.getRoles() != null && !t.getRoles().isEmpty()) {
                List<RoleVO> collect = t.getRoles().stream().map(role -> {
                    RoleVO roleVO = new RoleVO();
                    roleVO.setId(role.getId());
                    roleVO.setName(role.getRoleName());
                    roleVO.setType(role.getRoleType());
                    return roleVO;
                }).collect(Collectors.toList());
                userDetailVO.setRole(collect);
            }
            return userDetailVO;
        }).collect(Collectors.toList());
        return new PageResult<>(userDetailVOS, search.getTotal(), pageSize, current);

    }

    @GetMapping("/v1/user/ref")
    @ApiOperation(value = "用户列表", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "companyId", value = "公司id", required = false, dataType = "Long", paramType = "query"),
            @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = true, dataType = "Integer", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量-默认10", required = true, dataType = "Integer", paramType = "query"),
    })
    public PageResult<UserDetailVO> getUsersRef(@RequestParam(required = false) Long companyId, @RequestParam("current") @NotNull(message = "current must not null") Integer current, @RequestParam("pageSize") @NotNull(message = "pageSize must not null") Integer pageSize, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        return getUsers(companyId, current, pageSize, response);
    }

    @GetMapping("/v1/user")
    @ApiOperation(value = "用户列表", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "companyId", value = "公司id", required = false, dataType = "Long", paramType = "query"),
            @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = true, dataType = "Integer", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量-默认10", required = true, dataType = "Integer", paramType = "query"),
    })
    public PageResult<UserDetailVO> getUsers(@RequestParam(required = false) Long companyId, @RequestParam("current") @NotNull(message = "current must not null") Integer current, @RequestParam("pageSize") @NotNull(message = "pageSize must not null") Integer pageSize, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");

        Page<UserBO> page = new Page<>(current, pageSize);
        UserBO userBO = new UserBO();
        UserContext userContext = UserContext.getUserContext();
        Long userId = userContext.getUserId();
        UserBO bo = userService.getUserById(userId);
        Set<Long> ids = new HashSet<>();
        if (companyId == null) {
            ids.add(userContext.getCompanyId());
        } else {
            ids.add(companyId);
        }
        userBO.setCompany(ids);
        if (!bo.getCompanyId().equals(userContext.getCompanyId())) {
            userBO.setId(userId);
        }
        Page<UserBO> result = userService.searchUser(page, userBO);
        List<UserDetailVO> userDetailVOS = result.getRecords().stream().map(t -> {
            UserDetailVO userDetailVO = new UserDetailVO();
            BeanUtils.copyProperties(t, userDetailVO);
            getRoleInfo(t, userDetailVO);
            if (t.getPersonId() != null) {
                Long[] personIds = {t.getPersonId()};
                Map<Long, PersonDTO> longPersonDTOMap = personServiceAdapter.queryPersonsById(personIds);
                PersonDTO personDTO = longPersonDTOMap.get(t.getPersonId());
                if (personDTO != null) {
                    userDetailVO.setPersonName(personDTO.getName());
                    userDetailVO.setPersonCode(personDTO.getCode());
                }
            }
            return userDetailVO;
        }).collect(Collectors.toList());
        return new PageResult(userDetailVOS, result.getTotal(), result.getSize(), result.getCurrent());
    }

    private void getRoleInfo(UserBO userBO, UserDetailVO userDetailVO) {
        List<UserRoleBO> userRoleBOS = userService.batchGetByUserId(userBO.getId());
        if (userRoleBOS != null && !userRoleBOS.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            userRoleBOS.forEach(t -> builder.append(t.getRoleId()).append(","));
            String ids = builder.substring(0, builder.length() - 1);
            Map<Long, String> batchName = rbacServiceAdapter.findBatchName(ids);
            List<RoleVO> roleVOS = new ArrayList<>();
            userRoleBOS.forEach(roleDTO -> {
                if (!Constants.ROLE_ORG.equals(roleDTO.getRoleType())) {
                    RoleVO roleVO = new RoleVO();
                    roleVO.setId(roleDTO.getRoleId());
                    roleVO.setName(batchName.get(roleDTO.getRoleId()));
                    roleVO.setType(roleDTO.getRoleType());
                    roleVOS.add(roleVO);
                }
            });
            userDetailVO.setRole(roleVOS);
        }
    }

    @GetMapping("/v1/user/{id}")
    @ApiOperation(value = "用户详情", httpMethod = "GET")
    public Result<UserDetailVO> getUsersDetail(@ApiParam(name = "id", value = "用户id", required = true) @PathVariable("id") Long id, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        UserBO userBo = userService.getUserById(id);
        if (userBo == null) {
            throw new UserException(UserErrorEnum.USER_NOT_EXIST);
        } else {
            UserDetailVO userDetailVO = new UserDetailVO();
            BeanUtils.copyProperties(userBo, userDetailVO);
            getRoleInfo(userBo, userDetailVO);
            if (userBo.getPersonId() != null) {
                Long[] ids = {userBo.getPersonId()};
                Map<Long, PersonDTO> persons = personServiceAdapter.queryPersonsById(ids);
                PersonDTO personDTO = persons.get(userBo.getPersonId());
                if (personDTO != null) {
                    userDetailVO.setPersonName(personDTO.getName());
                }
            }
            return new Result<>(userDetailVO);
        }
    }

    @GetMapping("/v1/currentuser")
    public UserInfoResult<UserRoleStaffVO> getUsersDetailByUserName(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        Long userId = UserContext.getUserContext().getUserId();
        UserBO userBo = userService.getUserById(userId);
        if (userBo == null) {
            throw new UserException(UserErrorEnum.USER_NOT_EXIST);
        } else {
            UserRoleStaffVO userRoleStaffVO = new UserRoleStaffVO();
            userRoleStaffVO.setCid(UserContext.getUserContext().getCompanyId());
            userRoleStaffVO.setUsername(userBo.getUserName());
            userRoleStaffVO.setTimeZone(userBo.getTimeZone());
            userRoleStaffVO.setLockStatus(userBo.getHasLock() ? 1 : 0);
            userRoleStaffVO.setNeedChangePassword(userBo.getLoginFirst() ? 1 : 0);
            userRoleStaffVO.setUserType(userBo.getUserType());
            if (StringUtils.isNotEmpty(userBo.getThirdIdentity())) {
                userRoleStaffVO.setThirdIdentity(userBo.getThirdIdentity());
            }
            if (StringUtils.isNotEmpty(userBo.getThirdSource())) {
                userRoleStaffVO.setThirdSource(userBo.getThirdSource());
            }
            if (StringUtils.isNotBlank(userBo.getFaceUrl())) {
                userRoleStaffVO.setUploadUrl(userBo.getFaceUrl());
            }
            getRoleInfo(userBo, userRoleStaffVO);
            if (userBo.getPersonId() != null) {
                Long[] ids = {userBo.getPersonId()};
                Map<Long, PersonDTO> persons = personServiceAdapter.queryPersonsById(ids);
                PersonDTO personDTO = persons.get(userBo.getPersonId());
                if (personDTO != null) {
                    userRoleStaffVO.setStaffName(personDTO.getName());
                    userRoleStaffVO.setStaffId(personDTO.getId());
                    userRoleStaffVO.setStaffCode(personDTO.getCode());
                    userRoleStaffVO.setEmail(personDTO.getEmail());
                    userRoleStaffVO.setPhone(personDTO.getPhone());
                }
            }
            return new UserInfoResult<>(userRoleStaffVO);
        }
    }


    @PutMapping("/v1/currentuser")
    public Result<Boolean> updateCurrentUser(@Validated @RequestBody CurrentUserUpdateVO vo, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        UserContext userContext = UserContext.getUserContext();
        Long userId = userContext.getUserId();
        UserBO userBO = userService.getUserById(userId);
        if (StringUtils.isNotBlank(vo.getUploadUrl())) {
            UserBO temp = new UserBO();
            temp.setFaceUrl(vo.getUploadUrl());
            temp.setId(userBO.getId());
            userService.updateUser(temp, null);
        }
        if (StringUtils.isNotEmpty(vo.getEmail()) || StringUtils.isNotEmpty(vo.getPhone())) {
            userService.updateEmailOrPhone(userBO.getPersonId(), vo.getEmail(), vo.getPhone());
        }
        return new Result<>(true);
    }

    @PutMapping(path = "/v1/currentuser/password")
    @ResponseStatus(HttpStatus.OK)
    public Result<Boolean> updateCurrentUserPassword(@Validated @RequestBody CurrentUserPasswordUpdateVO vo) {
        userService.modifyCurrUserPassword(vo.getPassword(), vo.getRepassword(), vo.getPrepassword());
        return new Result<>(true);
    }

    @PutMapping("/v1/currentuser/password/reset")
    public Result<Boolean> updateCurrentUserPasswordReset() {
        UserContext userContext = UserContext.getUserContext();
        Long userId = userContext.getUserId();
        UserBO userBO = userService.getUserById(userId);
        String encryPassword = BCryptUtil.encode(Constants.DEFAULT_USER);
        userBO.setPassword(encryPassword);
        userService.updateUser(userBO, null);
        return new Result<>(true);
    }


    @GetMapping("/v1/user/userSessionInfo")
    @ApiOperation(value = "用户会话", httpMethod = "GET")
    public UserSessinResult<UserStaffVO> getUserSessionInfo(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        UserContext userContext = UserContext.getUserContext();
        Long userId = userContext.getUserId();
        UserBO userBo = userService.getUserById(userId);
        if (userBo == null) {
            throw new UserException(UserErrorEnum.USER_NOT_EXIST);
        } else {
            UserStaffVO userStaffVO = new UserStaffVO();
            userStaffVO.setUserId(userBo.getId());
            userStaffVO.setUsername(userBo.getUserName());
            userStaffVO.setCompanyCode(userContext.getCompanyCode());
            userStaffVO.setCompanyId(userContext.getCompanyId());
            userStaffVO.setCompanyName(userContext.getCompanyName());
            if (userBo.getPersonId() != null) {
                Long personId = userBo.getPersonId();
                log.info("" + personId);
                Long[] ids = {personId};
                Map<Long, PersonDTO> persons = personServiceAdapter.queryPersonsById(ids);
                PersonDTO personDTO = persons.get(userBo.getPersonId());
                userStaffVO.setStaffName(personDTO.getName());
                userStaffVO.setStaffId(personDTO.getId());
                userStaffVO.setStaffCode(personDTO.getCode());
            }
            return new UserSessinResult<>(userStaffVO);
        }
    }

    private void getRoleInfo(UserBO userBO, UserRoleStaffVO userRoleStaffVO) {
        List<UserRoleBO> userRoleBOS = userService.batchGetByUserId(userBO.getId());
        List<Long> ids = userRoleBOS.stream().map(UserRoleBO::getRoleId).collect(Collectors.toList());
        StringBuilder builder = new StringBuilder();
        if (!ids.isEmpty()) {
            List<RoleDTO> roleDTOS = rbacServiceAdapter.findRoleByIds(ids);
            List<UserRoleStaffVO.Role> roleVOS = new ArrayList<>();
            if (roleDTOS != null && !roleDTOS.isEmpty()) {
                for (RoleDTO roleDTO : roleDTOS) {
                    UserRoleStaffVO.Role role = new UserRoleStaffVO.Role();
                    role.setName(roleDTO.getName());
                    role.setShowName(roleDTO.getName());
                    role.setCid(roleDTO.getCid());
                    role.setRoleId(roleDTO.getId());
                    role.setUnderControlled(false);
                    role.setCreateTime(new Date());
                    roleVOS.add(role);
                }
            }
            userRoleStaffVO.setUserRoleList(roleVOS);
        }
    }

    @PostMapping("/v1/app/user")
    @ResponseStatus(HttpStatus.OK)
    public void createAppUser(@Validated @RequestBody UserAddAppVO vo) {
//        UserBO userBO = new UserBO();
//        BeanUtils.copyProperties(vo, userBO);
//        userBO.setId(IDGenerator.newInstance().generate().longValue());
//        if (StringUtils.isEmpty(userBO.getTimeZone())) {
//            userBO.setTimeZone(String.valueOf(TimeZone.getDefault().getRawOffset()));
//        }
//        String encodePassword = Optional.ofNullable(userBO.getPassword())
//                .map(BCryptUtil::encode)
//                .orElse(BCryptUtil.encode(Constants.DEFAULT_PASSWORD));
//        userBO.setPassword(encodePassword);
//        userBO.setCompanyId(UserContext.getUserContext().getCompanyId());
//        userService.creatUser(userBO, null);
////        userCache.update(userBO);
    }

    @ApiOperation(value = "根据id查询用户接口", httpMethod = "GET")
    @GetMapping(path = "/v1/user/common/get", produces = "application/json;charset=UTF-8")
    public String getUser(@ApiParam(name = "id", value = "用户id", required = true) @RequestParam(value = "id") @NotNull(message = "id不能为空") Long id,
                          @ApiParam(name = "includes", value = "返回对象包含的用户的字段，以逗号隔开。支持\".\"查询嵌套对象字段", required = true) @NotNull(message = "includes不能为空") @RequestParam("includes") String includes) {
        return "{\"code\":200,\"success\":true,\"data\":" + userService.getBapUserById(id, includes) + ",\"msg\":\"操作成功\"}";
    }

    @ApiOperation(value = "当前登陆人信息接口", httpMethod = "GET")
    @GetMapping("/v1/getCurrentLoginInfo")
    public BapResultVO<BapUserInfoVO> getCurrentLoginInfo() {
        BapUserInfoBO bapUserInfoBO = userService.getCurrentLoginInfo();
        BapUserInfoVO bapUserInfoVO = new BapUserInfoVO();
        BapUserInfoBO.Info staffBO = bapUserInfoBO.getStaff();
        if (staffBO != null) {
            BapUserInfoVO.Info staffVO = new BapUserInfoVO.Info();
            BeanUtils.copyProperties(staffBO, staffVO);
            bapUserInfoVO.setStaff(staffVO);
        }
        BapUserInfoBO.Info companyBO = bapUserInfoBO.getCompany();
        if (companyBO != null) {
            BapUserInfoVO.Info companyVO = new BapUserInfoVO.Info();
            BeanUtils.copyProperties(companyBO, companyVO);
            bapUserInfoVO.setCompany(companyVO);
        }
        BapUserInfoBO.Info departmentBO = bapUserInfoBO.getDepartment();
        if (departmentBO != null) {
            BapUserInfoVO.Info departmentVO = new BapUserInfoVO.Info();
            BeanUtils.copyProperties(departmentBO, departmentVO);
            bapUserInfoVO.setDepartment(departmentVO);
        }
        BapUserInfoBO.Info userBO = bapUserInfoBO.getUser();
        if (userBO != null) {
            BapUserInfoVO.Info userVO = new BapUserInfoVO.Info();
            BeanUtils.copyProperties(userBO, userVO);
            bapUserInfoVO.setUser(userVO);
        }
        BapUserInfoBO.Info mainPositionBO = bapUserInfoBO.getMainPosition();
        if (mainPositionBO != null) {
            BapUserInfoVO.Info mainPositionVO = new BapUserInfoVO.Info();
            BeanUtils.copyProperties(mainPositionBO, mainPositionVO);
            bapUserInfoVO.setMainPosition(mainPositionVO);
        }
        BapResultVO<BapUserInfoVO> result = new BapResultVO<>();
        result.setCode(200)
                .setSuccess(true)
                .setMsg("操作成功")
                .setData(bapUserInfoVO);
        return result;
    }

    @ApiOperation(value = "获取当前登录IP接口", httpMethod = "GET")
    @GetMapping("/v1/getCurrentIp")
    public BapResultVO<String> getCurrentIp(HttpServletRequest request) {
        String ipAddr = IpUtil.getIpAddr(request);
        BapResultVO<String> result = new BapResultVO<>();
        result.setCode(200)
                .setSuccess(true)
                .setMsg("操作成功")
                .setData(ipAddr);
        return result;
    }

    @ApiOperation(value = "获取当前登录人语言", httpMethod = "GET")
    @GetMapping("/v1/getCurrentLanguage")
    public BapResultVO<String> getCurrentLanguage(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        if (header.startsWith("zh")) {
            header = "zh_CN";
        } else if (header.startsWith("en")) {
            header = "en_US";
        }
        BapResultVO<String> result = new BapResultVO<>();
        result.setCode(200)
                .setSuccess(true)
                .setMsg("操作成功")
                .setData(header);
        return result;
    }

    @PostMapping(value = "/v1/user/login", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String login(@Validated @RequestBody LoginVO loginVO,
                        @RequestHeader("X-Real-IP") String realIp,
                        @RequestHeader(HttpHeaders.USER_AGENT) String userAgent,
                        @RequestHeader(name = HttpHeaders.REFERER, required = false) String referer,
                        @CookieValue(name = "KC_RESTART", required = false) String kcRestart,
                        @RequestHeader(value = "x-ticket", required = false) String ticket,
                        HttpServletResponse response) throws IOException {
        UserAgent userAgentInfo = UserAgentUtil.parse(userAgent);
        String quatoName = "";
        String deviceType = "";
        if (userAgentInfo.isMobile()) {
            quatoName = "MAX_MOBILE_LOGIN";
            deviceType = "mobile";
        } else {
            quatoName = "MAX_PC_LOGIN";
            deviceType = "pc";
        }
        LoginBO loginBO = new LoginBO();
        BeanUtils.copyProperties(loginVO, loginBO);
        loginBO.setGrantType("password");
        String headBranchOffice = UrlUtil.getQueryParam(referer, Constants.HEAD_BRANCH_OFFICE_CODE);
        LoginResponseBO login = userService.login(loginBO, realIp, quatoName, deviceType,  StringUtils.isEmpty(headBranchOffice) ? kcRestart : headBranchOffice, ticket);
        LoginResponseVO loginResponseVO = new LoginResponseVO();
        BeanUtils.copyProperties(login, loginResponseVO);
        ResponseCookie cookie = ResponseCookie.from("suposTicket", loginResponseVO.getTicket()) // key & value
                .httpOnly(true)		// 禁止js读取
                .secure(false)		// 在http下也传输
                .path("/")			// path
                .sameSite("Lax")	// 大多数情况也是不发送第三方 Cookie，但是导航到目标网址的 Get 请求除外
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        if (StringUtils.isNotEmpty(login.getRedirectUri())) {
            String redirectUri = login.getRedirectUri() + "?code=" + login.getCode() + "&state=" + login.getState();
            if (StringUtils.isEmpty(headBranchOffice)) {
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.sendRedirect(redirectUri);
            } else {
                loginResponseVO.setRedirectUri(redirectUri);
                loginResponseVO.setLoginType("sso");
                // 维护ticket和code的关联关系
                String key = String.format(Constants.HEAD_AUTH_TICKET_CODE_SET, loginResponseVO.getTicket());
                stringRedisTemplate.opsForSet().add(key, login.getCode());
                stringRedisTemplate.expire(key, 1, TimeUnit.DAYS);
            }
        }
        return JSON.toJSONString(loginResponseVO, SerializerFeature.DisableCircularReferenceDetect);
    }

    @PutMapping(value = "/v1/user/logout", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void logout(HttpServletRequest request) {
        String ticket = null;
        String authorization = request.getHeader("X-Ticket");
        if (StringUtils.isNotEmpty(authorization)) {
            ticket = authorization;
        } else {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("suposTicket")) {
                        ticket = cookie.getValue();
                        break;
                    }
                }
            }
        }
        if (StringUtils.isNotEmpty(ticket)) {
            userService.logout(ticket);
        }
    }

    @PutMapping(value = "/v1/company/change", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String companyChange(@RequestBody CompanyChangeVO companyChangeVO, @RequestHeader("x-ticket") String ticket) {
        LoginResponseBO responseBO = userService.companyChange(companyChangeVO.getCompanyId(), ticket);
        LoginResponseVO loginResponseVO = new LoginResponseVO();
        BeanUtils.copyProperties(responseBO, loginResponseVO);
        return JSON.toJSONString(loginResponseVO, SerializerFeature.DisableCircularReferenceDetect);
    }


    @PutMapping(value = "/v1/token/refresh")
    public void refreshToken(@RequestHeader("x-ticket") String ticket) {
        userService.refreshToken(ticket, getResponse());
    }

}
