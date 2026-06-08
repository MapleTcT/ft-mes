package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 快速查询实体
 *
 * @author fangzhibin
 */
@Data
@TableName(value = "ec_fast_query_json",autoResultMap = true)
public class FastQueryJson extends LogicBasePO {
    private static final long serialVersionUID = -634994973811501778L;

    @TableId
    private String code;

    private EcEnv ecEnv = EcEnv.product;

    private String queryConfig;

    private String layoutName;

    private Boolean projFlag;

    @TableField(value = "targetmodel_code")
    private String targetModelCode;// 当前fastQueryJson的关联模型

    private String viewCode;

//    @TableField()
//    private List<Field> fields;


    public void setView(String viewCode) {
        this.viewCode = viewCode;
        if (StringUtils.isNotBlank(viewCode)) {
            setCode(viewCode);
        }
    }

}
