package com.supcon.supfusion.rbac.api.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class OperateMenuDTO {
    private String name;
    // 菜单展示名
    private String nameDisplay;
    // 菜单编码
    @NotEmpty
    private String code;
    // 菜单所属模块 传所属app的code
    private String app;
    // 菜单所属公司ID 固定传1000
    private Long cid;
    // 模块名 先固定传‘rbac’
    private String moduleCode = "rbac";
    // 菜单访问地址 folder没有就不传，page要传
    private String url;
    // 菜单访问路由 和url保持一致
    private String route;
    // 菜单顺序
    private Double sort;
    // 菜单类型  app菜单1   普通菜单0
    private int status;
    // 父菜单code
    private String parentCode;
    /**
     * 请求方式 0:链接页面，1：链接URL
     * 安装app 传1
     */
    private Integer showType;
    /**
     * 层级  根节点传1
     */
    private Integer layNo;
    /**
     * 菜单类型
     * 0 app ,1 folder, 2 page
     */
    private Integer menuType;
    /**
     * 菜单来源  固定传‘app’
     */
    private String source;

    /**
     * 是否隐藏
     */
    private Boolean isHide=false;

    /**
     * CSS_CLASS(菜单样式用)
     */
    private String cssClass;
    /**
     * 备注
     */
    private String memo;

    private List<MenuOperateJsonDTO> menuOperates;
    private Boolean noRestrict=false;

    // 菜单打开方式 SELF：当前页面  BLANK：新页面
    private String target = "SELF";

    //菜单安装来源 1：本地安装,LDCP    0：其他方式
    private Integer flag;


}
