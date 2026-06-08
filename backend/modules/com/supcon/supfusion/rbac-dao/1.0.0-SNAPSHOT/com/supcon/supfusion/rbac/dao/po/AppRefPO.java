package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
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
public class AppRefPO extends PO {


    private static final long serialVersionUID = -6599233550684187998L;
    /**
     * 主键ID
     */
    @TableId(value = "ID")
    private Long id;

    /**
     * 回滚标识
     */
    @TableField("MENUID")
    private Long menuId;

    /**
     * 老数据
     */
    @TableField("APPID")
    private String appId;

}
