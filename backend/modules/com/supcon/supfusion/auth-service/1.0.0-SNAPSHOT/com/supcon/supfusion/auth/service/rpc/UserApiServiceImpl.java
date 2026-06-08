package com.supcon.supfusion.auth.service.rpc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.supcon.supfusion.auth.api.UserApiService;
import com.supcon.supfusion.auth.api.dto.*;
import com.supcon.supfusion.auth.api.result.UserSessinResult;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.constants.UserTypeEnum;
import com.supcon.supfusion.auth.common.exception.UserErrorEnum;
import com.supcon.supfusion.auth.common.exception.UserException;
import com.supcon.supfusion.auth.common.utils.BCryptUtil;
import com.supcon.supfusion.auth.common.utils.ThreadPoolUtils;
import com.supcon.supfusion.auth.dao.mapper.UserMapper;
import com.supcon.supfusion.auth.dao.po.UserPO;
import com.supcon.supfusion.auth.dao.po.UserRolePO;
import com.supcon.supfusion.auth.manager.RbacServiceAdapter;
import com.supcon.supfusion.auth.service.UserRoleService;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.auth.service.bo.UserRoleBO;
import com.supcon.supfusion.auth.service.cache.AuthTicketCache;
import com.supcon.supfusion.auth.service.cache.UserCache;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.exception.BizErrorEnum;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.framework.cloud.security.pojo.JwtUser;
import com.supcon.supfusion.framework.cloud.security.util.JwtTokenUtil;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.CompanyDTO;
import com.supcon.supfusion.organization.api.dto.*;
import com.supcon.supfusion.rbac.api.dto.RoleFRDTO;
import com.supcon.supfusion.rbac.api.dto.RoleUserFRDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.supcon.supfusion.auth.common.constants.Constants.ACCESS_TOKEN;
import static com.supcon.supfusion.auth.common.exception.UserErrorEnum.USER_ROLE_NOT_DELETE;

/**
 * @author lifangyuan
 */
@ServiceApiService
@Slf4j
public class UserApiServiceImpl extends BaseController implements UserApiService {
    private static final String pattern = "^(?![A-Za-z]+$)(?![A-Z0-9]+$)(?![a-z0-9]+$)(?![a-z\\W]+$)(?![A-Z\\W]+$)(?![0-9\\W]+$)[a-zA-Z0-9\\W]{8,16}$";
    private static final String userNamepattern = "^\\w+$";

    /**
     * 默认IN的最大值
     */
    private static final int MAX_IN_SIZE = 500;
    @Resource
    private UserService userService;
    @Resource
    private PersonApiService personApiService;

    @Resource
    private RbacServiceAdapter rbacServiceAdapter;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserRoleService userRoleService;
    @Resource
    private UserCache userCache;

    @Resource
    private JwtTokenUtil jwtTokenUtil;


    @Resource
    private AuthTicketCache authTicketCache;


