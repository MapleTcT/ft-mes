package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import lombok.Data;

@Data
@TableName(value = "ec_sql",autoResultMap = true)
public class Sql extends LogicBasePO {
    private static final long serialVersionUID = -7436616994105114860L;

    @TableId
    private String code;

    private EcEnv ecEnv = EcEnv.product;

    @TableField(value = "query_sql")
    private String sql;

    private Integer type;

    private String viewCode;

    private String dataGridCode;
    //增强型视图里面的列表PTcode
    private Boolean projFlag;

    public static final int TYPE_LIST_PENDING = 5;
    public static final int TYPE_LIST_QUERY = 6;
    public static final int TYPE_LIST_REFERENCE = 7;
    public static final int TYPE_USED_MNECODE = 8;
    public static final int TYPE_USED_TREE = 9;
    public static final int TYPE_USED_ORDERBY = 4;
    public static final int TYPE_USED_TOTALS = 3;

}
