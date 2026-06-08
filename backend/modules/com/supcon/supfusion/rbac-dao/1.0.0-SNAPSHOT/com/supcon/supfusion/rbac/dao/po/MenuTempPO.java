package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 菜单数据回滚表
 * </p>
 *
 * @author 袁阳
 * @since 2020-09-28
 */
@Data
@TableName(value = "rbac_menu_temp", autoResultMap=true)
public class MenuTempPO extends PO {


    /**
     * 主键ID
     */
    @TableId(value = "ID")
    private Long id;

    /**
     * 回滚标识
     */
    @TableField("UUID")
    private String uuid;

    /**
     * 老数据
     */
    @TableField("OLD_DATA")
    private String oldData;

    /**
     * 新数据
     */
    @TableField("NEW_DATA")
    private String newData;

}
