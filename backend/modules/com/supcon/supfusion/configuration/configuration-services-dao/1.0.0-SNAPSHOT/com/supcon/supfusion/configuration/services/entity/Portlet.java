package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = Portlet.TABLE_NAME)
public class Portlet extends AbstractAuditUniqueIdEntity implements Serializable {

    private static final long serialVersionUID = -3021912707032101211L;
    public static final String TABLE_NAME = "ec_portlet";

    private String code;
    //	private int version;
    private String url;

    @Column(name = "MORE_URL")
    private String moreUrl;

    @Column(name = "MORE_TARGET")
    private String moreTarget;

    @Column(name = "SIZE_NUM")
    private Integer sizeNum;
//    @BAPInternational(fieldName = "titleInternational", replace = false)
    private String title;
//    @BAPInternational(fieldName = "titleKeyInternational", replace = false)
    private String titleKey;

    @Column(name = "TITLE_COLOR")
    private String titleColor;

    @Column(name = "IS_DEFAULT")
    private Boolean isDefault = false;

    @Column(name = "POWER_FLAG", columnDefinition = "INTEGER")
    private Boolean powerFlag = false;//是否启用权限
    @Transient
    private Module module;//模块编码 冗余字段
    @Column(name = "MODULE_CODE")
    private String moduleCode;
    private Long cid;//公司ID
    private Integer scopeNum;//所属范围   0所有公司   1本公司

    @Column(name = "OPERATE_CODE")
    private String operateCode;//操作编码

    @Column(name = "MENU_CODE")
    private String menuCode;//菜单编码
    private MenuInfo menuInfo; //关联菜单
    private MenuOperate menuOperate; //关联操作

    @Column(name = "IFRAME_FLAG", columnDefinition = "INTEGER")
    private Boolean iframeFlag = false;//是否适用iframe

    @Column(name = "IS_HIDDEN", columnDefinition = "INTEGER")
    private Boolean hidden = false;//是否隐藏

    @Lob
    @Column(name = "ONLOAD_FUNC")
    private String onloadFunc;//onload事件

    @Lob
    @Column(name = "RESIZE_FUNC")
    private String resizeFunc;//resize事件

    @Column(name = "HEIGHT")
    private Integer height;//高度,iframeFlag为true时有效

    @Column(name = "MEMO")
    private String memo;//备注

    @Transient
    private String bakUrl;//原来URL 用于判断URL是否被编辑过

    public String getTitleKey() {
        if (titleKey == null || titleKey.equals("")) {
            return title;
        }
        return titleKey;
    }

    @Transient
    public String _getEntityName() {
        return Portlet.class.getName();
    }

    public Boolean getPowerFlag() {
        return null == powerFlag ? false : powerFlag;
    }


    public Boolean getIframeFlag() {
        return null == iframeFlag ? false : iframeFlag;
    }


    @ManyToOne
    @JoinColumn(name = "MENU_INFO_ID", referencedColumnName = "id")
    @Index(name = "INDEX_PORTLET_MENU_ID")
    @Fetch(FetchMode.SELECT)
    public MenuInfo getMenuInfo() {
        return menuInfo;
    }

    @OneToOne(cascade = CascadeType.REMOVE, optional = true)
    @JoinColumn(name = "MENU_OPERATE_ID")
    @Index(name = "INDEX_PORTLET_MOP_ID")
    @Fetch(FetchMode.SELECT)
    public MenuOperate getMenuOperate() {
        return menuOperate;
    }

}
