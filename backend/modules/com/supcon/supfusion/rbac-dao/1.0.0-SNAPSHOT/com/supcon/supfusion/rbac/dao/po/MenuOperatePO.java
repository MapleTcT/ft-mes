package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 操作表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "rbac_menuoperate", autoResultMap=true)
public class MenuOperatePO extends LogicDeleteBaseEntity {


    private static final long serialVersionUID = 3456214792787274426L;
    /**
     * 主键ID
     */
    @TableId(value = "ID")
    private Long id;

    /**
     * 版本
     */
    @TableField("ROW_VERSION")
    private Integer version;


    /**
     * 公司ID
     */
    @TableField("CID")
    private Long cid;

    /**
     * 是否允许委托
     */
    @TableField("IS_ALLOW_PROXY")
    private Boolean isAllowProxy;

    /**
     * 是否隐藏
     */
    @TableField("IS_HIDDEN")
    private Boolean isHidden;

    /**
     * 是否三员菜单
     */
    @TableField("THREE_ROLE")
    private Boolean threeRole;

    /**
     * 视图编码
     */
    @TableField("VIEW_CODE")
    private String viewCode;

    /**
     * 是否查询操作
     */
    @TableField("IS_QUERY")
    private Boolean isQuery;

    /**
     * 该操作的其他数据限制跟岗位限制等的或.与关系标志位,默认为AND
     */
    @TableField("IS_ORRELATION")
    private Boolean isOrrelation;

    /**
     * 启用数据权限
     */
    @TableField("ENABLE_DATAPERMISSION")
    private Boolean enableDataPermission;

    /**
     * 启用自定义权限
     */
    @TableField("ENABLE_CUSTOMPERMISSION")
    private Boolean enableCustomPermission;


    /**
     * 启用业务权限
     */
    @TableField("FOR_FLOW_PERMISSION")
    private Boolean forFlowPermission ;

    /**
     * 无限制
     */
    @TableField("ENABLE_NORESTRICT")
    private Boolean enableNorestrict;

    /**
     * 启用处理人
     */
    @TableField("ENABLE_DEALERPERMISSION")
    private Boolean enableDealerpermission;

    /**
     * 启用指定人员
     */
    @TableField("ENABLE_ASSIGNSTAFF")
    private Boolean enableAssignstaff;

    /**
     * 启用指定岗位
     */
    @TableField("ENABLE_ASSIGNPOS")
    private Boolean enableAssignpos;

    /**
     * 岗位限制
     */
    @TableField("ENABLE_POSRESTRICT")
    private Boolean enablePosrestrict;

    /**
     * 指定部门
     */
    @TableField("ENABLE_ASSIGNDEPT")
    private Boolean enableAssignDept;

    /**
     * 部门限制
     */
    @TableField("ENABLE_DEPTRICT")
    private Boolean enableDeptrict;

    /**
     * 启用组限制
     */
    @TableField("ENABLE_GROUPRESTRICT")
    private Boolean enableGrouprestrict;

    /**
     * 实体编码
     */
    @TableField("ENTITY_CODE")
    private String entityCode;

    /**
     * 忽视权限
     */
    @TableField("IGNORE_PERMISSION")
    private Boolean ignorePermission;

    /**
     * 是否是主列表视图的查询操作
     */
    @TableField("POWER_FLAG")
    private Boolean powerFlag;

    /**
     * 工作流版本
     */
    @TableField("FLOW_VERSION")
    private String flowVersion;

    /**
     * 工作流KEY
     */
    @TableField("FLOW_KEY")
    private String flowKey;

    @TableField("MSG_ASSEMBLED")
    private Integer msgAssembled;

    @TableField("DEPLOYMENT_ID")
    private Long deploymentId;

    @TableField("MENUOPERATETYPE")
    private String menuOperateType;

    /**
     * 菜单ID
     */
    @TableField("MENUINFO_ID")
    private Long menuinfoId;

    /**
     * ID
     */
    @TableField("ICON_CLS")
    private String iconCls;


    /**
     * 排序
     */
    @TableField("SORT")
    private Double sort;

    /**
     * 备注
     */
    @TableField("MEMO")
    private String memo;

    /**
     * 打开方式
     */
    @TableField("TARGET")
    private String target;

    /**
     * ACTION
     */
    @TableField("ACTION_URL")
    private String action;

    /**
     * 命名空间
     */
    @TableField("NAMESPACE")
    private String namespace;

    /**
     * 地址
     */
    @TableField("URL")
    private String url;

    /**
     * 中文名
     */
    @TableField("NAME_ZH_CN")
    private String nameZhCn;

    /**
     * 名称
     */
    @TableField("NAME")
    private String name;

    /**
     * 编码
     */
    @TableField("CODE")
    private String code;

    /**
     * 所属应用名
     */
    @TableField("APP")
    private String app;

    /**
     * 默认操作标识，默认操作不可删除
     */
    @TableField("DEFAULT_OPERATE")
    private Boolean defaultOperate;

    /**
     * 是否修改过 修改过的操作升级时不修改
     */
    @TableField("EDITED")
    private Boolean edited;

    /**
     * 名称国际化值
     */
    @TableField("NAME_DISPLAY")
    private String nameDisplay;

    /**
     * 菜单关联URL
     */
    @TableField(exist = false)
    private List<MenuOperateCodeUrlRefPO> urls;
    /**
     * 菜单全路径
     */
    @TableField(exist = false)
    private String fullPathName;



}
