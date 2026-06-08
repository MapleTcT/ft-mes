package com.supcon.supfusion.rbac.dao.query;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import com.supcon.supfusion.rbac.dao.po.TagPO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 角色表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Data
public class RoleQuery extends LogicDeleteBaseEntityQuery {


    /**
     * 主键ID
     */
    private Long id;


    /**
     * 公司ID
     */
    private Long cid;

    /**
     * 是否叶子
     */
    private Boolean leaf;

    /**
     * 层级全路径
     */
    private String fullPathName;

    /**
     * 上级节点ID
     */
    private Long parentId;

    /**
     * 层级
     */
    private Integer layNo;

    /**
     * 层级结构
     */
    private String layRec;

    /**
     * 用于软件公司同步接口
     */
    private String uuid;

    /**
     * 三员类型:1系统管理员,2安全保密员 ,3安全审计员
     */
    private Integer threeRoleType;

    /**
     * 角色类型
     * SystemCode:
     *  ROLE_TYPE/roletype 1 默认公司
     */
    private String roleType;

    /**
     * 排序
     */
    private Double sort;

    /**
     * 描述
     */
    private String description;

    /**
     * 名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;

    /**
     * 版本
     */
    private Integer version;

    private Long userId;

    private Long menuOperateId;

}
