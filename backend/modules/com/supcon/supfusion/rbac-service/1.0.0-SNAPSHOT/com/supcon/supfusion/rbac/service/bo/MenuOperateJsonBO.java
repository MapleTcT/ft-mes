package com.supcon.supfusion.rbac.service.bo;

import lombok.Data;

import java.util.List;

@Data
public class MenuOperateJsonBO {

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
    private Boolean isAllowProxy=true;

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
    private Boolean isQuery=false;

    /**
     * 该操作的其他数据限制跟岗位限制等的或.与关系标志位,默认为AND
     */
    private Boolean isOrrelation=false;

    /**
     * 启用数据权限
     */
    private Boolean enableDataPermission=false;

    /**
     * 启用自定义权限
     */
    private Boolean enableCustomPermission=false;


    /**
     * 启用业务权限
     */
    private Boolean forFlowPermission =false;

    /**
     * 无限制
     */
    private Boolean enableNorestrict=true;

    /**
     * 启用处理人
     */
    private Boolean enableDealerpermission=false;

    /**
     * 启用指定人员
     */
    private Boolean enableAssignstaff=false;

    /**
     * 启用指定岗位
     */
    private Boolean enableAssignpos=false;

    /**
     * 岗位限制
     */
    private Boolean enablePosrestrict=false;

    /**
     * 指定部门
     */
    private Boolean enableAssignDept=false;

    /**
     * 部门限制
     */
    private Boolean enableDeptrict=false;

    /**
     * 启用组限制
     */
    private Boolean enableGrouprestrict=false;

    /**
     * 实体编码
     */
    private String entityCode;

    /**
     * 忽视权限
     */
    private Boolean ignorePermission=false;

    /**
     * 是否是主列表视图的查询操作
     */
    private Boolean powerFlag=false;

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
     * 操作类型
     */
    private String type;

    /**
     * 菜单ID
     */
    private Long menuinfoId;

    /**
     * ID
     */
    private String iconCls;


    /**
     * 排序
     */
    private Double sort;

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

    /**
     * 默认操作标识，默认操作不可删除
     */
    private Boolean defaultOperate;

    private List<MenuOperateUrlJsonBO> urls;
}
