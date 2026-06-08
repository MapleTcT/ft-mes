package com.supcon.supfusion.notification.sms.service.runner;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.admin.openapi.feign.RegisterOpenApiFeign;
import com.supcon.supfusion.notification.admin.openapi.vo.ProtocolConfigVO;
import com.supcon.supfusion.notification.admin.openapi.vo.ProtocolTemplateVO;
import com.supcon.supfusion.notification.admin.openapi.vo.RegisterResponseVO;
import com.supcon.supfusion.notification.sms.config.SuposConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Arrays;

import static com.supcon.supfusion.notification.sms.Constants.*;
import static com.supcon.supfusion.notification.sms.service.runner.ConfigRunner.appcode;

/**
 * 初始化钉钉注册
 *
 * @author chenweinan
 * @create 2020/7/2
 */
@Component("smsJincangRegisterRunner")
@Slf4j
@Order(value = 2)
public class RegisterRunner implements CommandLineRunner {

    @Autowired
    SuposConfiguration suposConfiguration;
    @Autowired
    RegisterOpenApiFeign registerOpenApiFeign;
    @Value("${server.port:8080}")
    private Integer port;

    @Override
    public void run(String... args) throws Exception {

        ProtocolConfigVO protocolConfigVO = new ProtocolConfigVO();
        protocolConfigVO.setProtocol("sms");
        protocolConfigVO.setConfigUrl("configurl");
        protocolConfigVO.setName(APP_NAME);
        protocolConfigVO.setAppName(APP_SHOW_NAME);
        protocolConfigVO.setVenderName(VENDOR_NAME);

        // pod ip 上报
        protocolConfigVO.setServiceName(InetAddress.getLocalHost().getHostAddress() + ":" + port);
        protocolConfigVO.setSendUrl("/open-api/notification-sms-jincang/sms");
        protocolConfigVO.setI18nKey("短信");
        protocolConfigVO.setDefaultTemplateCode("sms001");
        protocolConfigVO.setSystemConfigCode(appcode);
        protocolConfigVO.setSystemConfigAppCode(suposConfiguration.getAppId());

        ProtocolTemplateVO t1 = new ProtocolTemplateVO();
        t1.setName("行政通知");
        t1.setCode("sms001");
        t1.setDescription("备注");
        t1.setTemplate("**部门发布了《${title}$》的通知，请注意查收！");

        ProtocolTemplateVO t2 = new ProtocolTemplateVO();
        t2.setName("待办消息");
        t2.setCode("sms002");
        t2.setDescription("备注");
        t2.setTemplate("${username}$，您有一条“${title}$”的待办，请及时处理！");

        protocolConfigVO.setTemplates(Arrays.asList(t1, t2));
        Result<RegisterResponseVO> register = null;
        try {
            register = registerOpenApiFeign.register(protocolConfigVO);
        } catch (Exception e) {
            log.error("-----调用notification失败-----,{}", e.getMessage());
        }
        log.info("-----短信注册完成-----,{}", register);
    }
}
