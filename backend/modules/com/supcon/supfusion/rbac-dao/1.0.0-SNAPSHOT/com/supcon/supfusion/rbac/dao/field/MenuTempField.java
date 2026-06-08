package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;

/**
 * <p>
 * 菜单数据回滚表
 * </p>
 *
 * @author 袁阳
 * @since 2020-09-28
 */
public class MenuTempField extends PO {


    /**
     * 主键ID
     */
    @TableId(value = "ID")
    public static String id="ID";

    /**
     * 回滚标识
     */
    @TableField("UUID")
    public static String uuid="UUID";

    /**
     * 老数据
     */
    @TableField("OLD_DATA")
    public static String oldData="OLD_DATA";

    /**
     * 新数据
     */
    @TableField("NEW_DATA")
    public static String newData="NEW_DATA";

}
