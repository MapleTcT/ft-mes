package com.supcon.supfusion.notification.app.openapi;


import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.protocol.model.Ack;
import com.supcon.supfusion.notification.protocol.model.Notice;
import com.supcon.supfusion.notification.app.service.NoticeSupplantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/open-api/notification-app")
public class SupplantController {

    @Autowired
    private NoticeSupplantService noticeSupplantService;

    @PostMapping("/app")
    @ResponseBody
    public Result<Ack> sendDingTalk(@RequestBody Notice notice){
        return new Result<>(noticeSupplantService.send(notice));
    }

}
