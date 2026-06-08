package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 标签表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "rbac_tag", autoResultMap=true)
public class TagPO extends LogicDeleteBaseEntity {


    private static final long serialVersionUID = -7344340800565083600L;
    /**
     * 主键ID
     */
    @TableId(value = "ID")
    private Long id;

    /**
     * 版本号
     */
    @TableField("VERSION")
    private Integer version;

    /**
     * 标签类型
     */
    @TableField("TYPE")
    private String type;

    /**
     * 标签名
     */
    @TableField("NAME")
    private String name;

    /**
     * 公司ID
     */
    @TableField("CID")
    private Long cid;

    /**
     * 关联ID
     */
    @TableField("OBJECTID")
    private Long objectid;


}
