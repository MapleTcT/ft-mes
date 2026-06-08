/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.common.enums.*;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 *
 *
 * @author zhuyuyin
 * @version $Id$
 */
@Data
@TableName(value = "runtime_field",autoResultMap = true)
public class Field extends LogicBasePO {
	private static final long serialVersionUID = -8043218844705909935L;

	@TableId
	private String code;

	private EcEnv ecEnv = EcEnv.product;

	private String moduleCode;

	private String entityCode;

	@TableField(value = "field_key")
	private String key; // 编码

	private String name;// 显示名称

	private String displayName;

	private RegionType regionType;

	private String fullPropertyCode;

	private DbColumnType columnType;

	private String config;

	private FieldType showType;// 显示类型

	private ShowFormat showFormat;// 显示格式

	private String layRec;// 层级关系

    private String layerType;

	private Boolean isHidden = false;// 是否隐藏

	private String none;

	private String cellCode;// 关联单元格code

	private Boolean projFlag;

//	@TableField(exist = false)
//	private Set<Event> events = new HashSet<Event>();

	@TableField(value = "datagrid_code")
	private String dataGridCode;

    @TableField(value = "fastqueryjson_code")
	private String fastQueryJsonCode;

    @TableField(value = "advqueryjson_code")
	private String advQueryJsonCode;

	private String propertyCode;// 关联字段

	private String viewCode;// 关联视图
}
