package com.supcon.supfusion.file.server.service.task;

import com.supcon.supfusion.file.server.common.constants.Constants;
import com.supcon.supfusion.file.server.common.utils.SystemUtil;
import com.supcon.supfusion.file.server.common.utils.TenantUtil;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfoLocalStorage;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Set;

/**
 * 初始化图标库
 **/
@Component
public class InitIcon implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (Constants.LINUX.equalsIgnoreCase(SystemUtil.getOS())) {
            Set<TenantInfo> tenantInfoSet = TenantInfoLocalStorage.getAll();
            for (TenantInfo tenantInfo : tenantInfoSet) {
                RpcContext rpcContext = RpcContext.getContext();
                rpcContext.setTenantId(tenantInfo.getId());
                FileUtils.copyDirectory(new File("/root/initFile"), new File("/data/" + TenantUtil.getTenantId()));
            }
        }
    }
}
