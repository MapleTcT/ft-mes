package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.base.enums.SystemDisplayType;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.io.Serializable;
import java.util.LinkedHashMap;
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
@Table(name = SystemEntity.TABLE_NAME)
public class SystemEntity extends AbstractAuditUniqueIdEntity implements Serializable {
    public static final String TABLE_NAME = "base_systementity";
    private static final long serialVersionUID = 1L;
    private String code;// 编码 -> SystemCode.
    private String name;// 名称
    private boolean sysDefault=false;//是否系统默认
    @Transient
    private SystemDisplayType listType;//显示类型
    @Column(name = "list_type")
    private String type;
    @Transient
    private String moduleId;
    private String moduleCode;
    private boolean multiFlag;//是否多选
    private String memo;//备注
    @Column(name = "cid")
    private Long cid;
    @OneToOne(fetch= FetchType.EAGER, targetEntity=Company.class)
    @JoinColumn(name="CID", insertable=false, updatable=false)
    @Fetch(FetchMode.SELECT)
    private Company company;
    @Transient
    private Map<String, SystemCode> systemCodes = new LinkedHashMap<String, SystemCode>();
    public SystemCode putSystemCode(String key, SystemCode value) {
        return systemCodes.put(key, value);
    }

    @Override
    protected String _getEntityName() {
        return null;
    }

    public String getModuleId() {
        if (StringUtils.isEmpty(moduleCode) || moduleCode.indexOf("_") <= 0) {
            return moduleCode;
        }
        return moduleCode.split("_")[0];
    }

    public SystemDisplayType getListType() {
        if ("list".equals(this.type)) {
            return SystemDisplayType.list;
        }
        return SystemDisplayType.tree;
    }

    public void setListType(SystemDisplayType systemDisplayType) {
        setType(systemDisplayType.toString().toLowerCase());
    }
}
