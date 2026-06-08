package com.supcon.supfusion.rbac.webapi.vo.MenuInfo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.rbac.dao.enums.MenuInfoTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;

/**
 * <p>
 * 菜单表
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
@ApiModel(description= "菜单新增、查询返回类")
public class MenuInfoVO extends VO {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /**
     * 名称
     */
    @ApiModelProperty(value = "名称",required = true)
    private String name;

    /**
     * 名称国际化值
     */
    @ApiModelProperty(value = "名称国际化值")
    private String nameDisplay;
    @ApiModelProperty(value = "是否隐藏")
    private Boolean isHide;
    private String moduleCode;
    private String cssClass;
    /**
     * 编码
     */
    @ApiModelProperty(value = "编码",required = true)
    private String code;

    @ApiModelProperty(value = "公司ID",required = true)
    private Long cid;

    /**
     * 父节点ID
     */
    @ApiModelProperty(value = "父节点ID")
    private Long parentId;

    @ApiModelProperty(value = "菜单url")
    private String url;

    /**
     * 请求方式 0:链接页面，1：链接URL
     */
    @ApiModelProperty(value = "请求方式 0:链接页面，1：链接URL")
    private Integer showType;

    /**
     * 菜单类型
     */
    @ApiModelProperty(value = "菜单类型")
    private Integer menuType;

    /**
     * SOURCE
     */
    @ApiModelProperty(value = "菜单来源")
    private String source;


    /**
     * 打开方式
     */
    @ApiModelProperty(value = "打开方式")
    private String target;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String memo;

    @ApiModelProperty(value = "菜单类型：门户，菜单")
    private MenuInfoTypeEnum type;

    /**
     * 适用范围
     */
    @ApiModelProperty(value = "适用范围",required = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Long> companyIds;

    @ApiModelProperty(value = "菜单子菜单")
    private List<MenuInfoVO> children;

    private String moduleName;

    private String app;

    private Integer status;

    private String route;

    private String extra;

    private Boolean company_readOnly; //适用范围只读还是可编辑

}
