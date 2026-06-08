package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.base.enums.OperateTarget;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.*;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/20
 */
@Setter
@Getter
@Entity
@Immutable
@Table(name = MenuInfo.TABLE_NAME)
public class MenuInfo extends AbstractAuditUniqueIdEntity implements Serializable {

    public static final String TABLE_NAME = "base_menuinfo";
    public static final Integer ORGANIZATION_MENU = 8;
    public static final Integer HEAD_HIDDEN = 1;
    public static final Integer GROUP_HIDDEN = 2;
    public static final Integer UNIT_HIDDEN = 4;
    public static final Integer ORGANIZATION_HIDDEN = 8;

    private static final long serialVersionUID = -4521269704975761641L;
    @XmlElement
    private String code; // 编码
    @XmlElement
    private String name; // 名称
    private String memo; // 备注
    @XmlElement
    private String url;// 程序URL
    private String namespace;
    private String action;
    @XmlElement
    private Double sort = (double) 0; // 排序
    private String cssClass;
    private String moduleCode;// 模型id
    private String ecEntityCode;
    private String entityCode;
    private Boolean groupOnly;// 是否系统默认
    private Boolean isHide = false;// 是否隐藏

    @OneToMany(mappedBy = "menuInfo", fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    private Set<MenuOperate> menuOperates = new HashSet<MenuOperate>();

    /**
     * 绝对隐藏  true时 菜单管理、权限管理、以及用户登录后所见的菜单中全部强制隐藏
     */
    private Boolean absoluteHidden = false;



    @Column(columnDefinition = "INTEGER", length = 1)
    private Boolean leaf = false;

    @Transient
    private MenuInfo parent;

    private Long parentId;

    @Transient
    private String parentCode;

    private Long cid;

    private String layRec;
    private Integer status;
    private String fullPathName;
    @Transient
    List<MenuInfo> children = new ArrayList<>();

    private Integer layNo = 0;
    @OneToOne(fetch=FetchType.EAGER, targetEntity=Company.class)
    @JoinColumn(name="CID", insertable=false, updatable=false)
    @Fetch(FetchMode.SELECT)
    private Company company;

    @Enumerated(EnumType.STRING)
    private OperateTarget target;// 打开方式

    @Transient
    private String app;

    @Transient
    public boolean getIsParent() {
        return getLeaf() == null || !getLeaf();
    }

    @Override
    protected String _getEntityName() {
        return MenuInfo.class.getName();
    }

}
