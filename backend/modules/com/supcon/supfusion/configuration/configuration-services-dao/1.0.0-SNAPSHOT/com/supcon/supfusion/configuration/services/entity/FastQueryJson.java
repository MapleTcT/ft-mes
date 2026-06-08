package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;
import lombok.Data;
import org.apache.commons.lang.StringEscapeUtils;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 快速查询实体
 *
 * @author fangzhibin
 */
@Data
@javax.persistence.Entity
//@Table(name = FastQueryJson.TABLE_NAME)
public class FastQueryJson extends AbstractCodeEntity implements Serializable {
    private static final long serialVersionUID = -634994973811501778L;
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    public static final String TABLE_NAME = "ec_fast_query_json";
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

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "TARGETMODEL_CODE", referencedColumnName = "code")
    @Fetch(FetchMode.SELECT)
    private Model targetModel;// 当前fastQueryJson的关联模型


    private String layoutName;
    @JsonIgnore
    @OneToMany(mappedBy = "fastQueryJson",fetch = FetchType.EAGER)
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
        if (view != null && (getCode() == null || getCode().equals(""))) {
            setCode(view.getCode());
        }
    }


    @SuppressWarnings("rawtypes")
    public Map getQueryConfigMap() {
        return queryConfigMap;
    }

    @SuppressWarnings("rawtypes")
    public void setQueryConfigMap(Map queryConfigMap) {
        this.queryConfigMap = queryConfigMap;
    }

    @Override
    protected String _getEntityName() {
        return FastQueryJson.class.getName();
    }

    public String getQueryConfigEscapeHtml() {
        return StringEscapeUtils.escapeHtml(queryConfig);
    }

    public View getView() {
        return view;
    }


    public Model getTargetModel() {
        return targetModel;
    }
}
