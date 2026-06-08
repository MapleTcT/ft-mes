/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import lombok.Data;

/**
 *
 *
 * @author zhuyuyin
 * @version 1.0
 */
@Data
@TableName(value = "runtime_validate",autoResultMap = true)
public class Validate extends LogicBasePO {

    private static final long serialVersionUID = -1528441220539565873L;
    public static final String TABLE_NAME = "EC_VALIDATE";
    @TableId
    private String code;

    private EcEnv ecEnv = EcEnv.product;

    private String type;//验证类型

    private String params;//内容 xml

    private Boolean projFlag;

    private String moduleCode;

    private String entityCode;

    private String fieldCode;//关联字段
}
