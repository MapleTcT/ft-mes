package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 菜单表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public class MenuInfoMneCodeField{


    /**
     * 主键ID
     */
    public static String id="ID";

    /**
     * 版本
     */
    public static String version="row_version";

    /**
     * 语言类型
     */
    public static String language="LANGUAGE";

    /**
     * 菜单ID
     */
    @TableField(value = "MENU_INFO")
    public static String menuInfoId="MENU_INFO";

    /**
     * 助记码
     */
    @TableField("MNE_CODE")
    public static String mneCode="MNE_CODE";

}
