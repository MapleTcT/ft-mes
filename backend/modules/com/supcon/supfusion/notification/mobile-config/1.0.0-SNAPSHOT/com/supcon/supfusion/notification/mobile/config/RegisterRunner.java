package com.supcon.supfusion.notification.mobile.config;


import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.notification.admin.api.RegisterInternalApi;
import com.supcon.supfusion.notification.admin.api.dto.ProtocolConfigDTO;
import com.supcon.supfusion.notification.admin.api.dto.ProtocolTemplateDTO;
import com.supcon.supfusion.notification.mobile.client.SuposClint;
import com.supcon.supfusion.notification.mobile.constant.MobileHostProperties;
import com.supcon.supfusion.notification.mobile.constant.SuposConfiguration;
import com.supcon.supfusion.notification.mobile.vo.ProtocolConfigVO;
import com.supcon.supfusion.notification.mobile.vo.ProtocolTemplateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * 初始化移动端注册
 *
 * @author chenweinan
 * @create 2020/7/13
 */
@Component("mobileRegisterRunner")
@Slf4j
@Order(value = 2)
public class RegisterRunner implements CommandLineRunner {
    @Qualifier("mobileSuposConfiguration")
    @Autowired
    private SuposConfiguration suposConfiguration;
    @Autowired
    private RegisterInternalApi registerInternalApi;
    @Value("${server.port:8080}")
    private Integer port;
    @Value("${supos_app_tenant_id:dt}")
    private String tenantId;
    @Override
    public void run(String... args) throws Exception {
        try {

            ProtocolConfigDTO protocolConfigDTO = new ProtocolConfigDTO();
            protocolConfigDTO.setProtocol("mobile");
            protocolConfigDTO.setName("notificationMobile.app_name");
            protocolConfigDTO.setI18nKey("notificationMobile.app_name");
            protocolConfigDTO.setAppName("mobile");
            protocolConfigDTO.setVenderName("supcon");
            protocolConfigDTO.setServiceName(InetAddress.getLocalHost().getHostAddress() + ":" + port);
            protocolConfigDTO.setSendUrl("/open-api/notification-mobile/mobile");
            protocolConfigDTO.setConfigUrl("configUrl");
//            protocolConfigVO.setSystemConfigAppCode("mobile");
//            protocolConfigVO.setSystemConfigCode("mobile123");
            protocolConfigDTO.setDefaultTemplateCode("mobile007");
            //添加模板
            ArrayList<ProtocolTemplateDTO> protocolTemplateVOS = new ArrayList<>();

            ProtocolTemplateDTO protocolTemplateDTO = new ProtocolTemplateDTO();
            protocolTemplateDTO.setName("行政通知");
//            protocolTemplateVO.setI18nKey("notificationAdmin.protocol_basic_module_admin");
            protocolTemplateDTO.setCode("mobile008");
            protocolTemplateDTO.setDescription("备注");
            protocolTemplateDTO.setTemplate("**部门发布了《${title}$》的通知，请注意查收！");
            protocolTemplateVOS.add(protocolTemplateDTO);

            ProtocolTemplateDTO protocolTemplateDTO1 = new ProtocolTemplateDTO();
            protocolTemplateDTO1.setName("待办消息");
//            protocolTemplateVO1.setI18nKey("notificationAdmin.protocol_basic_module_todo");
            protocolTemplateDTO1.setCode("mobile007");
            protocolTemplateDTO1.setDescription("备注");
            protocolTemplateDTO1.setTemplate("${username}$，您有一条“${title}$”的待办，请及时处理！");
            protocolTemplateVOS.add(protocolTemplateDTO1);

            protocolConfigDTO.setTemplates(protocolTemplateVOS);
            RpcContext context = RpcContext.getContext();
            context.setTenantId(tenantId);
            //调用注册中心第三方应用注册接口注册移动端通知
            registerInternalApi.register(protocolConfigDTO);
            log.info("-----移动端通知注册完成");
        } catch (Exception e) {
            log.info("调取消息中心接口报错", e);
        }

    }
}
