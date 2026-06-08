package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
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
@Table(name = "BASE_COMPANY")
public class Company extends AbstractAuditUniqueIdEntity implements Serializable {

    public static final long defaultCompanyId = 1000L;
    private static final long serialVersionUID = 3231984137432906763L;
    private String code;// 编码
    private String name;// 名称
    private String shortName;// 简称
    @Transient
    private String type;
    @Override
    protected String _getEntityName() {
        return Company.class.getName();
    }

}
