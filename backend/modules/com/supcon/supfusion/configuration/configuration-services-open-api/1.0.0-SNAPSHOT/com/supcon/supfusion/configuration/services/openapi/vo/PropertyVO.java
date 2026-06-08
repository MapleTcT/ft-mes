package com.supcon.supfusion.configuration.services.openapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.enums.FieldType;
import com.supcon.supfusion.configuration.services.enums.ShowFormat;
import com.supcon.supfusion.configuration.services.entity.EcEnv;
import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.i18n.DispalyNameInternationalSerialzer;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "PropertyVO", description = "PropertyVO")
public class PropertyVO extends VO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private int version;
    private String name;
    protected EcEnv ecEnv = EcEnv.product;
    private String moduleCode;
    private String entityCode;
    @JsonSerialize(using = DispalyNameInternationalSerialzer.class)
    private String displayName;
    // private Boolean isFk;// 是否做外键使用
    // private String fkTargetEntity;// 外键指向的Entity名，非表名.但是没有前缀
    // private String fkTargetColumn;// 外键指向的字段名,属性名,非表内字段名
    // private Property fkTargetProperty;
    // private Integer fkType;// 0 - 1->1 ; 1 - 1->N ; 2 - N->N
    // private Property fkThisProperty;//如果fkType =
    // 1,即1->N的关系时，需要建立关联表，此属性记录当前entity中哪个属性参与关联
    private DbColumnType type;// 数据库类型
    private ShowFormat format;// 显示格式
    private FieldType fieldType; // 显示类型
    private Boolean isIndex = false;// 是否索引
    private Boolean nullable = false;// 是否可空
    private Integer maxLength;// 最大长度
    private Integer decimalNum;// 小数位数,当类型是浮点数时可用
    private Boolean multable = false;// 是否可以多选
    private Boolean isIgnoreAudit = false; // 是否忽略数据日志
    private Boolean isUnique = false;// 是否唯一
    private Boolean isInherent = false;// 是否是固有字段
    private Boolean isPk = false;// 是否是主键
    private String fillcontent;// 填充值
    private String fillcontentEscapeHtml;
    private String attributes;// 编码配置
    private String attributesEscapeHtml;
    private String description;
    private Model model;
    private Boolean isUsedForList = false;// 是否可用于列表
    private Boolean isMainDisplay = false;// 是否主显示字段
    private Boolean sensitive;
    private String defaultValue;
    private Boolean isUsedMneCode = false;
    private Boolean isControl = false;// 是否受控
    private Boolean isBussinessKey = false; // 是否业务主键
    private String picWidth; // 图片字段宽
    private String picHeight;// 图片字段高
    private Boolean stretch; // 图片字是否拉伸
    private Boolean isUsedForSearch = false; // 字段用于全文检索（建索引）
    private Boolean isMainAssociated = false; // 关联到主模型的属性
    private Boolean noAnalyzer = false; // 是否分词（字符类型）
    private Boolean seniorSystemCode = false; // 是否高级系统编码
    private String columnName;
    private String orgColumnName;
    private String fetchMode = "SELECT";
    private Boolean isTreeSystemCode = false;
    private Integer showWidth;
    private Boolean isCustom = false; // 是否是自定义字段
    private Boolean isEngine; // 是否已被工程修改
    private Boolean projFlag;
    private Boolean projCustomInUse;// 该自定义字段是否在工程期启用
    private Boolean isGroupObject;
    private Boolean onlyLeaf = false;
    private Integer sort; // 排序字段
    private Boolean isHidden = false; // 是否是隐藏字段
    private Integer associatedType;// 1 - 1->1 ; 2 - N->1 ; 3 - 1->N ; 4 - N->N
    private Property associatedProperty;

}
