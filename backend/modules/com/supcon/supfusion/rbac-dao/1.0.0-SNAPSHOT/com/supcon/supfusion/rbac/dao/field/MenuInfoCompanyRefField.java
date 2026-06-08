package com.supcon.supfusion.rbac.dao.field;

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
public class MenuInfoCompanyRefField implements Serializable {


    /**
     * 主键ID
     */
    public static String id="ID";

    /**
     * 菜单ID
     */
    public static String menuinfoId="MENUINFO_ID";

    /**
     * 公司ID
     */
    public static String companyId="COMPANY_ID";

    /**
     * 公司名
     */
    public static String companyName="COMPANY_NAME";

    /**
     * APPID
     */
    public static String appId="APPID";
}
