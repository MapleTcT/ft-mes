package com.supcon.supfusion.auth.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.exception.OnlineUserErrorEnum;
import com.supcon.supfusion.auth.common.utils.HttpUtil;
import com.supcon.supfusion.auth.common.utils.SqlUtil;
import com.supcon.supfusion.auth.dao.mapper.OnlineUserMapper;
import com.supcon.supfusion.auth.dao.mapper.UserMapper;
import com.supcon.supfusion.auth.dao.po.OnlineUserPO;
import com.supcon.supfusion.auth.dao.po.UserPO;
import com.supcon.supfusion.auth.manager.KeyCloakServiceAdapter;
import com.supcon.supfusion.auth.manager.PersonServiceAdapter;
import com.supcon.supfusion.auth.manager.WebSocketServiceAdapter;
import com.supcon.supfusion.auth.manager.feign.client.TokenClient;
import com.supcon.supfusion.auth.service.AuthLoginLogService;
import com.supcon.supfusion.auth.service.BranchOfficeService;
import com.supcon.supfusion.auth.service.OnlineUserService;
import com.supcon.supfusion.auth.service.bo.OnlineUserBO;
import com.supcon.supfusion.auth.service.bo.OnlineUserQueryBO;
import com.supcon.supfusion.auth.service.cache.AuthTicketCache;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.common.util.DateTimeUtil;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import com.supcon.supfusion.ws.client.dto.NoticeMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.supcon.supfusion.auth.common.constants.Constants.*;

/**
 * @author caokele
 */
@Slf4j
@Service
public class OnlineUserServiceImpl implements OnlineUserService {
    private static final String ONLINE_USER = "onlineUser";
    @Autowired
    private OnlineUserMapper onlineUserMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AuthTicketCache authTicketCache;
    @Autowired
    private KeyCloakServiceAdapter keyCloakServiceAdapter;
    @Autowired
    private PersonServiceAdapter personServiceAdapter;
    @Autowired
    private WebSocketServiceAdapter webSocketServiceAdapter;
    @Autowired
    private AuthLoginLogService authLoginLogService;
    @Autowired
    private BranchOfficeService branchOfficeService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private TokenClient tokenClient;


    @Override
    public OnlineUserBO createOnlineUser(OnlineUserBO onlineUserBO) {
        OnlineUserPO onlineUserPO = new OnlineUserPO();
        BeanUtils.copyProperties(onlineUserBO, onlineUserPO);
        onlineUserPO.setUserId(onlineUserBO.getUserId());
        onlineUserPO.setCompanyId(onlineUserBO.getCompanyId());
        onlineUserPO.setLoginTime(DateTimeUtil.getUTC0());
        onlineUserPO.setDeviceType(onlineUserBO.getDeviceType());
        UserPO userPO = userMapper.selectOne(new QueryWrapper<UserPO>().lambda().eq(UserPO::getId, onlineUserBO.getUserId()));
        if (userPO != null) {
            onlineUserPO.setUserName(userPO.getUserName());
            if (userPO.getPersonId() != null) {
                Map<Long, PersonDTO> personMap = personServiceAdapter.queryPersonsById(new Long[]{userPO.getPersonId()});
                if (!personMap.isEmpty()) {
                    PersonDTO personDTO = personMap.get(userPO.getPersonId());
                    onlineUserPO.setPersonId(userPO.getPersonId());
                    onlineUserPO.setPersonName(personDTO.getName());
                    onlineUserPO.setPersonCode(personDTO.getCode());
                }
            }
        }
        onlineUserMapper.insert(onlineUserPO);
        BeanUtils.copyProperties(onlineUserPO, onlineUserBO);
        return onlineUserBO;
    }

