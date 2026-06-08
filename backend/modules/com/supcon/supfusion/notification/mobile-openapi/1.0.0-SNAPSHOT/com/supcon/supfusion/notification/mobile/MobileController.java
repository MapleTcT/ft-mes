package com.supcon.supfusion.notification.mobile;


import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.mobile.service.MobileService;
import com.supcon.supfusion.notification.protocol.model.Ack;
import com.supcon.supfusion.notification.protocol.model.Notice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/open-api/notification-mobile")
public class MobileController {

    @Autowired
    MobileService mobileService;

    @PostMapping("/mobile")
    @ResponseBody
    public Result<Ack> sendDingTalk(@RequestBody Notice notice){
        return new Result<>(mobileService.send(notice));
    }

}
