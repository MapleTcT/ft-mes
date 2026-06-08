package com.supcon.supfusion.configuration.services.openapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.i18n.NameInternationalSerialzer;
import com.supcon.supfusion.configuration.services.entity.EcEnv;
import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Data
@ApiModel(value = "ModuleVO", description = "ModuleVO")
public class ModuleVO extends VO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private int version;
    @JsonSerialize(using = NameInternationalSerialzer.class)
    private String name;
    private String artifact;
    private Boolean isInherentedBase;
    private String category;
    protected EcEnv ecEnv = EcEnv.product;
    private String projectVersion;// 当前版本
    private String lastVersion;// 上一个版本
    private String initialVersion;// 初始版本
    private String description;
    private String deployOrder;
    private Boolean isNewGenerate = false;
    private Boolean projFlag;
    private Boolean isReadOnly = false;
    private Boolean isHide = false;
    private String iconSkin;
    private Boolean isPublish = false;
    private Date publishTime;
    private Integer deployType; //1:快速发布 2：普通发布
    private Integer level;        //模块依赖层级
    private Integer entitySize;        //实体数量
    private Boolean isRelation;        //是否是被依赖模块
    private String type;    // 微服务类型:Mis，老的bap模块为null
    private String acronym;        //缩略名称，做为数据库前缀
    private Boolean isProto = false;
    private Boolean mainModule = false;
    private String moduleRelationDeleteIds;
    private String moduleReferenceDeleteIds;
    private String moduleReferenceAddIds;
    private String moduleReferencemultiselectIDs;
    private String moduleReferencemultiselectNames;
    private Set<Entity> entities = new HashSet<Entity>();

    private Boolean isParent = false;
    private Boolean open = false;
    private List<Module> children;

}