    @Override
    public PageResult<OnlineUserBO> queryOnlineUsers(OnlineUserQueryBO queryParams, Pagination pagination) {
        LambdaQueryWrapper<OnlineUserPO> lambdaQueryWrapper = new LambdaQueryWrapper<OnlineUserPO>()
                .eq(OnlineUserPO::getCompanyId, UserContext.getUserContext().getCompanyId())
                .orderByDesc(OnlineUserPO::getLoginTime);
        if (StringUtils.isNotEmpty(queryParams.getUserName())) {
            lambdaQueryWrapper.like(OnlineUserPO::getUserName, SqlUtil.escapeChar(queryParams.getUserName()));
        }
        if (StringUtils.isNotEmpty(queryParams.getStartLoginTime())) {
            lambdaQueryWrapper.ge(OnlineUserPO::getLoginTime, utcToTimestamp(queryParams.getStartLoginTime()));
        }
        if (StringUtils.isNotEmpty(queryParams.getEndLoginTime())) {
            lambdaQueryWrapper.le(OnlineUserPO::getLoginTime, utcToTimestamp(queryParams.getEndLoginTime()));
        }
        Page<OnlineUserPO> page = new Page<>();
        page.setCurrent(pagination.getCurrent()).setSize(pagination.getPageSize());
        Page<OnlineUserPO> onlineUserPOPage = onlineUserMapper.selectPage(page, lambdaQueryWrapper);
        List<OnlineUserBO> collect = onlineUserPOPage.getRecords().stream().map(entity -> {
            OnlineUserBO onlineUserBO = new OnlineUserBO();
            BeanUtils.copyProperties(entity, onlineUserBO);
            return onlineUserBO;
        }).collect(Collectors.toList());
        return new PageResult<>(collect, onlineUserPOPage.getTotal(), onlineUserPOPage.getSize(), onlineUserPOPage.getCurrent());
    }

    @Override
    public void logoutOnlineUsers(List<Long> ids) {
        LambdaQueryWrapper<OnlineUserPO> onLineUserWrapper = Wrappers.lambdaQuery(OnlineUserPO.class)
                .in(OnlineUserPO::getId, ids)
                .eq(OnlineUserPO::getCompanyId, UserContext.getUserContext().getCompanyId());
        List<OnlineUserPO> onlineUsers = onlineUserMapper.selectList(onLineUserWrapper);
        authLoginLogService.saveLogoutLog(onlineUsers.get(0).getTicket(), FORCE_LOGOUT);
        dealLogoutOnlineUsers(onlineUsers);
    }

    @Override
    public void removeOnlineUserByTicketActByManual(String ticket, String tenantId) {
        removeSessionByTicketWithKeyCloak(ticket, tenantId);
        branchOfficeService.logout(ticket);
    }

