package com.supcon.supfusion.custon.property.dao.entity.base;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

/**
 * @author zhang yafei
 */
@Data
public class LogicBasePO extends BasePO {


    @TableLogic(
            value = "1",
            delval = "0"
    )
    @TableField("valid")
    private Boolean valid;
}
