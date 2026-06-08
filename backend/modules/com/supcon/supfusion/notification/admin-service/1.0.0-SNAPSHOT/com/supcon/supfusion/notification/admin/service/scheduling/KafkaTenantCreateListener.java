package com.supcon.supfusion.notification.admin.service.scheduling;

import com.supcon.supfusion.framework.cloud.common.events.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class KafkaTenantCreateListener implements TenantEventListener {



    @Autowired
    TenantEventListenerManager tenantEventListenerManager;

    @PostConstruct
    public void init(){
        tenantEventListenerManager.addListener(this);
    }

    @Autowired
    DynamicTableTask dynamicTableTask;

    @SneakyThrows
    @Override
    public void onAdd(TenantAddEvent event) {
        TenantInfo tenant = event.getTenant();

        String id = tenant.getId();
        log.info("======> 创建 租户 动态表 {}",id);
        dynamicTableTask.createTable(-1,id);
        dynamicTableTask.createTable(0,id);
        dynamicTableTask.createTable(1,id);
    }

    @Override
    public void onDestroy(TenantDestroyEvent event) {

    }

    @Override
    public int order() {
        return 0;
    }


}
