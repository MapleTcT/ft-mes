package com.supcon.supfusion.configuration.services.openapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.configuration.services.entity.EcEnv;
import com.supcon.supfusion.configuration.services.entity.AssociatedInfo;
import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.i18n.NameInternationalSerialzer;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@ApiModel(value = "ModelVO", description = "ModelVO")
public class ModelVO extends VO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private int version;
    public static final int DATA_TYPE_NORMAL = 1;
    public static final int DATA_TYPE_TREE = 2;
    protected EcEnv ecEnv = EcEnv.product;
    @JsonSerialize(using = NameInternationalSerialzer.class)
    private String name;
    private String modelName;// 生成表名:同一个module下面不能重复,不同module下面可以重复，因为最终生成的数据库表以module为前缀
    private String description;
    private Entity entity;
    private Set<Property> properties = new HashSet<Property>();
    private Boolean isMain = false;// 是否主模型
    private Integer dataType;// 数据类型
    private Integer type;
    private Boolean isExtends = false;
    private Model extendsModelName;
    private String moduleCode;
    private String jpaName;
    private String ecVersion;
    private Boolean inherentCommonFlag = false;// 是否固有公用模型
    private String tableName;
    private String orgTableName;
    private Boolean enableSync = false;
    private Boolean enableOperationAudit = false;
    private Boolean enableDataAudit = false;
    private Boolean projFlag;
    private String sql; // sql模型语句
    private String viewSql; // sql模型数据库视图语句 oracle;sqlserver;...
    private Boolean isErrorSql = false;
    private String iconSkin;
    public static final String ICONSKIN_SQL = "sql";
    private Boolean isExtraCol = false;// 是否生成大字段
    public static final int TYPE_BASE = 1;// 基础
    public static final int TYPE_INHERENT_BASE = 2;// 已存在的固有基础
    public static final int TYPE_NORMAL = 0;// 普通
    public static final int TYPE_SQL = 3; // SQL模型
    public static final int TYPE_SQLERROR = 4; // SQL模型错误SQL，无字段
    public String entityClass;// 如果type == TYPE_INHERENT_BASE,则需录入此属性，记录完整类名。
    private List<AssociatedInfo> associatedInfos;
    private Boolean isCache = false;// 是否启用缓存
    private Boolean isControl = false;// 是否受控
    private Boolean isMneCode = false; // 是否是用于助记码实体
    private String treeAssCode;
    private Boolean isAndRelation = false;
    private Boolean isConfigSpecial = false;
    private String specialPerTemplateSQL;

}
