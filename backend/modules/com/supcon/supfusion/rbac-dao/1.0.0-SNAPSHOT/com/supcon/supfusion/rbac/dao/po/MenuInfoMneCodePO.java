package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import com.supcon.supfusion.rbac.dao.enums.MenuInfoTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 菜单表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "rbac_menu_mnecode", autoResultMap=true)
public class MenuInfoMneCodePO extends PO {


    private static final long serialVersionUID = -4266075050672342157L;
    /**
     * 主键ID
     */
    @TableId(value = "ID")
    private Long id;

    /**
     * 版本
     */
    @TableField("row_version")
    private String version;

    /**
     * 语言类型
     */
    @TableField("LANGUAGE")
    private String language;

    /**
     * 菜单ID
     */
    @TableField(value = "MENU_INFO")
    private Long menuInfoId;

    /**
     * 助记码
     */
    @TableField("MNE_CODE")
    private String mneCode;

}
