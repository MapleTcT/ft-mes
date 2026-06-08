package com.supcon.supfusion.license.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.license.api.dto.LicenseInfoDTO;
import com.supcon.supfusion.license.dao.po.LicenseInfoPO;
import com.supcon.supfusion.license.service.vo.LicenseInfoVO;

public interface LicenseService extends IService<LicenseInfoPO> {

    /**
     * 定时任务刷新授权信息
     */
    void scheduleRefresh();

    /**
     * app服务启动时，向nacos注册moduleCode和软件狗key相关信息
     */
    void registerLicenseInfo(LicenseInfoDTO licenseInfoDTO);

    /**
     * 根据模块code获取授权信息
     */
    Result<LicenseInfoVO> getLicenseByModule(String moduleCode);

    /**
     * 分页查询授权信息
     */
    PageResult<LicenseInfoVO> getLicensePage(Long current, Long size);

    /**
     * 根据授权码获取值
     */
    Integer getValueFromSCDog(String key);

    /**
     * 根据授权码从已注册授权信息中获取值
     */
    Integer getLicenseInfoByLicenseKeyFromRegistry(String licenseKey);
}
