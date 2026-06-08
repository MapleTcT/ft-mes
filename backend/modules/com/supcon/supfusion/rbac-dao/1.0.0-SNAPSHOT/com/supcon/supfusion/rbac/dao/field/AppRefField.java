package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * <p>
 * 菜单数据回滚表
 * </p>
 *
 * @author 袁阳
 * @since 2020-09-28
 */
@Data
@TableName(value = "rbac_app_ref", autoResultMap=true)
public class AppRefField{


    private static final long serialVersionUID = -6599233550684187998L;
    /**
     * 主键ID
     */
    public static String id = "ID";

    /**
     * 菜单id
     */
    public static String menuId = "MENUID";

    /**
     * appid
     */
    public static String appId = "APPID";

}
