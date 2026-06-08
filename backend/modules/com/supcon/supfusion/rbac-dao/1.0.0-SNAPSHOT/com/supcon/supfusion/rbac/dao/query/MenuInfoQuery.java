package com.supcon.supfusion.rbac.dao.query;


import com.supcon.supfusion.framework.cloud.common.annotation.Escape;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
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
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuInfoQuery extends LogicDeleteBaseEntityQuery implements Serializable {


    private static final long serialVersionUID = -3363474062103720904L;
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 父节点ID
     */
    private Long parentId;

    /**
     * 层级
     */
    private Integer layNo;

    /**
     * 层级
     */
    private String layRec;

    /**
     * 顺序
     */
    private Double sort;

    /**
     * 公司ID
     */
    private Long cid;

    /**
     * 全路径
     */
    private String fullPath;

    /**
     * 全路径 菜单名
     */
    private String fullPathName;

    /**
     * 密级
     * SystemCode:
     *  SECRET_CLASS/5 非密
     *  SECRET_CLASS/3 秘密
     *  SECRET_CLASS/2 机密
     *  SECRET_CLASS/6 内部资料
     *  SECRET_CLASS/7 核心商密
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
     * 请求方式 0:链接页面，1：链接URL
     */
    private Integer showType;


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
    private Boolean hide;

    /**
     * 是否仅集团使用
     */
    private Boolean groupOnly;

    /**
     * SOURCE
     */
    private String source;

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
     * ACTION
     */
    private String actionUrl;

    /**
     * 命名空间
     */
    private String namespace;

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
     * 是否启用
     */
    private Boolean enable;

    /**
     * 是否是叶子节点
     */
    private Boolean leaf;

    /**
     * 是否修改过 修改过的菜单升级时不修改
     */
    private Boolean edited;

    /**
     * 资源类型 0是菜单，后续更多的请看枚举类
     */
    private Integer type;

    /**
     * 是否隐藏
     */
    private Boolean isHide;

    /**
     * 名称国际化值
     */
    private String nameDisplay;

    /**
     * 所属应用名
     */
    private String app;

    /**
     * 是否不受权限控制
     */
    private Integer noRestrict;

    private Long refCompanyId;

    private List<Long> ids;

    private Long userId;

    private String route;

    private Integer status;

    private List<String> operateCodes;
}
