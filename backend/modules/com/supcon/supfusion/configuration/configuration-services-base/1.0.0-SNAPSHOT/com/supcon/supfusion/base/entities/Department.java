package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
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
@Table(name = Department.TABLE_NAME)
public class Department extends AbstractAuditUniqueIdEntity implements Serializable {

    private static final long serialVersionUID = -3174415856215520344L;

    public static final String TABLE_NAME = "base_department";

    private String code;// 编码
    private String name;// 名称
    @Transient
    private String type;// 部门类别
    @Transient
    private Long parentId;//上级部门Code
    private String layRec;
    private String description;// 描述


    @OneToOne(fetch=FetchType.EAGER, targetEntity=Company.class)
    @JoinColumn(name="CID", insertable=false, updatable=false)
    @Fetch(FetchMode.SELECT)
    private Company company;

    @Override
    protected String _getEntityName() {
        return Department.class.getName();
    }
}
