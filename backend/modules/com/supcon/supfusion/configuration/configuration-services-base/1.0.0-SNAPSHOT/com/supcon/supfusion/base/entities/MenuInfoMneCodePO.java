package com.supcon.supfusion.base.entities;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;

/**
 * <p>
 * 菜单表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Data
@Table(name = "rbac_menu_mnecode")
public class MenuInfoMneCodePO {


    /**
     * 主键ID
     */
    @Column(name = "ID")
    private Long id;

    /**
     * 版本
     */
    private String version;

    /**
     * 语言类型
     */
    @Column(name = "LANGUAGE")
    private String language;

    /**
     * 菜单ID
     */
    @Column(name = "MENU_INFO")
    private Long menuInfoId;

    /**
     * 助记码
     */
    @Column(name = "MNE_CODE")
    private String mneCode;

}
