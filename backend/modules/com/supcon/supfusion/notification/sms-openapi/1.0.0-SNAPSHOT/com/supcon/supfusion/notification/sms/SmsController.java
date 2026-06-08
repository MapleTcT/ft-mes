package com.supcon.supfusion.notification.sms;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.protocol.model.Ack;
import com.supcon.supfusion.notification.protocol.model.Notice;
import com.supcon.supfusion.notification.sms.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/open-api/notification-sms-jincang")
public class SmsController {

    @Autowired
    private SmsService smsService;

//        @PostMapping("/sms")
//        @ResponseBody
//        public Result<Ack> sendSms(@RequestBody Notice notice) {
//            return new Result<>(smsService.send(notice));
//        }
    @PostMapping("/sms")
    @ResponseBody
    public Result<Ack> sendSms(@RequestBody Notice notice) {
        return new Result<>(smsService.send(notice));
    }
}
