package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import com.supcon.supfusion.rbac.dao.enums.MenuInfoTypeEnum;

import java.util.List;

/**
 * <p>
 * 菜单表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public class MenuInfoField extends LogicDeleteBaseEntityField{

    public static final String[] sys_menus = {"rbac", "organization", "signature", "userManagement", "theme",
            "systemConfig", "systemCode", "i18n", "notificationAdmin", "appConfig", "appManager",
            "taskScheduler", "workflow"};

    /**
     * 主键ID
     */
    public static String id="ID";

    /**
     * 名称
     */
    public static String name="NAME";

    /**
     * 编码
     */
    public static String code="CODE";

    /**
     * 版本
     */
    public static String version="VERSION";

    /**
     * 父节点ID
     */
    public static String parentId="PARENT_ID";

    /**
     * 层级
     */
    public static String layNo="LAY_NO";

    /**
     * 层级
     */
    public static String layRec="LAY_REC";

    /**
     * 顺序
     */
    public static String sort="SORT";

    /**
     * 状态
     */
    public static String status="STATUS";

    /**
     * 公司ID
     */
    @TableField("CID")
    public static String cid="CID";

    /**
     * 全路径
     */
    public static String fullPath="FULL_PATH";

    /**
     * 全路径 菜单名
     */
    public static String fullPathName="FULL_PATH_NAME";

    /**
     * 密级
     * SystemCode:
     *  SECRET_CLASS/5 非密
     *  SECRET_CLASS/3 秘密
     *  SECRET_CLASS/2 机密
     *  SECRET_CLASS/6 内部资料
     *  SECRET_CLASS/7 核心商密
     */
    public static String securityClass="SECURITY_CLASS";

    /**
     * 绝对隐藏  1 时 菜单管理、权限管理、以及用户登录后所见的菜单中全部强制隐藏
     */
    public static String absoluteHidden="ABSOLUTE_HIDDEN";

    /**
     * 是否三员菜单
     */
    public static String threeRole="THREE_ROLE";

    /**
     * 请求方式 0:链接页面，1：链接URL
     */
    public static String showType="SHOW_TYPE";


    /**
     * 请求类型
     */
    public static String requestType="REQUEST_TYPE";

    /**
     * 隐藏类型
     */
    public static String hiddenType="HIDDEN_TYPE";

    /**
     * 菜单类型
     */
    public static String menuType="MENU_TYPE";

    /**
     * 是否隐藏
     */
    public static String isHide="IS_HIDE";

    /**
     * 是否仅集团使用
     */
    public static String groupOnly="GROUP_ONLY";

    /**
     * SOURCE
     */
    public static String source="SOURCE";

    /**
     * 实体编码
     */
    public static String entityCode="ENTITY_CODE";

    /**
     * 模型CODE
     */
    public static String moduleCode="MODULE_CODE";

    /**
     * 是否默认系统
     */
    public static String systemDefault="SYSTEM_DEFAULT";

    /**
     * CSS_CLASS(菜单样式用)
     */
    public static String cssClass="CSS_CLASS";

    /**
     * ACTION
     */
    public static String actionUrl="ACTION_URL";

    /**
     * 命名空间
     */
    public static String namespace="NAMESPACE";

    /**
     * 地址
     */
    public static String url="URL";

    /**
     * 打开方式
     */
    public static String target="TARGET";

    /**
     * 备注
     */
    public static String memo="MEMO";

    /**
     * 是否启用
     */
    public static String enable="ENABLE";

    /**
     * 是否是叶子节点
     */
    public static String leaf="LEAF";

    /**
     * 是否修改过 修改过的菜单升级时不修改
     */
    public static String edited = "EDITED";

    /**
     * 资源类型 0是菜单，后续更多的请看枚举类
     */
    @TableField("TYPE")
    public static String type="TYPE";

    /**
     * 名称国际化值
     */
    public static String nameDisplay="NAME_DISPLAY";

    /**
     * 所属应用名
     */
    public static String app="APP";
    /**
     * 是否不受权限控制
     */
    @TableField("NO_RESTRICT")
    public static String noRestrict="NO_RESTRICT";

    /**
     * 菜单属于app
     */
    public static String appId="APPID";

}
