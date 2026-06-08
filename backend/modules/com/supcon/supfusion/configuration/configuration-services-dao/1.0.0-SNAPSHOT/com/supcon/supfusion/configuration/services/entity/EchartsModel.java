package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.Transient;
import java.io.Serializable;

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
@javax.persistence.Entity
//@Table(name = EchartsModel.TABLE_NAME)
public class EchartsModel extends AbstractAuditUniqueCodeEntity implements Serializable {

	// @Fields serialVersionUID : TODO
	private static final long serialVersionUID = 1L;
	public static final String TABLE_NAME = "ec_echarts_model";
	public static final String DYNAMIC_CUSTOMSQL = "${DYNAMIC_CUSTOMSQL?}";
	// @Fields echartsCode : 图表Code
	private String echartsCode;
	// @Fields modelCode : 关联模型编码
	private String modelCode;
	// @Fields type : 显示类型,可选['bar','line','pie','gauge']
	private String type;
	// @Fields xAxis : X轴配置
	@Transient
	private EchartsXAxis xaxis;
	// @Fields xAxisStr : X轴配置Json格式
	private String xaxisStr;
	// @Fields yAxis : Y轴配置
	@Transient
	private EchartsYAxis yaxis;
	// @Fields xAxisStr : Y轴配置Json格式
	private String yaxisStr;
	// @Fields classificationColumn : 分类字段
	private String classificationColumn;
	// @FieldsvalueColumn : 值字段
	private String valueColumn;
	// @Fields seriesColumn : 系列名字字段
	private String seriesColumn;
	// @Fields isCustomConditions : 是否含有手写自定义条件
	private Boolean isCustomConditions = false;
	// @Fields customConditions : 手写自定义条件
	@Lob
	private String customConditions;
	// @Fields customConditionsConfjson : 用于打开自定义条件配置dialog
	@Lob
	private String customConditionsConfjson;
	// @Fields sql : 数据源SQL
	@Lob
	@Column(name="MODEL_SQL")
	private String sql;
	// @Fields projFlag : 是否工程期
	private Boolean projFlag;

	public static class Builder{
		private String echartsCode;
		private String modelCode;
		private String type;
		private String xaxisStr;
		private String yaxisStr;
		private EchartsXAxis xaxis;
		private EchartsYAxis yaxis;
		private String classificationColumn;
		private String valueColumn;
		private String seriesColumn;
		private Boolean isCustomConditions = false;
		private String customConditions;
		private String customConditionsConfjson;
		private String sql;
		public Builder(String echartsCode, String modelCode) {
			super();
			this.echartsCode = echartsCode;
			this.modelCode = modelCode;
		}
		public Builder type(String type) {
			this.type = type;
			return this;
		}
		public Builder xaxisStr(String xaxisStr) {
			this.xaxisStr = xaxisStr;
			return this;
		}
		public Builder yaxisStr(String yaxisStr) {
			this.yaxisStr = yaxisStr;
			return this;
		}
		public Builder xAxis(EchartsXAxis xaxis) {
			this.xaxis = xaxis;
			return this;
		}
		public Builder yAxis(EchartsYAxis yaxis) {
			this.yaxis = yaxis;
			return this;
		}
		public Builder classificationColumn(String classificationColumn) {
			this.classificationColumn = classificationColumn;
			return this;
		}
		public Builder seriesColumn(String seriesColumn) {
			this.seriesColumn = seriesColumn;
			return this;
		}
		public Builder valueColumn(String valueColumn) {
			this.valueColumn = valueColumn;
			return this;
		}
		public Builder isCustomConditions(Boolean isCustomConditions) {
			this.isCustomConditions = isCustomConditions;
			return this;
		}
		public Builder customConditions(String customConditions) {
			this.customConditions = customConditions;
			return this;
		}
		public Builder customConditionsConfjson(String customConditionsConfjson) {
			this.customConditionsConfjson = customConditionsConfjson;
			return this;
		}
		public Builder sql(String sql) {
			this.sql = sql;
			return this;
		}
		public EchartsModel builder() {
			return new EchartsModel(this);
		}
	}
	
	private EchartsModel(Builder builder) {
		this.echartsCode = builder.echartsCode;
		this.modelCode = builder.modelCode;
		this.type = builder.type;
		this.xaxisStr = builder.xaxisStr;
		this.yaxisStr = builder.yaxisStr;
		this.xaxis = builder.xaxis;
		this.yaxis = builder.yaxis;
        this.classificationColumn = builder.classificationColumn;
        this.valueColumn = builder.valueColumn;
        this.seriesColumn = builder.seriesColumn;
        this.isCustomConditions = builder.isCustomConditions;
        this.customConditions = builder.customConditions;
        this.customConditionsConfjson = builder.customConditionsConfjson;
        this.sql = builder.sql;
	}

	public EchartsModel() {}

	@Override
	protected String _getEntityName() {
		return getClass().getName();
	}

	@Override
	public String toString() {
		return "EchartsModel [echartsCode=" + getEchartsCode() + ", modelCode="
				+ getModelCode() + ", type=" + getType()
				+ ", xAxisStr=" + getXaxisStr() + ", yAxisStr="
				+ getYaxisStr() + ", classificationColumn=" + getClassificationColumn() + ", valueColumn="
				+ getValueColumn() + ", seriesColumn=" + getSeriesColumn()
				+ ", isCustomConditions=" + getIsCustomConditions()
				+ ", customConditions=" + getCustomConditions()
				+ ", customConditionsConfjson=" + getCustomConditionsConfjson() + "]";
	}
}
