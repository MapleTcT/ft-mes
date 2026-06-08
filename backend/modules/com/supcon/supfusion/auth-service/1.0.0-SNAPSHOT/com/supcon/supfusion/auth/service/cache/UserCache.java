package com.supcon.supfusion.auth.service.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.utils.SpringEventListListener;
import com.supcon.supfusion.auth.common.utils.ThreadPoolUtils;
import com.supcon.supfusion.auth.dao.mapper.UserMapper;
import com.supcon.supfusion.auth.dao.mapper.UserRoleMapper;
import com.supcon.supfusion.auth.dao.po.UserPO;
import com.supcon.supfusion.auth.manager.*;
import com.supcon.supfusion.auth.service.OnlineUserService;
import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.auth.service.config.AuthProperties;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import com.supcon.supfusion.tenant.api.dto.TenantDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class UserCache implements ApplicationListener<ApplicationStartedEvent> {

    @Resource
    private RbacServiceAdapter rbacServiceAdapter;
    @Resource
    private PersonServiceAdapter personServiceAdapter;

    private static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private TenantManagerAdapter tenantManagerAdapter;

    @Resource
    private OnlineUserService onlineUserService;

    @Resource
    private QuotaClientAdapter quotaClientAdapter;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private KeycliandAdminClient keycliandAdminClient;

    private volatile Boolean isDone = false;

    @Autowired
    private AuthProperties authProperties;

    @Value("${integration.supos.enabled:true}")
    private Boolean supOSEnabled;
    @Override
    public void onApplicationEvent(ApplicationStartedEvent ev) {
        try {
            if (!isDone) {
                log.info("start task on  background");
                new Thread(() -> {
//                    String os = System.getProperty("os.name").toLowerCase();
                    loadUser();
                    //以下方法 独立部署环境运行会报错，所以加上适配开关
                    if (supOSEnabled) {
                        // 2021-06-07 teplaerry remove delete online
                        deleteOnline();
                        ThreadPoolUtils.onlineUserService.scheduleAtFixedRate(this::reportOnlineUser, 10, 10, TimeUnit.MINUTES);
                        ThreadPoolUtils.onlineUserService.scheduleAtFixedRate(this::loadUser, 0, 10, TimeUnit.MINUTES);
                    }
                }).start();
            }
            isDone = true;
        } catch (Exception e) {
            log.error("create thread is error", e);
            System.exit(-1);
        }
    }

    private void deleteOnline() {
        try {
            int a = 0;
            Set<String> tenantInfos = new HashSet<>();
            for (; ; ) {
                PageResult<TenantDTO> result = tenantManagerAdapter.findByPage(a, 100);
                result.getList().stream().forEach(tenantDTO -> {
                    tenantInfos.add(tenantDTO.getId());
                });
                if (result.getList().size() < 100) {
                    break;
                }
                a++;
            }
            if (!tenantInfos.isEmpty()) {
                int pcTotal = 0;
                int mobileTotal = 0;
                for (String tenant : tenantInfos) {
                    RpcContext.getContext().setTenantId(tenant);
                    onlineUserService.deleteOnline("pc");
                    onlineUserService.deleteOnline("mobile");
                    HashMap<String, Object> tenantMap = new HashMap<>();
                    tenantMap.put(Constants.MAX_PC_LOGIN, 0);
                    tenantMap.put(Constants.MAX_MOBILE_LOGI, 0);
                    quotaClientAdapter.reportTenantUsedQuota(tenant, tenantMap);
                }
                HashMap<String, Object> tenantMap = new HashMap<>();
                tenantMap.put(Constants.MAX_PC_LOGIN, pcTotal);
                tenantMap.put(Constants.MAX_MOBILE_LOGI, mobileTotal);
                quotaClientAdapter.reportSystemUsedQuota(tenantMap);
                stringRedisTemplate.opsForValue().set(Constants.MAX_PC_LOGIN, String.valueOf(pcTotal));
                stringRedisTemplate.opsForValue().set(Constants.MAX_MOBILE_LOGI, String.valueOf(mobileTotal));
            }
        } catch (Exception e) {
            log.error("reportOnlineUser is error", e);
            System.exit(-1);
        }
    }

//    private void reportOnlineUser() {
//        try {
//            int a = 0;
//            Set<String> tenantInfos = new HashSet<>();
//            for (; ; ) {
//                PageResult<TenantDTO> result = tenantManagerAdapter.findByPage(a, 100);
//                result.getList().stream().forEach(tenantDTO -> {
//                    tenantInfos.add(tenantDTO.getId());
//                });
//                if (result.getList().size() < 100) {
//                    break;
//                }
//                a++;
//            }
//            if (!tenantInfos.isEmpty()) {
//                int pcTotal = 0;
//                int mobileTotal = 0;
//                for (String tenant : tenantInfos) {
//                    RpcContext.getContext().setTenantId(tenant);
//                    Integer pc = onlineUserService.getTotalOnline("pc");
//                    Integer mobile = onlineUserService.getTotalOnline("mobile");
//                    HashMap<String, Object> tenantMap = new HashMap<>();
//                    tenantMap.put(Constants.MAX_PC_LOGIN, pc);
//                    tenantMap.put(Constants.MAX_MOBILE_LOGI, mobile);
//                    quotaClientAdapter.reportTenantUsedQuota(tenant, tenantMap);
//                    pcTotal += pc;
//                    mobileTotal += mobile;
//                }
//                HashMap<String, Object> tenantMap = new HashMap<>();
//                tenantMap.put(Constants.MAX_PC_LOGIN, pcTotal);
//                tenantMap.put(Constants.MAX_MOBILE_LOGI, mobileTotal);
//                quotaClientAdapter.reportSystemUsedQuota(tenantMap);
//                stringRedisTemplate.opsForValue().set(Constants.MAX_PC_LOGIN, String.valueOf(pcTotal));
//                stringRedisTemplate.opsForValue().set(Constants.MAX_MOBILE_LOGI, String.valueOf(mobileTotal));
//            }
//        } catch (Exception e) {
//            log.error("reportOnlineUser is error", e);
//            System.exit(-1);
//        }
//    }

    private void reportOnlineUser() {
        try {
            int a = 0;
            Set<String> tenantInfos = new HashSet<>();
            for (; ; ) {
                PageResult<TenantDTO> result = tenantManagerAdapter.findByPage(a, 100);
                result.getList().stream().forEach(tenantDTO -> {
                    tenantInfos.add(tenantDTO.getId());
                });
                if (result.getList().size() < 100) {
                    break;
                }
                a++;
            }
            if (!tenantInfos.isEmpty()) {
                int pcTotal = 0;
                int mobileTotal = 0;
                for (String tenant : tenantInfos) {
                    RpcContext.getContext().setTenantId(tenant);
                    Integer pc = onlineUserService.getTotalOnline("pc");
                    Integer mobile = onlineUserService.getTotalOnline("mobile");
                    HashMap<String, Object> tenantMap = new HashMap<>();
                    tenantMap.put(Constants.MAX_PC_LOGIN, pc);
                    tenantMap.put(Constants.MAX_MOBILE_LOGI, mobile);
                    quotaClientAdapter.reportTenantUsedQuota(tenant, tenantMap);
                    pcTotal += pc;
                    mobileTotal += mobile;
                }
                HashMap<String, Object> tenantMap = new HashMap<>();
                tenantMap.put(Constants.MAX_PC_LOGIN, pcTotal);
                tenantMap.put(Constants.MAX_MOBILE_LOGI, mobileTotal);
                quotaClientAdapter.reportSystemUsedQuota(tenantMap);
                stringRedisTemplate.opsForValue().set(Constants.MAX_PC_LOGIN, String.valueOf(pcTotal));
                stringRedisTemplate.opsForValue().set(Constants.MAX_MOBILE_LOGI, String.valueOf(mobileTotal));
            }
        } catch (Exception e) {
            log.error("reportOnlineUser is error", e);
        }
    }

    public void loadUser() {
        try {
            LambdaQueryWrapper<UserPO> queryWrapper = Wrappers.lambdaQuery(UserPO.class);
            queryWrapper.isNull(UserPO::getPersonName).isNotNull(UserPO::getPersonId);
            Set<String> tenantInfos = new HashSet<>();
            int a = 0;
//            String os = System.getProperty("os.name").toLowerCase();
            //以下方法 独立部署环境运行会报错，所以加上适配开关
            if (supOSEnabled) {
                for (; ; ) {
                    PageResult<TenantDTO> result = tenantManagerAdapter.findByPage(a, 100);
                    if (result == null) {
                        log.info("result is null ");
                        break;
                    }
                    result.getList().stream().forEach(tenantDTO -> {
                        tenantInfos.add(tenantDTO.getId());
                    });
                    if (result.getList().size() < 100) {
                        break;
                    }
                    a++;
                }
            }else{
                Thread.sleep(10 * 1000);
                tenantInfos.add("dt");
            }
            log.info("tenantInfos size ====>" + tenantInfos.size());
            tenantInfos.forEach(tenantId -> {
                log.info("tenantId======>" + tenantId);
                HashMap<Long, UserBO> userMap = new HashMap<>();
                RpcContext rpcContext = RpcContext.getContext();
                rpcContext.setTenantId(tenantId);
                for (int num = 1; ; num++) {
                    Page<UserPO> page = new Page<>(num, 100);
                    Page<UserPO> userPage = userMapper.selectPage(page, queryWrapper);
                    userPage.getRecords().forEach(userPO -> {
                        try {
                            Map<Long, PersonDTO> map = personServiceAdapter.queryPersonsById(new Long[]{userPO.getPersonId()});
                            UserPO userPO1 = new UserPO();
                            PersonDTO personDTO = map.get(userPO.getPersonId());
                            userPO1.setPersonCode(personDTO.getCode());
                            userPO1.setPersonName(personDTO.getName());
                            userPO1.setLoginFirst(userPO.getLoginFirst());
                            userMapper.update(userPO1, Wrappers.lambdaUpdate(UserPO.class).eq(UserPO::getId, userPO.getId()));
//                            userMapper.updateById(userPO);
                        } catch (Exception e) {
                            log.error("get all user is error====>", e);
                        }
                    });
                    if (userPage.getRecords().size() < 100) {
                        break;
                    }
                }
            });
        } catch (Exception e) {
            log.error("load user is error", e);
        }
        SpringEventListListener.isSuccess = true;
    }

    //
    //    public Map<Long, UserBO> getAll() {
    //        return cache.asMap().get(RpcContext.getContext().getTenantId());
    //    }
}
