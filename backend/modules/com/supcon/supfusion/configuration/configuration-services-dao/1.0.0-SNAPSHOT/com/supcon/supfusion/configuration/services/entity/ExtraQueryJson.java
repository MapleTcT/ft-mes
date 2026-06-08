package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;
import lombok.Data;
import org.hibernate.annotations.*;

import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * 扩展的快速查询SQL组织JSON
 *
 * @author fangzhibin
 * @version $Id$
 */
@Data
@javax.persistence.Entity
//@javax.persistence.Table(name = ExtraQueryJson.TABLE_NAME)
public class ExtraQueryJson extends AbstractCodeEntity implements Serializable {

    private static final long serialVersionUID = 8724868988534990488L;
    public static final String TABLE_NAME = "ec_extra_query_json";
    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "VIEW_CODE")
    @Fetch(FetchMode.SELECT)
    @Index(name = "idx_Extra_VIEW")
    private View view;
    private String queryConfig;
    private Boolean projFlag;
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    public void setView(View view) {
        this.view = view;
        if (view != null) {
            setCode(view.getCode());
        }
    }


    @Override
    protected String _getEntityName() {
        return ExtraQueryJson.class.getName();
    }


    public View getView() {
        return view;
    }
}
