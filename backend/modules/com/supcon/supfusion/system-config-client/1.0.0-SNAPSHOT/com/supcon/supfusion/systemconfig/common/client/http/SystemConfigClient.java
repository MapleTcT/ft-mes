package com.supcon.supfusion.systemconfig.common.client.http;

import com.supcon.supfusion.systemconfig.common.client.util.HttpClient;
import com.supcon.supfusion.systemconfig.common.client.vo.CatalogsVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author lifangyuan
 */
@Component
public class SystemConfigClient {

    @Value("${systemconfig.addresss}")
    private String serverAddr;

    public boolean publishConfig(CatalogsVo catalogs) throws IOException {
        return HttpClient.publishConfig(serverAddr, catalogs);
    }

    public boolean deleteConfig(String appCode, String code) throws IOException {
        return HttpClient.deleteConfig(serverAddr, appCode, code);
    }
}
