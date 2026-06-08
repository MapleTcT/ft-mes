package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 菜单操作编码URL关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-22
 */
@Data
@TableName(value = "rbac_menuoperatecode_url_ref", autoResultMap=true)
public class MenuOperateCodeUrlRefPO implements Serializable {


    private static final long serialVersionUID = 3975132987757863622L;
    /**
     * 主键ID
     */
    @TableId("ID")
    private Long id;


    /**
     * 菜单操作编码
     */
    @TableField("MENUOPERATE_CODE")
    private String menuoperateCode;

    /**
     * 对应URL
     */
    @TableField("URL")
    private String url;

    /**
     * 应用名
     */
    @TableField("APP")
    private String app;

    /**
     * 请求方法，0 GET,1 POST,2 PUT,3 DELETE
     */
    @TableField("METHOD_TYPE")
    private Integer methodType;

    /**
     * 是否需要正则匹配
     */
    @TableField("REG_MATCH")
    private Boolean regMatch;

    /**
     * 是否自定义操作
     */
    @TableField("IS_CUSTOM")
    private Boolean isCustom;

    /**
     * 导入形式
     */
    @TableField("IMPORT_TYPE")
    private Integer importType;

}
