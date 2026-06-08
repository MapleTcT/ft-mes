package com.supcon.supfusion.base.license;

import com.supcon.supfusion.license.api.LicenseApiService;
import com.supcon.supfusion.license.api.dto.LicenseInfoDTO;
import lombok.extern.java.Log;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@Log
public class EcNacosLicense implements InitializingBean {
    private String artifact = "";
    /**
     * 软件狗key
     */
    private String licenseKey = "";
    /**
     * 应用模块名称（授权中文）
     */
    private String applicationName = "";
    /**
     * 应用模块型号（授权英文）
     */
    private String applicationType = "";

    @Autowired
    private LicenseApiService licenseApiService;

    @Override
    public void afterPropertiesSet() {
        //自定义代码区，对licenseKey，applicationName，applicationType进行赋值
        /* CUSTOM CODE START(serviceimpl,registerNacosLicense,hnjbxgl_1.0.0,hnjbxgl_1.0.0) */
// 自定义代码

        /* CUSTOM CODE END */
        artifact = "supPlant-Dev";
        licenseKey = "EdrvXM2VSorwfKb4iDrzMMRgDzfLimq73HOkrltncTOK1xXIAa+5nQJum1DguTH2XIxtGQ==";
        applicationName = "supPlant应用模块开发平台软件";
        applicationType = "supPlant-Dev";
        //向nacos注册模块信息
        //软件狗key不允许为空
        if (!ObjectUtils.isEmpty(licenseKey)) {
            new Thread(() -> {
                log.info("注册授权开始 ===== ");
                try {
                    int i = 0;
                    while (i == 0) {
                        LicenseInfoDTO licenseInfoDTO = new LicenseInfoDTO();
                        licenseInfoDTO.setModuleCode(artifact);
                        licenseInfoDTO.setLicenseKey(licenseKey);
                        licenseInfoDTO.setApplicationName(applicationName);
                        licenseInfoDTO.setApplicationType(applicationType);
                        try {
                            licenseApiService.registerLicenseInfo(licenseInfoDTO);
                            i = 1;
                            log.info("注册授权信息成功 ======");
                        } catch (Exception e) {
                            log.info("授权服务未启动，注册授权信息失败 =======");
                        }
                        Thread.sleep(30 * 1000);
                    }
                } catch (Exception e) {
                    log.info("注册授权发生错误 =====");
                }
            }).start();
        }
    }
}


