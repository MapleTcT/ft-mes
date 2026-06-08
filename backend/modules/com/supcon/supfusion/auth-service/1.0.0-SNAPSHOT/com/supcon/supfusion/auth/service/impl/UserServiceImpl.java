package com.supcon.supfusion.auth.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.constants.UserLockReasonEnum;
import com.supcon.supfusion.auth.common.constants.UserTypeEnum;
import com.supcon.supfusion.auth.common.exception.ThirdAuthErrorEnum;
import com.supcon.supfusion.auth.common.exception.ThirdAuthException;
import com.supcon.supfusion.auth.common.exception.UserErrorEnum;
import com.supcon.supfusion.auth.common.exception.UserException;
import com.supcon.supfusion.auth.common.utils.*;
import com.supcon.supfusion.auth.dao.mapper.ExcelOperateMapper;
import com.supcon.supfusion.auth.dao.mapper.UserDirectoryMapper;
import com.supcon.supfusion.auth.dao.mapper.UserMapper;
import com.supcon.supfusion.auth.dao.mapper.UserRoleMapper;
import com.supcon.supfusion.auth.dao.po.AuthExcelPO;
import com.supcon.supfusion.auth.dao.po.UserDirectoryPO;
import com.supcon.supfusion.auth.dao.po.UserPO;
import com.supcon.supfusion.auth.dao.po.UserRolePO;
import com.supcon.supfusion.auth.manager.*;
import com.supcon.supfusion.auth.manager.bo.AuthorizationDTO;
import com.supcon.supfusion.auth.manager.bo.LoginConfigBO;
import com.supcon.supfusion.auth.manager.feign.client.TokenClient;
import com.supcon.supfusion.auth.service.*;
import com.supcon.supfusion.auth.service.bo.*;
import com.supcon.supfusion.auth.service.bo.bap.*;
import com.supcon.supfusion.auth.service.bo.kafka.DeleteUserBo;
import com.supcon.supfusion.auth.service.bo.kafka.SaveAuthUserBo;
import com.supcon.supfusion.auth.service.cache.AuthTicketCache;
import com.supcon.supfusion.auth.service.cache.UserCache;
import com.supcon.supfusion.auth.service.config.AuthProperties;
import com.supcon.supfusion.auth.service.config.BranchOfficeProperties;
import com.supcon.supfusion.auth.service.kafka.AuthUserMessage;
import com.supcon.supfusion.flow.api.dto.TaskTotalsDTO;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.message.Message;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.iam.api.IdentityAndAccessService;
import com.supcon.supfusion.iam.api.dto.AccountDTO;
import com.supcon.supfusion.organization.api.dto.*;
import com.supcon.supfusion.rbac.api.dto.*;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.supcon.supfusion.auth.common.constants.Constants.*;
import static com.supcon.supfusion.auth.common.exception.UserErrorEnum.*;


