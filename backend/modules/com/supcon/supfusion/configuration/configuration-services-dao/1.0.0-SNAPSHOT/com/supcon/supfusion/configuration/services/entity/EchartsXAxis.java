package com.supcon.supfusion.configuration.services.entity;


import lombok.Data;

/**
 * Copyright: Copyright (c) 2019 SUPCON
 * 
 * @ClassName: EchartsXAxis.java
 * @Description: X轴配置
 *
 * @version: v1.0.0
 * @author: huning
 * @date: 2019年1月10日 上午10:48:25
 */
@Data
public class EchartsXAxis {

	// @Fields xAxisName : X轴名称（如：月份）
	private String name;
	// @Fields position : X轴位置,可选['top', 'bottom']
	private String position;
	// @Fields offset : X 轴相对于默认位置的偏移，在相同的 position 上有多个 X 轴的时候有用
	private int offset;
	
}
