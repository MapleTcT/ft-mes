package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.dao.entity.base.AbstractEntity;
import lombok.Data;

/**
 * @author songjiawei
 */
@Data
@TableName(value = "ec_extra_view",autoResultMap = true)
public class ExtraView extends AbstractEntity {
    private static final long serialVersionUID = 6841491452899758945L;

    private EcEnv ecEnv = EcEnv.product;

    @TableId
    private String code;

    private String config;

    private String fullConfig;
    private Boolean projFlag;

    private String viewJson;

   private String viewCode;



}