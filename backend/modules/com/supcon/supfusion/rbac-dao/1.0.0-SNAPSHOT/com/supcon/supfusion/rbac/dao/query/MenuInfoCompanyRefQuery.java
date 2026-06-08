package com.supcon.supfusion.rbac.dao.query;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 菜单公司关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-30
 */
@Data
public class MenuInfoCompanyRefQuery implements Serializable{


    private static final long serialVersionUID = -2516455796599917673L;
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 菜单ID
     */
    private Long menuinfoId;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 公司名
     */
    private String companyName;

    /**
     * 菜单名
     */
    private String menuinfoName;
}
