package com.supcon.supfusion.configuration.services.entity;

import lombok.Data;

import java.util.List;

/**
 * Copyright: Copyright (c) 2019 SUPCON
 * 
 * @ClassName: EchartsSeries.java
 * @Description: 图表系列配置
 *
 * @version: v1.0.0
 * @author: huning
 * @date: 2019年1月10日 上午10:31:29
 */
@Data
public class EchartsSeries {
	
	// @Fields name : 系列名称，与legendData对应
	private String name;
	// @Fields type : 显示类型,可选['bar','line','pie','gauge']
	private String type;
	private Integer yAxisIndex;
	// @Fields data : 图表数据,data.size=xAxisData.size
	private List<String> data;
	

}
