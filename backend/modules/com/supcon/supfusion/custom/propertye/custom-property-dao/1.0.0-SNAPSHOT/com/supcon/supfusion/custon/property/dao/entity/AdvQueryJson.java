package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.dao.entity.base.AbstractEntity;
import lombok.Data;

import java.util.List;

/**
 * 高级查询实体
 *
 * @author fangjiahan
 */
@Data
@TableName(value = "ec_adv_query_json", autoResultMap=true)
public class AdvQueryJson extends AbstractEntity {

    private static final long serialVersionUID = 6654036016637688384L;

    @TableId
    private String code;

    private EcEnv ecEnv = EcEnv.product;

    private String queryConfig;

    private String name;

    private String layoutName;

    private Boolean projFlag;

    private String viewCode;


//    private List<Field> fields;

    private String targetModelCode;// 当前advQueryJson的关联模型
}
