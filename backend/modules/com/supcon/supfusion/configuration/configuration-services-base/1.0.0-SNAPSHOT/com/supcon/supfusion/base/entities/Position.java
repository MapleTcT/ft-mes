package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Map;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/20
 */
@Data
@Entity
@Immutable
@Table(name = Position.TABLE_NAME)
public class Position extends AbstractAuditUniqueIdEntity implements Serializable {
    public static final String TABLE_NAME = "base_position";
    private static final long serialVersionUID = -3174415856215520344L;
    private String code;// 编码
    private String name;// 名称
    private String description;// 描述
    @ManyToOne(targetEntity = Department.class)
    @JoinColumn(name = "DEPARTMENT_ID", referencedColumnName="ID")
    @Fetch(FetchMode.SELECT)
    private Department department;
    @OneToOne(fetch=FetchType.EAGER, targetEntity=Company.class)
    @JoinColumn(name="CID", insertable=false, updatable=false)
    @Fetch(FetchMode.SELECT)
    private Company company;
    private Integer layNo = 0;
    private String layRec;
    private Long parentId;
    @Transient
    private Position parent;
    @Transient
    private Map<String,Object> attrMap;

    @Override
    protected String _getEntityName() {
        return Position.class.getName();
    }
}
