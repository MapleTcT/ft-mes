package com.supcon.supfusion.auth.api;

import com.supcon.supfusion.auth.api.dto.OnlineUserDTO;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;

/**
 * @author caokele
 */
@FeignClient(name = "auth", contextId = "online-user")
public interface OnlineUserApiService {

    String API_PREFIX = "/service-api/auth";

    /**
     * 新增在线用户
     */
    @PostMapping(API_PREFIX + "/v1/online-user")
    Result<OnlineUserDTO> createOnlineUser(@Valid @RequestBody OnlineUserDTO onlineUserDTO);

    /**
     * 根据ticket删除在线用户记录
     */
    @DeleteMapping(API_PREFIX + "/v1/online-user")
    Result removeOnlineUserByTicket(@RequestParam("ticket") String ticket);

    /**
     * 更新在线用户当前公司
     */
    @PutMapping(API_PREFIX + "/v1/online-user/company")
    Result<OnlineUserDTO> updateOnlineUserCompany(@RequestParam("ticket") String ticket, @RequestParam("companyId") Long companyId);

}
