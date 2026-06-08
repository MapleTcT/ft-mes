package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;
import lombok.Data;
import org.apache.commons.lang.StringEscapeUtils;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.*;

import javax.persistence.Entity;
import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 高级查询实体
 *
 * @author fangjiahan
 */
@Data
@Entity
//@Table(name = AdvQueryJson.TABLE_NAME)
public class AdvQueryJson extends AbstractCodeEntity implements Serializable {

    private static final long serialVersionUID = 6654036016637688384L;

    public static final String TABLE_NAME = "ec_adv_query_json";
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "VIEW_CODE")
    @Fetch(FetchMode.SELECT)
    @Index(name = "idx_Fast_VIEW")
    private View view;

    @Lob
    private String queryConfig;

    @Transient
    private String queryConfigEscapeHtml;

    private String name;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "TARGETMODEL_CODE", referencedColumnName = "code")
    @Fetch(FetchMode.SELECT)
    private Model targetModel;// 当前advQueryJson的关联模型

    private String layoutName;

    @JsonIgnore
    @OneToMany(mappedBy = "advQueryJson",fetch = FetchType.EAGER)
    @Where(clause = "valid = 1")
    @OrderBy(clause = "code asc")
    @Fetch(FetchMode.SELECT)
    private List<Field> fields;

    @JsonIgnore
    @Transient
    private List<Event> events;

    private Boolean projFlag;

    @SuppressWarnings("rawtypes")
    @Transient
    private Map queryConfigMap;

    public void setView(View view) {
        this.view = view;
        if (view != null && (getCode() == null || "".equals(getCode()))) {
            setCode(view.getCode());
        }
    }

    public String getQueryConfigEscapeHtml() {
        return StringEscapeUtils.escapeHtml(queryConfig);
    }

    @Override
    protected String _getEntityName() {
        return AdvQueryJson.class.getName();
    }
}
