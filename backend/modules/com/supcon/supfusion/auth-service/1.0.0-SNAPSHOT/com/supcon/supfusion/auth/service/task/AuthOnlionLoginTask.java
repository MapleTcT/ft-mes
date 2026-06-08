package com.supcon.supfusion.auth.service.task;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.auth.common.utils.HttpUtil;
import com.supcon.supfusion.auth.dao.mapper.OnlineUserMapper;
import com.supcon.supfusion.auth.dao.po.OnlineUserPO;
import com.supcon.supfusion.auth.manager.TenantManagerAdapter;
import com.supcon.supfusion.auth.service.OnlineUserService;
import com.supcon.supfusion.auth.service.bo.OnlineUserBO;
import com.supcon.supfusion.auth.service.cache.AuthTicketCache;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.tenant.api.dto.TenantDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.supcon.supfusion.auth.common.constants.Constants.*;
import static com.supcon.supfusion.auth.common.constants.Constants.TENANT_TICKET;

/**
 * @author wanghaifeng
 * @date 2021-05-28
 **/
@SuppressWarnings("校正回归")
@Slf4j
@Component
public class AuthOnlionLoginTask {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    OnlineUserMapper onlineUserMapper;
    @Autowired
    OnlineUserService onlineUserService;
    @Autowired
    private AuthTicketCache authTicketCache;

    @Resource
    private TenantManagerAdapter tenantManagerAdapter;

    @Scheduled(fixedRate = 10*60*1000,initialDelay = 1000*10)
    public void shceduleDeleteOutLineUser() {
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
            tenantInfos.forEach(tenantId -> {
                try {
                    log.info("tenantId======>" + tenantId);
                    RpcContext rpcContext = RpcContext.getContext();
                    rpcContext.setTenantId(tenantId);
                    log.info("=========shceduleDeleteOutLineUser start=========");
                    List<OnlineUserBO> onlineUsers = onlineUserService.queryAllOnlinUsers();

                    if (onlineUsers == null) {
                        stringRedisTemplate.opsForValue().set(tenantId+"_"+"admin",0+"");
                        log.info("=========shceduleDeleteOutLineUser No onlineUser end=========");
                        return;
                    }

                    List<Long> logoutIds = new ArrayList<>();
                    onlineUsers.forEach(onlineUser -> {

                        try {
                            log.info("=========shceduleDeleteOutLineUser onLinUser:{}=========", onlineUser);
                            if (StringUtils.isBlank(onlineUser.getTicket()) || StringUtils.isBlank(onlineUser.getDeviceType())) {
                                logoutIds.add(onlineUser.getId());
                            } else {
                                Long expireTtl = stringRedisTemplate.getExpire(String.format(AUTH_TICKET, onlineUser.getTicket()));
                                log.info("=========shceduleDeleteOutLineUser expireTtl:{}=========", expireTtl);
                                if (expireTtl == null || expireTtl <= 0) {
                                    onlineUserService.removeOnlineUserByTicketActByManual(onlineUser.getTicket(),tenantId);
                                }
                            }
                        } catch (Exception e) {

                        }
                    });
                    log.info("=========shceduleDeleteOutLineUser onlineUsers:{} need to Logout process....=========", logoutIds);
                    onlineUserService.deleteOnlineUserByIds(logoutIds);
                    log.info("=========shceduleDeleteOutLineUser onlineUsers:{} have Logout end=========", logoutIds);
                    Integer admin = onlineUserMapper.selectCount(Wrappers.lambdaQuery(OnlineUserPO.class)
                            .eq(OnlineUserPO::getUserName, "admin"));
                    stringRedisTemplate.opsForValue().set(tenantId+"_"+"admin",admin+"");
                } catch (Throwable e) {
                    log.error("delete online user is error", e);
                }
            });
        }
    }

}
