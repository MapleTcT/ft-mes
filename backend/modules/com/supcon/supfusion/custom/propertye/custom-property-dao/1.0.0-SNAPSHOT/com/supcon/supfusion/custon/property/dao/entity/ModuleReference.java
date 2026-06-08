package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.Data;

/**
 * @author zhang yafei
 */
@Data
@TableName(value = "ec_module_reference", autoResultMap=true)
public class ModuleReference extends LogicDeleteBaseEntity {

    private String targetModuleCode;

    private String moduleCode;

    private Boolean projFlag;

}