    @Override
    public void removeSessionByTicketWithKeyCloak(String ticket, String tenantId) {
        // keycloak
        tenantId = StringUtils.isEmpty(tenantId) ? (String) stringRedisTemplate.opsForHash().get(String.format(TENANT_TICKET, ticket), TENANT_ID) : tenantId;
        {
            try {
                Map<Object, Object> authorizationMap = authTicketCache.getMapByTicket(ticket);
                if (authorizationMap != null && !authorizationMap.isEmpty()) {
                    String accessToken = (String) authorizationMap.get(ACCESS_TOKEN);
                    String tokenType = (String) authorizationMap.get(TOKEN_TYPE);
                    String refreshToken = (String) authorizationMap.get(REFRESH_TOKEN);
                    String client_id = (String) authorizationMap.get(CLIENT_ID);
                    HashMap<String, String> map = new HashMap<>();
                    map.put(CLIENT_ID, client_id);
                    map.put(REFRESH_TOKEN, refreshToken);
                    tokenClient.keycloakLogout(tenantId, map, tokenType + " " + accessToken);
                }
            } catch (Exception e) {
                log.warn("keycloak logout fail {} {}", ticket, ExceptionUtils.getStackFrames(e));
            }
        }
        if (StringUtils.isEmpty(tenantId)) {
            tenantId = "dt";
        }
        RpcContext.getContext().setTenantId(tenantId);
        // 正常情况下，返回的ticket用户数为1
        List<OnlineUserPO> onlineUserPOs = onlineUserMapper.selectList(Wrappers.lambdaQuery(OnlineUserPO.class).eq(OnlineUserPO::getTicket, ticket));

        if (!onlineUserPOs.isEmpty()) {
            deleteOnlineUserMaxCount(onlineUserPOs.get(0).getUserName(), tenantId);
        }
        onlineUserMapper.delete(Wrappers.lambdaUpdate(OnlineUserPO.class).eq(OnlineUserPO::getTicket, ticket));
        String backLogoutUrl = (String) stringRedisTemplate.opsForHash().get(String.format(TENANT_TICKET, ticket), BACKEND_LOGOUT_URL);
        String accessToken = (String) stringRedisTemplate.opsForHash().get(String.format(TENANT_TICKET, ticket), OAUTH2_TOKEN);

        if (StringUtils.isNotEmpty(backLogoutUrl)) {
            List<String> list = JSON.parseArray(backLogoutUrl, String.class);
            list.forEach(t -> {
                if (StringUtils.isNotEmpty(accessToken))
                    HttpUtil.doGet(t, accessToken);
            });
        }
        stringRedisTemplate.delete(String.format(TENANT_TICKET, ticket));
        stringRedisTemplate.delete(String.format(AUTH_TICKET, ticket));

    }

    @Override
    public void updateOnlineUserCompany(String ticket, Long companyId) {
        OnlineUserPO onlineUserPO = new OnlineUserPO();
        onlineUserPO.setCompanyId(companyId);
        onlineUserMapper.update(onlineUserPO, Wrappers.lambdaUpdate(OnlineUserPO.class).eq(OnlineUserPO::getTicket, ticket));
    }

    @Override
    public void updateOnlineUserCompanyId(String ticket, Long companyId, String accessToken) {
        OnlineUserPO onlineUserPO = new OnlineUserPO();
        onlineUserPO.setCompanyId(companyId);
        onlineUserMapper.update(onlineUserPO, Wrappers.lambdaUpdate(OnlineUserPO.class).eq(OnlineUserPO::getTicket, ticket));
    }

    /**
     * 生成注销消息
     */
    private NoticeMessageDTO generateMessage(String userName, String ticket) {
        NoticeMessageDTO noticeMessageDTO = new NoticeMessageDTO();
        JSONObject data = new JSONObject();
        data.put(Constants.CODE, OnlineUserErrorEnum.LOGOUT.getCode());
        data.put(Constants.MESSAGE, OnlineUserErrorEnum.LOGOUT.getMessage());
        data.put(Constants.TICKET, ticket);
        noticeMessageDTO.setData(data);
        noticeMessageDTO.setUserName(userName);
        return noticeMessageDTO;
    }

    @Override
    public void logoutOnlineUsersByUserIds(List<Long> userIds) {
        LambdaQueryWrapper<OnlineUserPO> onLineUserWrapper = Wrappers.lambdaQuery(OnlineUserPO.class).in(OnlineUserPO::getUserId, userIds);
        List<OnlineUserPO> onlineUsers = onlineUserMapper.selectList(onLineUserWrapper);
        dealLogoutOnlineUsers(onlineUsers);
    }

    private void dealLogoutOnlineUsers(List<OnlineUserPO> onlineUsers) {
        if (onlineUsers.isEmpty()) {
            return;
        }


        String tenantId = RpcContext.getContext().getTenantId();
        List<NoticeMessageDTO> messages = new LinkedList<>();
        for (OnlineUserPO onlineUser : onlineUsers) {
            // 从keycloak注销
            String ticket = onlineUser.getTicket();
            Map<Object, Object> authorizationMap = authTicketCache.getMapByTicket(ticket);
            if (!authorizationMap.isEmpty()) {
                messages.add(generateMessage(onlineUser.getUserName(), ticket));
            }
            // 删除在线用户记录
            removeOnlineUserByTicketActByManual(ticket, tenantId);
            branchOfficeService.logout(ticket);
        }
        // 通过webSocket提示用户被踢出
        webSocketServiceAdapter.pushNoticeMessages(ONLINE_USER, messages);
    }

