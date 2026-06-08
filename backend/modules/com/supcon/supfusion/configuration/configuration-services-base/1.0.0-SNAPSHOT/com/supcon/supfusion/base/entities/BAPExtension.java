package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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
@Table(name = BAPExtension.TABLE_NAME)
public class BAPExtension extends AbstractAuditUniqueCodeEntity implements Serializable {

    public static final String TABLE_NAME = "base_extension";
    private static final long serialVersionUID = 2916924463567273081L;

    private String code;
    private String name;
    private String moduleCode;
    private String moduleName;
    private String cssName;
    private String menuOperateCode;
    private String url;
    private String script;
    private Boolean needPermission = false;
    private String memo;
    private String cssStyle;
    private Boolean isSystem = false;
    @ManyToOne(fetch=FetchType.EAGER, optional=true, targetEntity = SystemCode.class)
    @JoinColumn(name="ZONE_TYPE", nullable=true)
    @Fetch(FetchMode.SELECT)
    private SystemCode zoneType;
    private Integer sort;
    private Boolean isHidden = false;

    @Override
    protected String _getEntityName() {
        return this.getClass().getName();
    }

}
