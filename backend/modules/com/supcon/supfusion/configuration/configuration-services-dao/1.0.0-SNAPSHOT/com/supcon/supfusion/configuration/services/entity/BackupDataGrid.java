/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.*;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * @author fangzhibin
 * @version $Id$
 */
@Data
@Entity
//@javax.persistence.Table(name = BackupDataGrid.TABLE_NAME)
public class BackupDataGrid extends AbstractAuditUniqueCodeEntity implements Serializable {

    private static final long serialVersionUID = 6398337238087110556L;

    public static final String TABLE_NAME = "ec_backup_data_grid";


    @ManyToOne
    @JoinColumn(name = "BACKUPVIEW_CODE")
    @Fetch(FetchMode.SELECT)
    private BackupView backupView;

    @Lob
    private String config;

    @Lob
    private String dgFieldConfig;

    private String name;


    @ManyToOne
    @JoinColumn(name = "VIEW_CODE")
    @Fetch(FetchMode.SELECT)
    @Index(name = "idx_bk_datagrid_view")
    private View view;


    @ManyToOne
    @JoinColumn(name = "TARGETMODEL_CODE", referencedColumnName = "code")
    @Fetch(FetchMode.SELECT)
    private Model targetModel;// 当前datagrid的关联模型

    @ManyToOne
    @JoinColumn(name = "ORGPROPERTY_CODE", referencedColumnName = "code")
    @Fetch(FetchMode.SELECT)
    private Property orgProperty;// １对多关联中，多的一方关联１的关联字段

    private Boolean ex;// 是否增强版


    /**
     * 把页面编辑过的信息反序列化,以Map形式给出
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public Map deserializedAreas() {
        if (config != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readValue(config, Map.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Collections.EMPTY_MAP;
    }


    public Boolean getEx() {
        if (null == ex) {
            ex = false;
        }
        return ex;
    }


    /*
     * (non-Javadoc)
     *
     * @see com.supcon.orchid.orm.entities.EcCodeEntity#_getEntityName()
     */
    @Override
    protected String _getEntityName() {
        // TODO Auto-generated method stub
        return null;
    }


    public BackupView getBackupView() {
        return backupView;
    }


    public Property getOrgProperty() {
        return orgProperty;
    }


    public Model getTargetModel() {
        return targetModel;
    }


    public View getView() {
        return view;
    }
}
