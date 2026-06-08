package com.supcon.supfusion.base.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.supcon.supfusion.base.enums.MenuOperateType;
import com.supcon.supfusion.base.enums.OperateTarget;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;


/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/20
 */
@Data
@Entity
@Immutable
@Table(name = MenuOperate.TABLE_NAME)
public class MenuOperate extends AbstractAuditUniqueIdEntity implements Serializable {

    private static final long serialVersionUID = 5894260316071608064L;

    public static final String TABLE_NAME = "base_menuoperate";
    @XmlElement
    private String code; // 编码
    @XmlElement
    private String name; // 名称
    @XmlElement
    private String url; // 程序URL
    private String namespace;
    private String action;
    private String memo;// 备注
    private Integer sort = 0;// 排序
    @XmlElement
    private String module;// 模块信息：bundle的SymbolicName+Version组成
    @XmlElement
    private String iconCls;// 小图标
    @JsonIgnoreProperties("menuOperates")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MENUINFO_ID")
    @Fetch(FetchMode.SELECT)
    private MenuInfo menuInfo;
    @Column(name = "TYPE")
    @Enumerated(EnumType.STRING)
    private MenuOperateType menuOperateType;
    private Long deploymentId;
    private Integer msgAssembled;
    private String flowKey;
    private String flowVersion;
    @Transient
    private String flowName;
    private Boolean powerFlag=false;
    private String entityCode;
    @Transient
    private String moduleCode;
    private Boolean ignorePermission = false;
    private Boolean isAllowProxy = true;//是否允许委托
    @Enumerated(EnumType.STRING)
    private OperateTarget target;
    /**
     *   true时  权限管理中强制隐藏
     */
    private Boolean isHidden = false;


    /**
     * 启用菜单操作的限制方式
     *
     * @author tangjie
     */
    @Column(name = "ENABLE_GROUPRESTRICT")
    private Boolean enableGroupRestrict = false;

    @Column(name="ENABLE_POSRESTRICT")
    private Boolean enablePosRestrict = false;

    @Column(name="ENABLE_ASSIGNPOS")
    private Boolean enableAssignPos = false;

    @Column(name="ENABLE_ASSIGNSTAFF")
    private Boolean enableAssignStaff = false;

    @Column(name="ENABLE_DEALERPERMISSION")
    private Boolean enableDealerPermission = false;

    @Column(name="ENABLE_NORESTRICT")
    private Boolean enableNoRestrict = true;
    @Column(name = "FOR_DATA_PERMISSION")
    private Boolean forDataPermission = false;

    @Column(name="ENABLE_OTHERRESTRICT")
    private Boolean enableOtherRestrict=false;

    @Column(name="ENABLE_SPECIALPERMISSION")
    private Boolean enableSpecialPermission=false;

    private String   viewCode;
    private Boolean  isQuery = false;

    @OneToOne(fetch= FetchType.EAGER, targetEntity=Company.class)
    @JoinColumn(name="CID", insertable=false, updatable=false)
    @Fetch(FetchMode.SELECT)
    private Company company;
    private Long cid;
    @Column(name = "IS_ORRELATION", columnDefinition = "INTEGER", length = 1)
    private Boolean  isOrRelation=false;

    @Override
    protected String _getEntityName() {
        return MenuOperate.class.getName();
    }
}
