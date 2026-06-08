package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import lombok.Data;

/**
 * Copyright: Copyright (c) 2019 SUPCON
 * 
 * @ClassName: EchartsModel.java
 * @Description: 图表数据源配置
 *
 * @version: v1.0.0
 * @author: huning
 * @date: 2019年1月10日 上午10:31:08
 */
@Data
@TableName(value = "runtime_echarts_model",autoResultMap = true)
public class EchartsModel extends LogicBasePO {

	// @Fields serialVersionUID : TODO
	private static final long serialVersionUID = 1L;
	public static final String DYNAMIC_CUSTOMSQL = "${DYNAMIC_CUSTOMSQL?}";

	@TableId
	private String code;

	private String echartsCode;

	private String modelCode;

	private String type;

	private String xaxisStr;
//	private EchartsYAxis yaxis;

	private String yaxisStr;

	private String classificationColumn;

	private String valueColumn;

	private String seriesColumn;

	private Boolean isCustomConditions = false;

	private String customConditions;

	private String customConditionsConfjson;

	@TableField(value = "MODEL_SQL")
	private String sql;

	private Boolean projFlag;
}
