package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 菜单操作编码URL关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-22
 */
public class MenuOperateCodeUrlRefField implements Serializable {


    /**
     * 主键ID
     */
    public static String id="ID";


    /**
     * 菜单操作编码
     */
    public static String menuoperateCode="MENUOPERATE_CODE";

    /**
     * 对应URL
     */
    public static String url="URL";

    /**
     * 应用名
     */
    public static String app="APP";

    /**
     * 请求方法，0 GET,1 POST,2 PUT,3 DELETE
     */
    public static String methodType="METHOD_TYPE";

    /**
     * 是否需要正则匹配
     */
    public static String regMatch="REG_MATCH";

    /**
     * 是否自定义操作
     */
    public static String isCustom="IS_CUSTOM";

    /**
     * 导入形式
     */
    public static String importType="IMPORT_TYPE";

}
