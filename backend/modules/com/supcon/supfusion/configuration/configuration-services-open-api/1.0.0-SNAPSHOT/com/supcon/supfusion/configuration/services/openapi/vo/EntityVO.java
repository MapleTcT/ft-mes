package com.supcon.supfusion.configuration.services.openapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.configuration.services.entity.EcEnv;
import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.entity.PrintTemplate;
import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.configuration.services.i18n.NameInternationalSerialzer;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Data
@ApiModel(value = "EntityVO", description = "EntityVO")
public class EntityVO extends VO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private int version;
    @JsonSerialize(using = NameInternationalSerialzer.class)
    private String name;
    private String entityName;
    private String prefix;
    private Boolean workflowEnabled = false;
    private Boolean groupEnabled = false;
    private Boolean isBase = false;
    private Boolean isInherentedBase = false;
    private Boolean crossCompanyFlag = false;
    protected EcEnv ecEnv = EcEnv.product;
    private String description;
    private Boolean payCloseAttention = false;// 是否启用关注
    private Module module;
    private Boolean inherentCommonFlag = false;// 是否固有公用模型
    private Boolean isControl = false;// 是否受控
    private Set<Model> models = new HashSet<Model>();
    private Set<View> views = new HashSet<View>();
    private Boolean mobile = false;// 移动支持
    private Boolean enableAclRestrict = false;// 是否启用ACL限制
    private Boolean enableAudit = false; // 是否启用日志
    private Boolean enableRest = false; // 是否启用REST接口
    private Boolean enableWs = false; // 是否启用webservice接口
    private Boolean enableFieldsPermissionConf = false; // 是否启用字段权限配置
    private SystemCode entityType;
    private Boolean projFlag;
    private List<PrintTemplate> printTemplates = new ArrayList<PrintTemplate>();
}
