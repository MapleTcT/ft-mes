package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/20
 */
@Entity
@Table(name = UserPSpecialPermission.TABLE_NAME)
@Data
@Immutable
public class UserPSpecialPermission extends AbstractAuditUniqueIdEntity implements Serializable {
    private static final long serialVersionUID = -2693446995788447603L;

    public static final String TABLE_NAME = "base_user_specialpermission";

    private UserPermission userPermission;
    private String content;
    private String configString;

    @Override
    protected String _getEntityName() {
        return null;
    }
}
