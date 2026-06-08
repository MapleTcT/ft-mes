package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * 特殊权限
 *
 * @author zhangbobin
 * @date 2015年10月10日
 */

@Data
@javax.persistence.Entity
@Table(name = SpecialPermissionForUShow.TABLE_NAME)
public class SpecialPermissionForUShow extends AbstractAuditUniqueCodeEntity implements Serializable, Cloneable {

    private static final long serialVersionUID = -4783679812329932628L;

    public static final String TABLE_NAME = "ec_special_permission_ushow";

    //用户ID
    @Column(name = "USER_ID")
    private Long userId;

    //关联的特殊权限
    @ManyToOne
    @JoinColumn(name = "SPECIAL_PERMISSION_CODE")
    private SpecialPermission specialPermission;
    //id
    @Column(name = "VALUE_ID")
    private String valueId;
    //title
    @Column(name = "VALUE_TITLE")
    private String valueTitle;
    //code
    @Column(name = "VALUE_CODE")
    private String valueCode;
    //操作ID
    @Column(name = "OPERATE_ID")
    private Long operateId;
    //是否包含上下级
    @Column(name = "IS_INCLUDE_SUB")
    private String isIncludeSub;
    //layRec
    @Column(name = "LAY_REC")
    private String layRec;
    //isAssigned
    @Column(columnDefinition = "INTEGER", length = 1, name = "IS_ASSIGNED")
    private Boolean isAssigned;

    public SpecialPermission getSpecialPermission() {
        return specialPermission;
    }

    @Override
    protected String _getEntityName() {
        return SpecialPermissionForUShow.class.getName();
    }

    @Override
    public Object clone() {
        SpecialPermissionForUShow ushow = null;
        try {
            ushow = (SpecialPermissionForUShow) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return ushow;
    }


}
