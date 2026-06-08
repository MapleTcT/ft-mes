package com.supcon.supfusion.custon.property.dao.entity.base;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import lombok.Data;

/**
 * @author zhang yafei
 */
@Data
public class AbstractEntity extends PO {


    @Version
    @TableField("version")
    private Long version;

}
