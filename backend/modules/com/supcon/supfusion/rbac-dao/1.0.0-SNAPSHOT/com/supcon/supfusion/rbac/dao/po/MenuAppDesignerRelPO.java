package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName(value = "rbac_menu_app_designer", autoResultMap=true)
public class MenuAppDesignerRelPO extends PO implements Serializable {
    private static final long serialVersionUID = -5369838515131482267L;
    /**
     * 主键ID
     */
    @TableId(value = "ID")
    private Long id;

    /*    *//**
     * 菜单id
     *//*
    @TableField("MENU_ID")
    private Long menuId;

    *//**
     * 菜单父id
     *//*
    @TableField("PARENT_ID")
    private Long parentId;*/

    /**
     * appid
     */
    @TableField("APPID")
    private String appId;

    /**
     * 菜单编码
     */
    @TableField("code")
    private String code;

    /**
     * 菜单父编码
     */
    @TableField("parent_code")
    private String parentCode;
}
