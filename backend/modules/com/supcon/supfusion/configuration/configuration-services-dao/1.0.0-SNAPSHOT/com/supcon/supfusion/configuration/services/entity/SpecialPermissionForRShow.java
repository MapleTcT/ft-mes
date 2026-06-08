package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * 特殊权限角色展示
 *
 * @author zhangbobin
 * @date 2015年10月10日
 */
@Data
@javax.persistence.Entity
@Table(name = SpecialPermissionForRShow.TABLE_NAME)
public class SpecialPermissionForRShow extends AbstractAuditUniqueCodeEntity implements Serializable, Cloneable {

    private static final long serialVersionUID = -4783679812329932628L;

    public static final String TABLE_NAME = "ec_special_permission_rshow";

    //角色ID
    private Long roleId;

    //关联的特殊权限
    @ManyToOne
    @JoinColumn(name = "SPECIAL_PERMISSION_CODE")
    private SpecialPermission specialPermission;
    //id
    private String valueId;
    //title
    private String valueTitle;
    //code
    private String valueCode;
    //操作ID
    private Long operateId;
    //是否包含上下级
    private String isIncludeSub;
    //layRec
    private String layRec;
    //isAssigned
    private Boolean isAssigned;


    public SpecialPermission getSpecialPermission() {
        return specialPermission;
    }

    @Override
    protected String _getEntityName() {
        // TODO Auto-generated method stub
        return SpecialPermissionForRShow.class.getName();
    }

    @Override
    public Object clone() {
        SpecialPermissionForRShow ushow = null;
        try {
            ushow = (SpecialPermissionForRShow) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return ushow;
    }

}