/**
 * @author lifangyuan
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, UserPO> implements UserService {

    private static String bigSmall = "(?=.*[a-z])(?=.*[A-Z])";

    private static String number = "(?=.*[0-9])";

    private static String special = "(?=.*[!@#$%^&*();'?.,])";

    private static String all = "[A-Za-z0-9!@#$%^&*();'?.,]";

    private static final FastDateFormat fastDateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ", TimeZone.getTimeZone(ZoneId.of("UTC")));
    private static final String AUTH_USER_KAFKA_TOPIC = "supOS_user_event";
    private static final String AUTH_USER_KAFKA_SENDER = "auth";

    @Value("${integration.supos.enabled:true}")
    private Boolean supOSEnabled;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private UserRoleService userRoleService;

    @Resource
    private ExcelOperateMapper excelOperateMapper;

    @Resource
    private ExcelService excelService;

    @Resource
    private PersonServiceAdapter personServiceAdapter;

    @Resource
    private SystemCodeServiceAdapter systemCodeServiceAdapter;

    @Resource
    private OnlineUserService onlineUserService;

    @Resource
    private RbacServiceAdapter rbacServiceAdapter;

    @Resource
    private UserCache userCache;

    @Resource
    private TokenClient tokenClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private KeycliandAdminClient keycliandAdminClient;

    @Resource
    private QuotaClientAdapter quotaClientAdapter;

    @Resource
    private AuthTicketCache authTicketCache;

    @Autowired
    private AuthProperties authProperties;

    @Resource
    private UserDirectoryMapper userDirectoryMapper;

    @Resource
    private TaskServiceApiAdapter taskServiceApiAdapter;

    @Resource
    private IpBlackWhiteService ipBlackWhiteService;

    @Resource
    private PasswordService passwordService;

    @Resource
    private ThirdAuthService thirdAuthService;
    @Autowired
    IdentityAndAccessService identityAndAccessService;
    @Autowired
    AuthUserMessage authUserMessage;
    @Autowired
    private AuthLoginLogService loginLogService;
    @Autowired
    private KeyCloakServiceAdapter keyCloakServiceAdapter;
    @Autowired
    private BranchOfficeService branchOfficeService;
    @Autowired
    private BranchOfficeProperties branchOfficeProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelet(List<Long> ids) {
        Long userId = UserContext.getUserContext().getUserId();
        if (ids.contains(userId)) {
            throw new UserException(UserErrorEnum.CURRENT_USER_CANNOT_DELETE, UserContext.getUserContext().getUserName());
        }
        if (ids.contains(1L)) {
            throw new UserException(UserErrorEnum.ADMIN_USER_CANNOT_DELETE);
        }
        TaskTotalsDTO taskTotal = taskServiceApiAdapter.getTaskTotal(ids);
        if (taskTotal.isContainTask()) {
            throw new UserException(UserErrorEnum.USER_TASK_CANNOT_DELETE);
        }
        LambdaUpdateWrapper<UserRolePO> updateUserRoleWrapper = new UpdateWrapper<UserRolePO>().lambda()
                .in(UserRolePO::getUserId, ids);
        userRoleService.remove(updateUserRoleWrapper);

        //删除人员表绑定用户
        List<UserBO> userBOS = this.batchGetByIds(ids);
        List<Long> personIds = userBOS.stream().map(UserBO::getPersonId).collect(Collectors.toList());
        personServiceAdapter.deleteUsersByPersonIds(personIds);

        LambdaUpdateWrapper<UserPO> updateWrapper = new UpdateWrapper<UserPO>().lambda()
                .in(UserPO::getId, ids);
        List<UserPO> userPOS = userMapper.selectList(updateWrapper);
        List<UserPO> collect = userPOS.stream().filter(t -> t.getUserType().compareTo(UserTypeEnum.SYSTEM_USER.getCode()) == 0).collect(Collectors.toList());
        if (!collect.isEmpty()) {
            collect.forEach(t -> {
                personServiceAdapter.deletePersonById(t.getPersonId());
            });
        }
        userMapper.delete(updateWrapper);
        // 登出在线用户
        onlineUserService.logoutOnlineUsersByUserIds(ids);
        // 删除用户时删除用户角色
        rbacServiceAdapter.deleteRolesByUserIds(ids);

        {
            // kafka message
            deleteAuthUserKafkaMessage(BijectionUtils.applys(userBOS, x -> x.getUserName()));
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void creatUser(UserBO bo, List<UserRoleBO> userRoleBO) {
        String encodePassword = verify(bo.getPassword());
        bo.setPassword(encodePassword);
        int total = count(Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getUserName, bo.getUserName()));
        if (total > 0) {
            throw new UserException(UserErrorEnum.USER_NAME_REPEATE);
        }
        if (bo.getUserType() == null) {
            bo.setUserType(Constants.COMMON_TYPE);
        }
        if (bo.getPersonId() != null) {
            int personExistCount = count(Wrappers.lambdaQuery(UserPO.class)
                    .eq(UserPO::getPersonId, bo.getPersonId()));
            if (personExistCount > 0) {
                throw new UserException(UserErrorEnum.USER_PERSON_REPEATE);
            }
            if (StringUtils.isEmpty(bo.getPersonName())) {
                Map<Long, PersonDTO> map = personServiceAdapter.queryPersonsById(new Long[]{bo.getPersonId()});
                PersonDTO personDTO = map.get(bo.getPersonId());
                bo.setPersonName(personDTO.getName());
                bo.setPersonCode(personDTO.getCode());
            }
        } else {
            Result<Long> result = personServiceAdapter.addVirtualPerson(bo.getUserName(), bo.getCompanyId());
            bo.setPersonId(result.getData());
            bo.setPersonName(bo.getUserName());
            bo.setPersonCode(bo.getUserName());
        }
        // 为管理员分配权限
        if (bo.getUserType().equals(Constants.ADMIN_TYPE)) {
            processAdminRole(bo);
        } else {
            if (userRoleBO != null && !userRoleBO.isEmpty()) {
                userRoleService.remove(Wrappers.lambdaUpdate(UserRolePO.class).eq(UserRolePO::getUserId, bo.getId()));
                userRoleService.batchInsert(userRoleBO);
                addUserRolesToRbac(bo, userRoleBO);
            }
        }
        UserPO userPo = new UserPO();
        BeanUtils.copyProperties(bo, userPo);
        userMapper.insert(userPo);

        //新增人员表中用户信息
        PersonUserDTO personUserDTO = new PersonUserDTO(userPo.getPersonId(), userPo.getId(), userPo.getUserName());
        personServiceAdapter.saveOrUpdateUsers(Lists.newArrayList(personUserDTO));

        {
            // kafka message
            List<Long> roleIds = bo.getUserType().equals(ADMIN_TYPE) ? BijectionUtils.applys(bo.getRoles(), x -> x.getRoleId()) :
                    BijectionUtils.applys(userRoleBO, UserRoleBO::getRoleId);
            SaveAuthUserBo authUserBo = new SaveAuthUserBo()
                    .setPerson(new SaveAuthUserBo.PersonDTO().setCode(bo.getPersonCode()).setName(bo.getPersonName()))
                    .setDescription(bo.getDescription())
                    .setRowVersion(0)
                    .setName(bo.getUserName())
                    .setRoleList(getUserRoleCodeAndNameByIds(roleIds));


            Message authMessage = new AuthUserMessage.Builder<SaveAuthUserBo>()
                    .setSender(AUTH_USER_KAFKA_SENDER)
                    .setTenantId(RpcContext.getContext().getTenantId())
                    .setCreateTime(fastDateFormat.format(new Date()))
                    .setTopic(AUTH_USER_KAFKA_TOPIC)
                    .setHeader(ImmutableMap.of("encode", "json", "event", "CREATE"))
                    .setBody(Collections.singletonList(authUserBo))
                    .build();

            authUserMessage.publishMessage(authMessage);
        }
    }

    private void processAdminRole(UserBO bo) {
        AdminRoleDTO adminRoleDTO = new AdminRoleDTO();
        adminRoleDTO.setCid(bo.getCompanyId());
        adminRoleDTO.setCompanyCode(bo.getCompanyCode());
        adminRoleDTO.setUserId(bo.getId());
        adminRoleDTO.setUserName(bo.getUserName());
        UserRolePO userRolePO = new UserRolePO();
        UserRoleBO userRoleBO1 = new UserRoleBO();
        if (bo.getIsServiceApi() != null && bo.getIsServiceApi()) {
            RoleDTO roleDTO = rbacServiceAdapter.createAdminRole(adminRoleDTO);
            userRolePO.setRoleId(roleDTO.getId());
            userRoleBO1.setRoleId(roleDTO.getId());
        } else {
            RoleDTO roleDTO = rbacServiceAdapter.bindCompanyAdminUser(adminRoleDTO);
            userRolePO.setRoleId(roleDTO.getId());
            userRoleBO1.setRoleId(roleDTO.getId());
        }
        List<UserRoleBO> userRoleBOS = new ArrayList<>();
        userRolePO.setUserId(bo.getId());
        userRoleBO1.setUserId(bo.getId());
        userRoleBO1.setRoleType(Constants.ROLE_USER);
        userRoleBO1.setId(IDGenerator.newInstance().generate().longValue());
        userRoleBOS.add(userRoleBO1);
        bo.setRoles(userRoleBOS);
        userRoleService.saveOrUpdate(userRolePO);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserBO bo, List<UserRoleBO> userRoleBOS) {
        UserPO userPO = new UserPO();
        userPO.setDescription(bo.getDescription());
        if (!StringUtils.isEmpty(bo.getPassword())) {
            String encryPassword = verify(bo.getPassword());
            userPO.setPassword(encryPassword);
        }
        LambdaUpdateWrapper<UserPO> updateWrapper = Wrappers.lambdaUpdate(UserPO.class).eq(UserPO::getId, bo.getId());
        if (bo.getHasLock() != null) {
            userPO.setHasLock(bo.getHasLock());
        }
        if (bo.getPersonId() != null) {
            userPO.setPersonId(bo.getPersonId());
            PersonDTO personDTO = personServiceAdapter.queryPersonsById(new Long[]{bo.getPersonId()}).get(bo.getPersonId());
            userPO.setPersonName(personDTO.getName());
            userPO.setPersonCode(personDTO.getCode());
        }
        if (bo.getLoginFirst() != null) {
            userPO.setLoginFirst(bo.getLoginFirst());
        }
        if (!StringUtils.isEmpty(bo.getFaceUrl())) {
            userPO.setFaceUrl(bo.getFaceUrl());
        }
        if (!StringUtils.isEmpty(bo.getTimeZone())) {
            userPO.setTimeZone(bo.getTimeZone());
        }
        userMapper.update(userPO, updateWrapper);
        if (Optional.ofNullable(bo.getHasLock()).orElse(false)) {
            // 登出在线用户
            onlineUserService.logoutOnlineUsersByUserIds(Collections.singletonList(bo.getId()));
        }
        if (userRoleBOS != null) {
            if (!userRoleBOS.isEmpty()) {
                userRoleService.remove(new QueryWrapper<UserRolePO>().lambda().eq(UserRolePO::getUserId, bo.getId()));
                userRoleService.batchInsert(userRoleBOS);
                addUserRolesToRbac(bo, userRoleBOS);
            } else if (bo.getUserType().compareTo(UserTypeEnum.SYSTEM_USER.getCode()) != 0) {
                userRoleService.remove(new QueryWrapper<UserRolePO>().lambda().eq(UserRolePO::getUserId, bo.getId()));
                bo.setCompanyId(UserContext.getUserContext().getCompanyId());
                addUserRolesToRbac(bo, null);
            }
        }

        // 获取用户当前角色
        List<Long> userCurrentRoleIds = userRoleService.getRole(bo.getId()).stream().map(UserRoleBO::getRoleId).collect(Collectors.toList());
        // 获取用户姓名
        UserPO currentUser = userMapper.selectById(bo.getId());
        {
            // kafka message
            List<SaveAuthUserBo.RoleListDTO> roleList = getUserRoleCodeAndNameByIds(userCurrentRoleIds);
            SaveAuthUserBo authUserBo = new SaveAuthUserBo()
                    .setPerson(new SaveAuthUserBo.PersonDTO().setCode(currentUser.getPersonCode()).setName(currentUser.getPersonName()))
                    .setDescription(currentUser.getDescription())
                    .setRowVersion(0)
                    .setName(currentUser.getUserName())
                    .setRoleList(roleList);

            Message authMessage = new AuthUserMessage.Builder<SaveAuthUserBo>()
                    .setSender(AUTH_USER_KAFKA_SENDER)
                    .setTenantId(RpcContext.getContext().getTenantId())
                    .setCreateTime(fastDateFormat.format(new Date()))
                    .setTopic(AUTH_USER_KAFKA_TOPIC)
                    .setHeader(ImmutableMap.of("encode", "json", "event", "UPDATE"))
                    .setBody(Collections.singletonList(authUserBo))
                    .build();

            authUserMessage.publishMessage(authMessage);
        }
    }

    private List<SaveAuthUserBo.RoleListDTO> getUserRoleCodeAndNameByIds(List<Long> userCurrentRoleIds) {
        List<SaveAuthUserBo.RoleListDTO> roleList = CollectionUtils.isEmpty(userCurrentRoleIds) ? Collections.emptyList() : rbacServiceAdapter.findRoleByIds(userCurrentRoleIds).stream().map(x -> new SaveAuthUserBo.RoleListDTO().setCode(x.getCode()).setName(x.getName())).collect(Collectors.toList());
        return roleList;
    }

    void updateUserRolesToRbac(UserBO userBo, List<Long> roleIds) {
        RoleUserAddBatchDTO roleUserAddBatchDTO = new RoleUserAddBatchDTO();
        roleUserAddBatchDTO.setUserId(userBo.getId());
        roleUserAddBatchDTO.setUserName(userBo.getUserName());
        PersonDTO person = getPersonDTO(userBo.getPersonId());
        if (null != person) {
            roleUserAddBatchDTO.setPersonCode(person.getCode());
            roleUserAddBatchDTO.setPersonName(person.getName());
        }
        roleUserAddBatchDTO.setRoleIds(roleIds);
        rbacServiceAdapter.batchUpdateOneUser(roleUserAddBatchDTO);
    }

    @Override
    public Page<UserBO> searchUser(Page<UserBO> page, UserBO bo) {
        Page<UserPO> userPage = new Page<>(page.getCurrent(), page.getSize());
        UserPO userPO = new UserPO();
        BeanUtils.copyProperties(bo, userPO);
        LambdaQueryWrapper<UserPO> lambda = new QueryWrapper<UserPO>().lambda();
        if (bo.getCompany() != null && !bo.getCompany().isEmpty()) {
            lambda.in(UserPO::getCompanyId, bo.getCompany()).ne(UserPO::getUserType, 2);
        }
        ListResult<Long> personIds = personServiceAdapter.queryMultiCompanyPersonsByCompanyId(UserContext.getUserContext().getCompanyId());
        if (personIds != null && personIds.getList() != null && !personIds.getList().isEmpty()) {
            lambda.or(items -> {
                for (Long personId : personIds.getList()) {
                    items.or().eq(UserPO::getPersonId, personId).ne(UserPO::getUserType, 2);
                }
            });

        }
        if (bo.getId() != null) {
            lambda.or().eq(UserPO::getId, bo.getId()).ne(UserPO::getUserType, 2);
        }
        if (!StringUtils.isEmpty(bo.getDescription())) {
            lambda.like(UserPO::getDescription, bo.getDescription());
        }
        if (bo.getHasLock() != null) {
            lambda.eq(UserPO::getHasLock, bo.getHasLock());
        }
        lambda.orderByDesc(UserPO::getCreateTime);
        Page<UserPO> pages = super.page(userPage, lambda);
        List<UserBO> collect = pages.getRecords().stream().map(t -> {
            UserBO userBO = new UserBO();
            BeanUtils.copyProperties(t, userBO);
            return userBO;
        }).collect(Collectors.toList());
        Page<UserBO> temp = new Page<>(pages.getCurrent(), pages.getSize(), pages.getTotal());
        temp.setRecords(collect);
        return temp;

    }

    @Override
    public UserBO getUserById(Long id) {
        UserPO userPo = userMapper.selectById(id);
        if (userPo != null) {
            UserBO userBo = new UserBO();
            BeanUtils.copyProperties(userPo, userBo);
            List<UserDirectoryPO> userDirectoryPOS = userDirectoryMapper.selectList(Wrappers.lambdaQuery(UserDirectoryPO.class).eq(UserDirectoryPO::getEnabled, true));
            if (!userDirectoryPOS.isEmpty()) {
                userBo.setLoginFirst(false);
            }
            return userBo;
        } else {
            return null;
        }
    }

    @Override
    public List<UserBO> batchGetByIds(List<Long> ids) {
        List<UserPO> userPOS = userMapper.selectBatchIds(ids);
        return userPOS.stream().map(t -> {
            UserBO userBO = new UserBO();
            BeanUtils.copyProperties(t, userBO);
            return userBO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserBO> batchGetByPersonIds(List<Long> ids) {
        List<UserPO> userPOS = userMapper.selectList(new QueryWrapper<UserPO>().lambda().in(UserPO::getPersonId, ids));
        return userPOS.stream().map(t -> {
            UserBO userBO = new UserBO();
            BeanUtils.copyProperties(t, userBO);
            return userBO;
        }).collect(Collectors.toList());
    }

    @Override
    public UserBO selectByPersonId(Long personId) {
        UserPO userPO = userMapper.selectOne(Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getPersonId, personId).eq(UserPO::getValid, true));
        if (userPO != null) {
            UserBO userBO = new UserBO();
            BeanUtils.copyProperties(userPO, userBO);
            return userBO;
        }
        return null;
    }

    @Override
    public List<UserRoleBO> batchGetByUserId(Long userId) {
        List<UserRolePO> userRolePOS = userRoleService.list(new QueryWrapper<UserRolePO>().lambda().eq(UserRolePO::getUserId, userId));
        return userRolePOS.stream().map(t -> {
            UserRoleBO userRoleBO = new UserRoleBO();
            BeanUtils.copyProperties(t, userRoleBO);
            return userRoleBO;
        }).collect(Collectors.toList());
    }

    @Override
    public UserBO findByUserName(String userName) {
        UserPO userPO = userMapper.selectOne(new QueryWrapper<UserPO>().lambda().eq(UserPO::getUserName, userName));
        UserBO userBO = new UserBO();
        if (userPO != null) {
            BeanUtils.copyProperties(userPO, userBO);
        }
        return userBO;
    }

    @Override
    public List<UserBO> findBatchUserName(String[] userName) {
        List<UserPO> userPOS = userMapper.selectList(new QueryWrapper<UserPO>().lambda().in(UserPO::getUserName, userName));
        return userPOS.stream().map(t -> {
            UserBO userBO = new UserBO();
            if (t != null) {
                BeanUtils.copyProperties(t, userBO);
            }
            return userBO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserBO> findCompanyAdminUser(Long companyId) {
        List<UserPO> userPOS = userMapper.selectList(new QueryWrapper<UserPO>().lambda()
                .in(UserPO::getCompanyId, companyId)
                .eq(UserPO::getUserType, 1));
        return userPOS.stream().map(t -> {
            UserBO userBO = new UserBO();
            if (t != null) {
                BeanUtils.copyProperties(t, userBO);
            }
            return userBO;
        }).collect(Collectors.toList());
    }

    @Override
    public Boolean deleteUserByPersonIds(Long[] personIds) {
        List<UserPO> userPOS = userMapper.selectList(new QueryWrapper<UserPO>().lambda()
                .in(UserPO::getPersonId, personIds));
        if (userPOS.isEmpty()) {
            return true;
        }
        List<Long> userIds = userPOS.stream().map(UserPO::getId).collect(Collectors.toList());
        LambdaUpdateWrapper<UserPO> updateWrapper = new UpdateWrapper<UserPO>().lambda()
                .in(UserPO::getId, userIds);
        int update = userMapper.delete(updateWrapper);
        LambdaUpdateWrapper<UserRolePO> updateUserRoleWrapper = new UpdateWrapper<UserRolePO>().lambda()
                .in(UserRolePO::getId, userIds);
        userRoleService.remove(updateUserRoleWrapper);
        // 登出在线用户
        onlineUserService.logoutOnlineUsersByUserIds(userIds);
        // 删除用户时删除用户角色
        rbacServiceAdapter.deleteRolesByUserIds(userIds);
        //删除人员表绑定用户
        personServiceAdapter.deleteUsersByPersonIds(Arrays.asList(personIds));

        deleteAuthUserKafkaMessage(BijectionUtils.applys(userPOS, x -> x.getUserName()));
        return update > 0;
    }

    @Override
    public Boolean deleteUserByCompanyId(Long companyId) {
        List<UserPO> userPOS = userMapper.selectList(new QueryWrapper<UserPO>().lambda()
                .eq(UserPO::getCompanyId, companyId));
        if (userPOS.isEmpty()) {
            return true;
        }
        List<Long> userIds = userPOS.stream().map(UserPO::getId).collect(Collectors.toList());
        LambdaUpdateWrapper<UserPO> updateWrapper = new UpdateWrapper<UserPO>().lambda()
                .in(UserPO::getCompanyId, companyId);
        int update = userMapper.delete(updateWrapper);
        LambdaUpdateWrapper<UserRolePO> updateUserRoleWrapper = new UpdateWrapper<UserRolePO>().lambda()
                .in(UserRolePO::getId, userIds);
        userRoleService.remove(updateUserRoleWrapper);
        // 登出在线用户
        onlineUserService.logoutOnlineUsersByUserIds(userIds);
        // 删除用户时删除用户角色
        rbacServiceAdapter.deleteRolesByUserIds(userIds);
        //删除人员表绑定用户
        List<Long> personIds = userPOS.stream().map(UserPO::getPersonId).collect(Collectors.toList());
        personServiceAdapter.deleteUsersByPersonIds(personIds);

        deleteAuthUserKafkaMessage(BijectionUtils.applys(userPOS, UserPO::getUserName));
        return update > 0;
    }

    @Override
    public void excuteExcelState(AuthExcelPO excelPO) {
        excelService.excuteExcelState(excelPO);
    }

    @Override
    public String getBapUserById(Long id, String includes) {
        UserPO userPo = userMapper.selectById(id);
        if (userPo == null) {
            throw new UserException(USER_NOT_EXIST);
        }
        // 根据人员ID获取人员信息
        if (userPo.getPersonId() != null) {
            JSONObject personInfo = personServiceAdapter.getPersonById(userPo.getPersonId(), null);
            JSONObject staff = personInfo.getJSONObject("staff");
            JSONObject company = personInfo.getJSONObject("company");
            JSONObject department = personInfo.getJSONObject("department");
            JSONObject mainPosition = personInfo.getJSONObject("mainPosition");

            BapCompanyBO companyBO = company.toJavaObject(BapCompanyBO.class);
            BapDepartmentBO departmentBO = department.toJavaObject(BapDepartmentBO.class);
            BapStaffBO bapStaffBO = staff.toJavaObject(BapStaffBO.class);
            BapPositionBO mainPositionBO = mainPosition.toJavaObject(BapPositionBO.class);
            companyBO.setValid(true);
            BapUserBO bapUserBO = new BapUserBO();
            bapUserBO.setId(userPo.getId())
                    .setName(userPo.getUserName())
                    .setPassword(userPo.getPassword())
                    .setStaff(bapStaffBO)
                    .setLocked(getLocked(userPo.getHasLock(), userPo.getLockReason()))
                    .setValid(true)
                    .setTimeZone(userPo.getTimeZone());
            bapStaffBO.setValid(true)
                    .setMainPosition(mainPositionBO);
            String sexCode = staff.getString("sex");
            if (sexCode != null) {
                bapStaffBO.setSex(getSystemCodeByCode(sexCode));
            }
            String workStatusCode = staff.getString("workStatus");
            if (workStatusCode != null) {
                bapStaffBO.setWorkStatus(getSystemCodeByCode(workStatusCode));
            }
            String securityClassCode = staff.getString("securityClass");
            if (securityClassCode != null) {
                bapStaffBO.setSecurityClass(getSystemCodeByCode(securityClassCode));
            }
            mainPositionBO.setCompany(companyBO)
                    .setDepartment(departmentBO)
                    .setValid(true);
            departmentBO.setCompany(companyBO)
                    .setValid(true);
            JSONObject managerObject = department.getJSONObject("manager");
            if (!managerObject.isEmpty()) {
                BapStaffBO manager = managerObject.toJavaObject(BapStaffBO.class);
                departmentBO.setManager(manager);
            }
            return JSONPlainSerializer.serializeAsJSON(bapUserBO, includes);
        } else {
            BapUserBO bapUserBO = new BapUserBO();
            bapUserBO.setId(userPo.getId())
                    .setName(userPo.getUserName())
                    .setPassword(userPo.getPassword())
                    .setLocked(getLocked(userPo.getHasLock(), userPo.getLockReason()))
                    .setValid(userPo.getValid())
                    .setTimeZone(userPo.getTimeZone());
            return JSONPlainSerializer.serializeAsJSON(bapUserBO, includes);
        }
    }

    private BapSystemCodeBO getSystemCodeByCode(String code) {
        SystemCodeResultDTO systemCodeByCode = systemCodeServiceAdapter.getSystemCodeByCode(code);
        BapSystemCodeBO bapSystemCodeBO = new BapSystemCodeBO();
        bapSystemCodeBO.setId(systemCodeByCode.getId())
                .setCode(systemCodeByCode.getCode())
                .setValue(systemCodeByCode.getName())
                .setZhCnValue(systemCodeByCode.getDisplayName())
                .setType(systemCodeByCode.getType())
                .setMemo(systemCodeByCode.getMemo())
                .setCodeDesA(systemCodeByCode.getDesA())
                .setCodeDesB(systemCodeByCode.getDesB())
                .setCodeDesC(systemCodeByCode.getDesC())
                .setDefaultFlag(systemCodeByCode.getDefaultFlag() == 1);
        return bapSystemCodeBO;
    }

    private Integer getLocked(Boolean hasLock, Integer lockReason) {
        Boolean isLocked = Optional.ofNullable(hasLock).orElse(false);
        if (!isLocked) {
            return 0;
        } else {
            return Objects.equals(lockReason, UserLockReasonEnum.ARTIFICIAL.getCode()) ? 2 : 1;
        }
    }

    @Override
    public BapUserInfoBO getCurrentLoginInfo() {
        UserContext userContext = UserContext.getUserContext();
        BapUserInfoBO bapUserInfoBO = new BapUserInfoBO();
        bapUserInfoBO.setUser(new BapUserInfoBO.Info().setId(userContext.getUserId()).setName(userContext.getUserName()));
        if (userContext.getDepartmentId() != null) {
            bapUserInfoBO.setDepartment(new BapUserInfoBO.Info().setId(userContext.getDepartmentId()).setCode(userContext.getDepartmentCode()).setName(userContext.getDepartmentName()));
        }
        if (userContext.getStaffId() != null) {
            bapUserInfoBO.setStaff(new BapUserInfoBO.Info().setId(userContext.getStaffId()).setCode(userContext.getStaffCode()).setName(userContext.getStaffName()));
        }
        if (userContext.getPositionId() != null) {
            bapUserInfoBO.setMainPosition(new BapUserInfoBO.Info().setId(userContext.getPositionId()).setCode(userContext.getPositionCode()).setName(userContext.getPositionName()));
        }
        if (userContext.getCompanyId() != null) {
            bapUserInfoBO.setCompany(new BapUserInfoBO.Info().setId(userContext.getCompanyId()).setCode(userContext.getCompanyCode()).setName(userContext.getCompanyName()));
        }
        return bapUserInfoBO;
    }

    @Override
    public void changeCurrentCompany(String userName, Long companyId) {
        userMapper.updateCurrentCompanyId(userName, companyId);
    }

    @Override
    public void updateEmailOrPhone(Long personId, String email, String phone) {
        PersonUpdateDTO personUpdateDTO = new PersonUpdateDTO();
        personUpdateDTO.setId(personId);
        if (!StringUtils.isEmpty(email)) {
            personUpdateDTO.setEmail(email);
        }
        if (!StringUtils.isEmpty(phone)) {
            personUpdateDTO.setEmail(phone);
        }
        personServiceAdapter.updatePerson(personUpdateDTO);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void modifyCurrUserPassword(String password, String rePassword, String prePassword) {
//        if (org.apache.commons.lang3.StringUtils.isNotEmpty(password)) {
//            if (!password.matches(pattern)) {
//                throw new UserException(UserErrorEnum.USER_PASSWORD_RULE_ERROR);
//            }
//        }
        verify(password);
        if (!password.trim().equals(rePassword.trim())) {
            throw new UserException(UserErrorEnum.USER_PASSWORD_NOT_RIGHT);
        }
        UserContext userContext = UserContext.getUserContext();
        Long userId = userContext.getUserId();
        UserBO userBO = this.getUserById(userId);
        if (password.trim().equals(prePassword.trim())) {
            UserPO userPO = new UserPO();
            userPO.setLoginFirst(false);
            userMapper.update(userPO, Wrappers.lambdaUpdate(UserPO.class).eq(UserPO::getUserName, userBO.getUserName()));
            return;
        }
        //
        if (BCryptUtil.matches(prePassword, userBO.getPassword())) {
            String encryPassword = BCryptUtil.encode(password);
            UserPO userPO = new UserPO();
            userPO.setLoginFirst(false);
            userPO.setPassword(encryPassword);
            userMapper.update(userPO, Wrappers.lambdaUpdate(UserPO.class).eq(UserPO::getUserName, userBO.getUserName()));
        } else {
            throw new UserException(UserErrorEnum.USER_PASSWORD_CURRENT_ERROR);
        }
    }

    @Override
    public UserBO selectUserName(String userName) {
        UserPO userPO = userMapper.selectUserName(userName);
        UserBO userBO = new UserBO();
        if (userPO != null) {
            BeanUtils.copyProperties(userPO, userBO);
        }
        return userBO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseBO login(LoginBO loginBO,
                                 String realIp,
                                 String quatoName,
                                 String deviceType,
                                 String cookieValue,
                                 String ticket) {
        if (branchOfficeProperties.getEnable()) {
            return branchOfficeService.login(loginBO, realIp, deviceType, quatoName);
        }
        UserBO userBO = this.findByUserName(loginBO.getUserName());
        boolean ipBlackWhite = ipBlackWhiteService.verifyIp(realIp, loginBO.getCompanyId() != null ? loginBO.getCompanyId() : userBO.getCompanyId());
        if (!ipBlackWhite) {
            throw new UserException(ACCESS_IP_FORBIDDEN);
        }
        List<UserDirectoryPO> userDirectoryPOS = userDirectoryMapper.selectList(Wrappers.lambdaQuery(UserDirectoryPO.class).eq(UserDirectoryPO::getEnabled, true));
        loginBO.setLdap(!userDirectoryPOS.isEmpty());
        if ("admin".equals(loginBO.getUserName())) {
            loginBO.setLdap(false);
        }
        if (supOSEnabled) {
            if ("admin".equals(loginBO.getUserName())) {
                verifySystemAdmin(loginBO.getUserName());
            } else if (supOSEnabled) {
                verifyLicense(quatoName, deviceType);
            }
        }


        JSONObject jsonObject = new JSONObject();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        boolean ticketExists = false;
        if (StringUtils.isEmpty(ticket)) {
            jsonObject = loginKeycloak(loginBO, realIp, deviceType);
        } else {
            String key = String.format(AUTH_TICKET, ticket);
            Map entries = (stringRedisTemplate.opsForHash().entries(key));
            if (entries != null && !entries.isEmpty()) {
                jsonObject.putAll(entries);
                uuid = ticket;
                ticketExists = true;
            } else {
                jsonObject = loginKeycloak(loginBO, realIp, deviceType);
            }

        }
        verifyLoginResult(jsonObject);
        String key = String.format(AUTH_TICKET, uuid);
        LoginResponseBO loginResponseBO = convertLoginResponse(loginBO, userBO);
        loginResponseBO.setTicket(uuid);
        loginResponseBO.setTenantId(RpcContext.getContext().getTenantId());
        if (loginBO.getLdap()) {
            loginResponseBO.setStatus("ok");
        } else {
            loginResponseBO.setStatus(userBO.getLoginFirst() ? "firstLogin" : "ok");
        }
        loginResponseBO.setUserType(userBO.getUserType());
        loginResponseBO.setAccessToken(jsonObject.getString("access_token"));
        loginResponseBO.setRefreshToken(jsonObject.getString("refresh_token"));
        loginResponseBO.setExpiresIn(jsonObject.getInteger("expires_in"));
        loginResponseBO.setClientId(deviceType + "_" + RpcContext.getContext().getTenantId());

        //缓存ticket信息
        if (!ticketExists) {
            //增加在线用户
            OnlineUserBO onlineUserBO = buildOnlineUserBO(realIp, deviceType, uuid, loginResponseBO);
            onlineUserService.createOnlineUser(onlineUserBO);
            cacheTicket(jsonObject, key, loginResponseBO);
        }
        if ("admin".equals(loginBO.getUserName()) && StringUtils.isEmpty(ticket)) {
            addSystemAdmin(loginBO.getUserName());
        }
        if (!StringUtils.isEmpty(cookieValue)) {
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(cookieValue);
            if (!entries.isEmpty()) {
                String redirectUri = (String) entries.get("redirectUri");
                String state = (String) entries.get("state");
                try {
                    String code = MD5Generator.getInstance().generateValue();
                    loginResponseBO.setCode(code);
                    HashMap<String, String> map = new HashMap<>();
                    map.put("ticket", loginResponseBO.getTicket());
                    map.put("state", state);
                    stringRedisTemplate.opsForHash().putAll(code, map);
                    stringRedisTemplate.expire(code, 10, TimeUnit.MINUTES);
                } catch (Exception e) {
                    log.error("error ====>", e);
                }
                loginResponseBO.setState(state);
                loginResponseBO.setRedirectUri(redirectUri);
                stringRedisTemplate.delete(cookieValue);
            }
        }
        //记录登录日志
        loginLogService.generateLoginLog(loginResponseBO, deviceType, realIp);
        return loginResponseBO;
    }

    @Override
    public void verifyLoginResult(JSONObject jsonObject) {
        if (jsonObject.containsKey("code")) {
            if (jsonObject.getInteger("code").compareTo(USER_PASSWORD_ERROR.getCode()) == 0) {
                throw new UserException(USER_PASSWORD_ERROR);
            }
            if (jsonObject.getInteger("code").compareTo(USER_NOT_EXIST.getCode()) == 0) {
                throw new UserException(USER_NOT_EXIST);
            }
            if (jsonObject.getInteger("code").compareTo(GET_IP_FAILED.getCode()) == 0) {
                throw new UserException(GET_IP_FAILED);
            }
            if (jsonObject.getInteger("code").compareTo(ACCESS_IP_FORBIDDEN.getCode()) == 0) {
                throw new UserException(ACCESS_IP_FORBIDDEN);
            }
            if (jsonObject.getInteger("code").compareTo(USER_HAS_LOCK.getCode()) == 0) {
                throw new UserException(USER_HAS_LOCK);
            }
            if (jsonObject.getInteger("code").compareTo(USER_OR_PASSWORD_ERROR.getCode()) == 0) {
                throw new UserException(USER_OR_PASSWORD_ERROR);
            }
        }
    }

    private void verifyLicense(String quatoName, String deviceType) {
        if (deviceType.equals("mobile")) {
            return;
        }
        //校验在线用户数
        Integer tenantTotal = quotaClientAdapter.getTenantFeatureQuota(RpcContext.getContext().getTenantId(), quatoName);
        Integer auth = onlineUserService.getTotalOnline(deviceType);
        Integer total = quotaClientAdapter.getFeatureQuota(quatoName);
        String totalStr = stringRedisTemplate.opsForValue().get(quatoName);
        Integer totalOnline = 0;
        if (tenantTotal == null || total == null) {
            throw new UserException(USER_ONLIEN_ZERO);
        }
        if (!StringUtils.isEmpty(totalStr)) {
            totalOnline = Integer.valueOf(totalStr);
        }
        if (auth > tenantTotal - 1 || totalOnline > total - 1) {
            throw new UserException(USER_ONLIEN_LIMITE);
        }
    }

    private void verifySystemAdmin(String userName) {
        String total = stringRedisTemplate.opsForValue().get(RpcContext.getContext().getTenantId() + "_" + userName);
        if (!StringUtils.isEmpty(total)) {
            if (Integer.parseInt(total) >= authProperties.getMaxAdminUserSize()) {
                throw new UserException(USER_ONLIEN_LIMITE);
            }
        }
    }

    public void addSystemAdmin(String userName) {
        stringRedisTemplate.opsForValue().increment(RpcContext.getContext().getTenantId() + "_" + userName);
    }

    public void cacheTicket(JSONObject jsonObject, String key, LoginResponseBO loginResponseBO) {
        Map<String, String> authorizationMap = new HashMap<>();
        authorizationMap.put(ACCESS_TOKEN, jsonObject.getString(ACCESS_TOKEN));
        authorizationMap.put(REFRESH_TOKEN, jsonObject.getString(REFRESH_TOKEN));
        authorizationMap.put(TOKEN_TYPE, jsonObject.getString(TOKEN_TYPE));
        authorizationMap.put(TENANT_ID, loginResponseBO.getTenantId());
        authorizationMap.put(CLIENT_ID, loginResponseBO.getClientId());
        authorizationMap.put(USER_NAME, loginResponseBO.getUsername());

        authorizationMap.put(LOGIN_TYPE, loginResponseBO.getLoginType());
        if (!StringUtils.isEmpty(loginResponseBO.getProtocolType())) {
            authorizationMap.put(PROTOCOL_TYPE, loginResponseBO.getProtocolType());
        }
        if ("1".equals(loginResponseBO.getLoginType())) {
            authorizationMap.put("clientAccessToken", loginResponseBO.getClientAccessToken());
            authorizationMap.put("clientRefreshToken", loginResponseBO.getClientRefreshToken());
        }

        LoginResponseBO.Company currentCompany = loginResponseBO.getCurrentCompany();
        if (currentCompany != null && currentCompany.getId() != null) {
            authorizationMap.put(COMPANY_ID, String.valueOf(currentCompany.getId()));
            authorizationMap.put(COMPANY_CODE, currentCompany.getCode());
        }
        stringRedisTemplate.opsForHash().putAll(key, authorizationMap);
        HashMap<String, String> map = new HashMap<>();
        map.put(TENANT_ID, loginResponseBO.getTenantId());
        stringRedisTemplate.opsForHash().putAll(String.format(TENANT_TICKET, loginResponseBO.getTicket()), map);
        stringRedisTemplate.expire(key, jsonObject.getLongValue(EXPIRES_IN), TimeUnit.SECONDS);
    }

    public OnlineUserBO buildOnlineUserBO(String realIp, String deviceType, String uuid, LoginResponseBO loginResponseBO) {
        OnlineUserBO onlineUserBO = new OnlineUserBO();
        onlineUserBO.setTicket(uuid);
        if (loginResponseBO.getCurrentCompany() != null) {
            onlineUserBO.setCompanyId(loginResponseBO.getCurrentCompany().getId());
        }
        onlineUserBO.setLoginIp(realIp);
        onlineUserBO.setUserId(loginResponseBO.getUserId());
        onlineUserBO.setUserName(loginResponseBO.getUsername());
//        onlineUserBO.setLoginTime(DateTimeUtil.getUTC0());
        if (!StringUtils.isEmpty(deviceType)) {
            onlineUserBO.setDeviceType(deviceType);
        }
        return onlineUserBO;
    }

    @Override
    public JSONObject loginKeycloak(LoginBO loginBO, String realIp, String deviceType) {
        Map<String, Object> map = new HashMap<>();
        map.put(USERNAME, loginBO.getUserName());
        map.put(PASSWORD, loginBO.getPassword());
        map.put("ldap", loginBO.getLdap());
        map.put(CLIENT_ID, deviceType + "_" + RpcContext.getContext().getTenantId());
        map.put(GRANT_TYPE, loginBO.getGrantType());
        String authenticateJSON = tokenClient.keycloakLogin(RpcContext.getContext().getTenantId(), map);
        JSONObject jsonObject = JSON.parseObject(authenticateJSON);
        return jsonObject;
    }

    private boolean compareCompanies(Set<Long> currentCompanyIds, Set<Long> lastCompanyIds) {
        if (currentCompanyIds.size() != lastCompanyIds.size()) {
            return false;
        }
        return currentCompanyIds.containsAll(lastCompanyIds);
    }

    private String verify(String password) {
        StringBuilder str = new StringBuilder();
        StringBuilder remind = new StringBuilder();
        LoginConfigBO loginConfig = passwordService.getLoginConfig();
        String passwordRemind = null;
        if (Constants.COMBINATION_PWD_RULE.compareTo(loginConfig.getRuleType()) == 0) {
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
        if (loginConfig.getMin() != null) {
            str.append(all).append("{").append(loginConfig.getMin()).append(",");
        }

        if (loginConfig.getMax() != null) {
            str.append(loginConfig.getMax()).append("}");
        }
        Pattern compile = null;
        if (Constants.COMBINATION_PWD_RULE.compareTo(loginConfig.getRuleType()) == 0) {
            try {
                compile = Pattern.compile("^" + str + "$");
            } catch (PatternSyntaxException e) {
                throw new UserException(PASSWD_REGREX_WRONG);
            }

            passwordRemind = !StringUtils.isEmpty(remind.toString()) ? remind.substring(0, remind.length() - 1) : "大小写、数字、特殊字符（仅支持!@#$%^&*();'?.,)";
        } else {
            passwordRemind = loginConfig.getHint();
            try {
                compile = Pattern.compile(loginConfig.getRegularExpression());
            } catch (PatternSyntaxException e) {
                throw new UserException(PASSWD_REGREX_WRONG);
            }
        }

        Matcher matcher = compile.matcher(password);
        if (!matcher.find()) {
            if (CUSTOM_PWD_RULE.compareTo(loginConfig.getRuleType()) == 0) {
                throw new UserException(USER_PASSWORD_RULE_ERROR1, loginConfig.getHint());
            } else {
                String remindStr = String.format(Constants.RULE_ERROR, passwordRemind, loginConfig.getMin(), loginConfig.getMax());
                throw new UserException(UserErrorEnum.USER_PASSWORD_RULE_ERROR1, remindStr);
            }
        }
        return BCryptUtil.encode(password);
    }

    @Override
    public void logout(String ticket) {

        String tenantId = RpcContext.getContext().getTenantId();

        Map<Object, Object> authorizationMap = authTicketCache.getMapByTicket(ticket);
        if (!authorizationMap.isEmpty()) {
            String protocolType = (String) authorizationMap.get(PROTOCOL_TYPE);
            if ("bluetron".equals(protocolType)) {
                IdentityCenterConfigBO identityCenterConfigBO = thirdAuthService.queryClientIdentityConfigInfo(protocolType);
                String appId = identityCenterConfigBO.getAppId();
                String appSecret = identityCenterConfigBO.getAppSecret();
                String logoutUrl = identityCenterConfigBO.getLogoutUrl();
                String token = (String) authorizationMap.get("clientAccessToken");
                thirdAuthService.logout(protocolType, appId, appSecret, token, logoutUrl);
            }
            onlineUserService.removeOnlineUserByTicketActByManual(ticket, tenantId);
            //记录登出日志
            loginLogService.saveLogoutLog(ticket, Constants.ACTIVE_LOGOUT);
        }
    }

    @Override
    public LoginResponseBO companyChange(Long companyId, String ticket) {
        Map<Object, Object> authorizationMap = authTicketCache.getMapByTicket(ticket);
        String accessToken = (String) authorizationMap.get(ACCESS_TOKEN);
        String tokenType = (String) authorizationMap.get(TOKEN_TYPE);
        String refreshToken = (String) authorizationMap.get(REFRESH_TOKEN);
        String clientId = (String) authorizationMap.get(CLIENT_ID);
        String tenantId = (String) authorizationMap.get(TENANT_ID);
        String userName = (String) authorizationMap.get(USER_NAME);
        HashMap<String, String> map = new HashMap<>();
        map.put(CLIENT_ID, clientId);
        map.put(REFRESH_TOKEN, refreshToken);
        map.put(GRANT_TYPE, REFRESH_TOKEN);
        map.put(COMPANY_ID, companyId.toString());
        String authenticateJSON = tokenClient.refreshToken(RpcContext.getContext().getTenantId(), map, tokenType + " " + accessToken);
        AuthorizationBO authorizationBO = JSONObject.parseObject(authenticateJSON, AuthorizationBO.class);
        authorizationBO.setCompanyId(companyId);
        authorizationBO.setClientId(clientId);
        authorizationBO.setTenantId(tenantId);
        authorizationBO.setUserName(userName);
        authTicketCache.storeAuthorization(ticket, authorizationBO);
        this.changeCurrentCompany(userName, companyId);
        onlineUserService.updateOnlineUserCompanyId(ticket, companyId, authorizationBO.getAccessToken());
        UserBO userBO = this.findByUserName(userName);
        LoginBO loginBO = new LoginBO();
        loginBO.setCompanyId(companyId);
        LoginResponseBO loginResponseBO = convertLoginResponse(loginBO, userBO);
        loginResponseBO.setTicket(ticket);
        loginResponseBO.setTenantId(RpcContext.getContext().getTenantId());
        if (!StringUtils.isEmpty(userBO.getLdapUserName())) {
            loginResponseBO.setStatus("ok");
        } else {
            loginResponseBO.setStatus(userBO.getLoginFirst() ? "firstLogin" : "ok");
        }
        loginResponseBO.setUserType(userBO.getUserType());
        return loginResponseBO;
    }

    @Override
    public void refreshToken(String ticket, HttpServletResponse response) {
        try {
            Long expire = authTicketCache.getExpire(ticket);
            if (expire == -2) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            Map<Object, Object> authorizationMap = authTicketCache.getMapByTicket(ticket);
            String loginType = (String) authorizationMap.get("loginType");
            log.info("登录类型loginType: {}", loginType);
            // 校验缓存会话中的loginType参数,默认为0,表示supOS登录,则原来的保活机制不变;如果为1,表示竹云单点登录.
            String accessToken = (String) authorizationMap.get(ACCESS_TOKEN);
            String tokenType = (String) authorizationMap.get(TOKEN_TYPE);
            String refreshToken = (String) authorizationMap.get(REFRESH_TOKEN);
            String companyId = (String) authorizationMap.get(COMPANY_ID);
            String clientId = (String) authorizationMap.get(CLIENT_ID);
            HashMap<String, String> map = new HashMap<>();
            map.put(CLIENT_ID, clientId);
            map.put(REFRESH_TOKEN, refreshToken);
            map.put(GRANT_TYPE, REFRESH_TOKEN);
            String authenticateJSON = tokenClient.refreshToken(RpcContext.getContext().getTenantId(), map, tokenType + " " + accessToken);
            if (authenticateJSON.contains("error")) {
                log.info("保活失败");
                return;
            }
            AuthorizationBO authorizationBO = JSONObject.parseObject(authenticateJSON, AuthorizationBO.class);
            if (companyId != null) {
                authorizationBO.setCompanyId(Long.valueOf(companyId));
            }
            if ("1".equals(loginType)) {
                try {
                    // loginType为1,表示是竹云单点登录,保活supOS会话,同时需要刷新保活第三方的accessToken
                    ThirdAuthServiceImpl.ThirdToken thirdToken = refreshClientToken(authorizationMap);
                    log.info("refreshToken from zhuyun , response body is: {}", thirdToken);
                    if (null == thirdToken) {
                        throw new ThirdAuthException(ThirdAuthErrorEnum.REFRESH_TOKEN_BY_ZHUYU_FAILED);
                    }
                    authorizationBO.setClientAccessToken(thirdToken.getAccessToken());
                    authorizationBO.setClientRefreshToken(thirdToken.getRefreshToken());
                    authorizationBO.setLoginType("1");
                    authorizationBO.setProtocolType((String) authorizationMap.get(PROTOCOL_TYPE));
                    Integer expires_in = thirdToken.getExpireIn();
                    authorizationBO.setExpiresIn(expires_in <= authorizationBO.getExpiresIn() ? expires_in : authorizationBO.getExpiresIn());
                } catch (Exception e) {
                    log.error("refresh third token is error", e);
                }
            }
            authorizationBO.setClientId(clientId);
            authorizationBO.setTenantId(RpcContext.getContext().getTenantId());
            String userName = UserContext.getUserContext().getUserName();
            authorizationBO.setUserName(userName);
            authTicketCache.storeAuthorization(ticket, authorizationBO);
            boolean isExpire = branchOfficeService.refreshToken(ticket);
            if (isExpire) {
                logout(ticket);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (Exception e) {
            log.error("refresh token is error", e);
        }
    }

    private ThirdAuthServiceImpl.ThirdToken refreshClientToken(Map<Object, Object> authorizationMap) {
        String protocolType = (String) authorizationMap.get("protocolType");
        IdentityCenterConfigBO identityCenterConfigBO = thirdAuthService.queryClientIdentityConfigInfo(protocolType);
        String appId = identityCenterConfigBO.getAppId();
        String appSecret = identityCenterConfigBO.getAppSecret();
        String refreshTokenUrl = identityCenterConfigBO.getRefreshUrl();
        String clientRefreshToken = (String) authorizationMap.get("clientRefreshToken");
        return thirdAuthService.refreshToken(protocolType, appId, appSecret, clientRefreshToken, refreshTokenUrl);
    }

    @Override
    public LoginResponseBO accessToken(String ticket) {
        Map<Object, Object> authorizationMap = authTicketCache.getMapByTicket(ticket);
        if (authorizationMap.isEmpty()) {
            return null;
        }
        String accessToken = (String) authorizationMap.get(ACCESS_TOKEN);
        String tokenType = (String) authorizationMap.get(TOKEN_TYPE);
        String refreshToken = (String) authorizationMap.get(REFRESH_TOKEN);
        String companyId = (String) authorizationMap.get(COMPANY_ID);
        String clientId = (String) authorizationMap.get(CLIENT_ID);
        String userName = (String) authorizationMap.get(USER_NAME);
        HashMap<String, String> map = new HashMap<>();
        map.put(CLIENT_ID, clientId);
        map.put(REFRESH_TOKEN, refreshToken);
        map.put(GRANT_TYPE, REFRESH_TOKEN);
        map.put(COMPANY_ID, companyId);
        String authenticateJSON = tokenClient.refreshToken(RpcContext.getContext().getTenantId(), map, tokenType + " " + accessToken);
        AuthorizationBO authorizationBO = JSONObject.parseObject(authenticateJSON, AuthorizationBO.class);
        authorizationBO.setCompanyId(Long.valueOf(companyId));
        authorizationBO.setClientId(clientId);
        authorizationBO.setTenantId(RpcContext.getContext().getTenantId());
        authorizationBO.setUserName(userName);
        authTicketCache.storeAuthorization(ticket, authorizationBO);
        LoginResponseBO loginResponseBO = new LoginResponseBO();
        loginResponseBO.setAccessToken(authorizationBO.getAccessToken());
        loginResponseBO.setRefreshToken(authorizationBO.getRefreshToken());
        loginResponseBO.setExpiresIn(authorizationBO.getExpiresIn());
        return loginResponseBO;

    }

    /**
     * 将用户信息转换成登录响应参数
     */
    public LoginResponseBO convertLoginResponse(LoginBO loginBO, UserBO userBO) {
        if (StringUtils.isEmpty(userBO.getUserName())) {
            throw new UserException(USER_OR_PASSWORD_ERROR);
        }
        LoginResponseBO loginResponseBO = new LoginResponseBO();
        LoginResponseBO.User user = new LoginResponseBO.User();
        user.setUserName(userBO.getUserName());
        user.setId(userBO.getId());
        user.setUserType(userBO.getUserType());
        loginResponseBO.setUserId(userBO.getId());
        loginResponseBO.setUsername(userBO.getUserName());
        loginResponseBO.setUser(user);
        loginResponseBO.setUserType(userBO.getUserType());
        ListResult<CompanyDTO> companies = personServiceAdapter.queryCompanyIdByPersonId(userBO.getPersonId());
        List<LoginResponseBO.Company> collect = companies.getList().stream().map(t -> {
            LoginResponseBO.Company loginCompanyResponse = new LoginResponseBO.Company();
            loginCompanyResponse.setCode(t.getCode());
            loginCompanyResponse.setId(t.getId());
            loginCompanyResponse.setName(t.getShortName());
            return loginCompanyResponse;
        }).collect(Collectors.toList());
        loginResponseBO.setCompanies(collect);
        Set<Long> currentCompanyIds = collect.stream().map(LoginResponseBO.Company::getId).collect(Collectors.toSet());
        Long userId = loginResponseBO.getUser().getId();
        String userkey = String.format(Constants.LAST_UID_CIDS, RpcContext.getContext().getTenantId(), userId);
        if (userBO.getCurrentCompanyId() != null) {
            Set<String> companyIds = stringRedisTemplate.opsForSet().members(userkey);
            Set<Long> lastCompanyIds = (companyIds == null || companyIds.isEmpty()) ? Collections.emptySet() : companyIds.stream().map(Long::parseLong).collect(Collectors.toSet());
            // 比较公司组织结构是否改变
            if (!compareCompanies(currentCompanyIds, lastCompanyIds)) {
                if (currentCompanyIds.size() > 1) {
                    // 去除返回值中的当前公司
                    loginResponseBO.setCurrentCompany(null);
                } else {
                    if (collect.size() == 1) {
                        LoginResponseBO.Company temp = new LoginResponseBO.Company();
                        temp.setName(collect.get(0).getName());
                        temp.setId(collect.get(0).getId());
                        temp.setCode(collect.get(0).getCode());
                        loginResponseBO.setCurrentCompany(temp);
                    }
                    this.changeCurrentCompany(loginResponseBO.getUser().getUserName(), collect.get(0).getId());
                }
                // 用户最后所属公司持久化
                stringRedisTemplate.delete(userkey);
                if (currentCompanyIds != null && !currentCompanyIds.isEmpty()) {
                    String[] companyIdArray = currentCompanyIds.stream().map(Object::toString).toArray(String[]::new);
                    stringRedisTemplate.opsForSet().add(userkey, companyIdArray);
                }
            } else {
                Optional<LoginResponseBO.Company> first = collect.stream().filter(t -> Objects.equals(userBO.getCurrentCompanyId(), t.getId())).findFirst();
                first.ifPresent(loginResponseBO::setCurrentCompany);
            }
        } else {
            if (collect.size() == 1) {
                LoginResponseBO.Company temp = new LoginResponseBO.Company();
                temp.setName(collect.get(0).getName());
                temp.setId(collect.get(0).getId());
                temp.setCode(collect.get(0).getCode());
                loginResponseBO.setCurrentCompany(temp);
                this.changeCurrentCompany(loginResponseBO.getUser().getUserName(), collect.get(0).getId());
            } else {
                if (loginBO.getCompanyId() != null) {
                    Optional<LoginResponseBO.Company> first = collect.stream().filter(t -> Objects.equals(loginBO.getCompanyId(), t.getId())).findFirst();
                    if (first.isPresent()) {
                        loginResponseBO.setCurrentCompany(first.get());
                        this.changeCurrentCompany(loginResponseBO.getUser().getUserName(), first.get().getId());
                    }
                }
            }
            // 用户最后所属公司持久化
            stringRedisTemplate.delete(userkey);
            if (currentCompanyIds != null && !currentCompanyIds.isEmpty()) {
                String[] companyIdArray = currentCompanyIds.stream().map(Object::toString).toArray(String[]::new);
                stringRedisTemplate.opsForSet().add(userkey, companyIdArray);
            }
        }
        return loginResponseBO;
    }

    @Override
    public Page<UserBO> openGetUsers(Page<UserBO> page, String keyword, String companyCode, String roleCode, String modifyTime, HttpServletResponse response) {
        if (page.getSize() > 500) {
            errorResponse(response, 400, 100106500, "pageSize超过最大值500");
            return null;
        } else if (page.getSize() < 1) {
            errorResponse(response, 400, 100106500, "pageSize小于最小值1");
            return null;
        }
        Result<CompanyResultDTO> companyByCode = personServiceAdapter.findCompanyByCode(companyCode);
        if (companyByCode.getData().getId() == null) {
            errorResponse(response, 400, 100106014, companyCode + "对应公司不存在");
            return null;
        }
        Page<UserPO> userPage = new Page<>(page.getCurrent(), page.getSize());
        LambdaQueryWrapper<UserPO> query = Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getCompanyId, companyByCode.getData().getId()).ne(UserPO::getUserType, 2).orderByDesc(UserPO::getModifyTime);
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(keyword)) {
            query.and(q -> q.like(UserPO::getDescription, keyword).or().like(UserPO::getUserName, keyword));
        }
        if (!StringUtils.isEmpty(modifyTime)) {
            try {
                String replace = modifyTime.replace(" ", "+");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                Date date = sdf.parse(replace);
                LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
                query.gt(UserPO::getModifyTime, Timestamp.valueOf(ldt));
            } catch (Exception e) {
                if (companyByCode.getData().getId() == null) {
                    errorResponse(response, 400, 100106500, "时间格式不对");
                    return null;
                }
            }
        }
        if (!StringUtils.isEmpty(roleCode)) {
            List<RoleDTO> roleByCodes = rbacServiceAdapter.findRoleByCodes(Arrays.asList(roleCode));
            if (roleByCodes.isEmpty()) {
                errorResponse(response, 400, 100106014, roleCode + "对应角色不存在");
                return null;
            }
            List<UserRolePO> userRolePOS = userRoleMapper.selectList(Wrappers.lambdaQuery(UserRolePO.class).eq(UserRolePO::getRoleId, roleByCodes.get(0).getId()));
            if (userRolePOS != null && !userRolePOS.isEmpty()) {
                query.and(items -> {
                    for (UserRolePO userRolePO : userRolePOS) {
                        items.or().eq(UserPO::getId, userRolePO.getUserId()).ne(UserPO::getUserType, 2);
                    }
                });
            }
        }
        Page<UserPO> pages = super.page(userPage, query);
        List<UserBO> collect = pages.getRecords().stream().map(t -> {
            UserBO userBO = new UserBO();
            BeanUtils.copyProperties(t, userBO);
            return userBO;
        }).collect(Collectors.toList());
        Page<UserBO> temp = new Page<>(pages.getCurrent(), pages.getSize(), pages.getTotal());
        temp.setRecords(collect);
        return temp;
    }

    @Override
    public List<UserBO> openGetAllUsers(UserBO bo) {
        LambdaQueryWrapper<UserPO> query = Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getCompanyId, bo.getCompanyId()).ne(UserPO::getUserType, 2);
        ListResult<Long> personIds = personServiceAdapter.queryMultiCompanyPersonsByCompanyId(bo.getCompanyId());
        if (personIds != null && personIds.getList() != null && !personIds.getList().isEmpty()) {
            query.or(items -> {
                for (Long personId : personIds.getList()) {
                    items.or().eq(UserPO::getPersonId, personId).ne(UserPO::getUserType, 2);
                }
            });
        }
        query.orderByDesc(UserPO::getCreateTime);
        List<UserPO> userPOS = userMapper.selectList(query);
        List<UserBO> collect = userPOS.stream().map(t -> {
            UserBO userBO = new UserBO();
            BeanUtils.copyProperties(t, userBO);
            return userBO;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void loadAllUser(Long userDirectoryId, Long companyId, NamingEnumeration<NameClassPair> list) {
        try {
            ArrayList<UserPO> userPOS = new ArrayList<>();
            if (list != null) {
                while (list.hasMore()) {
                    NameClassPair ncp = list.next();
                    String cn = ncp.getName();
                    if (cn.indexOf("=") != -1) {
                        int index = cn.indexOf("=");
                        cn = cn.substring(index + 1, cn.length());
                        UserPO userPO = new UserPO();
                        userPO.setId(IDGenerator.newInstance().generate().longValue());
                        userPO.setUserDirectoryId(userDirectoryId);
                        userPO.setUserName(cn);
                        userPO.setPassword(BCryptUtil.encode("Supos@1304@"));
                        userPO.setUserType(UserTypeEnum.COMMON_USER.getCode());
                        userPO.setLdapUserName(ncp.getNameInNamespace());
                        userPO.setCompanyId(companyId);
                        userPO.setHasLock(false);
                        userPOS.add(userPO);
                        Result<Long> result = personServiceAdapter.addVirtualPerson(userPO.getUserName(), userPO.getCompanyId());
                        userPO.setPersonId(result.getData());
                        if (userPOS.size() == 100) {
                            this.saveBatch(userPOS);
                            userPOS.clear();
                        }
                    }
                }
                if (!userPOS.isEmpty()) {
                    this.saveBatch(userPOS);
                    userPOS.clear();
                }
            }
        } catch (NamingException e) {
            log.error("loadAllUser error===>", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeletByNames(List<String> userNames, HttpServletResponse response) {
        if (userNames != null && !userNames.isEmpty()) {
            if (userNames.size() > 100) {
                errorResponse(response, 400, 100106500, "删除个数超过最大值100");
                return;
            }
            List<UserPO> userPOS = userMapper.selectList(Wrappers.lambdaQuery(UserPO.class).in(UserPO::getUserName, userNames));
            StringBuilder str = new StringBuilder();
            if (!userPOS.isEmpty()) {
                userPOS.forEach(t -> {
                    if (t.getUserType().compareTo(UserTypeEnum.SYSTEM_USER.getCode()) == 0) {
                        str.append(t.getUserName()).append(",");
                    }
                });
                if (str.length() > 0) {
                    errorResponse(response, 400, 100106018, str.substring(0, str.length() - 1) + "系统管理员用户不可删除");
                    return;
                }
                List<Long> userIds = userPOS.stream().map(UserPO::getId).collect(Collectors.toList());
                TaskTotalsDTO taskTotal = taskServiceApiAdapter.getTaskTotal(userIds);
                if (taskTotal.isContainTask()) {
                    List<Long> ids = taskTotal.getList().stream().map(TaskTotalsDTO.TaskTotal::getUserId).collect(Collectors.toList());
                    List<UserPO> userExist = userMapper.selectList(Wrappers.lambdaQuery(UserPO.class).in(UserPO::getId, ids));
                    userExist.forEach(t -> {
                        str.append(t.getUserName()).append(",");
                    });
                    errorResponse(response, 400, 100106017, str.substring(0, str.length() - 1) + "有待办不可删除");
                    return;
                }
                userPOS.forEach(t -> {
                    userMapper.delete(Wrappers.lambdaUpdate(UserPO.class).eq(UserPO::getUserName, t.getUserName()));
                    userRoleMapper.delete(Wrappers.lambdaUpdate(UserRolePO.class).eq(UserRolePO::getUserId, t.getId()));
                });
                List<Long> ids = userPOS.stream().map(UserPO::getId).collect(Collectors.toList());
                rbacServiceAdapter.deleteRolesByUserIds(ids);


                deleteAuthUserKafkaMessage(BijectionUtils.applys(userPOS, x -> x.getUserName()));

            }
        } else {
            errorResponse(response, 400, 100106500, "删除个数最小为1");
            return;
        }
    }

    private void deleteAuthUserKafkaMessage(List<String> userPOS) {
        // kafka message
        if (CollectionUtils.isEmpty(userPOS)) {
            return;
        }
        List<DeleteUserBo> deleteUserBos = BijectionUtils.applys(userPOS, x -> new DeleteUserBo().setRowVersion(0).setName(x));

        Message authMessage = new AuthUserMessage.Builder<List<DeleteUserBo>>()
                .setSender(AUTH_USER_KAFKA_SENDER)
                .setTenantId(RpcContext.getContext().getTenantId())
                .setCreateTime(fastDateFormat.format(new Date()))
                .setTopic(AUTH_USER_KAFKA_TOPIC)
                .setHeader(ImmutableMap.of("encode", "json", "event", "DELETE"))
                .setBody(deleteUserBos)
                .build();

        authUserMessage.publishMessage(authMessage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void creatOpenUser(UserBO bo, HttpServletResponse response) {
        if (!verifyOpenPassword(bo, response)) {
            return;
        }
        List<UserPO> list = userMapper.selectList(Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getUserName, bo.getUserName()));
        if (!list.isEmpty()) {
            errorResponse(response, 400, 100106004, bo.getUserName() + "用户名称重复");
            return;
        }
        Result<CompanyResultDTO> companyByCode = personServiceAdapter.findCompanyByCode(bo.getCompanyCode());
        if (companyByCode.getData() != null && companyByCode.getData().getId() == null) {
            errorResponse(response, 400, 100106014, bo.getCompanyCode() + "对应公司不存在");
            return;
        } else {
            bo.setCompanyId(companyByCode.getData().getId());
        }
        ListResult<PersonDetailDTO> persons = personServiceAdapter.queryPersonByCodes(Arrays.asList(bo.getPersonCode()));
        if (persons.getList().isEmpty()) {
            errorResponse(response, 400, 100106012, bo.getPersonCode() + "对应人员不存在");
            return;
        }
        Optional<PersonDetailDTO> first = persons.getList().stream().findFirst();
        if (first.isPresent()) {
            PersonDetailDTO personDetailDTO = first.get();
            List<UserPO> userPOS = userMapper.selectList(Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getPersonId, personDetailDTO.getId()));
            if (!userPOS.isEmpty()) {
                errorResponse(response, 400, 100106005, "人员编码" + bo.getPersonCode() + "已绑定用户");
                return;
            }
            ListResult<CompanyDTO> companyDTOListResult = personServiceAdapter.queryCompanyIdByPersonId(personDetailDTO.getId());
            if (companyDTOListResult.getList() != null) {
                boolean anyMatch = companyDTOListResult.getList().stream().anyMatch(t -> t.getId().compareTo(companyByCode.getData().getId()) == 0);
                if (!anyMatch) {
                    errorResponse(response, 400, 100106020, bo.getPersonCode() + "对应人员不属于" + bo.getCompanyCode() + "对应公司");
                    return;
                }
            }
            bo.setPersonId(personDetailDTO.getId());
            bo.setPersonCode(personDetailDTO.getCode());
            bo.setPersonName(personDetailDTO.getName());
        }
        UserPO userPO = new UserPO();
        BeanUtils.copyProperties(bo, userPO);
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
            StringBuilder stringBuilder = new StringBuilder();
            roleByCodes.forEach(t -> {
                if (t.getCid().compareTo(companyByCode.getData().getId()) != 0) {
                    stringBuilder.append(t.getCode()).append(",");
                }
            });
            if (stringBuilder.length() > 0) {
                errorResponse(response, 400, 100106022, stringBuilder.substring(0, stringBuilder.length() - 1) + "对应角色不属于" + bo.getCompanyCode() + "对应公司");
                return;
            }
            List<UserRoleBO> roles = roleByCodes.stream().map(t -> {
                UserRoleBO userRoleBO = new UserRoleBO();
                userRoleBO.setUserId(bo.getId());
                userRoleBO.setRoleType(ROLE_USER);
                userRoleBO.setRoleId(t.getId());
                userRoleBO.setRoleName(t.getName());
                return userRoleBO;
            }).collect(Collectors.toList());
            bo.setRoles(roles);
            userRoleService.batchInsert(roles);
            addUserRolesToRbac(bo, roles);
        }
        userMapper.insert(userPO);

        {
            // kafka message
            SaveAuthUserBo authUserBo = new SaveAuthUserBo()
                    .setPerson(new SaveAuthUserBo.PersonDTO().setCode(bo.getPersonCode()).setName(bo.getPersonName()))
                    .setDescription(bo.getDescription())
                    .setRowVersion(0)
                    .setName(bo.getUserName())
                    .setRoleList(getUserRoleCodeAndNameByIds(Optional.ofNullable(bo.getRoles()).orElse(userRoleService.getRole(bo.getId())).stream().map(UserRoleBO::getRoleId).collect(Collectors.toList())));

            Message authMessage = new AuthUserMessage.Builder<SaveAuthUserBo>()
                    .setSender(AUTH_USER_KAFKA_SENDER)
                    .setTenantId(RpcContext.getContext().getTenantId())
                    .setCreateTime(fastDateFormat.format(new Date()))
                    .setTopic(AUTH_USER_KAFKA_TOPIC)
                    .setHeader(ImmutableMap.of("encode", "json", "event", "CREATE"))
                    .setBody(Collections.singletonList(authUserBo))
                    .build();

            authUserMessage.publishMessage(authMessage);
        }
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

    private Boolean verifyOpenPassword(UserBO bo, HttpServletResponse response) {
        StringBuilder str = new StringBuilder();
        StringBuilder remind = new StringBuilder();
        LoginConfigBO loginConfig = passwordService.getLoginConfig();
        String passwordRemind = null;
        if (Constants.COMBINATION_PWD_RULE.compareTo(loginConfig.getRuleType()) == 0) {
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
        if (Constants.COMBINATION_PWD_RULE.compareTo(loginConfig.getRuleType()) == 0) {
            String composeStr = !org.apache.commons.lang3.StringUtils.isEmpty(remind.toString()) ? remind.substring(0, remind.length() - 1) : "大小写、数字、特殊字符（仅支持!@#$%^&*();'?.,)";
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openUpdateUser(UserBO userBO, List<String> addRoleCodes, List<String> deleteRoleCodes, HttpServletResponse response) {
        List<UserRoleBO> roleBOS = new ArrayList<>();
        if (addRoleCodes.size() > 10) {
            errorResponse(response, 400, 100106500, "添加角色最大10个");
            return;
        }
        if (deleteRoleCodes.size() > 10) {
            errorResponse(response, 400, 100106500, "删除角色最大10个");
            return;
        }
        addRoleCodes.removeIf(StringUtils::isEmpty);
        deleteRoleCodes.removeIf(StringUtils::isEmpty);
        if (userBO != null) {
            if (userBO.getUserType().compareTo(UserTypeEnum.SYSTEM_USER.getCode()) == 0) {
                errorResponse(response, 400, 100106019, "系统管理员用户不可编辑角色");
                return;
            }
            List<UserRoleBO> userRoleBOS = new ArrayList<>();
            List<RoleDTO> addRole = !addRoleCodes.isEmpty() ? rbacServiceAdapter.findRoleByCodes(addRoleCodes) : new ArrayList<>();
            if (userBO.getPersonId() != null) {
                ListResult<CompanyDTO> companyDTOListResult = personServiceAdapter.queryCompanyIdByPersonId(userBO.getPersonId());
                addRole.forEach(t -> {
                    StringBuilder stringBuilder = new StringBuilder();
                    boolean anyMatch = companyDTOListResult.getList().stream().anyMatch(companyDTO -> companyDTO.getId().compareTo(t.getCid()) == 0);
                    if (!anyMatch) {
                        stringBuilder.append(t.getCode()).append(",");
                    }
                    if (stringBuilder.length() > 0) {
                        errorResponse(response, 400, 100106022, stringBuilder.substring(0, stringBuilder.length() - 1) + "对应角色不属于" + userBO.getUserName() + "用户绑定人员所属公司");
                        return;
                    }
                });
            }

            List<RoleDTO> deleteRole = !deleteRoleCodes.isEmpty() ? rbacServiceAdapter.findRoleByCodes(deleteRoleCodes) : new ArrayList<>();
            Set<Long> deleteRoleIds = deleteRole.stream().map(RoleDTO::getId).collect(Collectors.toSet());
            Set<String> roleNotExist = getRoleNotExist(addRoleCodes, addRole, deleteRoleCodes, deleteRole);
            if (!roleNotExist.isEmpty()) {
                StringBuilder errorRole = new StringBuilder();
                roleNotExist.forEach(t -> {
                    errorRole.append(t).append(",");
                });
                errorResponse(response, 400, 100106013, errorRole.substring(0, errorRole.length() - 1) + "对应角色不存在");
                return;
            }
            List<UserRolePO> list = userRoleService.list(Wrappers.lambdaQuery(UserRolePO.class).eq(UserRolePO::getUserId, userBO.getId()));
            ListIterator<UserRolePO> listIterator = list.listIterator();

            while (listIterator.hasNext()) {
                UserRolePO next = listIterator.next();
                if (!deleteRoleIds.add(next.getRoleId())) {
                    if (next.getRoleType().compareTo(Constants.ROLE_REPEATE) == 0) {
                        UserRoleBO userRoleBO = new UserRoleBO();
                        userRoleBO.setRoleId(next.getRoleId());
                        userRoleBO.setUserId(next.getUserId());
                        userRoleBO.setRoleType(Constants.ROLE_ORG);

                        userRoleBOS.add(userRoleBO);
                    }
                } else {
                    UserRoleBO userRoleBO = new UserRoleBO();
                    userRoleBO.setRoleId(next.getRoleId());
                    userRoleBO.setUserId(next.getUserId());
                    userRoleBO.setRoleType(Constants.ROLE_USER);
                    userRoleBOS.add(userRoleBO);
                }
            }

            Set<Long> collect = userRoleBOS.stream().map(UserRoleBO::getRoleId).collect(Collectors.toSet());
            addRole.forEach(t -> {
                if (collect.add(t.getId())) {
                    UserRoleBO userRoleBO = new UserRoleBO();
                    userRoleBO.setRoleId(t.getId());
                    userRoleBO.setUserId(userBO.getId());
                    userRoleBO.setRoleType(Constants.ROLE_USER);
                    userRoleBOS.add(userRoleBO);
                } else {
                    UserRolePO one = userRoleService.getOne(Wrappers.lambdaQuery(UserRolePO.class).eq(UserRolePO::getRoleId, t.getId()).eq(UserRolePO::getUserId, userBO.getId()));
                    if (one.getRoleType().compareTo(Constants.ROLE_ORG) == 0) {
                        UserRoleBO userRoleBO = new UserRoleBO();
                        userRoleBO.setRoleId(t.getId());
                        userRoleBO.setUserId(userBO.getId());
                        userRoleBO.setRoleType(Constants.ROLE_REPEATE);
                        userRoleBOS.add(userRoleBO);
                    }
                }
            });

            userRoleService.remove(Wrappers.lambdaQuery(UserRolePO.class).eq(UserRolePO::getUserId, userBO.getId()));
            userRoleService.batchInsert(userRoleBOS);
            userBO.setRoles(userRoleBOS);
            addUserRolesToRbac(userBO, userRoleBOS);


            List<Long> userCurrentRoleIds = userRoleService.getRole(userBO.getId()).stream().map(x -> x.getRoleId()).collect(Collectors.toList());
            UserPO currentUser = userMapper.selectById(userBO.getId());

            {
                // kafka message
                SaveAuthUserBo authUserBo = new SaveAuthUserBo()
                        .setPerson(new SaveAuthUserBo.PersonDTO().setCode(currentUser.getPersonCode()).setName(currentUser.getPersonName()))
                        .setDescription(currentUser.getDescription())
                        .setRowVersion(0)
                        .setName(currentUser.getUserName())
                        .setRoleList(getUserRoleCodeAndNameByIds(userCurrentRoleIds));

                Message authMessage = new AuthUserMessage.Builder<SaveAuthUserBo>()
                        .setSender(AUTH_USER_KAFKA_SENDER)
                        .setTenantId(RpcContext.getContext().getTenantId())
                        .setCreateTime(fastDateFormat.format(new Date()))
                        .setTopic(AUTH_USER_KAFKA_TOPIC)
                        .setHeader(ImmutableMap.of("encode", "json", "event", "UPDATE"))
                        .setBody(Collections.singletonList(authUserBo))
                        .build();

                authUserMessage.publishMessage(authMessage);
            }
        }
    }

    private Set<String> getRoleNotExist(List<String> addRoleCodes, List<RoleDTO> addRole, List<String> deleteRoleCodes, List<RoleDTO> deleteRole) {
        HashSet<String> roleNotExist = new HashSet<>();
        if (addRole.size() != addRoleCodes.size()) {
            Set<String> roleSet = addRole.stream().map(RoleDTO::getCode).collect(Collectors.toSet());
            addRoleCodes.stream().forEach(t -> {
                if (roleSet.add(t)) {
                    roleNotExist.add(t);
                }
            });
        }
        if (deleteRole.size() != deleteRoleCodes.size()) {
            Set<String> roleSet = deleteRole.stream().map(RoleDTO::getCode).collect(Collectors.toSet());
            deleteRoleCodes.stream().forEach(t -> {
                if (roleSet.add(t)) {
                    roleNotExist.add(t);
                }
            });
        }
        return roleNotExist;
    }

    @Override
    public Page<UserBO> search(String keyword, Integer current, Integer pageSize) {
        Page<UserPO> page = new Page<>(current, pageSize);
        Page<UserPO> result = userMapper.selectPage(page, Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getCompanyId, UserContext.getUserContext().getCompanyId()).and(q -> {
            q.like(UserPO::getUserName, keyword).or().like(UserPO::getDescription, keyword).or().like(UserPO::getPersonName, keyword);
        }));
        List<UserBO> collect = result.getRecords().stream().map(t -> {
            UserBO userBO = new UserBO();
            BeanUtils.copyProperties(t, userBO);
            List<UserRolePO> userRolePOS = userRoleMapper.selectList(Wrappers.lambdaQuery(UserRolePO.class).eq(UserRolePO::getUserId, t.getId()).ne(UserRolePO::getRoleType, ROLE_ORG));
            if (!userRolePOS.isEmpty()) {
                StringBuilder str = new StringBuilder();
                userRolePOS.stream().forEach(userRolePO -> {
                    str.append(userRolePO.getRoleId()).append(",");
                });
                Map<Long, String> names = rbacServiceAdapter.findBatchName(str.substring(0, str.length() - 1));
                List<UserRoleBO> collect1 = userRolePOS.stream().map(userRolePO -> {
                    UserRoleBO userRoleBO = new UserRoleBO();
                    userRoleBO.setRoleName(names.get(userRolePO.getRoleId()));
                    userRoleBO.setRoleType(userRolePO.getRoleType());
                    userRoleBO.setId(userRolePO.getId());
                    return userRoleBO;
                }).collect(Collectors.toList());
                userBO.setRoles(collect1);
            }
            return userBO;
        }).collect(Collectors.toList());
        Page<UserBO> userBOPage = new Page<>();
        userBOPage.setCurrent(result.getCurrent());
        userBOPage.setSize(result.getSize());
        userBOPage.setTotal(result.getTotal());
        userBOPage.setRecords(collect);
        return userBOPage;

    }

    @Override
    public Long getIdByUserName(String userName) {
        QueryWrapper<UserPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id");
        queryWrapper.eq("user_name", userName);
        UserPO userPO = getOne(queryWrapper);
        if (!ObjectUtils.isEmpty(userPO)) {
            return userPO.getId();
        }
        return null;
    }

    @Override
    public List<UserBO> getAllPersonsUsers() {
        QueryWrapper<UserPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("valid", 1);
        List<UserPO> userPOS = list(queryWrapper);
        List<UserBO> result = Lists.newArrayList();
        for (UserPO userPO : userPOS) {
            UserBO userBO = new UserBO();
            BeanUtils.copyProperties(userPO, userBO);
            result.add(userBO);
        }
        return result;
    }


    @Override
    public String resetAdminPassword(@NotNull String authorization) {

        String tmp = authorization.replaceFirst("Sign", "").trim();
        String[] sp = tmp.split("-");
        final String accessKey = sp[0];
        final String sign = sp[1];


        Result<AccountDTO> aksk = identityAndAccessService.findByAccessKey(accessKey);


        LoginConfigBO loginConfig = passwordService.getLoginConfig();

        Integer ruleType = loginConfig.getRuleType();
        Boolean bigSmall = loginConfig.getBigSmall();
        Integer min = Optional.ofNullable(loginConfig.getMin()).orElse(8);
        Boolean specialChar = loginConfig.getSpecialChar();
        Boolean number = loginConfig.getNumber();


        String passwd = "";
        if (Objects.equals(ruleType, Constants.COMBINATION_PWD_RULE)) {
            Set<Supplier<Character>> sets = new HashSet<>();
            if (bigSmall) {
                sets.add(() -> RandomStringUtils.randomAlphabetic(1).toCharArray()[0]);
            }
            if (number) {
                sets.add(() -> RandomStringUtils.randomNumeric(1).toCharArray()[0]);
            }
            if (specialChar) {
                sets.add(() -> {
                    String s = "!@#$%^&*();'?.,";
                    return s.toCharArray()[RandomUtils.nextInt(0, s.length())];
                });
            }
            sets.add(() -> RandomStringUtils.randomAlphanumeric(1).toCharArray()[0]);
            Supplier[] suppliers = sets.toArray(new Supplier[0]);

            passwd = IntStream.range(0, min).mapToObj(x -> x < suppliers.length ? suppliers[x % suppliers.length].get() + ""
                    : suppliers[RandomUtils.nextInt(0, suppliers.length)].get() + "").unordered().parallel().collect(Collectors.joining(""));

        } else {
            passwd = RandomStringUtils.randomAlphanumeric(min);
        }

        UserPO admin = userMapper.selectUserName("admin");
        admin.setPassword(BCryptUtil.encode(passwd));
        UserPO userPO = new UserPO();
        userPO.setPassword(BCryptUtil.encode(passwd));
        userPO.setId(admin.getId());
        userMapper.updateById(userPO);
        log.info("admin password reset to {}", passwd);
        String encodePasswd = AESUtils.encryptData(passwd, aksk.getData().getSk());

        return encodePasswd;
    }

    @Override
    public void unBindUserThridIdentitys(String username, List<String> identityIds) {
        LambdaUpdateWrapper<UserPO> update = Wrappers.lambdaUpdate(UserPO.class).set(UserPO::getThirdIdentity, null)
                .set(UserPO::getThirdSource, null)
                .eq(UserPO::getUserName, username)
                .in(UserPO::getThirdIdentity, identityIds);
        this.update(update);
    }

    @Override
    public LoginResponseBO simulateLogin(String userName, Long companyId, String realIp, String quatoName, String deviceType) {
        LoginBO loginBO = new LoginBO();
        loginBO.setUserName(userName);
        loginBO.setCompanyId(companyId);
        loginBO.setLdap(false);
        loginBO.setGrantType("password");
        UserBO userBO = this.findByUserName(loginBO.getUserName());
        boolean ipBlackWhite = ipBlackWhiteService.verifyIp(realIp, loginBO.getCompanyId() != null ? loginBO.getCompanyId() : userBO.getCompanyId());
        if (!ipBlackWhite) {
            throw new UserException(ACCESS_IP_FORBIDDEN);
        }
        if (supOSEnabled) {
            if ("admin".equals(loginBO.getUserName())) {
                verifySystemAdmin(loginBO.getUserName());
            } else if (supOSEnabled) {
                verifyLicense(quatoName, deviceType);
            }
        }
        AuthorizationDTO authorizationDTO = keyCloakServiceAdapter.simulatedLoginToken(RpcContext.getContext().getTenantId(), Constants.SIMULATED_CLIENT_ID, loginBO.getUserName(), loginBO.getCompanyId());
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String key = String.format(AUTH_TICKET, uuid);
        LoginResponseBO loginResponseBO = convertLoginResponse(loginBO, userBO);
        loginResponseBO.setTicket(uuid);
        loginResponseBO.setTenantId(RpcContext.getContext().getTenantId());
        if (loginBO.getLdap()) {
            loginResponseBO.setStatus("ok");
        } else {
            loginResponseBO.setStatus(userBO.getLoginFirst() ? "firstLogin" : "ok");
        }
        loginResponseBO.setUserType(userBO.getUserType());
        loginResponseBO.setAccessToken(authorizationDTO.getAccessToken());
        loginResponseBO.setRefreshToken(authorizationDTO.getRefreshToken());
        loginResponseBO.setExpiresIn(authorizationDTO.getExpiresIn());
        loginResponseBO.setClientId(deviceType + "_" + RpcContext.getContext().getTenantId());
        //增加在线用户
        OnlineUserBO onlineUserBO = buildOnlineUserBO(realIp, deviceType, uuid, loginResponseBO);
        onlineUserService.createOnlineUser(onlineUserBO);
        //缓存ticket信息
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(ACCESS_TOKEN, authorizationDTO.getAccessToken());
        jsonObject.put(REFRESH_TOKEN, authorizationDTO.getRefreshToken());
        jsonObject.put(TOKEN_TYPE, authorizationDTO.getTokenType());
        jsonObject.put(EXPIRES_IN, authorizationDTO.getExpiresIn());
        cacheTicket(jsonObject, key, loginResponseBO);
        if ("admin".equals(loginBO.getUserName())) {
            addSystemAdmin(loginBO.getUserName());
        }
        return loginResponseBO;
    }
}

