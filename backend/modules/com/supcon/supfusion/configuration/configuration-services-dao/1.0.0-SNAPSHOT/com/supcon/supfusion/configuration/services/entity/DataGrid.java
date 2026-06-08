package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.configuration.services.utils.SerializeUitls;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@javax.persistence.Entity
//@Table(name = DataGrid.TABLE_NAME)
public class DataGrid extends AbstractAuditUniqueCodeEntity implements Serializable {
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    private String moduleCode;
    private String entityCode;
    /**
     *
     */
    private static final long serialVersionUID = 5784761384089509802L;
    public static final String TABLE_NAME = "ec_data_grid";
    private String name;

  //@JsonIgnore
    @ManyToOne
    @JoinColumn(name = "VIEW_CODE")
    @Fetch(FetchMode.SELECT)
    @Index(name = "idx_datagrid_view")
    private View view;

    private String config;


    @Lob
    private String fullConfig;

  //@JsonIgnore
    @ManyToOne
    @JoinColumn(name = "TARGETMODEL_CODE", referencedColumnName = "code")
    @Fetch(FetchMode.SELECT)
    private Model targetModel;// 当前datagrid的关联模型

  //@JsonIgnore
    @ManyToOne
    @JoinColumn(name = "ORGPROPERTY_CODE", referencedColumnName = "code")
    @Fetch(FetchMode.SELECT)
    private Property orgProperty;// １对多关联中，多的一方关联１的关联字段

    private Boolean ex;// 是否增强版

//    @BAPInternational(replace = true)
    private String dataGridName;

    private Integer dataGridType; // PT类型，默认是编辑pt 0， 列表PT 1
    private String permissionCode;
    private Boolean isPermission = false; // 启用

//    @BAPInternational(fieldName = "operateNameInternational", replace = false)
    private String operateName;// 列表pt权限名称

    @OneToMany(mappedBy = "dataGrid")
    @Where(clause = "valid = 1")
    @OrderBy(clause = "code asc")
    @Fetch(FetchMode.SELECT)
    private List<Field> fields;

  //@JsonIgnore
    @OneToMany(mappedBy = "dataGrid")
    @Where(clause = "valid = 1")
    @OrderBy(clause = "code asc")
    @Fetch(FetchMode.SELECT)
    private List<Button> buttons;
  //@JsonIgnore
    @Transient
    private List<Event> events;
    @Transient
    private Map<String, Object> configMap;
    private Boolean projFlag;
    @Lob
    private String dataGridJson;


    /**
     * 把页面编辑过的信息反序列化,以Map形式给出
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public Map deserializedAreas() {
//		if (config != null) {
//			return new JSONDeserializer<Map>().deserialize(config);
//		} else {
        return Collections.EMPTY_MAP;
//		}
    }

    @Lob
    public String getConfig() {
        if (config != null && config.length() > 0) {
            if (config.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") == -1) {
                config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + config;
            }
        }
        return config;
    }

    @Override
    protected String _getEntityName() {
        return DataGrid.class.getName();
    }


    @SuppressWarnings("rawtypes")
    @Transient
    public Map<String, Object> getConfigMap() {
		if (null == this.configMap) {
			this.configMap = (Map<String, Object>) SerializeUitls.deserialize(getConfig());
		}
        return configMap;
    }


    @Override
    public String toString() {
        return _getEntityName() + " [code=" + getCode() + "]";
    }

}