package com.supcon.supfusion.auth.service.impl;

import com.supcon.supfusion.auth.dao.mapper.ExcelOperateMapper;
import com.supcon.supfusion.auth.dao.mapper.UserDirectoryMapper;
import com.supcon.supfusion.auth.dao.mapper.UserMapper;
import com.supcon.supfusion.auth.dao.mapper.UserRoleMapper;
import com.supcon.supfusion.auth.dao.po.UserPO;
import com.supcon.supfusion.auth.manager.*;
import com.supcon.supfusion.auth.manager.feign.client.TokenClient;
import com.supcon.supfusion.auth.service.ExcelService;
import com.supcon.supfusion.auth.service.OnlineUserService;
import com.supcon.supfusion.auth.service.OpenUserService;
import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.auth.service.bo.UserRoleBO;
import com.supcon.supfusion.auth.service.cache.AuthTicketCache;
import com.supcon.supfusion.auth.service.cache.UserCache;
import com.supcon.supfusion.auth.service.config.AuthProperties;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.supcon.supfusion.auth.common.constants.Constants.ROLE_USER;

@Slf4j
@Service
public class OpenUserServiceImpl implements OpenUserService {

    private static String bigSmall = "(?=.*[a-z])(?=.*[A-Z])";
    private static String number = "(?=.*[0-9])";
    private static String special = "(?=.*[!@#$%^&*();'?.,])";
    private static String all = "[A-Za-z0-9!@#$%^&*();'?.,]";

    private static final String pattern = "^(?![A-Za-z]+$)(?![A-Z0-9]+$)(?![a-z0-9]+$)(?![a-z\\W]+$)(?![A-Z\\W]+$)(?![0-9\\W]+$)[a-zA-Z0-9\\W]{8,16}$";

    @Value("${keycloak.client:ms-content-sample}")
    private String client;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

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


    @Override
    public void creatOpenUser(UserBO bo,HttpServletResponse response) {
        ListResult<PersonDetailDTO> persons = personServiceAdapter.queryPersonByCodes(Arrays.asList(bo.getPersonCode()));
        Optional<PersonDetailDTO> first = persons.getList().stream().findFirst();
        first.ifPresent(personDetailDTO -> bo.setPersonId(personDetailDTO.getId()));
        List<String> codes = bo.getRoles().stream().map(UserRoleBO::getRoleCode).collect(Collectors.toList());
        List<RoleDTO> roleByCodes = rbacServiceAdapter.findRoleByCodes(codes);
        List<UserRoleBO> roles = roleByCodes.stream().map(t -> {
            UserRoleBO userRoleBO = new UserRoleBO();
            userRoleBO.setUserId(bo.getId());
            userRoleBO.setRoleType(ROLE_USER);
            userRoleBO.setRoleId(t.getId());
            return userRoleBO;
        }).collect(Collectors.toList());
        UserPO userPO = new UserPO();
        BeanUtils.copyProperties(bo,userPO);
        userMapper.insert(userPO);
    }
}

