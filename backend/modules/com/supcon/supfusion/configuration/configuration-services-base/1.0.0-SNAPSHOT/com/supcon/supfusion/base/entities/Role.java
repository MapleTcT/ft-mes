package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.ArrayList;
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
@Table(name = "base_role")
public class Role extends AbstractAuditUniqueIdEntity implements Serializable {

    private static final long serialVersionUID = -3174415856215520344L;
    private String code;// 编码
    private String name;// 名称
    private String description;// 描述
    private Integer sort;// 排序
    private Long cid; //公司
    @Override
    protected String _getEntityName() {
        return null;
    }
}
