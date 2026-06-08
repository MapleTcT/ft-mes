package com.supcon.supfusion.auth.service.rpc;

import com.supcon.supfusion.auth.api.OnlineUserApiService;
import com.supcon.supfusion.auth.api.dto.OnlineUserDTO;
import com.supcon.supfusion.auth.service.OnlineUserService;
import com.supcon.supfusion.auth.service.bo.OnlineUserBO;
import com.supcon.supfusion.auth.service.cache.AuthTicketCache;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.exception.BizErrorEnum;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

import static com.supcon.supfusion.auth.common.constants.Constants.*;

/**
 * @author caokele
 */
@ServiceApiService
public class OnlineUserApiServiceImpl extends BaseController implements OnlineUserApiService {

    @Resource
    private OnlineUserService onlineUserService;

    @Resource
    private AuthTicketCache authTicketCache;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @SuppressWarnings("unchecked")
    @Override
    public Result<OnlineUserDTO> createOnlineUser(OnlineUserDTO onlineUserDTO) {
        OnlineUserBO onlineUserBO = new OnlineUserBO();
        BeanUtils.copyProperties(onlineUserDTO, onlineUserBO);
        onlineUserBO = onlineUserService.createOnlineUser(onlineUserBO);
        return Result.custom()
                .data(onlineUserBO)
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }

    @Override
    public Result removeOnlineUserByTicket(String ticket) {

        String key = String.format(AUTH_TICKET, ticket);
        stringRedisTemplate.delete(key);

        String tenantId = RpcContext.getContext().getTenantId();
        onlineUserService.removeOnlineUserByTicketActByManual(ticket, tenantId);
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Result updateOnlineUserCompany(String ticket, Long companyId) {
        onlineUserService.updateOnlineUserCompany(ticket, companyId);
        return Result.custom()
                .code(BizErrorEnum.SYSTEM_OK.getCode())
                .build();
    }
}
