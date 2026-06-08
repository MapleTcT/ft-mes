package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.Data;

/**
 * @author zhang yafei
 */
@Data
@TableName(value = "runtime_module_reference", autoResultMap=true)
public class ModuleReference extends LogicBasePO {

    @TableId
    private String code;

    private EcEnv ecEnv = EcEnv.product;

    private String targetModuleCode;

    private String moduleCode;

    private Boolean projFlag;

}
