package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;

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
public class MenuInfoDTO extends DTO {


    private static final long serialVersionUID = -2609425048885889135L;
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
     * 密级
     */
    private String securityClass;

    /**
     * 绝对隐藏  1 时 菜单管理、权限管理、以及用户登录后所见的菜单中全部强制隐藏
     */
    private Boolean absoluteHidden;

    /**
     * 是否三员菜单
     */
    private Boolean threeRole;

    /**
     * 请求方式0:内嵌，1：弹出式
     */
    private Integer showType;

    private Integer layNo;

    private String layRec;

    private Boolean leaf;

    private String fullPathName;

    private String fullPath;

    /**
     * 请求类型
     */
    private Integer requestType;

    /**
     * 隐藏类型
     */
    private Integer hiddenType;

    /**
     * 菜单类型
     */
    private Integer menuType;

    /**
     * 是否隐藏
     */
    private Boolean isHide;

    private Boolean groupOnly;

    /**
     * 实体编码
     */
    private String entityCode;


    /**
     * 模型CODE
     */
    private String moduleCode;

    /**
     * 是否默认系统
     */
    private Boolean systemDefault;

    /**
     * CSS_CLASS(菜单样式用)
     */
    private String cssClass;


    /**
     * 排序
     */
    private Double sort;

    /**
     * ACTION
     */
    private String actionUrl;

    /**
     * 命名空间
     */
    private String namespace;

    private Long parentId;

    /**
     * 地址
     */
    private String url;

    /**
     * 打开方式
     */
    private String target;

    /**
     * 备注
     */
    private String memo;

    /**
     * 名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;


}
