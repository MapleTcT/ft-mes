package com.supcon.supfusion.rbac.webapi.vo.menuOperate;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.rbac.webapi.vo.rolePermission.RolePermissionFindVO;
import com.supcon.supfusion.rbac.webapi.vo.userPermission.UserPermissionFindVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * <p>
 * 操作表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description= "操作返回类")
public class MenuOperateAssignVO extends VO {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "菜单ID")
    private Long id;


    /**
     * 公司ID
     */
    @ApiModelProperty(value = "公司ID")
    private Long cid;

    /**
     * 是否允许委托
     */
    @ApiModelProperty(value = "是否允许委托")
    private Boolean isAllowProxy;

    /**
     * 是否隐藏
     */
    @ApiModelProperty(value = "是否隐藏")
    private Boolean isHidden;

    /**
     * 是否三员菜单
     */
    @ApiModelProperty(value = "是否三员菜单")
    private Boolean threeRole;

    /**
     * 视图编码
     */
    @ApiModelProperty(value = "视图编码")
    private String viewCode;

    /**
     * 是否查询操作
     */
    @ApiModelProperty(value = "是否查询操作")
    private Boolean isQuery;

    /**
     * 该操作的其他数据限制跟岗位限制等的或.与关系标志位,默认为AND
     */
    @ApiModelProperty(value = "该操作的其他数据限制跟岗位限制等的或.与关系标志位,默认为AND")
    private Boolean isOrrelation = false;

    /**
     * 启用数据权限
     */
    @ApiModelProperty(value = "启用数据权限")
    private Boolean enableDataPermission = false;

    /**
     * 启用自定义权限
     */
    @ApiModelProperty(value = "启用自定义权限")
    private Boolean enableCustomPermission = false;


    /**
     * 启用业务权限
     */
    @ApiModelProperty(value = "启用业务权限")
    private Boolean forFlowPermission = false;

    /**
     * 无限制
     */
    @ApiModelProperty(value = "无限制")
    private Boolean enableNorestrict = true;

    /**
     * 启用处理人
     */
    @ApiModelProperty(value = "启用处理人")
    private Boolean enableDealerpermission = false;

    /**
     * 启用指定人员
     */
    @ApiModelProperty(value = "启用指定人员")
    private Boolean enableAssignstaff = false;

    /**
     * 启用指定岗位
     */
    @ApiModelProperty(value = "启用指定岗位")
    private Boolean enableAssignpos = false;

    /**
     * 岗位限制
     */
    @ApiModelProperty(value = "岗位限制")
    private Boolean enablePosrestrict = false;

    /**
     * 指定部门
     */
    @ApiModelProperty(value = "指定部门")
    private Boolean enableAssignDept=false;

    /**
     * 部门限制
     */
    @ApiModelProperty(value = "部门限制")
    private Boolean enableDeptrict=false;

    /**
     * 启用组限制
     */
    @ApiModelProperty(value = "启用组限制")
    private Boolean enableGrouprestrict = false;

    /**
     * 实体编码
     */
    @ApiModelProperty(value = "实体编码")
    private String entityCode;

    /**
     * 忽视权限
     */
    @ApiModelProperty(value = "忽视权限")
    private Boolean ignorePermission = false;

    /**
     * 是否是主列表视图的查询操作
     */
    @ApiModelProperty(value = "是否是主列表视图的查询操作")
    private Boolean powerFlag = false;

    /**
     * 工作流版本
     */
    @ApiModelProperty(value = "工作流版本")
    private String flowVersion;

    /**
     * 工作流KEY
     */
    @ApiModelProperty(value = "工作流KEY")
    private String flowKey;


    @ApiModelProperty(value = "流程ID")
    private Long deploymentId;

    /**
     * 菜单ID
     */
    @ApiModelProperty(value = "菜单ID")
    private Long menuinfoId;

    /**
     * 菜单名
     */
    @ApiModelProperty(value = "菜单名")
    private String menuinfoName;

    /**
     * 操作样式
     */
    @ApiModelProperty(value = "操作样式")
    private String iconCls;


    /**
     * 排序
     */
    @ApiModelProperty(value = "排序")
    private Double sort;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String memo;

    /**
     * 打开方式
     */
    @ApiModelProperty(value = "打开方式")
    private String target;


    /**
     * 命名空间
     */
    @ApiModelProperty(value = "命名空间")
    private String namespace;

    /**
     * 地址
     */
    @ApiModelProperty(value = "默认操作地址")
    private String url;

    /**
     * 默认操作标识，默认操作不可删除
     */
    @ApiModelProperty(value = "默认操作标识，默认操作不可删除")
    private Boolean defaultOperate;

    /**
     * ID
     */
    @ApiModelProperty(value = "中文名")
    private String nameZhCn;

    /**
     * 名称
     */
    @ApiModelProperty(value = "名称")
    private String name;

    /**
     * 编码
     */
    @ApiModelProperty(value = "编码")
    private String code;

    /**
     * 角色权限
     */
    @ApiModelProperty(value = "角色权限")
    private RolePermissionFindVO rolePermission;

    /**
     * 层级
     */
    @ApiModelProperty(value = "层级")
    private Integer layNo;

    /**
     * 用户权限
     */
    @ApiModelProperty(value = "用户权限")
    private UserPermissionFindVO userPermission;

    /**
     * 名称国际化值
     */
    @ApiModelProperty(value = "名称国际化值")
    private String nameDisplay;

}
