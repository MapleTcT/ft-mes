package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;

/**
 * <p>
 * 操作表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MenuOperateDTO extends DTO {


    private static final long serialVersionUID = 4239136538816406751L;
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 版本
     */
    private Integer version;


    /**
     * 公司ID
     */
    private Long cid;

    /**
     * 是否允许委托
     */
    private Boolean isAllowProxy;

    /**
     * 是否隐藏
     */
    private Boolean isHidden;

    /**
     * 是否三员菜单
     */
    private Boolean threeRole;

    /**
     * 视图编码
     */
    private String viewCode;

    /**
     * 是否查询操作
     */
    private Boolean isQuery;

    /**
     * 该操作的其他数据限制跟岗位限制等的或.与关系标志位,默认为AND
     */
    private Boolean isOrrelation;

    /**
     * 启用特殊权限
     */
    private Boolean enableSpecialpermission;

    /**
     * 启用自定义权限
     */
    private Boolean enableOtherrestrict;

    /**
     * 操作类型
     */
    private Boolean menuoperateIscontainer;

    /**
     * 启用业务权限
     */
    private Boolean forFlowPermission;

    /**
     * 无限制
     */
    private Boolean enableNorestrict;

    /**
     * 启用处理人
     */
    private Boolean enableDealerpermission;

    /**
     * 启用指定人员
     */
    private Boolean enableAssignstaff;

    /**
     * 启用指定岗位
     */
    private Boolean enableAssignpos;

    /**
     * 岗位限制
     */
    private Boolean enablePosrestrict;

    /**
     * 启用组限制
     */
    private Boolean enableGrouprestrict;

    /**
     * 实体编码
     */
    private String entityCode;

    private String menuOperateType;

    /**
     * 忽视权限
     */
    private Boolean ignorePermission;

    /**
     * 是否是主列表视图的查询操作
     */
    private Boolean powerFlag;

    /**
     * 工作流版本
     */
    private String flowVersion;

    /**
     * 工作流KEY
     */
    private String flowKey;

    private Integer msgAssembled;

    private Long deploymentId;


    /**
     * 菜单ID
     */
    private Long menuinfoId;

    /**
     * ID
     */
    private String iconCls;

    /**
     * 模块信息：BUNDLE的SYMBOLICNAME组成
     */
    private String module;

    /**
     * 排序
     */
    private Long sort;

    /**
     * 备注
     */
    private String memo;

    /**
     * 打开方式
     */
    private String target;

    /**
     * ACTION
     */
    private String action;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 地址
     */
    private String url;

    /**
     * ID
     */
    private String nameZhCn;

    /**
     * 名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;


}
