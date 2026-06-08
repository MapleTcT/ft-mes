package com.supcon.supfusion.notification.app.config.config;


import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.notification.admin.api.RegisterInternalApi;
import com.supcon.supfusion.notification.admin.api.dto.ProtocolConfigDTO;
import com.supcon.supfusion.notification.admin.api.dto.ProtocolTemplateDTO;
import com.supcon.supfusion.notification.app.config.constant.SuposConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
@Component("appRegisterRunner")
@Slf4j
@Order(value = 2)
public class RegisterRunner implements CommandLineRunner {
    @Autowired
    private RegisterInternalApi registerInternalApi;
    @Value("${server.port:8080}")
    private Integer port;
    @Value("${supos_app_tenant_id:dt}")
    private String tenantId;

    @Autowired
    private SuposConfiguration suposConfiguration;

    @Override
    public void run(String... args) throws Exception {
        try {
            ProtocolConfigDTO protocolConfigDTO = new ProtocolConfigDTO();
            protocolConfigDTO.setProtocol("app");
            protocolConfigDTO.setName("notificationAdmin.src_common_app");
            protocolConfigDTO.setI18nKey("notificationAdmin.src_common_app");
            protocolConfigDTO.setAppName("business");
            protocolConfigDTO.setVenderName("supcon");
            protocolConfigDTO.setServiceName(InetAddress.getLocalHost().getHostAddress() + ":" + port);
            protocolConfigDTO.setSendUrl("/open-api/notification-app/app");
            protocolConfigDTO.setConfigUrl("configUrl");
            protocolConfigDTO.setDefaultTemplateCode("supplant001");
            //添加模板
            ArrayList<ProtocolTemplateDTO> protocolTemplateDTOS = new ArrayList<>();

            ProtocolTemplateDTO protocolTemplateDTO = new ProtocolTemplateDTO();
            protocolTemplateDTO.setName("行政通知");
//            protocolTemplateDTO.setI18nKey("notificationAdmin.protocol_basic_module_admin");
            protocolTemplateDTO.setCode("supplant008");
            protocolTemplateDTO.setDescription("备注");
            protocolTemplateDTO.setTemplate("**部门发布了《${title}$》的通知，请注意查收！");
            protocolTemplateDTOS.add(protocolTemplateDTO);

            ProtocolTemplateDTO protocolTemplateDTO1 = new ProtocolTemplateDTO();
            protocolTemplateDTO1.setName("待办消息");
//            protocolTemplateDTO1.setI18nKey("notificationAdmin.protocol_basic_module_todo");
            protocolTemplateDTO1.setCode("supplant007");
            protocolTemplateDTO1.setDescription("备注");
            protocolTemplateDTO1.setTemplate("${username}$，您有一条“${title}$”的待办，请及时处理！");
            protocolTemplateDTOS.add(protocolTemplateDTO1);

            protocolConfigDTO.setTemplates(protocolTemplateDTOS);

            //调用注册中心第三方应用注册接口注册移动端通知
            RpcContext context = RpcContext.getContext();
            context.setTenantId(tenantId);
            registerInternalApi.register(protocolConfigDTO);
            System.out.println("-----应用通知注册完成");
        } catch (Exception e) {
            log.info("调取消息中心接口报错", e);
        }

    }
}
