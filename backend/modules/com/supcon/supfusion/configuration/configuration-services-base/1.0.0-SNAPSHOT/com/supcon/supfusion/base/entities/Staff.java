package com.supcon.supfusion.base.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/20
 */
@Data
@Entity
@Immutable
@Table(name = Staff.TABLE_NAME)
public class Staff extends AbstractAuditUniqueIdEntity implements Serializable {

    private static final long serialVersionUID = 1687625233192456434L;

    public static final String TABLE_NAME = "base_staff";

    private String code;// 员工编号
    private String name;// 姓名

    @Transient
    private String userName;

    @Column(name = "MAIN_POSITION_ID")
    private Long mainPositionId;
    @JoinColumn(name = "MAIN_POSITION_ID" ,insertable=false,updatable=false)
    @OneToOne( targetEntity=Position.class)
    @Fetch(FetchMode.SELECT)
    private Position mainPosition;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "staff")
    @Fetch(FetchMode.SELECT)
    @JsonIgnore
    private List<PositionWork> positionWorks = new ArrayList<PositionWork>();
    @OneToOne(cascade = CascadeType.REMOVE, optional = true)
    @JoinColumn(name="USER_ID")
    @Fetch(FetchMode.SELECT)
    @JsonIgnore
    private User user;

    @Override
    protected String _getEntityName() {
        return Staff.class.getName();
    }
}
