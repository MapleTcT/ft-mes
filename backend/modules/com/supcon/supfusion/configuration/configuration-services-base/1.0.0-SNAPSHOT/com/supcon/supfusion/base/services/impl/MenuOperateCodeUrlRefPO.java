package com.supcon.supfusion.base.services.impl;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

/**
 * <p>
 * 菜单操作编码URL关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-22
 */
@Data
@Entity
@Table(name="rbac_menuoperatecode_url_ref")
public class MenuOperateCodeUrlRefPO implements Serializable {


    private static final long serialVersionUID = 3975132987757863622L;

    @Id
    private Long id;


    /**
     * 菜单操作编码
     */
    private String menuoperateCode;

    /**
     * 对应URL
     */
    private String url;

    /**
     * 应用名
     */
    private String app;

    /**
     * 请求方法，0 GET,1 POST,2 PUT,3 DELETE
     */
    private Integer methodType;

    /**
     * 是否需要正则匹配
     */
    private Boolean regMatch;

    /**
     * 是否自定义操作
     */
    private Boolean isCustom;

    /**
     * 导入形式
     */
    private Integer importType;

}
