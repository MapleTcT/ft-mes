package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.configuration.services.utils.BAPEcJsonSerializer;
import com.supcon.supfusion.configuration.services.utils.SerializeUitls;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author songjiawei
 */
@Getter
@Setter
@javax.persistence.Entity
//@Table(name = ExtraView.TABLE_NAME)
public class ExtraView extends AbstractCodeEntity implements Serializable {
    private static final long serialVersionUID = 6841491452899758945L;
    public static final String TABLE_NAME = "ec_extra_view";
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
//    @JsonIgnore
    @JsonSerialize(using= BAPEcJsonSerializer.class)
    @OneToOne
    @JoinColumn(name = "VIEW_CODE")
    @Fetch(FetchMode.SELECT)
    @Index(name = "ind_extra_view_VIEW_CODE", columnNames = {"view"})
    private View view;
    private String config;

    @Lob
    private String fullConfig;
    private Boolean projFlag;
    private String viewJson;

    @SuppressWarnings("rawtypes")
    @Transient
    private Map configMap;

    @Transient
    private List<Echarts> echartsList;

    public void setView(View view) {
        this.view = view;
        if (view != null) {
            setCode(view.getCode());
        }
    }


    @SuppressWarnings("rawtypes")
    @Transient
    public Map getConfigMap() {
		if (this.configMap == null) {
			this.configMap = (Map) SerializeUitls.deserialize(getConfig());
		}
        return this.configMap;
    }

    @SuppressWarnings("rawtypes")
    public void setConfigMap(Map configMap) {
        this.configMap = configMap;
    }

    @Override
    protected String _getEntityName() {
        return ExtraView.class.getName();
    }


    public View getView() {
        return view;
    }

}