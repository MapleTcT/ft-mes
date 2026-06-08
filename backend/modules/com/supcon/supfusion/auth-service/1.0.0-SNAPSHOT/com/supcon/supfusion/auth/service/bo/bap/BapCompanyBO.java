package com.supcon.supfusion.auth.service.bo.bap;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * bap公司
 *
 * @author caokele
 */
@Data
@Accessors(chain = true)
public class BapCompanyBO {
    /**
     * 主键id
     */
    private Long id;
    /**
     * 版本号
     */
    private Integer version = 0;
    /**
     * 编码
     */
    private String code;
    /**
     * 描述
     */
    private String description;
    /**
     * 简称
     */
    private String shortName;
    /**
     * 全称
     */
    private String name;
    /**
     * 地址
     */
    private String address;
    /**
     * 上级公司id
     */
    private Long parentId;
    /**
     * 是否有效
     */
    private Boolean valid;
}