    @Override
    public Result<String> createUser(BatchInsertDTO batchInsertDTO) {
        ArrayList<UserRoleBO> temp = new ArrayList<>();
        List<UserAddDTO> users = batchInsertDTO.getUsers();
        if (users == null || users.isEmpty()) {
            return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), "success");
        }
        UserAddDTO userAddDTO = users.get(0);
        if (StringUtils.isNotEmpty(userAddDTO.getUserName())) {
            if (!userAddDTO.getUserName().matches(userNamepattern)) {
                throw new UserException(UserErrorEnum.USER_NAME_RULE);
            }
        }
        UserBO userBO = new UserBO();
        BeanUtils.copyProperties(userAddDTO, userBO);
        userBO.setTimeZone(Constants.DEFAULT_TIME_ZONE);
        userBO.setId(IDGenerator.newInstance().generate().longValue());
        List<Long> roleIds = userAddDTO.getRoleIds();
        if (roleIds != null && !roleIds.isEmpty()) {
            roleIds.stream().forEach(role -> {
                UserRoleBO userRoleBO = new UserRoleBO();
                userRoleBO.setRoleId(role);
                userRoleBO.setId(IDGenerator.newInstance().generate().longValue());
                userRoleBO.setUserId(userBO.getId());
                Map<Long, String> batchName = rbacServiceAdapter.findBatchName(String.valueOf(role));
                userRoleBO.setRoleName(batchName.get(role));
                userRoleBO.setRoleType(Constants.ROLE_USER);
                temp.add(userRoleBO);
            });
        }
        userBO.setRoles(temp);
        userBO.setIsServiceApi(true);
        userService.creatUser(userBO, temp);
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), "success");
    }

    @Override
    public Map<Long, UserDetailDTO> getUsersDetail(String userIds) {
        String[] ids = userIds.split(",");
        List<String> temp = Arrays.asList(ids);
        List<Long> collect = temp.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserBO> userBOS = userService.batchGetByIds(collect);
        HashMap<Long, UserDetailDTO> map = new HashMap<>();
        userBOS.stream().forEach(t -> {
            UserDetailDTO userDetailDTO = new UserDetailDTO();
            BeanUtils.copyProperties(t, userDetailDTO);
            Long personId = t.getPersonId();
            if (personId != null) {
                Long[] person = {personId};
                userDetailDTO.setPersonId(personId);
                Map<Long, PersonDTO> longPersonDTOMap = personApiService.queryPersonsById(person);
                PersonDTO personDTO = longPersonDTOMap.get(personId);
                if (personDTO != null) {
                    userDetailDTO.setPersonName(personDTO.getName());
                }
            }
            map.put(t.getId(), userDetailDTO);

        });
        return map;
    }

    @Override
    public Map<Long, UserDetailDTO> getUsersDetailByPerson(String personIds) {
        String[] ids = personIds.split(",");
        List<String> temp = Arrays.asList(ids);
        List<Long> collect = temp.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserBO> userBOS = userService.batchGetByPersonIds(collect);
        HashMap<Long, UserDetailDTO> map = new HashMap<>();
        userBOS.stream().forEach(t -> {
            UserDetailDTO userDetailDTO = new UserDetailDTO();
            BeanUtils.copyProperties(t, userDetailDTO);
            map.put(t.getPersonId(), userDetailDTO);

        });
        return map;
    }

    @Override
    public Result<UserOrgDetailDTO> getUsersDetailByName(String userName, Long companyId) {
        UserBO bo = userService.findByUserName(userName);
        UserOrgDetailDTO userOrgDetailDTO = new UserOrgDetailDTO();
        if (StringUtils.isEmpty(bo.getUserName())) {
            return new Result<>(userOrgDetailDTO);
        }

        BeanUtils.copyProperties(bo, userOrgDetailDTO);
        userOrgDetailDTO.setHasLock(bo.getHasLock());
        // 人员
        Map<Long, PersonDTO> map = personApiService.queryPersonsById(new Long[]{bo.getPersonId()});
        PersonDTO personDTO = map.get(bo.getPersonId());
        userOrgDetailDTO.setPersonCode(personDTO.getCode());
        userOrgDetailDTO.setPersonName(personDTO.getName());
        userOrgDetailDTO.setPhone(personDTO.getPhone());
        userOrgDetailDTO.setEmail(personDTO.getEmail());
        // 所属公司列表
        ListResult<CompanyDTO> companyListResult = personApiService.queryCompanyIdByPersonId(bo.getPersonId());
        List<UserOrgDetailDTO.Company> companies = companyListResult.getList().stream().map(companyDTO -> {
            UserOrgDetailDTO.Company company = new UserOrgDetailDTO.Company();
            company.setCompanyId(companyDTO.getId());
            company.setCompanyName(companyDTO.getShortName());
            company.setCompanyCode(companyDTO.getCode());
            return company;
        }).collect(Collectors.toList());
        userOrgDetailDTO.setCompanies(companies);
        // 当前公司
        UserOrgDetailDTO.Company currentCompany = null;
        if (companies.size() == 1) {
            // 如果用户只有一个公司，则该公司就是当前公司
            currentCompany = companies.get(0);
        } else if (companies.size() > 1) {
            if (companyId != null) {
                // 如果用户指定了companyId
                Optional<UserOrgDetailDTO.Company> first = companies.stream().filter(company -> Objects.equals(company.getCompanyId(), companyId)).findFirst();
                if (first.isPresent()) {
                    currentCompany = first.get();
                }
            } else if (bo.getCurrentCompanyId() != null) {
                // 如果用户有上次选择的公司
                Optional<UserOrgDetailDTO.Company> first = companies.stream().filter(company -> Objects.equals(company.getCompanyId(), bo.getCurrentCompanyId())).findFirst();
                if (first.isPresent()) {
                    currentCompany = first.get();
                }
            }
        }
        if (currentCompany != null) {
            userOrgDetailDTO.setCompanyId(currentCompany.getCompanyId());
            userOrgDetailDTO.setCompanyName(currentCompany.getCompanyName());
            userOrgDetailDTO.setCompanyCode(currentCompany.getCompanyCode());
            // 岗位
            ListResult<PositionDetailDTO> positionListResult = personApiService.queryPersonPositionsByPersonId(personDTO.getId());
            List<PositionDetailDTO> positions = (List<PositionDetailDTO>) positionListResult.getList();
            PositionDetailDTO mainPosition = null;
            if (positions.size() == 1) {
                mainPosition = positions.get(0);
            } else if (positions.size() > 1) {
                // 如果有多个岗位，需要使用主岗
                Optional<PositionDetailDTO> first = positions.stream()
                        .filter(positionDetailDTO -> Optional.ofNullable(positionDetailDTO.getMainPosition()).orElse(false))
                        .findFirst();
                if (first.isPresent()) {
                    mainPosition = first.get();
                }
                if (mainPosition != null && mainPosition.getCompanyId().compareTo(currentCompany.getCompanyId()) != 0) {
                    UserOrgDetailDTO.Company finalCurrentCompany = currentCompany;
                    Optional<PositionDetailDTO> first1 = positions.stream()
                            .filter(positionDetailDTO -> positionDetailDTO.getCompanyId().compareTo(finalCurrentCompany.getCompanyId()) == 0)
                            .findFirst();
                    if (first1.isPresent()) {
                        mainPosition = first1.get();
                    }
                }

            }
            if (mainPosition != null) {
                userOrgDetailDTO.setPositionId(mainPosition.getId());
                userOrgDetailDTO.setPositionCode(mainPosition.getCode());
                userOrgDetailDTO.setPositionName(mainPosition.getName());
                userOrgDetailDTO.setPositionCompanyId(mainPosition.getCompanyId());
            }
            // 部门
            ListResult<DepartmentDetailDTO> departmentListResult = personApiService.queryPersonsDepartmentsByPersonIds(Collections.singletonList(personDTO.getId()));
            List<DepartmentDetailDTO> departments = (List<DepartmentDetailDTO>) departmentListResult.getList();
            DepartmentDetailDTO mainDepartment = null;
            if (departments.size() == 1) {
                mainDepartment = departments.get(0);
            } else if (departments.size() > 1 && mainPosition != null) {
                // 多个部门，使用主岗过滤部门
                Long mainPositionDepId = mainPosition.getDepId();
                Optional<DepartmentDetailDTO> first = departments.stream()
                        .filter(departmentDetailDTO -> Objects.equals(departmentDetailDTO.getId(), mainPositionDepId))
                        .findFirst();
                if (first.isPresent()) {
                    mainDepartment = first.get();
                }

            }
            if (mainDepartment != null) {
                userOrgDetailDTO.setDepartmentId(mainDepartment.getId());
                userOrgDetailDTO.setDepartmentCode(mainDepartment.getCode());
                userOrgDetailDTO.setDepartmentName(mainDepartment.getName());
            }
        }
        return new Result<>(userOrgDetailDTO);
    }

    @Override
    public Result<UserOrgDetailDTO> getUsersDetailById(Long userId) {
        UserBO bo = userService.getUserById(userId);
        UserOrgDetailDTO userOrgDetailDTO = new UserOrgDetailDTO();
        BeanUtils.copyProperties(bo, userOrgDetailDTO);
        userOrgDetailDTO.setHasLock(bo.getHasLock());
        List<UserOrgDetailDTO.Company> collect = new ArrayList<>();
        HashSet<Long> set = new HashSet<>();
        Result<CompanyResultDTO> company = personApiService.findCompany(bo.getCompanyId());
        set.add(company.getData().getId());
        UserOrgDetailDTO.Company company1 = new UserOrgDetailDTO.Company();
        company1.setCompanyName(company.getData().getShortName());
        company1.setCompanyCode(company.getData().getCode());
        company1.setCompanyId(company.getData().getId());
        collect.add(company1);
        if (bo.getPersonId() != null) {
            ListResult<CompanyDTO> companies = personApiService.queryCompanyIdByPersonId(bo.getPersonId());
            companies.getList().stream().forEach(t -> {
                if (set.add(t.getId())) {
                    UserOrgDetailDTO.Company temp = new UserOrgDetailDTO.Company();
                    temp.setCompanyName(t.getShortName());
                    temp.setCompanyCode(t.getCode());
                    temp.setCompanyId(t.getId());
                    collect.add(temp);
                }
            });
            Map<Long, PersonDTO> map = personApiService.queryPersonsById(new Long[]{bo.getPersonId()});
            PersonDTO personDTO = map.get(bo.getPersonId());
            userOrgDetailDTO.setPersonCode(personDTO.getCode());
            userOrgDetailDTO.setPersonName(personDTO.getName());
        }
        userOrgDetailDTO.setCompanies(collect);
        return new Result<>(userOrgDetailDTO);
    }

    @Override
    public Result<UserOrgDetailDTO> getUserOrgDetailByName(String userName) {
        UserBO bo = userService.findByUserName(userName);
        UserOrgDetailDTO userOrgDetailDTO = new UserOrgDetailDTO();
        BeanUtils.copyProperties(bo, userOrgDetailDTO);
        if (bo.getPersonId() == null) {
            Result<CompanyResultDTO> company = personApiService.findCompany(bo.getCompanyId());
            CompanyResultDTO data = company.getData();
            if (data != null) {
                ArrayList<UserOrgDetailDTO.Company> companies = new ArrayList<>();
                UserOrgDetailDTO.Company temp = new UserOrgDetailDTO.Company();
                temp.setCompanyCode(data.getCode());
                temp.setCompanyId(data.getId());
                temp.setCompanyName(data.getShortName());
                companies.add(temp);
                userOrgDetailDTO.setCompanies(companies);
            }
        } else {
            ListResult<CompanyDTO> companies = personApiService.queryCompanyIdByPersonId(bo.getPersonId());
            Map<Long, PersonDTO> map = personApiService.queryPersonsById(new Long[]{bo.getPersonId()});
            PersonDTO personDTO = map.get(bo.getPersonId());
            userOrgDetailDTO.setPersonCode(personDTO.getCode());
            userOrgDetailDTO.setPersonName(personDTO.getUserName());
            List<UserOrgDetailDTO.Company> collect = companies.getList().stream().map(t -> {
                UserOrgDetailDTO.Company temp = new UserOrgDetailDTO.Company();
                temp.setCompanyCode(t.getCode());
                temp.setCompanyId(t.getId());
                temp.setCompanyName(t.getShortName());
                return temp;
            }).collect(Collectors.toList());
            userOrgDetailDTO.setCompanies(collect);
        }
        return new Result<>(userOrgDetailDTO);
    }

    @Override
    public Result<LoginResponseDTO> loginCustom(LoginCustomDTO loginCustomDTO) {
        UserBO userBO = userService.findByUserName(loginCustomDTO.getUserName());
        LoginResponseDTO loginResponseDTO = convertLoginResponse(userBO);
        return new Result<>(loginResponseDTO);
    }

    @Override
    public Result<LoginResponseDTO> companyChange(CompanyChangeDTO companyChangeDTO) {
        userService.changeCurrentCompany(companyChangeDTO.getUserName(), companyChangeDTO.getCompanyId());
        UserBO userBO = userService.findByUserName(companyChangeDTO.getUserName());
        LoginResponseDTO loginResponseDTO = convertLoginResponse(userBO);
        return new Result<>(loginResponseDTO);
    }

    /**
     * 将用户信息转换成登录响应参数
     */
    private LoginResponseDTO convertLoginResponse(UserBO userBO) {
        if (userBO == null) {
            return null;
        }
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO();
        loginResponseDTO.setUsername(userBO.getUserName());
        loginResponseDTO.setUserId(userBO.getId());
        loginResponseDTO.setUserType(userBO.getUserType());
        loginResponseDTO.setStatus(userBO.getLoginFirst() ? "firstLogin" : "ok");
        if (userBO.getPersonId() == null) {
            loginResponseDTO.setUserId(userBO.getId());
            loginResponseDTO.setUsername(userBO.getUserName());
            Result<CompanyResultDTO> company = personApiService.findCompany(userBO.getCompanyId());
            CompanyResultDTO data = company.getData();
            if (data != null) {
                loginResponseDTO.setCompanyCode(data.getCode());
                loginResponseDTO.setCompanyId(data.getId());
                loginResponseDTO.setCompanyName(data.getShortName());
                List<LoginResponseDTO.Company> companies = new ArrayList<>();
                LoginResponseDTO.Company loginCompanyResponse = new LoginResponseDTO.Company();
                loginCompanyResponse.setCompanyCode(data.getCode());
                loginCompanyResponse.setCompanyId(data.getId());
                loginCompanyResponse.setCompanyName(data.getShortName());
                companies.add(loginCompanyResponse);
                loginResponseDTO.setCompany(companies);
            }
        } else {
            ListResult<CompanyDTO> companies = personApiService.queryCompanyIdByPersonId(userBO.getPersonId());
            List<LoginResponseDTO.Company> collect = companies.getList().stream().map(t -> {
                LoginResponseDTO.Company loginCompanyResponse = new LoginResponseDTO.Company();
                loginCompanyResponse.setCompanyCode(t.getCode());
                loginCompanyResponse.setCompanyId(t.getId());
                loginCompanyResponse.setCompanyName(t.getShortName());
                return loginCompanyResponse;
            }).collect(Collectors.toList());
            loginResponseDTO.setCompany(collect);
            if (collect.size() == 1) {
                loginResponseDTO.setCompanyCode(collect.get(0).getCompanyCode());
                loginResponseDTO.setCompanyId(collect.get(0).getCompanyId());
                loginResponseDTO.setCompanyName(collect.get(0).getCompanyName());
            } else if (userBO.getCurrentCompanyId() != null) {
                collect.stream().anyMatch(t -> {
                    if (t.getCompanyId().equals(userBO.getCurrentCompanyId())) {
                        loginResponseDTO.setCompanyCode(t.getCompanyCode());
                        loginResponseDTO.setCompanyId(t.getCompanyId());
                        loginResponseDTO.setCompanyName(t.getCompanyName());
                        return true;
                    }
                    return false;
                });
            }
        }
        return loginResponseDTO;
    }

    public static void main(String[] args) {
        List<UserBO> batchUserName = new ArrayList<>();
        UserBO userBO = new UserBO();
        batchUserName.add(userBO);
        List<Long> collect = batchUserName.stream().map(UserBO::getPersonId).filter(Objects::nonNull).collect(Collectors.toList());
        System.out.println(collect.size());
    }

    @Override
    public Result<Map<String, UserDetailDTO>> getBatchUsersDetailByName(String[] userNames) {
        List<UserBO> batchUserName = userService.findBatchUserName(userNames);
        Map<Long, PersonDTO> longPersonDTOMap = new HashMap<>();
        List<Long> collect = batchUserName.stream().map(UserBO::getPersonId).filter(Objects::nonNull).collect(Collectors.toList());
        if (!collect.isEmpty()) {
            longPersonDTOMap = personApiService.queryPersonsById(collect.toArray(new Long[collect.size()]));
        }
        HashMap<String, UserDetailDTO> map = new HashMap<>();
        for (UserBO userBO : batchUserName) {
            UserDetailDTO userDetailDTO = new UserDetailDTO();
            BeanUtils.copyProperties(userBO, userDetailDTO);
            if (userBO.getPersonId() != null) {
                PersonDTO personDTO = longPersonDTOMap.get(userBO.getPersonId());
                if (personDTO != null) {
                    userDetailDTO.setPhone(personDTO.getPhone());
                    userDetailDTO.setEmail(personDTO.getEmail());
                    userDetailDTO.setPersonCode(personDTO.getCode());
                    userDetailDTO.setPersonName(personDTO.getName());
                }
            }
            map.put(userBO.getUserName(), userDetailDTO);
        }
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), map);
    }

    @Override
    public ListResult<UserDetailDTO> companyAdminUser(Long companyId) {
        List<UserBO> userBos = userService.findCompanyAdminUser(companyId);
        List<UserDetailDTO> result = userBos.stream().map(userBO -> {
            UserDetailDTO userDetailDTO = new UserDetailDTO();
            BeanUtils.copyProperties(userBO, userDetailDTO);
            return userDetailDTO;
        }).collect(Collectors.toList());
        ListResult<UserDetailDTO> userDetailList = new ListResult<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage());
        userDetailList.setList(result);
        return userDetailList;
    }

    @Override
    public Result<Boolean> deleteCompanyUser(Long companyId) {
        Boolean success = userService.deleteUserByCompanyId(companyId);
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), success);
    }

    @Override
    public Result<Boolean> deleteCompanyUser(Long[] personIds) {
        Boolean success = userService.deleteUserByPersonIds(personIds);
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), success);
    }

    @Override
    public Result<Boolean> bindRole(UserRoleDTO userRoleDTO) {
        List<Long> addUserIds = userRoleDTO.getAddUserIds();
        List<Long> deleteUserIds = userRoleDTO.getDeleteUserIds();
        if (!ObjectUtils.isEmpty(addUserIds)) {
            addUserIds.forEach(userId -> {
                //如果该用户关联过该角色，不重复关联
                UserRolePO one = getOne(userId, userRoleDTO.getRoleId());
                if (one != null) {
                    if (Constants.ROLE_ORG.compareTo(one.getRoleType()) == 0) {
                        one.setRoleType(Constants.ROLE_REPEATE);
                    }
                } else {
                    UserRolePO userRolePO = new UserRolePO();
                    userRolePO.setRoleId(userRoleDTO.getRoleId());
                    userRolePO.setUserId(userId);
                    userRolePO.setRoleType(Constants.ROLE_USER);
                    one = userRolePO;
                }
                userRoleService.saveOrUpdate(one);
            });

        }
        if (!ObjectUtils.isEmpty(deleteUserIds)) {
            List<UserPO> userPOS = userMapper.selectList(Wrappers.lambdaQuery(UserPO.class).in(UserPO::getId, deleteUserIds));
            boolean isSystemRole = userPOS.stream().anyMatch(userBO -> userBO.getUserType().compareTo(UserTypeEnum.SYSTEM_USER.getCode()) == 0);
            if (isSystemRole) {
                throw new UserException(USER_ROLE_NOT_DELETE);
            }
            deleteUserIds.forEach(userId -> {
                UserRolePO one = getOne(userId, userRoleDTO.getRoleId());
                if (one != null) {
                    if (Constants.ROLE_REPEATE.compareTo(one.getRoleType()) == 0) {
                        one.setRoleType(Constants.ROLE_ORG);
                        userRoleService.updateById(one);
                    } else {
                        userRoleService.removeById(one.getId());
                    }
                }
            });
        }
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), true);
    }

    private UserRolePO getOne(Long userId, Long roleId) {
        return userRoleService.getOne(Wrappers.lambdaQuery(UserRolePO.class).eq(UserRolePO::getUserId, userId).eq(UserRolePO::getRoleId, roleId));
    }

    @Override
    public Map<Long, UserDetailDTO> queryUsersDetail(UserQueryDTO userQueryDTO) {
        List<Long> personIds = userQueryDTO.getPersonIds();
        if (personIds == null || personIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, UserDetailDTO> userDetailMap = new HashMap<>(personIds.size());
        double maxPages = Math.ceil(personIds.size() * 1.0 / MAX_IN_SIZE);
        for (int i = 0; i < maxPages; i++) {
            int fromIndex;
            int toIndex;
            fromIndex = i * MAX_IN_SIZE;
            if (i == maxPages - 1) {
                toIndex = fromIndex + (personIds.size() - i * MAX_IN_SIZE);
            } else {
                toIndex = fromIndex + MAX_IN_SIZE;
            }
            LambdaQueryWrapper<UserPO> queryWrapper = Wrappers.lambdaQuery(UserPO.class)
                    .in(UserPO::getPersonId, personIds.subList(fromIndex, toIndex))
                    .like(StringUtils.isNotBlank(userQueryDTO.getKeyword()), UserPO::getUserName, userQueryDTO.getKeyword());
            List<UserPO> users = userMapper.selectList(queryWrapper);
            for (UserPO user : users) {
                UserDetailDTO userDetailDTO = new UserDetailDTO();
                BeanUtils.copyProperties(user, userDetailDTO);
                userDetailDTO.setPassword(null);
                userDetailMap.put(user.getPersonId(), userDetailDTO);
            }
        }
        return userDetailMap;
    }

    @Override
    public Result<Boolean> changeRole(List<PersonRoleDTO> personRoleDTO) {
        ThreadPoolUtils.getThreadPool().execute(() -> {
            try {
                Long now = System.currentTimeMillis();
                log.info("enter changeRole");
                personRoleDTO.stream().forEach(t -> {
                    UserBO userBO = userService.selectByPersonId(t.getPersonId());
                    HashMap<Long, UserRoleBO> map = new HashMap<>();
                    List<UserRoleBO> roleBOS = new ArrayList<>();
                    if (userBO != null) {
                        List<UserRolePO> list = userRoleService.list(Wrappers.lambdaQuery(UserRolePO.class).eq(UserRolePO::getUserId, userBO.getId()).ne(UserRolePO::getRoleType, Constants.ROLE_ORG));
                        if (list != null && !list.isEmpty()) {
                            list.forEach(role -> {
                                UserRoleBO userRoleBO = new UserRoleBO();
                                userRoleBO.setUserId(userBO.getId());
                                userRoleBO.setRoleId(role.getRoleId());
                                userRoleBO.setRoleType(Constants.ROLE_USER);
                                map.put(role.getRoleId(), userRoleBO);
                                roleBOS.add(userRoleBO);
                            });
                        }
                        Set<Long> roleId = t.getRoleId();
                        if (roleId != null && !roleId.isEmpty()) {
                            roleId.forEach(id -> {
                                UserRoleBO roleBO = map.get(id);
                                if (roleBO != null) {
                                    roleBO.setRoleType(Constants.ROLE_REPEATE);
                                } else {
                                    UserRoleBO userRoleBO = new UserRoleBO();
                                    userRoleBO.setUserId(userBO.getId());
                                    userRoleBO.setRoleId(id);
                                    userRoleBO.setRoleType(Constants.ROLE_ORG);
                                    roleBOS.add(userRoleBO);
                                }
                            });
                        }
                        userRoleService.remove(Wrappers.lambdaUpdate(UserRolePO.class).eq(UserRolePO::getUserId, userBO.getId()));
                        if (!roleBOS.isEmpty()) {
                            log.info("personid====>" + userBO.getPersonId());
                            userRoleService.batchInsert(roleBOS);
                        }
                        userBO.setRoles(roleBOS);
                        addUserRolesToRbac(userBO, roleBOS);
                    }
                });
                long costTime = System.currentTimeMillis() - now;
                log.info("cost time====>" + costTime);
            } catch (Throwable e) {
                log.error("change role error ====>", e);
            }
        });
        return new Result<>(true);
    }

    void addUserRolesToRbac(UserBO userBo, List<UserRoleBO> role) {
        log.info("change role rbac");
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
        List<RoleFRDTO> roleFRDTOS = new ArrayList<>();
        if (role != null && !role.isEmpty()) {
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
    private PersonDTO getPersonDTO(Long peronId) {
        if (null != peronId) {
            Long[] personIds = new Long[1];
            personIds[0] = peronId;
            PersonDTO personDTO = personApiService.queryPersonsById(personIds).get(peronId);
            return personDTO;
        }
        return null;
    }

    @Override
    public UserSessinResult<UserStaffDTO> getUserSessionInfo(String ticket) {
        Map<Object, Object> authorizationMap = authTicketCache.getMapByTicket(ticket);
        // 转发accessToken
        String accessToken = (String) authorizationMap.get(ACCESS_TOKEN);
        if (StringUtils.isEmpty(accessToken)) {
            return new UserSessinResult<>();
        }
        JwtUser jwtUser = jwtTokenUtil.getJwtUserFromToken(accessToken);
        UserBO userBo = userService.getUserById(jwtUser.getUserId());
        if (userBo == null) {
            throw new UserException(UserErrorEnum.USER_NOT_EXIST);
        } else {
            UserStaffDTO userStaffVO = new UserStaffDTO();
            userStaffVO.setUserId(userBo.getId());
            userStaffVO.setUsername(userBo.getUserName());
            userStaffVO.setCompanyCode(jwtUser.getCompanyCode());
            userStaffVO.setCompanyId(jwtUser.getCompanyId());
            userStaffVO.setCompanyName(jwtUser.getCompanyName());
            if (userBo.getPersonId() != null) {
                Long personId = userBo.getPersonId();
                log.info("" + personId);
                Long[] ids = {personId};
                Map<Long, PersonDTO> persons = personApiService.queryPersonsById(ids);
                PersonDTO personDTO = persons.get(userBo.getPersonId());
                userStaffVO.setStaffName(personDTO.getName());
                userStaffVO.setStaffId(personDTO.getId());
                userStaffVO.setStaffCode(personDTO.getCode());
            }
            return new UserSessinResult<>(userStaffVO);
        }
    }

    @Override
    public Result<Boolean> checkPassword(PasswordDTO passwordDTO) {
        UserBO bo = userService.findByUserName(passwordDTO.getUserName());
        if (StringUtils.isNotEmpty(bo.getUserName())) {
            if (BCryptUtil.matches(passwordDTO.getPassword(), bo.getPassword())) {
                return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), true);
            }
        }
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), false);
    }

    @Override
    public Result<RoleInfoDTO> getRole(Long userId) {
        List<UserRoleBO> role = userRoleService.getRole(userId);
        RoleInfoDTO roleInfoDTO = new RoleInfoDTO();
        roleInfoDTO.setUserId(userId);
        List<Long> collect = role.stream().map(UserRoleBO::getRoleId).collect(Collectors.toList());
        roleInfoDTO.setRoleId(collect);
        return new Result<>(roleInfoDTO);
    }

    @Override
    public Result<List<UserFlowInfoDTO>> getUsersByRoleId(Long roleId) {
        List<UserBO> userBOS = userRoleService.selectUserRole(roleId);
        List<UserFlowInfoDTO> collect = userBOS.stream().map(t -> {
            UserFlowInfoDTO userFlowInfoDTO = new UserFlowInfoDTO();
            BeanUtils.copyProperties(t, userFlowInfoDTO);
            return userFlowInfoDTO;
        }).collect(Collectors.toList());
        return Result.data(collect);
    }

    @Override
    public Map<Long, String> getUsernameByIds(List<Long> userIds) {
        Map<Long, String> map = new HashMap<>();
        List<UserPO> userPOS = userMapper.selectList(Wrappers.lambdaQuery(UserPO.class).in(UserPO::getId, userIds));
        if (userPOS != null && !userPOS.isEmpty()) {
            userPOS.forEach(t -> {
                map.put(t.getId(), t.getUserName());
            });
        }
        return map;
    }

    @Override
    public Result<Long> getIdByUserName(String userName) {
        Long userId = userService.getIdByUserName(userName);
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), userId);
    }

    @Override
    public ListResult<UserDetailDTO> getAllPersonsUsers() {
        List<UserBO> userBOS = userService.getAllPersonsUsers();
        List<UserDetailDTO> result = Lists.newArrayList();
        for (UserBO userBO : userBOS) {
            UserDetailDTO userDetailDTO = new UserDetailDTO();
            BeanUtils.copyProperties(userBO, userDetailDTO);
            result.add(userDetailDTO);
        }
        return new ListResult<>(result);
    }

    @Override
    public void changeUserPersonName(List<UserPersonNames> userPersonNames) {
        if (userPersonNames != null && !userPersonNames.isEmpty()) {
            userPersonNames.forEach(t -> {
                UserBO userBO = userService.selectByPersonId(t.getPersonId());
                if (userBO != null && StringUtils.isNotEmpty(userBO.getUserName())) {
                    UserPO userPO = new UserPO();
                    userPO.setPersonId(t.getPersonId());
                    userPO.setPersonName(t.getPersonName());
                    userPO.setId(userBO.getId());
                    userService.updateById(userPO);
                }
            });
        }
    }
}
