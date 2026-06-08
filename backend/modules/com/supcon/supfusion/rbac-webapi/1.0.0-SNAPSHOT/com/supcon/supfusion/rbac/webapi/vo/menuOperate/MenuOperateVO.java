package com.supcon.supfusion.rbac.webapi.vo.menuOperate;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.rbac.webapi.vo.MenuOperateCodeUrlRef.MenuOperateCodeUrlRefVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;

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
@ApiModel(description= "菜单操作返回类")
public class MenuOperateVO extends VO {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    private Long id;


    /**
     * 公司ID
     */
    @ApiModelProperty(value = "公司ID")
    private Long cid;

    @ApiModelProperty(value = "默认操作的URL")
    private String url;

    /**
     * 启用自定义权限
     */
    @ApiModelProperty(value = "启用自定义权限")
    private Boolean enableCustomPermission;

    /**
     * 操作类型
     */
    @ApiModelProperty(value = "操作类型")
    private Long menuoperateIscontainer;

    /**
     * 启用指定人员
     */
    @ApiModelProperty(value = "启用指定人员")
    private Boolean enableAssignstaff;

    /**
     * 启用指定岗位
     */
    @ApiModelProperty(value = "启用指定岗位")
    private Boolean enableAssignpos;

    /**
     * 岗位限制
     */
    @ApiModelProperty(value = "岗位限制")
    private Boolean enablePosrestrict;

    /**
     * 指定部门
     */
    @ApiModelProperty(value = "指定部门")
    private Boolean enableAssignDept;

    /**
     * 部门限制
     */
    @ApiModelProperty(value = "部门限制")
    private Boolean enableDeptrict;

    /**
     * 启用处理人
     */
    @ApiModelProperty(value = "启用处理人")
    private Boolean enableDealerpermission;

    /**
     * 无限制
     */
    @ApiModelProperty(value = "无限制")
    private Boolean enableNorestrict=true;

    /**
     * 实体编码
     */
    @ApiModelProperty(value = "实体编码")
    private String entityCode;


    /**
     * 菜单ID
     */
    @ApiModelProperty(value = "菜单ID")
    private Long menuinfoId;

    /**
     * 操作样式
     */
    @ApiModelProperty(value = "操作样式")
    private String iconCls;


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
     * 名称
     */
    @ApiModelProperty(value = "名称")
    private String name;

    /**
     * 名称国际化值
     */
    @ApiModelProperty(value = "名称国际化值")
    private String nameDisplay;

    /**
     * 编码
     */
    @ApiModelProperty(value = "编码")
    private String code;

    private Boolean defaultOperate;
    /**
     * 操作关联URL
     */
    @ApiModelProperty(value = "操作关联URL")
    private List<MenuOperateCodeUrlRefVO> urls;

    @ApiModelProperty(value = "是否默认操作")
    private Boolean isDefault = false;

    @ApiModelProperty(value = "菜单全路径")
    private String fullPathName;
}
