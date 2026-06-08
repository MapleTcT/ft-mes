package com.supcon.supfusion.configuration.services.entity;

import lombok.Data;

/**
 * Copyright: Copyright (c) 2019 SUPCON
 * 
 * @ClassName: EchartsYAxis.java
 * @Description: 图表Y轴配置
 *
 * @version: v1.0.0
 * @author: huning
 * @date: 2019年1月10日 上午10:31:58
 */
@Data
public class EchartsYAxis {
	
	// @Fields name : Y轴名称（如：水量ml）
	private String name;
	// @Fields min : Y轴最小值
	private String min;
	// @Fields max : Y轴最大值
	private String max;
	// @Fields position : Y轴位置,可选['left','right']
	private String position;
	// @Fields offset : Y 轴相对于默认位置的偏移,在相同的 position 上有多个Y轴的时候有用。
	private Integer offset;
	// @Fields index : Y轴从左到右index
	private Integer index;
}
