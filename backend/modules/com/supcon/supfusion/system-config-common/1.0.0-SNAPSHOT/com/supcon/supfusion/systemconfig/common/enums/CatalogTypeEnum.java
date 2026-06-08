package com.supcon.supfusion.systemconfig.common.enums;

import com.supcon.supfusion.systemconfig.common.exception.SystemConfigErrorEnum;
import com.supcon.supfusion.systemconfig.common.exception.SystemConfigException;

/**
 * @author lifangyuan
 */
public enum CatalogTypeEnum {
    /**
     * 系统配置
     */
    SYSTEM("system", 1),
    /**
     * app配置
     */
    APP("app", 2);


    private Integer type;

    private String name;

    CatalogTypeEnum(String name, Integer type) {
        this.name = name;
        this.type = type;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static CatalogTypeEnum getCatalogType(Integer type) {
        for (CatalogTypeEnum catalogTypeEnum : CatalogTypeEnum.values()) {
            if (catalogTypeEnum.getType().compareTo(type) == 0) {
                return catalogTypeEnum;
            }
        }
        throw new SystemConfigException(SystemConfigErrorEnum.INSER_TYPE_EXIST);
    }
}
