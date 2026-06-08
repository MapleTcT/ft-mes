/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import lombok.Data;

/**
 * @author zhuyuyin
 * @version $Id$
 */
@Data
@TableName(value = "runtime_event",autoResultMap = true)
public class Event extends LogicBasePO {
    private static final long serialVersionUID = -7099791205704038205L;

    @TableId
    private String code;

    private String name;

    @TableField(value = "event_function")
    private String function;

    private String layoutCode;

    private String tabCode;

    private String sectionCode;

    private Boolean projFlag;

    private EcEnv ecEnv = EcEnv.product;

    @TableField(value = "event_function_es5")
    private String function_es5;

    private String moduleCode;

    private String entityCode;


    private String fieldCode;

    private String buttonCode;

}
