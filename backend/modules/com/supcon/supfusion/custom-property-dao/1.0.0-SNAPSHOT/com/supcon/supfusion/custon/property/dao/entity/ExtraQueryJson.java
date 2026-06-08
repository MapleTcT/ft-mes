package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 扩展的快速查询SQL组织JSON
 *
 * @author fangzhibin
 * @version $Id$
 */
@Data
@TableName(value = "runtime_extra_query_json",autoResultMap = true)
public class ExtraQueryJson extends LogicBasePO {

    private static final long serialVersionUID = 8724868988534990488L;

    @TableId
    private String code;

    private String queryConfig;

    private Boolean projFlag;

    private EcEnv ecEnv = EcEnv.product;

    private String viewCode;

    public void setView(String viewCode) {
        this.viewCode = viewCode;
        if (StringUtils.isNotBlank(viewCode)) {
            setCode(viewCode);
        }
    }

}
