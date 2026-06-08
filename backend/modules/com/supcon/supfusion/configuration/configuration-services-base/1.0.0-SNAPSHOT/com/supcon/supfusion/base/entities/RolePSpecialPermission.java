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
@Data
@Immutable
@Table(name = RolePSpecialPermission.TABLE_NAME)
public class RolePSpecialPermission extends AbstractAuditUniqueIdEntity implements Serializable {

    private static final long serialVersionUID = 6888077898689262174L;

    public static final String TABLE_NAME = "base_role_specialpermission";
    private String content;
    private String configString;

    @Override
    protected String _getEntityName() {
        return null;
    }
}
