package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import lombok.Data;

/**
 * 默认视图的高级查询条件类
 *
 * @author 谭正阳
 */
@Data
@TableName(value = "ec_default_adv_cond",autoResultMap = true)
public class DefaultAdvCond extends LogicBasePO {

    /**
     *
     */
    private static final long serialVersionUID = 6437287216798097678L;

    private EcEnv ecEnv = EcEnv.product;

    @TableId
    private String code;
    /**
     * 内容
     */

    private String content = null;

    /**
     * 视图code
     */
    private String viewCode = null;

}
