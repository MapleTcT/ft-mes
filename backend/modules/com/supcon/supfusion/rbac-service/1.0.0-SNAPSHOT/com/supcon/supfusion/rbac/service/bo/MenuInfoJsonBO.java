package com.supcon.supfusion.rbac.service.bo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.supcon.supfusion.rbac.dao.enums.MenuInfoTypeEnum;
import lombok.Data;

import java.util.List;

@Data
public class MenuInfoJsonBO {

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
    private MenuInfoJsonBO parent;

    private String app;

    private String nameDisplay;

    /**
     * 层级
     */
    private Integer layNo;

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
    private Boolean absoluteHidden=false;

    /**
     * 是否三员菜单
     */
    private Boolean threeRole=false;

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
    private Boolean isHide=false;

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

    private String ecEntityCode;

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

    private Integer status;
    private String route;
    private String extra;

    /**
     * 是否修改过 修改过的菜单升级时不修改
     */
    private MenuInfoTypeEnum type;

    private List<MenuOperateJsonBO> menuOperates;

    private Boolean noRestrict=false;
}
