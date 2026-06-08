package com.supcon.supfusion.rbac.webapi.vo.MenuInfo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
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
public class MenuInfoSuposVO extends VO {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    private Long id;

    private String code;

    /**
     * 名称
     */
    @ApiModelProperty(value = "名称",required = true)
    private String name;

    /**
     * 名称国际化值
     */
    @ApiModelProperty(value = "名称国际化值")
    private String displayName;

    @ApiModelProperty(value = "菜单url")
    private String url;

    /**
     * 请求方式 0:链接页面，1：链接URL
     */
    @ApiModelProperty(value = "请求方式 0:链接页面，1：链接URL")
    private Integer type;


    /**
     * 打开方式
     */
    @ApiModelProperty(value = "打开方式")
    private Boolean newTab;

    private Double index;

    private String route;

    private MenuInfoIcon icon;


    @ApiModelProperty(value = "菜单子菜单")
    private List<MenuInfoSuposVO> children;

}
