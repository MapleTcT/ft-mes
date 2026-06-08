package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
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
@Table(name = "base_roleuser")
public class RoleUser extends AbstractAuditUniqueIdEntity implements Serializable {

    private static final long serialVersionUID = -1137603076147988314L;

    private Role role;

    @Override
    protected String _getEntityName() {
        return null;
    }
}
