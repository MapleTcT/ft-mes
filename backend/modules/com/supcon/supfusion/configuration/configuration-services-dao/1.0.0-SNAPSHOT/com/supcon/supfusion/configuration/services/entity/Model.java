package com.supcon.supfusion.configuration.services.entity;

import com.alibaba.druid.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.configuration.services.annotation.International;
import com.supcon.supfusion.configuration.services.i18n.NameInternationalSerialzer;
import com.supcon.supfusion.configuration.services.utils.Inflector;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.*;

import javax.persistence.Table;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 实体配置：模型
 *
 * @author songjiawei
 */
@Data
@javax.persistence.Entity
//@Table(name = Model.TABLE_NAME)
public class Model extends AbstractAuditUniqueCodeEntity implements Serializable {

    private static final long serialVersionUID = -5167596036941833535L;

    public static final String TABLE_NAME = "ec_model";

    public static final int DATA_TYPE_NORMAL = 1;
    public static final int DATA_TYPE_TREE = 2;
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    @International
    @JsonSerialize(using = NameInternationalSerialzer.class)
    private String name;
    private String modelName;// 生成表名:同一个module下面不能重复,不同module下面可以重复，因为最终生成的数据库表以module为前缀
    private String description;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "ENTITY_CODE", referencedColumnName = "code")
    @Fetch(FetchMode.SELECT)
    @NotFound(action = NotFoundAction.IGNORE)
    private Entity entity;
    @JsonIgnore
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "model")
    @Fetch(FetchMode.SELECT)
    @Where(clause = "valid = 1")
    @OrderBy(clause = "sort asc,code asc") // 新增字段排序
    private Set<Property> properties = new HashSet<Property>();
    private Boolean isMain = false;// 是否主模型
    private Integer dataType;// 数据类型
    private Integer type;
    private Boolean isExtends = false;
    @ManyToOne
    @JoinColumn(name = "extends_model_code", referencedColumnName = "code")
    @NotFound(action = NotFoundAction.IGNORE)
    private Model extendsModelName;
    private String moduleCode;
    private String jpaName;
    private String ecVersion;
    private Boolean inherentCommonFlag = false;// 是否固有公用模型

    private String tableName;
    @Transient
    private String orgTableName;
    private Boolean enableSync = false;

    private Boolean enableOperationAudit = false;
    private Boolean enableDataAudit = false;

    @Column(name = "PROJ_FLAG")
    private Boolean projFlag;

    @Transient
    private String iconSkin;

    public static final String ICONSKIN_SQL = "sql";

    private Boolean isExtraCol = false;// 是否生成大字段
    public static final int TYPE_BASE = 1;// 基础
    public static final int TYPE_INHERENT_BASE = 2;// 已存在的固有基础
    public static final int TYPE_NORMAL = 0;// 普通
    public static final int TYPE_SQL = 3; // SQL模型
    @XmlTransient
    public String entityClass;// 如果type == TYPE_INHERENT_BASE,则需录入此属性，记录完整类名。
    //	@JsonIgnore
    @Transient
    private List<AssociatedInfo> associatedInfos;
    private Boolean isCache = false;// 是否启用缓存
    private Boolean isControl = false;// 是否受控
    private Boolean isMneCode = false; // 是否是用于助记码实体
    @Transient
    private String treeAssCode;
    @Column(name = "SPECIAL_AUTH_ISANDREL")
    private Boolean isAndRelation = false;
    private Boolean isConfigSpecial = false;
    @Column(name = "SPECIALPER_TEMPLATE_SQL")
    private String specialPerTemplateSQL;

    @Column(name = "model_sql")
    private String sql;

    @Transient
    private SqlModel sqlModel;

    //	@Transient
    @Column(name = "TABLE_NAME")
    public String getTableName() {
        if (tableName != null && tableName.length() > 0) {
            return tableName;
        }
        if (entity == null) {
            return "";
        }
        if (entity.getIsInherentedBase()) {
            if (null != entityClass) {
                Table table = null;
                try {
                    Class<?> clz = Class.forName(entityClass.trim());
                    table = clz.getAnnotation(Table.class);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                if (table != null)
                    return table.name();
            }
        }
        if (null != entity.getModule() && !"".equals(entity.getModule().toString())) {
            if (!StringUtils.isEmpty(entity.getModule().getAcronym())) {
                return Inflector.getInstance().tableize(entity.getModule().getAcronym(), modelName);
            } else if (null != entity.getModule().getArtifact()) {
                return Inflector.getInstance().tableize(entity.getModule().getArtifact(), modelName);
            }
        }
        return null;
    }

    public void addProperty(Property property) {
        properties.add(property);
    }

    @Override
    protected String _getEntityName() {
        return getClass().getName();
    }

    /**
     * @return the jpaName
     */
    public String getJpaName() {
        if (jpaName == null || jpaName.length() == 0) {
            if (this.getEntity() != null && this.getEntity().getModule() != null) {
//                return firstLetterToUpper(this.getEntity().getModule().getArtifact()) + this.getModelName();
            }
        }
        return jpaName;
    }

    /**
     * @param jpaName the jpaName to set
     */
    public void setJpaName(String jpaName) {
        this.jpaName = jpaName;
    }

    /**
     * @return the ecVersion
     */
    public String getEcVersion() {
        if (ecVersion == null || ecVersion.length() == 0) {
            return "2.0";
        }
        return ecVersion;
    }

    public String getOrgTableName() {
        if (null == orgTableName || orgTableName.trim().length() == 0) {
            orgTableName = getTableName();
        }
        return orgTableName;
    }

    @Override
    public String toString() {
        return _getEntityName() + " [code=" + getCode() + "]";
    }

    @Override
    public Integer getVersion() {
        return null == super.getVersion() ? 0 : super.getVersion();
    }

    public String firstLetterToUpper(String str) {
        if (null == str)
            return null;
        if (str.length() > 0) {
            return Character.toUpperCase(str.charAt(0)) + str.substring(1);
        }
        return "";
    }

}
