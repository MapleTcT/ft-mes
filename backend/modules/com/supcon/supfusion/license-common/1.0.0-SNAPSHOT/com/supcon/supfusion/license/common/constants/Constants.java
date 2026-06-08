package com.supcon.supfusion.license.common.constants;

/**
 * 常量类
 */

public interface Constants {
    //------------------------表名------------------------
    String LICENSE_INFO = "license_info";

    /**
     * 实体配置试用时间（12小时）
     */
    Long ecEntityTime = 12 * 60 * 60 * 1000L;
    /**
     * 业务模块试用时间（6小时）
     */
    Long moduleTime = 6 * 60 * 60 * 1000L;

    String licenseRedisKey = "LICENSE:INFO";

    String licenseInitRedisKey = "LICENSE:INIT";

    String Salt = "licenseSalt";

    String ec_module = "ec";

//    String concurrent_module = "supPlant-Server-S0C";

    String nacosLicenseDataId = "supfusion-license-info.properties";

    String nacosLicenseGroup = "prod";

    //未授权
    Integer noLicense =-1;
    //已授权
    Integer haveLicense = 0;
    //未授权 但在试用期内
    Integer trialLicense = -2;

    String noLicenseDes = "未授权";
    String haveLicenseDes = "已授权";
    String trialLicenseDes = "未授权(剩余试用时间:%s)";

}
