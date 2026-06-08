package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
@EqualsAndHashCode(callSuper = false)
@TableName(value = "rbac_menuinfo_company_ref", autoResultMap=true)
public class MenuInfoCompanyRefPO implements Serializable {


    private static final long serialVersionUID = 7914391263576426954L;
    /**
     * 主键ID
     */
    @TableId("ID")
    private Long id;

    /**
     * 菜单ID
     */
    @TableField("MENUINFO_ID")
    private Long menuinfoId;

    /**
     * 公司ID
     */
    @TableField("COMPANY_ID")
    private Long companyId;

    /**
     * 公司名
     */
    @TableField("COMPANY_NAME")
    private String companyName;

    /**
     * 菜单名
     */
    @TableField(exist = false)
    private String menuinfoName;

    /**
     * appId
     */
    @TableField("APPID")
    private String appId;
}
