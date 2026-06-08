package com.supcon.supfusion.license.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.license.common.constants.Constants;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 授权相关数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = Constants.LICENSE_INFO, autoResultMap = true)
public class LicenseInfoPO extends BaseEntity {

    private Long id;
    private String moduleCode;
    private String licenseKey;
    private String value;
    private String time;
    private String hashCode;
    private String applicationName;
    private String applicationType;
    private String description;
}
