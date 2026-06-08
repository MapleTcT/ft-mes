package com.supcon.supfusion.rbac.dao.query;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import com.supcon.supfusion.rbac.dao.po.RolePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 角色用户表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Data
public class RoleUserQuery extends LogicDeleteBaseEntityQuery{


    /**
     * 主键ID
     */
    private Long id;

    /**
     * 版本信息
     */
    private Integer version;

    /**
     * 是否仅是岗位带入的角色
     */
    private Boolean positionFlag;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 用户ID
     */
    private Long userId;


    /**
     * 调出时间
     */
    private String endTime;

    /**
     * 调入时间
     */
    private String startTime;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 人员名
     */
    private String personName;

    /**
     * 人员编码
     */
    private String personCode;

    /**
     * 来源 1 来源于用户 2 来源于岗位 3 两者都有
     */
    private Integer fromPosition;

    private Long cid;

    private String code;

    private List<Long> ids;
}
