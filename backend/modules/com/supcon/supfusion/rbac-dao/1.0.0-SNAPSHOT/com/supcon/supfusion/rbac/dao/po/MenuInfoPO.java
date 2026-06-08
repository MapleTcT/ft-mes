package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import com.supcon.supfusion.rbac.dao.enums.MenuInfoTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 菜单表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "rbac_menuinfo", autoResultMap=true)
public class MenuInfoPO extends LogicDeleteBaseEntity {


    private static final long serialVersionUID = 3581834963733980305L;
    /**
     * 主键ID
     */
    @TableId(value = "ID")
    private Long id;

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
     * 版本
     */
    @TableField("VERSION")
    private Integer version;

    /**
     * 父节点ID
     */
    @TableField(value = "PARENT_ID")
    private Long parentId;

    /**
     * 层级
     */
    @TableField("LAY_NO")
    private Integer layNo;

    /**
     * 层级
     */
    @TableField("LAY_REC")
    private String layRec;

    /**
     * 顺序
     */
    @TableField("SORT")
    private Double sort;

    /**
     * 公司ID
     */
    @TableField("CID")
    private Long cid;

    /**
     * 全路径
     */
    @TableField("FULL_PATH")
    private String fullPath;

    /**
     * 全路径 菜单名
     */
    @TableField("FULL_PATH_NAME")
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
    @TableField("SECURITY_CLASS")
    private String securityClass;

    /**
     * 绝对隐藏  1 时 菜单管理、权限管理、以及用户登录后所见的菜单中全部强制隐藏
     */
    @TableField("ABSOLUTE_HIDDEN")
    private Boolean absoluteHidden;

    /**
     * 是否三员菜单
     */
    @TableField("THREE_ROLE")
    private Boolean threeRole;

    /**
     * 请求方式 0:链接页面，1：链接URL
     */
    @TableField("SHOW_TYPE")
    private Integer showType;


    /**
     * 请求类型
     */
    @TableField("REQUEST_TYPE")
    private Integer requestType;

    /**
     * 隐藏类型
     */
    @TableField("HIDDEN_TYPE")
    private Integer hiddenType;

    /**
     * 菜单类型
     * 0 folder ,1 page, 4 app
     */
    @TableField("MENU_TYPE")
    private Integer menuType;

    /**
     * 是否隐藏
     */
    @TableField("IS_HIDE")
    private Boolean isHide;

    /**
     * 是否仅集团使用
     */
    @TableField("GROUP_ONLY")
    private Boolean groupOnly;

    /**
     * SOURCE
     */
    @TableField("SOURCE")
    private String source;

    /**
     * 实体编码
     */
    @TableField("ENTITY_CODE")
    private String entityCode;

    /**
     * 模型CODE
     */
    @TableField("MODULE_CODE")
    private String moduleCode;

    /**
     * 是否默认系统
     */
    @TableField("SYSTEM_DEFAULT")
    private Boolean systemDefault;

    /**
     * CSS_CLASS(菜单样式用)
     */
    @TableField("CSS_CLASS")
    private String cssClass;

    /**
     * ACTION
     */
    @TableField("ACTION_URL")
    private String actionUrl;

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
     * 打开方式
     */
    @TableField("TARGET")
    private String target;

    /**
     * 备注
     */
    @TableField("MEMO")
    private String memo;

    /**
     * 是否启用
     */
    @TableField("ENABLE")
    private Boolean enable;

    /**
     * 是否是叶子节点
     */
    @TableField("LEAF")
    private Boolean leaf;

    /**
     * 是否修改过 修改过的菜单升级时不修改
     */
    @TableField("EDITED")
    private Boolean edited;

    /**
     * 资源类型 0是菜单，后续更多的请看枚举类
     */
    @TableField("TYPE")
    private MenuInfoTypeEnum type;

    /**
     * 名称国际化值
     */
    @TableField("NAME_DISPLAY")
    private String nameDisplay;

    /**
     * 所属模块
     */
    @TableField("APP")
    private String app;
    /**
     * 是否不受权限控制
     */
    @TableField("NO_RESTRICT")
    private Boolean noRestrict;

    /**
     * 组态期 0，运行期 1
     */
    @TableField("STATUS")
    private Integer status;

    /**
     * 地址
     */
    @TableField("ROUTE")
    private String route;

    /**
     * 额外信息
     */
    @TableField("EXTRA")
    private String extra;


    @TableField(exist = false)
    private List<MenuInfoPO> children;

    @TableField(exist = false)
    private List<Long> companyIds;

    @TableField(exist = false)
    private String parentCode;

    @TableField("APPID")
    private String appId;

    //菜单安装方式 1：本地安装
    @TableField(exist = false)
    private Integer flag;

    //菜单子节点
    @TableField(exist = false)
    private MenuInfoPO nodeMenu;

    @TableField(exist = false)
    private Boolean company_readOnly; //适用范围只读

    @Override
    public int hashCode() {
        if (this.id != null) {
            return this.id.hashCode();
        } else if (this.code != null && !this.code.equals("")) {
            return this.code.hashCode();
        }
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MenuInfoPO) {
            MenuInfoPO obj = (MenuInfoPO) o;
            return obj.getId().equals(this.id) || obj.getCode().equals(this.code);
        }
        return false;
    }

    public void addChlidResource(MenuInfoPO menuInfoPO) {
        if (Objects.isNull(this.getChildren())) {
            List<MenuInfoPO> tmpResourceList = new ArrayList<MenuInfoPO>();
            tmpResourceList.add(menuInfoPO);
            this.setChildren(tmpResourceList);
        } else {
            this.children.add(menuInfoPO);
        }
    }
}
