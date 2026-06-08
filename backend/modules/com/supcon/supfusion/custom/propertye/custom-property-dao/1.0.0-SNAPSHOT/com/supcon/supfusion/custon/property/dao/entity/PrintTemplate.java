/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import lombok.Data;

/**
 * @author fangzhibin
 * @version $Id$
 */
@Data
@TableName(value = "ec_print_template",autoResultMap = true)
public class PrintTemplate extends LogicBasePO {
    private static final long serialVersionUID = -7436616994105114860L;
    @TableId
    private String code;
    private String template; // 模版内容

    private String description; // 描述

    private String viewCode;

    private Integer processVersion;

    private String processKey;
    private String templateName;

    private String templateCode;

    private String modelCode;

    private Integer isPublish;

    private String templateRemark;

    private String templateScript;
    private Boolean projFlag;

    private Boolean templateEnabled;//是否启用

    private Boolean extraParam;

    private Integer extraParamCount;

    private Integer extraPicParamCount;

    private String extraParamScript;


    public Entity getEntity() {
        return entity;
    }

    public String getViewCode() {
        return viewCode;
    }

    private Entity entity;



}
