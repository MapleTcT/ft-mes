package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

/**
 * 自定义数据权限
 *
 * @author zhangbobin
 * @date 2015年7月6日
 */

@Data
@javax.persistence.Entity
@Table(name = OtherRestrict.TABLE_NAME)
public class OtherRestrict extends AbstractAuditUniqueCodeEntity implements Serializable {
    private static final long serialVersionUID = 3330836159364429513L;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "VIEW_CODE")
    @Fetch(FetchMode.SELECT)
    @XmlTransient
    private View view;

    @Column(length = 4000, nullable = true)
    private String jsonCondition;

    @Column(length = 1000, nullable = true, name = "CONDITION_SQL")
    private String sql;

    private String title;

    @Column(name = "HAND_WRITING_FLAG")
    private Boolean isHandWriting = false;

    @Transient
    private Boolean checked = false; // 当前用户权限是否受此限制

    @Column(length = 200, nullable = true, name = "MEMO")
    private String memo; // 备注


    public static final String TABLE_NAME = "ec_other_restrict";


    public View getView() {
        return view;
    }


    @Override
    protected String _getEntityName() {
        // TODO Auto-generated method stub
        return OtherRestrict.class.getName();
    }


}
