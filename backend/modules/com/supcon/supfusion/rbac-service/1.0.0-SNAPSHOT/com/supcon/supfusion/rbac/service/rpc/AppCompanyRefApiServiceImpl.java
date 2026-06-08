package com.supcon.supfusion.rbac.service.rpc;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.rbac.api.IAppCompanyRefApiService;
import com.supcon.supfusion.rbac.service.IAppCompanyRefService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@ServiceApiService
@Slf4j
public class AppCompanyRefApiServiceImpl implements IAppCompanyRefApiService {

    @Autowired
    IAppCompanyRefService appCompanyRefService;

    @Override
    public void deleteAppCompanyRef(String appId) {
        appCompanyRefService.deleteAppCompanyRef(appId);
    }
}
