package com.supcon.supfusion.notification.apiserver.manager.fegin;


import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth")
@InternalApi(path = "/api/auth/")
public interface StaffClient {
    @GetMapping(value = "notice/noticeList")
    JSONObject getNoticeListByUserIds(@RequestParam("userNames") String userNames, @RequestParam("type") Integer type);
}
