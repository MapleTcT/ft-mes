/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年10月28日 下午7:45:13
 */
@Data
@TableName("wfm_diagram_permission")
public class DiagramPermissionPO extends BaseEntity {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private Long id;
    /**
     * 流程编号
     */
    private String processKey;
    /**
     * 流程名称(冗余)
     */
    private String diagramName;
    /**
     * 用户ID或者all
     */
    private String staffId;
    /**
     * 角色ID
     */
    private Long roleId;
    /**
     * 岗位ID
     */
    private Long positionId;
    /**
     * 部门ID
     */
    private Long departmentId;
    /**
     * 是否在移动端启动 0-no, 1-yes
     */
    private Integer mobileStart;
    /**
     * 租户ID
     */
    private String tenantId;
    
}
