package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Date;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/20
 */
@Data
@Entity
@Immutable
@Table(name = UserPermission.TABLE_NAME)
public class UserPermission extends AbstractAuditUniqueIdEntity implements Serializable {

    private static final long serialVersionUID = -2321013745505416221L;
    public static final String TABLE_NAME = "base_userpermission";

    @XmlTransient
    private User user;
    @XmlTransient
    private Company company;
    private MenuOperate menuOperate;
    private String urlPattern;
    private Integer groupFlag;// 组限制：0 1
    private Integer positionFlag;// 岗位限制：0 1
    private Integer assignPosFlag;// 指定岗位：0 1
    private Integer assignStaffFlag;// 指定人员：0 1
    private Integer assignOtherRestrictFlag;// 指定其他限制：0 1
    private Integer assignSpecialPermissionFlag;// 指定特殊限制：0 1
    private Integer noRestrictFlag;// 无限制：0 1
    private Integer dealerPermissionFlag;//处理人权限：0 1
    private Integer typeFlag = 1;// 角色权限与数据权限区分标记：0:数据权限 1：角色权限
    private int purviewType;// 授权方式：角色(0)or用户(1)
    private Date dealTime;// 处理时间
    private Staff dealStaff;// 处理人

    @Override
    protected String _getEntityName() {
        return null;
    }
}