    // 刪除用戶redis 在线数
    private void deleteOnlineUserMaxCount(String userName, String tenantId) {
        if (Objects.equals("admin", userName)) {
            String key = tenantId + "_" + "admin";
            Optional.ofNullable(stringRedisTemplate.opsForValue().get(key))
                    .map(Long::parseLong).filter(x -> x >= 0).ifPresent(x -> {
                stringRedisTemplate.opsForValue().decrement(key);
            });
        }
    }

    private Timestamp utcToTimestamp(String utc) {
        ZonedDateTime utc0 = ZonedDateTime.parse(utc, DateTimeUtil.UTC0_FORMAT).withZoneSameInstant(ZoneId.of("UTC"));
        return Timestamp.valueOf(utc0.toLocalDateTime());
    }

    @Override
    public Integer getTotalOnline(String deviceType) {
        return onlineUserMapper.selectCount(Wrappers.lambdaQuery(OnlineUserPO.class).eq(OnlineUserPO::getDeviceType, deviceType).ne(OnlineUserPO::getUserId, 1));
    }

    @Override
    public void deleteOnline(String deviceType) {
        String keys = "AUTH:TICKET:*";
        String tenants = "TENANT:TICKET:*";
        Set<String> tickets = stringRedisTemplate.keys(keys);
        Set<String> tenantKeys = stringRedisTemplate.keys(tenants);
        if (tickets != null && !tickets.isEmpty()) {
            stringRedisTemplate.delete(tickets);
        }
        if (tenantKeys != null && !tenantKeys.isEmpty()) {
            tenantKeys.forEach(tenantTicket -> {
                try {
                    String backLogoutUrl = (String) stringRedisTemplate.opsForHash().get(tenantTicket, BACKEND_LOGOUT_URL);
                    String accessToken = (String) stringRedisTemplate.opsForHash().get(tenantTicket, OAUTH2_TOKEN);
                    if (StringUtils.isNotEmpty(backLogoutUrl)) {
                        List<String> list = JSON.parseArray(backLogoutUrl, String.class);
                        list.forEach(t -> {
                            if (StringUtils.isNotEmpty(accessToken))
                                HttpUtil.doGet(t, accessToken);
                        });
                    }
                } catch (Exception e) {
                    log.error("delete TENANT:TICKET=== {}", tenantTicket);
                }
            });
            stringRedisTemplate.delete(tenantKeys);
        }
        stringRedisTemplate.delete(RpcContext.getContext().getTenantId() + "_" + "admin");
        onlineUserMapper.delete(Wrappers.lambdaQuery(OnlineUserPO.class).eq(OnlineUserPO::getDeviceType, deviceType));
        onlineUserMapper.delete(Wrappers.lambdaQuery(OnlineUserPO.class).isNull(OnlineUserPO::getDeviceType));
    }

    @Override
    public List<OnlineUserBO> queryAllOnlinUsers() {
        QueryWrapper<OnlineUserPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().orderByAsc(OnlineUserPO::getId);
        List<OnlineUserPO> onlineUsers = onlineUserMapper.selectList(queryWrapper);
        if (onlineUsers == null || onlineUsers.size() == 0) {
            return null;
        }
        List<OnlineUserBO> collect = onlineUsers.stream().map(entity -> {
            OnlineUserBO onlineUserBO = new OnlineUserBO();
            BeanUtils.copyProperties(entity, onlineUserBO);
            return onlineUserBO;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public void deleteOnlineUserByIds(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            onlineUserMapper.deleteBatchIds(ids);
        }
    }

}
