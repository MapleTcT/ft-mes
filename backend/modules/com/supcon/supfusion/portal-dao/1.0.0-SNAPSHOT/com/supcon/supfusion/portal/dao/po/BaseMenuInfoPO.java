package com.supcon.supfusion.portal.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * @Author kk.C
 * @Description 菜单PO
 * @Date 2020/10/22 16:31
 * @Param
 * @return
 **/
@Data
@TableName(value = BaseMenuInfoPO.TABLE_NAME)
public class BaseMenuInfoPO implements Serializable {

    public static final String TABLE_NAME = "base_menuinfo";
    public static final Integer ORGANIZATION_MENU = 8;
    public static final Integer HEAD_HIDDEN = 1;
    public static final Integer GROUP_HIDDEN = 2;
    public static final Integer UNIT_HIDDEN = 4;
    public static final Integer ORGANIZATION_HIDDEN = 8;

    private static final long serialVersionUID = -4521269704975761641L;

    private int id;
    private String code; // 编码
    private String name; // 名称
    private String memo; // 备注
    private String url;// 程序URL
    private int valid;
//    private String namespace;
//    private String action;
//    private Double sort = (double) 0; // 排序
//    private String cssClass;
//    private String moduleCode;// 模型id
//    private String ecEntityCode;
//    private String entityCode;
//    private Boolean groupOnly;// 是否系统默认
//    private Boolean isHide = false;// 是否隐藏
//
//    @OneToMany(mappedBy = "menuInfo", fetch = FetchType.EAGER)
//    @Fetch(FetchMode.SELECT)
//    private Set<BaseMenuOperatePO> menuOperates = new HashSet<BaseMenuOperatePO>();
//
//    /**
//     * 绝对隐藏  true时 菜单管理、权限管理、以及用户登录后所见的菜单中全部强制隐藏
//     */
//    private Boolean absoluteHidden = false;
//
//
//
//    @Column(columnDefinition = "INTEGER", length = 1)
//    private Boolean leaf = false;
//
//    @Transient
//    private BaseMenuInfoPO parent;
//
//    private Long parentId;
//
//    @Transient
//    private String parentCode;
//
//    private Long cid;
//
//    private String layRec;
//
//    private String fullPathName;
//    @Transient
//    List<BaseMenuInfoPO> children = new ArrayList<>();
//
//    private Integer layNo = 0;
//    @OneToOne(fetch=FetchType.EAGER, targetEntity=BaseCompanyPO.class)
//    @JoinColumn(name="CID", insertable=false, updatable=false)
//    @Fetch(FetchMode.SELECT)
//    private BaseCompanyPO company;
//
//    @Enumerated(EnumType.STRING)
//    private OperateTarget target;// 打开方式
//
//    @Transient
//    private String app;
//
//    @Transient
//    public boolean getIsParent() {
//        return getLeaf() == null || !getLeaf();
//    }
//

}
