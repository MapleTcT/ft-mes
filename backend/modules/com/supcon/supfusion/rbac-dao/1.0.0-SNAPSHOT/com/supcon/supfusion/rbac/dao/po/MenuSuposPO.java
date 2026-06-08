package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import lombok.*;

import java.util.*;


/**
 * @author tomcat
 * @date 21-6-1 下午11:50
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "rbac_menuinfo", autoResultMap=true)
public class MenuSuposPO extends PO implements Comparable<MenuSuposPO> {
    private static final long serialVersionUID = 789539623106754996L;

    @TableId(value = "ID")
    private Long id;

    @TableField(value = "PARENT_ID")
    private Long parentId;

    @TableField("CODE")
    private String code;

    @TableField("NAME")
    private String name;

    @TableField("URL")
    private String url;

    @TableField("SHOW_TYPE")
    private Integer showType;

    @TableField("TARGET")
    private String target;

    @TableField("NAME_DISPLAY")
    private String nameDisplay;

    @TableField("ROUTE")
    private String route;

    @TableField("CSS_CLASS")
    private String cssClass;

    @TableField("SORT")
    private Double sort;

    @TableField("MENU_TYPE")
    private Integer menuType;

    @TableField("lay_rec")
    private String layRec;

    @TableField(exist = false)
    private Integer layNo;

    @TableField(exist = false)
    private List<MenuSuposPO> children;

    public MenuSuposPO(Long id, Long parentId, String code, String name, String url,
                       Integer showType, String target, String nameDisplay, String route,
                       String cssClass, Double sort, Integer menuType, String layRec) {
        this.id = id;
        this.parentId = parentId;
        this.code = code;
        this.name = name;
        this.url = url;
        this.showType = showType;
        this.target = target;
        this.nameDisplay = nameDisplay;
        this.route = route;
        this.cssClass = cssClass;
        this.sort = sort;
        this.menuType = menuType;
        this.layRec = layRec;
        computeLayNo();
    }

    private void computeLayNo() {
        this.layNo = this.layRec.split("-").length;
    }

    public void addChildren(MenuSuposPO child) {
        if (Objects.isNull(this.children)) {
            this.children = new LinkedList<>();
        }
        this.children.add(child);
    }

    public void addChildrens(Collection<MenuSuposPO> childs) {
        if (Objects.isNull(this.children)) {
            this.children = new LinkedList<>();
        }
        this.children.addAll(childs);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MenuSuposPO) {
            return ((MenuSuposPO) o).getId().equals(this.id);
        }
        return false;
    }

    @Override
    public int compareTo(MenuSuposPO next) {
        int c = this.layNo.compareTo(next.getLayNo());
        if (c == 0) {
            return this.sort.compareTo(next.getSort());
        }
        return c;
    }
}
